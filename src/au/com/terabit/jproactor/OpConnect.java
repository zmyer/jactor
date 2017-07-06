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

/**
 * The <tt>connect</tt> operation implementation.
 *
 * @author <a href="mailto:libman@terabit.comg">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class OpConnect extends IOOperation {
    //远程连接地址
    protected SocketAddress m_remoteAddr = null;

    /** Operation's handler */
    //异步连接处理器
    private AsynchConnectHandler m_handler = null;


    /* ---------------------------------------------------------------------- */

    // TODO: 17/5/15 by zmyer
    OpConnect(AsynchChannel channel,
        AsynchConnectHandler handler,
        SocketAddress addr) throws Exception {
        super(IOOperation.OP_CONNECT, channel);
        //设置异步连接处理器
        this.m_handler = handler;
        //设置远程连接地址
        this.m_remoteAddr = addr;
    }

    // TODO: 17/5/15 by zmyer
    protected boolean execute() {
        try {
            //开始远程连接服务
            boolean rc = m_asynchChannel.finishConnect(this);

            if (rc) {
                //连接成功
                m_state = IOOperation.STATE_FINISHED;
            }
        } catch (Exception e) {
            m_state = IOOperation.STATE_FINISHED;
            m_errorCause = e;
        }

        if (m_state == IOOperation.STATE_FINISHED) {
            //递增异步通道写入次数
            ++m_asynchChannel.m_writeInfo.m_opFinished;
            return true;
        }
        return false;

    }

    // TODO: 17/5/15 by zmyer
    public void onComplete() {
        try {
            if (m_handler != null) {
                //完成流程
                m_handler.connectCompleted(this);
            }
        } catch (Exception e) {
            System.out.println("OpConnect::onComplete: " + e.getMessage());
        }
    }

    /**
     * Tells whether connect operation on the channel has successfully completed
     * or not.
     *
     * @return <tt>true</tt> if associated asynchronous chanel is connected, <tt>false</tt>
     * otherwise
     */
    // TODO: 17/5/15 by zmyer
    public boolean isConnected() {
        return m_state == STATE_FINISHED && getError() == null;
    }
}
