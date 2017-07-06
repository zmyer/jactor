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

//import java.nio.channels.SelectableChannel;

import java.nio.channels.SelectionKey;

/**
 * The <code>IOOperation</code> class defines commonality shared between all IO
 * operations in the <i>JavaProactor</i>.
 * <p>
 * When operation can be executed in non-blocking manner <code>TCPAsynchChannel</code>
 * calls {@link IOoperation#execute()} method to do IO operation. Once operation
 * is executed multiplexor might query ask whether this operation is completed
 * so the next operation, {@link IOOperation#nextPossible()}, if any, will be
 * executed while <code>TCPAsynchChannel</code> is active for the channel.
 *
 * @author Alexander Libman
 * @author Yevgeny Libman
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public abstract class IOOperation {

    /** Accept operation key */
    //接受连接操作键
    final static int OP_ACCEPT = SelectionKey.OP_ACCEPT;
    /** Connect operation key */
    //连接操作键
    final static int OP_CONNECT = SelectionKey.OP_CONNECT;
    /** Read operation key */
    //读取操作键
    final static int OP_READ = SelectionKey.OP_READ;
    /** Write operation key */
    //写入操作键
    final static int OP_WRITE = SelectionKey.OP_WRITE;
    //操作键集合
    final static int OP_ALL = OP_ACCEPT | OP_CONNECT | OP_READ | OP_WRITE;

    /** Pseudo operation - request to cancel all operations */
    //final static int OP_CANCEL  = -1;

    /** IO operation is free, initial m_state */
    //io操作起始状态
    final static int STATE_FREE = 1;
    /** IO operation has been queued for execution */
    //io操作排队状态
    final static int STATE_QUEUED = 2;
    /** IO operation has been started */
    //io操作开始状态
    final static int STATE_STARTED = 3;
    /** IO operation has been cancelled */
    //io操作取消状态
    final static int STATE_CANCELED = 4;
    /** IO operation has finished */
    //io操作完成状态
    final static int STATE_FINISHED = 5;

    ///////////////////////////////////////////////////////////////////////////
    //
    // Instance variables

    /**
     * This operation m_type, one of the following values:<p>
     * <tt>OP_ACCEPT</tt>, <tt>OP_READ</tt>, <tt>OP_WRITE</tt> or <tt>OP_CONNECT</tt>
     */
    int m_type = 0;

    /** State of IO operation */
    int m_state = STATE_FREE;

    /** Operations' error cause, if any */
    protected Exception m_errorCause = null;

    /** Operations' m_asynchChannel once operation is completed. */
    //异步通道对象
    protected AsynchChannel m_asynchChannel = null;

    ///////////////////////////////////////////////////////////////////////////
    //
    // Public methods

    /**
     * Creates IOOperation instance.
     *
     * @param m_type IO operation m_type, where m_type is one of the following: {@link #OP_ACCEPT},
     * {@link #OP_READ}, {@link #OP_WRITE} or {@link #OP_CONNECT}
     * @param m_asynchChannel <code>TCPAsynchChannel</code> instance
     */
    // TODO: 17/5/15 by zmyer
    protected IOOperation(int type, AsynchChannel handler) {
        this.m_type = type;
        this.m_asynchChannel = handler;
    }

    //执行函数
    abstract protected boolean execute();

    /**
     * Dispatch completed IO operation.
     * <p>Usually results of completed IO operation are passed up to
     * the protocol m_asynchChannel.
     */
    //完成函数
    abstract void onComplete();

    /**
     * Returns <code>AsycnhChannel</code> of this operation.
     *
     * @return <code>SelectableChannel</code>instance associated with this operation
     * @see TCPAsynchChannel#getChannel()
     */
    // TODO: 17/5/15 by zmyer
    public AsynchChannel getChannel() {
        return m_asynchChannel;
    }

    /**
     * Returns operation's m_state
     *
     * @return one of the following states: {@link #STATE_FREE}, {@link #STATE_QUEUED}, {@link
     * #STATE_STARTED}, {@link #STATE_FINISHED} or {@link #STATE_CANCELED}
     */
    // TODO: 17/5/15 by zmyer
    public int getState() {
        return m_state;
    }

    /**
     * Returns last error during IO operation
     *
     * @return last error or <tt>null</tt> otherwise
     */
    // TODO: 17/5/15 by zmyer
    public Exception getError() {
        return m_errorCause;
    }

    // TODO: 17/5/15 by zmyer
    void setCancelled(Exception e) {
        m_state = STATE_CANCELED;
        m_errorCause = e;
        switch (m_type) {
            case OP_READ:
            case OP_ACCEPT:
                ++m_asynchChannel.m_readInfo.m_opCancelled;
                break;
            case OP_WRITE:
            case OP_CONNECT:
                ++m_asynchChannel.m_writeInfo.m_opCancelled;
                break;
        }
    }

}
