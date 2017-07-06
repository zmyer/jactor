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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
//import java.nio.channels.SocketChannel;

/**
 * The <code>Acceptor</code> class allows to start user defined server-side
 * protocol servers.
 * <br>Acceptor opens {@link java.net.ServerSocketChannel} and requests
 * {@link Demultiplexor} to execute accept operation, once accept is completed
 * registered {@link AsynchChannelHandlerFactory} instantiates a new {@link Protocol}
 * object and starts execution using the same multiplexor instance.
 * <p>
 * The following code fragment shows how to start test EchoServer using acceptor:
 * <blockquote><pre>
 * <i>// Create and run Demultiplexor instance for EchoServer on port 9999 </i>
 * Demultiplexor m = new Demultiplexor();
 * m.start();
 *
 * <i>// Create acceptor instance for EchoServer on port 9999 </i>
 * Acceptor a = new Acceptor( m, new EchoServerProtocolFactory(), 9999 );
 * <i>// start listening for incoming connections</i>
 * a.start();
 * </pre></blockquote>
 *
 * @author Alexander Libman
 * @author Yevgeny Libman
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class Acceptor implements AsynchAcceptHandler, AsynchChannelHandler {

    /** How may accepts to perform, default value */
    //默认的accept线程的数量
    public final static int NUM_INITIAL_ACCEPTS = 5;

    /** Acceptors' socket address */
    //accept套接字地址
    SocketAddress m_address = null;

    /** Server socket channel for this acceptor */
    //服务器套接字通道对象
    ServerSocketChannel m_serverChannel = null;

    /**
     * Procotol factory assigned for this socket address
     */
    //异步通道处理器工厂对象
    AsynchChannelHandlerFactory m_factory = null;

    /**
     * Demultiplexor instance
     */
    //多路复用器
    Demultiplexor m_multiplexor = null;

    /**
     * IO m_asynchChannel instance
     */
    //tcp异步通道对象
    TCPAsynchChannel m_achannel = null;

    // ------------------------------------------------------------------------
    // Public methods

    /**
     * Creates acceptor for the specified <code>AsynchHandlerFactory</code>
     * and on the specified tcp/ip port.
     *
     * @param m - multiplexor instance
     * @param factory - protocol factory
     * @param port - tcp/ip port to listen on
     */

    // TODO: 17/5/15 by zmyer
    public Acceptor(Demultiplexor m,
        int port,
        AsynchChannelHandlerFactory factory)
        throws UnknownHostException {
        this(m, new InetSocketAddress(InetAddress.getLocalHost(), port), factory);
    }

    /**
     * Creates acceptor for the specified <code>AsynchHandlerFactory</code>
     * and on the specified tcp/ip port and host.
     *
     * @param m - multiplexor instance
     * @param factory - protocol factory
     * @param port - tcp/ip port to listen on
     * @param host - host name
     */
    // TODO: 17/5/15 by zmyer
    public Acceptor(Demultiplexor m,
        int port,
        String host,
        AsynchChannelHandlerFactory factory) {
        this(m, new InetSocketAddress(host, port), factory);
    }

    /**
     * Creates acceptor for the specified <code>AsynchHandlerFactory</code>
     * and on the specified <code>Socket Address</code>.
     *
     * @param m - multiplexor instance
     * @param factory - protocol factory
     * @param address - socket address to listen on
     */

    // TODO: 17/5/15 by zmyer
    public Acceptor(Demultiplexor m,
        SocketAddress address,
        AsynchChannelHandlerFactory factory) {
        this.m_factory = factory;
        this.m_address = address;
        this.m_multiplexor = m;

        try {
            // create channel for accepting connections
            // and bind it to the specified address
            //首先创建服务器套接字通道对象
            this.m_serverChannel = ServerSocketChannel.open();
            //设置tcp性能优化参数
            this.m_serverChannel.socket().setPerformancePreferences(1, 0, 0);
            //设置可复用地址标记
            this.m_serverChannel.socket().setReuseAddress(true);
            //绑定地址
            this.m_serverChannel.socket().bind(address);

            // Create TCPAsynchChannel for accepting connections
            //创建异步通道对象
            this.m_achannel = new TCPAsynchChannel(m_multiplexor, m_serverChannel);
            //设置异步处理对象
            this.m_achannel.setChannelHandler(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    // Interface AsynchAcceptHandler implementation
    //

    /**
     * Implementation of {@link AsynchChannelHandler#acceptCompleted(OpAccept op)}.
     * Executes accept operation, non-blocking, instantiates protocol on the
     * accepted channel, starts protocol using acceptor's multiplexor instance.
     *
     * @throws Exception if this method is called
     */

    // TODO: 17/5/15 by zmyer
    public void acceptCompleted(OpAccept op) throws Exception {
        //读取异步通道对象
        AsynchChannel asynchChannel = op.getAcceptedChannel();

        if (asynchChannel != null) {
            // start accept operation again
            //开始接受新的连接
            m_achannel.accept(this);
            // now handle new connection
            //创建异步通道处理器
            AsynchChannelHandler protocol = m_factory.createChannelHandler();
            //设置异步通道处理器
            asynchChannel.setChannelHandler(protocol);
        }
    }

    //
    // Interface AsynchChannelHandler implementation
    //

    // TODO: 17/5/15 by zmyer
    public void channelAttached(AsynchChannel channel) throws Exception {
    }

    // TODO: 17/5/15 by zmyer
    public void channelClosed(AsynchChannel channel) throws Exception {
        System.out.println("Acceptor Closed");
    }

    /**
     * Starts acceptor, using {@link #NUM_INITIAL_ACCEPTS} default number of
     * accept operations.
     * <br>
     * This method can be called more than once.
     *
     * @see au.com.terabit.multiplexor.AsynchChannelHandler#start()
     * @see #start(int numInitialAccepts)
     */
    // TODO: 17/5/15 by zmyer
    public void start() throws Exception {
        start(NUM_INITIAL_ACCEPTS);
    }

    /**
     * Starts accept operation.
     * <br>
     * This method can be called more than once.
     *
     * @throws IOException if error occurs, the operation won't be started in this case.
     */
    // TODO: 17/5/15 by zmyer
    public void start(int numInitialAccepts) throws Exception {
        for (int i = 0; i < numInitialAccepts; ++i) {
            m_achannel.accept(this);
        }
    }
}