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

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The <code>AsynchChannel</code> is an intermidiate interface that serves as
 * mediator between <code>Demultiplexor</code> and <code>AsynchHandler</code>
 * <p>
 * All implemented protocols should be based on this interface.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 * @see IOOperation
 */

// TODO: 17/5/15 by zmyer
public abstract class AsynchChannel {

    /** Channel states */
    //通道状态
    public enum State {
        OPENED, CONNECTED, CLOSING, CLOSED
    }


    /* ---------------------------------------------------------------------- */
    //
    // instance variables

    /** Internal lock for synchronization */
    //重入锁
    protected ReentrantLock m_lock = new ReentrantLock();

    /** This channel's handler, protocol instance */
    //异步通道处理器
    private AtomicReference<AsynchChannelHandler> m_channelHandler =
        new AtomicReference<AsynchChannelHandler>(null);

    /** channelState : OPENED, CONNECTED, CLOSING, CLOSED */
    //通道状态
    protected State m_channelState = State.OPENED;

    /** queue for the requested read operations */
    //读取操作队列
    protected ConcurrentLinkedQueue<IOOperation> m_readQue =
        new ConcurrentLinkedQueue<IOOperation>();

    /** queue for the requested write operations */
    //写操作队列
    protected ConcurrentLinkedQueue<IOOperation> m_writeQue =
        new ConcurrentLinkedQueue<IOOperation>();

    /** queue for the completed write operations */
    //protected ConcurrentLinkedQueue<IOOperation> m_completedQue = 
    //    new ConcurrentLinkedQueue<IOOperation>();
        //读操作信息
    public IOStatistic m_readInfo = new IOStatistic();
    //写操作信息
    public IOStatistic m_writeInfo = new IOStatistic();


    /* ---------------------------------------------------------------------- */

    /**
     * Creates <code>AsynchChannel</code> instance given specified
     * <code>Demultiplexor</code>, <code>AsynchHandler</code> and
     * <code>SelectableChannel</code> instances.
     *
     * @param m <code>Demultiplexor</code> instance this m_asynchChannel works with
     * @param p <code>AsynchHandler</code> protocol instance
     * @param sc <code>SelectableChannel</code> instance this m_asynchChannel operates on
     */
    // TODO: 17/5/15 by zmyer
    public AsynchChannel() {
    }

