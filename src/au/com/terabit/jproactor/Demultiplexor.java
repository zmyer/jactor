/*
 *********************************************************************
 * Created on ${date}
 *
 * Copyright (C) 2003 Terabit Pty Ltd.  All rights reserved.
 *
 * This file may be distributed and used only under the terms of the  
 * Terabit Public License as defined by Terabit Pty Ltd of Australia   
 * and appearing in the file tlicense.txt included in the packaging of
 * this module and available at http://www.terabit.com.au/license.php.
 *
 * Contact support@terabit.com.au for any information
 *********************************************************************
 */

package au.com.terabit.jproactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

//import java.nio.channels.SelectableChannel;

/**
 * The <code>Demultiplexor</code> class is the core engine of the <i>JavaProactor</i>.
 * <p>
 * Demultiplexor acts as IO processor and  manages all IO operations requested
 * by {@link TCPAsynchChannel} class. The IO oprations are proccessed asynchrously by
 * the pool of threads using leader/follower pattern. Number of processing
 * thread can be configured by {@link Demultiplexor#start(int)} operation.
 * <p>
 * Usually client applications does not require direct access to mutiplexor
 * class, only when multiple protocols would like to reuse the same multiplexor
 * instance.
 * <p>
 * Each multiplexor instance instantiates one {@link java.nio.channels.Selector}
 * instance.
 *
 * @author Alexander Libman
 * @author Yevgeny Libman
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class Demultiplexor implements Runnable {

    // ----------------------------------------------------------------------
    // Static section

    /** Default Thread Pool size */
    //默认线程池大小
    public static final int POOL_SIZE = 5;

    // ----------------------------------------------------------------------
    // Instance variables】
    //事件分发类型,快分发与均衡分发
    public static final int STRATEGY_FAST = 0;
    public static final int STRATEGY_BALANCE = 1;

    static public int dispatchStategy_ = STRATEGY_FAST;

    /** Selector - should work in separate thread or in the user thread */
    //IO选择器
    private Selector selector_ = null;

    /** Iterator for set of ready keys */
    //就绪key集合迭代器
    Iterator<SelectionKey> itrKey_ = null;

    /** Iterator of the set of expired timers from the timer Queue */
    //超时定时器集合迭代器
    Iterator<OpTimer> itrTimer_ = null;

    /** Stop flag */
    //停止标记
    private AtomicBoolean stopFlag_ = new AtomicBoolean(false);

    /** Requests Queue - collection of IOHandlers */
    //请求队列,IO处理器队列
    private ConcurrentLinkedQueue<TCPAsynchChannel> requestQue_ =
        new ConcurrentLinkedQueue<TCPAsynchChannel>();

    /** total number threads */
    //线程总数
    private int numThreads1_ = 0;
    /** number of threads runnning event loop */
    //事件循环中的线程总数
    private int numThreads2_ = 0;
    //锁对象
    private Object lockMultiplexor_ = new Object();

    /** number threads doing user upcalls */
    //事件分发器线程数目
    private AtomicInteger numDispatchers_ = new AtomicInteger(0);

    /** number threads waiting for job */
    //等待任务的线程数量
    private AtomicInteger numFollowers_ = new AtomicInteger(0);

    /** number handlers ready for dispatch job */
    //
    private int numReadyHandlers_ = 0;

    //事件分发主线程
    /** leader thread */
    private AtomicReference<DemultiplexorThread> leader_ =
        new AtomicReference<DemultiplexorThread>(null);

    /** lock to obtain leadership - to avoid conditional variable */
    //重入锁
    private ReentrantLock leaderLock_ = new ReentrantLock();

    //定时器队列
    private TimerQueue timerQue_ = new TimerQueue();

    // TODO: 17/5/15 by zmyer
    private class DemultiplexorThread implements Runnable {

        public static final int ST_ZERO = 0;
        public static final int ST_FOLLOWER = 1;
        public static final int ST_LEADER = 2;
        public static final int ST_HANDLER = 4;
        public static final int ST_TIMER = 8;
        public static final int ST_STOP = 16;

        int m_state = ST_ZERO;
        TCPAsynchChannel m_readyChannel = null;
        int m_readyEvents = 0;
        int m_remainEvents = 0;
        OpTimer m_timer = null;

        // ----------------------------------------------------------------------
        // Runnable Interface

        // TODO: 17/5/15 by zmyer
        public void run() {
            for (; m_state != ST_STOP; ) {
                //变更多路复用器状态
                m_state = ST_FOLLOWER;
                //递增目前等待事件的线程数目
                numFollowers_.incrementAndGet();

                //加锁
                leaderLock_.lock();
                try {
                    // Leader -only one thread at moment
                    // can execute this block

                    //if (leader.compareAndSet(null, this))
                    //设置当前线程为主线程
                    leader_.set(this);
                    //更新状态
                    m_state = ST_LEADER;
                    //递减等待事件线程数目
                    numFollowers_.decrementAndGet();
                    //获取待分发事件状态
                    m_state = getEventForDispatch();
                } finally {
                    //撤销主线程
                    leader_.set(null);
                    //解锁
                    leaderLock_.unlock();
                }

                if (m_state == ST_HANDLER) {
                    //递增事件分发线程数目
                    numDispatchers_.incrementAndGet();
                    //开始分发就绪事件
                    m_readyChannel.dispatchEvents(m_readyEvents, m_remainEvents);
                    //递减事件分发线程数目
                    numDispatchers_.decrementAndGet();
                    //重置准备就绪的通道对象
                    m_readyChannel = null;
                } else if (m_state == ST_TIMER) {
                    //定时器完成事件处理
                    m_timer.onComplete();
                    //重置定时器
                    m_timer = null;
                }
            }
        }

        // TODO: 17/5/15 by zmyer
        private int getEventForDispatch() {
            while (!stopFlag_.get()) {
                // if there are no current operations to execute then wait on select
                if (itrKey_ == null && itrTimer_ == null) {
                    if (!updateInterestSet()) {
                        // m_readyChannel contains cancelled m_asynchChannel
                        return ST_HANDLER;
                    }

                    waitOnSelect();
                    numReadyHandlers_ = selector_.selectedKeys().size();
                    itrKey_ = selector_.selectedKeys().iterator();
                    itrTimer_ = timerQue_.getExpiredTimers().iterator();
                }

                if (itrKey_ != null && itrKey_.hasNext()) {
                    //读取选择键值
                    SelectionKey key = itrKey_.next();

                    // retrieve associated TCPAsynchChannel instance
                    // and ready events mask
                    //从选择键值中读取就绪的通道对象
                    m_readyChannel = (TCPAsynchChannel) key.attachment();
                    //读取准备就绪的事件类型
                    m_readyEvents = key.readyOps();
                    // remove ready events from interest mask.
                    //从感兴趣的事件类型中剔除掉已经就绪的事件
                    m_remainEvents = key.interestOps() & ~m_readyEvents;

                    // this call won't block as there is no active select at this point
                    //更新选择键值下一次感兴趣的事件类型
                    key.interestOps(m_remainEvents);

                    //删除就绪的键值
                    itrKey_.remove();
                    --numReadyHandlers_;
                    //返回
                    return ST_HANDLER;
                }
                //重置键值迭代器
                itrKey_ = null;

                // check expired timers iterator
                //检查超时定时器
                if (itrTimer_ != null && itrTimer_.hasNext()) {
                    m_timer = itrTimer_.next();
                    itrTimer_.remove();
                    return ST_TIMER;
                }
                itrTimer_ = null;
            }
            return ST_STOP;
        }

        // TODO: 17/5/15 by zmyer
        private boolean updateInterestSet() {
            //异步通道对象
            TCPAsynchChannel achannel = null;

            try {
                //从请求队列中读取异步通道对象
                while ((achannel = requestQue_.poll()) != null) {
                    //更新通道对象感兴趣的事件类型
                    if (!achannel.updateInterest(selector_)) {
                        m_readyChannel = achannel;
                        m_remainEvents = 0;
                        m_readyEvents = IOOperation.OP_ALL;
                        return false;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        // TODO: 17/5/15 by zmyer
        private int waitOnSelect() {
            int rc = 0;
            //从定时器队列中读取最近一次延时
            long delay = timerQue_.getFirstDelay();

            if (delay < 0) {
                System.out.println("Internal error: waitOnSelect() - negative delay");
                // return immediately to process expired timers
                return rc;
            }
            try {
                //开始执行select操作
                rc = selector_.select(delay);

                if (rc == 0) {
                    //System.out.println ("Select returned 0");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return rc;
        }

    } // DemultiplexorThread 

    // ----------------------------------------------------------------------
    // Public methods

    /**
     * Creates multipliexor instance with default pool size.
     *
     * @throws IOException
     */
    // TODO: 17/5/15 by zmyer
    public Demultiplexor() throws IOException {
        selector_ = SelectorProvider.provider().openSelector();
    }

    /**
     * Start multiplexor.
     *
     * @param poolSize thread pool size
     */
    // TODO: 17/5/15 by zmyer
    public void start(int poolSize) {
        synchronized (lockMultiplexor_) {
            for (int i = 0; i < poolSize && !stopFlag_.get(); ++i) {
                new Thread(this).start();
                ++numThreads1_;
            }
        }
    }

    /**
     * Start multiplexor using default thread pool size.
     *
     * @see Demultiplexor#POOL_SIZE
     */
    // TODO: 17/5/15 by zmyer
    public void start() {
        start(POOL_SIZE);
    }

    // ----------------------------------------------------------------------
    // Runnable Interface

    // TODO: 17/5/15 by zmyer
    public void run() {

        synchronized (lockMultiplexor_) {
            //统计事件线程数量
            ++numThreads2_;

            System.out.println("Enter Demultiplexor::run thread="
                + Thread.currentThread().getName()
                + " total=" + numThreads2_);
        }

        //创建多路复用线程对象
        DemultiplexorThread thread = new DemultiplexorThread();

        //启动线程
        thread.run();

        synchronized (lockMultiplexor_) {
            --numThreads1_;
            --numThreads2_;
            System.out.println("Leave Demultiplexor::run thread="
                + Thread.currentThread().getName()
                + " total=" + numThreads2_);

            lockMultiplexor_.notifyAll();
        }
    }

    /**
     * Register m_asynchChannel.
     */
    // TODO: 17/5/15 by zmyer
    public void declareInterest(TCPAsynchChannel achannel) {
        //将通道对象插入到队列中
        requestQue_.add(achannel);
        if (leader_.get() != null) {
            //唤醒selector对象
            selector_.wakeup();
        }
    }

    /**
     * Demultiplexor shutdown. Stops all IO operations.
     */
    // TODO: 17/5/15 by zmyer
    public void shutdown() throws InterruptedException {
        stopFlag_.set(true);
        selector_.wakeup();

        // wait for all threads
        synchronized (lockMultiplexor_) {
            while (numThreads1_ > 0) {
                try {
                    lockMultiplexor_.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            TCPAsynchChannel achannel;

            itrKey_ = selector_.keys().iterator();
            while (itrKey_.hasNext()) {
                SelectionKey key = (SelectionKey) itrKey_.next();

                // retrieve associated TCPAsynchChannel instance
                // and ready events mask
                achannel = (TCPAsynchChannel) key.attachment();
                achannel.onSelectorClosed(selector_);
                achannel.dispatchEvents(IOOperation.OP_ALL, 0);
            }

            while ((achannel = requestQue_.poll()) != null) {
                achannel.onSelectorClosed(selector_);
                achannel.dispatchEvents(IOOperation.OP_ALL, 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // close the Selector
            selector_.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Schedules timer operation.
     *
     * @param timer operation
     */
    // TODO: 17/5/15 by zmyer
    public void startTimer(final OpTimer timer) {
        if (timerQue_.add(timer)) {
            if (leader_.get() != null) {
                selector_.wakeup();
            }
        }
    }
}
