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

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The <code>TCPAsynchChannel</code> is an intermidiate interface that serves as
 * mediator between <code>Demultiplexor</code> and <code>AsynchHandler</code>
 * <p>
 * All implemented protocols should be based on this interface.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 * @see IOOperation
 */

// TODO: 17/5/15 by zmyer
final public class TCPAsynchChannel extends AsynchChannel {

    /* ---------------------------------------------------------------------- */
    //
    // instance variables
    private boolean m_flgOnce =
        (Demultiplexor.dispatchStategy_ == Demultiplexor.STRATEGY_BALANCE);

    /** multiplexor reference */
    private Demultiplexor m_demultiplexor = null;

    /** m_asynchChannel's channel */
    private SelectableChannel m_selChannel = null;

    /** dispatch m_state can be DISP_NONE, DISP_QUEUED, DISP_ACTIVE */
    private AtomicBoolean m_dispatchActive =
        new AtomicBoolean(false);

    /** ready events as reported by selector */
    private int m_readyEvents = 0;

    /** event mask that is currently active in selector */
    private int m_remainEvents = 0;

    /** new mask that should be activated in selector after dispatch operation */
    private int m_interestEvents = 0;
    
    

    /* ---------------------------------------------------------------------- */

    /**
     * Creates <code>TCPAsynchChannel</code> instance given specified
     * <code>Demultiplexor</code>, <code>AsynchHandler</code> and
     * <code>SelectableChannel</code> instances.
     *
     * @param m <code>Demultiplexor</code> instance this m_asynchChannel works with
     * @param p <code>AsynchHandler</code> protocol instance
     * @param sc <code>SelectableChannel</code> instance this m_asynchChannel operates on
     */

    // TODO: 17/5/15 by zmyer
    public TCPAsynchChannel(Demultiplexor m, SelectableChannel sc) {
        //super (handler);
        super();
        //设置多路复用对象
        this.m_demultiplexor = m;
        //设置通道对象
        this.m_selChannel = sc;
        //将该通道对象插入到多路复用器中
        m_demultiplexor.declareInterest(this);
    }

    /**
     * Completion dispather for this m_asynchChannel.
     * This method is called from Demultiplexor thread pool when
     * m_asynchChannel has completed operations.
     *
     * It extacts all completioned operations from the completion queue
     * assosiated with this {@link #m_asynchChannel} and performs  upcalls
     * to the associated ProtocolHandlers
     */

