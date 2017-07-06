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

//import java.io.IOException;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

//import java.net.UnknownHostException;

/**
 * @author Alexander Libman
 * @author Yevgeny Libman
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class Connector implements AsynchConnectHandler {
    /**
     * Procotol factory assigned for this socket address
     */
    //异步通道处理器工厂对象
    AsynchChannelHandlerFactory m_factory = null;

    /**
     * Demultiplexor instance
     */
    //多路复用对象
    Demultiplexor m_multiplexor = null;

    // ------------------------------------------------------------------------
    // Public methods

    /**
     */
    // TODO: 17/5/15 by zmyer
    public Connector(Demultiplexor m, AsynchChannelHandlerFactory factory) {
        this.m_factory = factory;
        this.m_multiplexor = m;

    }

    //
    // Interface AsynchConnectHandler implementation
    //

    /**
     * Implementation of {@link AsynchChannelHandler#acceptCompleted(OpAccept op)}.
     * Executes accept operation, non-blocking, instantiates protocol on the
     * accepted channel, starts protocol using acceptor's multiplexor instance.
     *
     * @throws Exception if this method is called
     */

    // TODO: 17/5/15 by zmyer
    public void connectCompleted(OpConnect op) throws Exception {
        //读取异步通道对象
        AsynchChannel asynchChannel = op.getChannel();

        if (op.getError() != null) {
            System.out.println(op.getError().toString());
            //如果连接有异常,则直接关闭该通道对象
            asynchChannel.close();
            return;

        }
        // now handle new connection
        //创建异步通道处理器对象
        AsynchChannelHandler protocol = m_factory.createChannelHandler();
        //设置异步通道对象的事件处理器
        asynchChannel.setChannelHandler(protocol);
    }

    /**
     *
     */
    // TODO: 17/5/15 by zmyer
    public void start(SocketAddress address) throws Exception {
        TCPAsynchChannel achannel = new TCPAsynchChannel(m_multiplexor, SocketChannel.open());
        achannel.connect(address, this);
    }

}