    // TODO: 17/5/15 by zmyer
    public AsynchChannelHandler setChannelHandler(AsynchChannelHandler handler) {
        AsynchChannelHandler h = m_channelHandler.getAndSet(handler);
        try {
            //将通道对象注册到异步通道处理器对象中
            handler.channelAttached(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return h;
    }

    /**
     * Returns AsynchCloseHandler instance for this AsynchChannel
     *
     * @return m_closeHandler
     */
    // TODO: 17/5/15 by zmyer
    public AsynchChannelHandler getChannelHandler() {
        return m_channelHandler.get();
    }

    // TODO: 17/5/15 by zmyer
    protected abstract void startRead(OpRead op) throws Exception;

    // TODO: 17/5/15 by zmyer
    protected abstract void startWrite(OpWrite op) throws Exception;

    // TODO: 17/5/15 by zmyer
    protected abstract void startAccept(OpAccept op) throws Exception;

    // TODO: 17/5/15 by zmyer
    protected abstract void startConnect(OpConnect op) throws Exception;

    // TODO: 17/5/15 by zmyer
    public abstract void startTimer(OpTimer op) throws Exception;

    // TODO: 17/5/15 by zmyer
    protected abstract boolean startClose();

    /**
     * this method is should be called by derived implemnations
     * with locked m_lock
     *
     * @return true  if channel is just or already closed false otherwise
     */

    // TODO: 17/5/15 by zmyer
    protected boolean checkForClose() {
        AsynchChannelHandler handler = null;
        switch (m_channelState) {
            case CLOSING:
                m_channelState = State.CLOSED;
                handler = m_channelHandler.getAndSet(null);
                break;
            case CLOSED:
                return true;
            default:
                return false;
        }

        if (handler != null) {
            m_lock.unlock();
            try {
                //关闭通道对象
                handler.channelClosed(this);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                m_lock.lock();
            }
        }
        return true;
    }

    /**
     * @param op
     * @return > 0 number bytes read 0   not finished yet < 0 errors or end of data
     * @throws Exception
     */
    // TODO: 17/5/15 by zmyer
    protected abstract int finishRead(OpRead op) throws Exception;

    /**
     * @param op
     * @return > 0 number bytes written 0   not finished yet < 0 errors or peer closed
     * @throws Exception
     */
    // TODO: 17/5/15 by zmyer
    protected abstract int finishWrite(OpWrite op) throws Exception;

    /**
     * @param op
     * @return new accepted AsynchChannel null not finished yet
     * @throws Exception
     */
    // TODO: 17/5/15 by zmyer
    protected abstract AsynchChannel finishAccept(OpAccept op) throws Exception;

    /**
     * @param op
     * @return true if connect finished false if not may be to fix: 1  finished OK, 0  not finished
     * yet -1 finished with errors
     * @throws Exception
     */
    // TODO: 17/5/15 by zmyer
    protected abstract boolean finishConnect(OpConnect op) throws Exception;

    /**
     * @param completedQue
     */
    // TODO: 17/5/15 by zmyer
    protected boolean dispatchCompletions(ConcurrentLinkedQueue<IOOperation> completedQue) {
        boolean rc = true;
        for (; ; ) {
            // take operation from the queue 
            IOOperation op = completedQue.poll();
            if (op == null) {
                break;
            }

            try {
                op.onComplete();
            } catch (Exception e) {
                e.printStackTrace();
                rc = false;
            }
        }
        return rc;
    }

    /**
     * @param reqQue
     * @return null if all operations in reqQue are executed first operation that is still in reqQue
     * and was not executed
     */

    // TODO: 17/5/15 by zmyer
    protected IOOperation executeListAndDispatch(ConcurrentLinkedQueue<IOOperation> reqQue) {
        IOOperation op = null;
        boolean flgCancel = isClosed();

        //从请求队列中读取指定的操作对象
        while ((op = reqQue.peek()) != null) {
            //开始操作执行对象
            if (!executeOp(op, flgCancel)) {
                break;
            }

            //删除已经处理完毕的操作对象
            reqQue.poll();  // remove it
            //解锁
            m_lock.unlock();
            try {
                //触发完成事件
                op.onComplete();
            } catch (Exception e) {
                e.printStackTrace();
                flgCancel = true;
            } finally {
                //加锁
                m_lock.lock();
            }
        }

        return op;
    }

    /**
     * @param reqQue
     * @param completedQue
     * @return null if all operations in reqQue are executed first operation that is still in reqQue
     * and was not executed
     */
    // TODO: 17/5/15 by zmyer
    protected IOOperation executeListAndAdd(ConcurrentLinkedQueue<IOOperation> reqQue,
        boolean flgCancel,
        ConcurrentLinkedQueue<IOOperation> completedQue) {
        IOOperation op = null;

        for (; (op = reqQue.peek()) != null; ) {
            if (!executeOpAndAdd(op, flgCancel, completedQue)) {
                break;
            }
            reqQue.poll();  // remove it
        }

        return op;
    }

    // TODO: 17/5/15 by zmyer
    protected boolean executeOpAndAdd(IOOperation op,
        boolean flgCancel,
        ConcurrentLinkedQueue<IOOperation> completedQue) {
        if (executeOp(op, false)) {
            completedQue.add(op);
            return true;
        }
        return false;
    }

    // TODO: 17/5/15 by zmyer
    protected boolean executeOp(IOOperation op, boolean flgCancel) {

        switch (op.m_state) {
            //操作完成或者取消
            case IOOperation.STATE_FINISHED:
            case IOOperation.STATE_CANCELED:
                return true;
        }

        if (flgCancel || isClosed()) {
            op.setCancelled(null);
            return true;
        }
        //开始执行操作
        return op.execute();
    }

    // TODO: 17/5/15 by zmyer
    protected void enqueueOperation(IOOperation op,
        ConcurrentLinkedQueue<IOOperation> que,
        IOStatistic info) throws Exception {
        if (isClosed()) {
            throw new Exception(
                "AsynchChannel::startOperation: attempt to start IO on inactive channel");
        }
        //设置操作对象的状态为排队
        op.m_state = IOOperation.STATE_QUEUED;
        //将该操作对象插入到队列中
        que.add(op);
        //递增操作事件启动次数
        ++info.m_opStarted;

    }

    /**
     * Request for read operation.
     * This is main interface to start asynchronous read operation.
     *
     * @param buf buffer to read data to
     * @return OpRead operation - future result
     * @throws exception if error occurs
     */

    // TODO: 17/5/15 by zmyer
    public OpRead read(ByteBuffer buf, AsynchReadHandler handler) throws Exception {
        //将读取操作封装为opread对象
        OpRead op = new OpRead(this, handler, buf, false);

        m_lock.lock();
        try {
            //将opread对象插入到读取队列中
            enqueueOperation(op, m_readQue, m_readInfo);
            //开始读取操作
            startRead(op);
        } finally {
            m_lock.unlock();
        }
        //返回结果
        return op;
    }

    /**
     * Request to execute non-partitional read.
     * Read exactly {@link ByteBuffer#limit()} bytes to the buffer.
     *
     * @param buf
     * @return OpRead operation - future result
     * @throws exception if error occurs
     */

    // TODO: 17/5/15 by zmyer
    public OpRead read_n(ByteBuffer buf, AsynchReadHandler handler) throws Exception {
        OpRead op = new OpRead(this, handler, buf, true);

        m_lock.lock();
        try {
            enqueueOperation(op, m_readQue, m_readInfo);
            startRead(op);
        } finally {
            m_lock.unlock();
        }

        return op;
    }

    /**
     * Requets for write operation.
     * This is main interface to start asynchronous write operation.
     *
     * @param buf buffer to write data from
     * @return OpWrite operation - future result
     * @throws exception if error occurs
     */
    // TODO: 17/5/15 by zmyer
    public OpWrite write(ByteBuffer buf, AsynchWriteHandler handler) throws Exception {
        //封装为opwrite对象
        OpWrite op = new OpWrite(this, handler, buf, false);

        m_lock.lock();
        try {
            //将opwrite对象插入到队列中
            enqueueOperation(op, m_writeQue, m_writeInfo);
            //启动写入操作
            startWrite(op);
        } finally {
            m_lock.unlock();
        }

        return op;
    }

    /**
     * Request to execute non-partitional write.
     * Write exactly {@link ByteBuffer#limit()} bytes to the associated channel.
     *
     * @param buf buffer to write data from
     * @return OpWrite operation - future result
     * @throws exception if error occurs
     */
    // TODO: 17/5/15 by zmyer
    public OpWrite write_n(ByteBuffer buf, AsynchWriteHandler handler) throws Exception {
        OpWrite op = new OpWrite(this, handler, buf, true);

        m_lock.lock();
        try {
            enqueueOperation(op, m_writeQue, m_writeInfo);
            startWrite(op);
        } finally {
            m_lock.unlock();
        }

        return op;
    }

    /**
     * Request to execute accept operation.
     *
     * @return OpAccept operation - future result
     * @throws Exception if error occurs
     */
    // TODO: 17/5/15 by zmyer
    public OpAccept accept(AsynchAcceptHandler handler) throws Exception {
        //封装为opaccept对象
        OpAccept op = new OpAccept(this, handler);

        m_lock.lock();
        try {
            //将opaccept对象插入到队列中
            enqueueOperation(op, m_readQue, m_readInfo);
            //开始接受连接
            startAccept(op);
        } finally {
            m_lock.unlock();
        }
        return op;
    }

    /**
     * Request to execute connect operation.
     *
     * @param SocketAddress address to connect
     * @return OpConnect operation - future result
     * @throws Exception if error occurs
     */

    // TODO: 17/5/15 by zmyer
    public OpConnect connect(SocketAddress addr, AsynchConnectHandler handler) throws Exception {
        //创建连接事件对象
        OpConnect op = new OpConnect(this, handler, addr);

        //加锁
        m_lock.lock();
        try {
            //将该链接操作对象插入到队列中
            enqueueOperation(op, m_writeQue, m_writeInfo);
            //启动链接流程
            startConnect(op);
        } finally {
            m_lock.unlock();
        }

        return op;
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
    public OpTimer schedulerTimer(long delay, AsynchTimerHandler handler) throws Exception {
        if (delay < 0)
            throw new IllegalArgumentException("Negative timeout delay");
        //创建optimer对象
        OpTimer op = new OpTimer(delay, handler);
        //启动定时器
        startTimer(op);
        return op;
    }

    public void close() {
        m_lock.lock();
        try {
            if (setClosing()) {
                startClose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            m_lock.unlock();
        }

    }

    // TODO: 17/5/15 by zmyer
    public boolean isOpened() {
        return m_channelState == State.OPENED ||
            m_channelState == State.CONNECTED;
    }

    public boolean isConnected() {
        return m_channelState == State.CONNECTED;
    }

    // TODO: 17/5/15 by zmyer
    public boolean isClosed() {
        return m_channelState == State.CLOSING || m_channelState == State.CLOSED;
    }

    public boolean isCloseStarted() {
        return m_channelState == State.CLOSING;
    }

    public boolean isCloseFinished() {
        return m_channelState == State.CLOSED;
    }

    // -----------------------------------------------------------------------
    //
    // Package visible methods

    // TODO: 17/5/15 by zmyer
    protected final boolean setConnected() {
        switch (m_channelState) {
            case OPENED:
                //变更通道连接状态
                m_channelState = State.CONNECTED;
                return true;
        }
        return false;
    }

    protected final boolean setClosing() {
        switch (m_channelState) {
            case OPENED:
            case CONNECTED:
                m_channelState = State.CLOSING;
                return true;
        }
        return false;
    }

}