    // TODO: 17/5/15 by zmyer
    final void dispatchEvents(int readyEvents, int remainEvents) {
        boolean flgInterest = false;
        //加锁
        m_lock.lock();
        //设置通道激活状态
        boolean activeFlg = m_dispatchActive.compareAndSet(false, true);
        try {
            //设置已经就绪的事件类型
            m_readyEvents |= readyEvents;
            //设置剩下未就绪的事件类型
            m_remainEvents = remainEvents;
            //从感兴趣的事件类型中剔除掉已经收到的事件类型
            m_interestEvents &= ~readyEvents;

            //如果通道对象未激活,则直接退回
            if (!activeFlg)
                return;

            // execute each IO operation only once then update interested events
            // mask it's possible that during IOOperation::onComplete() new
            // operations will be added
            for (; ; ) {
                //IO操作对象
                IOOperation op = null;

                if ((m_readyEvents & (SelectionKey.OP_ACCEPT | SelectionKey.OP_READ)) != 0) {
                    //开始读处理事件
                    op = executeListAndDispatch(m_readQue);
                    if (op == null) {
                        //从感兴趣的事件中剔除掉接收事件和读事件
                        m_interestEvents &= ~(SelectionKey.OP_ACCEPT | SelectionKey.OP_READ);
                    } else {
                        //设置感兴趣的事件类型
                        m_interestEvents |= op.m_type;
                        //从就绪的事件类型中剔除掉感兴趣的事件类型
                        m_readyEvents &= ~op.m_type;
                    }
                }

                if ((m_readyEvents & (SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE)) != 0) {
                    //开始处理写类型事件
                    op = executeListAndDispatch(m_writeQue);
                    if (op == null) {
                        //清理事件
                        m_interestEvents &= ~(SelectionKey.OP_CONNECT | SelectionKey.OP_WRITE);
                    } else {
                        //设置感兴趣的事件类型
                        m_interestEvents |= op.m_type;
                        //剔除掉感兴趣事件
                        m_readyEvents &= ~op.m_type;
                    }
                }

                //如果感兴趣事件类型中没有准备就绪的事件,则直接退出
                if (m_flgOnce || (m_interestEvents & m_readyEvents) == 0) {
                    break;
                }
            }

            //检查通道是否关闭
            if (!checkForClose()) {
                if (m_interestEvents != 0 &&
                    m_interestEvents != m_remainEvents) // YL
                {
                    //需要重新注册该通道对象,因为感兴趣的事件与剩余的事件类型不一致,
                    //这说明事件类型有变动,需要重新注册
                    flgInterest = true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            m_lock.unlock();
        }

        //设置通道非激活状态
        m_dispatchActive.set(false);

        if (flgInterest) {
            //重新在多路复用对象中注册该通道对象
            m_demultiplexor.declareInterest(this);
        }
    }

    // TODO: 17/5/15 by zmyer
    protected int finishRead(OpRead op) throws Exception {
        //System.out.println( Thread.currentThread().getName() + ": Executing Read IO" );
        //转换套接字通道对象
        SocketChannel channel = (SocketChannel) m_selChannel;
        //开始从通道对象中读取数据
        return channel.read(op.getBuffer());
    }

    /**
     * Writes data into associated channel, when channel is ready for
     * non-blocking write.
     *
     * @see IOOperation#execute()
     */

    // TODO: 17/5/15 by zmyer
    protected int finishWrite(OpWrite op) throws Exception {
        //System.out.println( Thread.currentThread().getName() + ": Executing Write IO" );

        SocketChannel channel = (SocketChannel) m_selChannel;
        return channel.write(op.getBuffer());
    }

    // TODO: 17/5/15 by zmyer
    protected AsynchChannel finishAccept(OpAccept op) throws Exception {
        //System.out.println( Thread.currentThread().getName() + ": Executing Accept IO" );

        // get server socket channel from m_asynchChannel
        //服务套接字通道对象
        ServerSocketChannel server = (ServerSocketChannel) m_selChannel;
        //开始接受新的连接
        SocketChannel acceptedChannel = server.accept();

        // check that accept actually has happened 
        if (acceptedChannel != null) {
            //设置地址复用标记
            acceptedChannel.socket().setReuseAddress(true);

            //创建新的异步通道对象
            TCPAsynchChannel asynchChannel =
                new TCPAsynchChannel(m_demultiplexor, acceptedChannel);

            //返回异步通道对象
            return asynchChannel;
        }

        return null;
    }

    // TODO: 17/5/15 by zmyer
    protected boolean finishConnect(OpConnect op) throws Exception {
        //System.out.println( Thread.currentThread().getName() + ": Executing Read IO" );
        //套接字通道对象
        SocketChannel channel = (SocketChannel) m_selChannel;
        //通道连接完成
        if (channel.finishConnect()) {
            //设置通道连接成功标记
            setConnected();
            return true;
        }
        return false;
    }

    public boolean startClose() {
        m_demultiplexor.declareInterest(this);
        return true;
    }

    // TODO: 17/5/15 by zmyer
    protected void startRead(OpRead op) throws Exception {
        //设置感兴趣的事件类型
        boolean flgInterest = setInterest(op.m_type);
        if (flgInterest) {
            //将该通道对象插入到多路复用器中
            m_demultiplexor.declareInterest(this);
        }
    }

    // TODO: 17/5/15 by zmyer
    protected void startWrite(OpWrite op) throws Exception {
        boolean flgInterest = setInterest(op.m_type);
        if (flgInterest) {
            m_demultiplexor.declareInterest(this);
        }
    }

    // TODO: 17/5/15 by zmyer
    protected void startAccept(OpAccept op) throws Exception {
        boolean flgInterest = setInterest(op.m_type);
        if (flgInterest) {
            m_demultiplexor.declareInterest(this);
        }
    }

    // TODO: 17/5/15 by zmyer
    protected void startConnect(OpConnect op) throws Exception {
        //转换套接字通道对象
        SocketChannel channel = (SocketChannel) m_selChannel;
        //设置非阻塞标记
        channel.configureBlocking(false);

        int mask = op.m_type;

        //开始连接
        if (channel.connect(op.m_remoteAddr)) {
            //设置操作对象为完成状态
            op.m_state = IOOperation.STATE_FINISHED;
            //设置连接完成状态
            this.setConnected();
            //增加可写事件
            mask |= SelectionKey.OP_WRITE;
        } else {
            //设置操起开始状态
            op.m_state = IOOperation.STATE_STARTED;
        }

        //开始为通道对象设置感兴趣的事件类型
        boolean flgInterest = setInterest(mask);
        if (flgInterest) {
            //重新即将该通道对象插入到多路复用对象中
            m_demultiplexor.declareInterest(this);
        }
    }

    // TODO: 17/5/15 by zmyer
    private boolean setInterest(int opType) {
        //设置通道感兴趣的事件类型
        m_interestEvents |= opType;
        //如果遗留的事件类型中已经有该事件,则直接退出
        if ((m_remainEvents & opType) != 0)
            return false;

        //如果当前的通道对象已经激活了,则直接退出
        if (m_dispatchActive.get())
            return false;
        // need to wake up selector
        return true;
    }

    /**
     * Schedules timeout action to be called back on the associated
     * <code>AsynchHandler</code> instance.
     * <p>
     * Note: while IO operations will be delivered to the handler in a
     * serialized way, timer notifications might be delivered concurrently.
     *
     * @param delay delay in milliseconds before timeout action is to be executed.
     * @return OpTimer operation - future result
     */

    // TODO: 17/5/15 by zmyer
    public void startTimer(OpTimer op) {
        m_demultiplexor.startTimer(op);
    }

    // -----------------------------------------------------------------------
    //
    // Package visible methods

    /**
     * Returns Selectable channel associated
     * with this Asynch channel.
     */
    SelectableChannel getSelectableChannel() {
        return m_selChannel;
    }

    /**
     * updateInterest is called from Demultiplexor
     * in LEADER_STATE, i.e. with locked leader lock
     *
     * @param selector to declare/cancel m_asynchChannel interest
     * @return true  - if interesrt updated false - failure, m_Channel is closed and must be
     * dispatched
     */

    // TODO: 17/5/15 by zmyer
    boolean updateInterest(Selector selector) {
        boolean rc = false;
        m_lock.lock();
        try {
            if (isOpened()) {
                //设置感兴趣的事件类型
                m_remainEvents |= m_interestEvents;
                //清理之前的感兴趣事件
                m_interestEvents = 0;
                //根据选择器查找对应的键值
                SelectionKey key = m_selChannel.keyFor(selector);

                if (key == null) {
                    //如果键值为空,则重新设置通道对象非阻塞标记
                    m_selChannel.configureBlocking(false);
                    //将该通道对象注册到选择器中
                    key = m_selChannel.register(selector, m_remainEvents, this);
                } else {
                    //设置感兴趣的事件类型
                    key.interestOps(m_remainEvents);
                }
                rc = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            rc = false;
        } finally {
            if (!rc) {
                //关闭通道对象
                closeInternal();
            }
            //解锁
            m_lock.unlock();
        }
        return rc;
    }

    boolean onSelectorClosed(Selector selector) {
        m_lock.lock();
        try {
            setClosing();
            closeInternal();
        } finally {
            m_lock.unlock();
        }
        return true;
    }

    private void closeInternal() {
        // this will automatically cancel all keys
        try {
            if (m_selChannel != null) {
                m_selChannel.close();
            }
        } catch (Exception xc) {
            xc.printStackTrace();
        } finally {
            m_selChannel = null;
            m_remainEvents = IOOperation.OP_ALL;
            m_interestEvents = 0;
        }
    }

}