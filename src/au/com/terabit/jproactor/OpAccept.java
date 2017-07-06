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

//import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;

/**
 * Th <code>OpAccept</code> class implements <i>accept</i> IO operation.
 * <p>
 * Copyright &copy; 2003 Terabit Pty Ltd. All rights reserved.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class OpAccept extends IOOperation {

    /** Newly accepted connection instance */
    //异步通道对象
    protected AsynchChannel m_acceptedChannel = null;

    /** Operation's handler */
    //异步接收处理器
    private AsynchAcceptHandler m_handler = null;

    /**
     * Creates accept operation using specified <code>TCPAsynchChannel</code>.
     *
     * @param achannel <code>TCPAsynchChannel</code> instance for this accept operation
     */
    // TODO: 17/5/15 by zmyer
    OpAccept(AsynchChannel achannel, AsynchAcceptHandler handler) {
        super(IOOperation.OP_ACCEPT, achannel);

        //设置开始状态
        this.m_state = IOOperation.STATE_STARTED;
        //设置事件处理器
        this.m_handler = handler;
    }

    // TODO: 17/5/15 by zmyer
    protected boolean execute() {
        try {
            //接收新的连接
            m_acceptedChannel = m_asynchChannel.finishAccept(this);
            if (m_acceptedChannel != null) {
                //设置完成状态
                m_state = IOOperation.STATE_FINISHED;
            }
        } catch (Exception e) {
            //设置事件完成状态
            m_state = IOOperation.STATE_FINISHED;
            //设置错误消息
            m_errorCause = e;
        }

        if (m_state == IOOperation.STATE_FINISHED) {
            //更新异步通道对象的读取完成次数
            ++m_asynchChannel.m_readInfo.m_opFinished;
            return true;
        }
        return false;

    }

    /**
     * Dispatches accepted channel to the associated protocol so it can start
     * execution.
     *
     * @see IOOperation#onComplete()
     * @see AsynchChannelHandler#acceptCompleted(OpAccept)
     * @see
     */

    // TODO: 17/5/15 by zmyer
    public void onComplete() {
        try {
            if (m_handler != null) {
                //接受新连接完成
                m_handler.acceptCompleted(this);
            }
        } catch (Exception e) {
            System.out.println("OpAccept::onComplete:" + e.getMessage());
        }
    }

    /**
     * Returns the result of the accept operation.
     *
     * @return a newly accepted socket channel
     */
    // TODO: 17/5/15 by zmyer
    public AsynchChannel getAcceptedChannel() {
        return m_acceptedChannel;
    }

}
