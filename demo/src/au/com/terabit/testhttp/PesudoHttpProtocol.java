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
package au.com.terabit.testhttp;

import au.com.terabit.jproactor.AsynchChannel;
import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchReadHandler;
import au.com.terabit.jproactor.AsynchWriteHandler;
import au.com.terabit.jproactor.IOStatistic;
import au.com.terabit.jproactor.OpRead;
import au.com.terabit.jproactor.OpWrite;
import java.nio.ByteBuffer;

/**
 * The <code>EchoServerProtocol</code> class is a simple <tt>Echo</tt> server
 * that demostrates usage and abilities of <i>JavaProactor</i> pattern.
 *
 * @author <a href="libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */
class PesudoHttpProtocol
    implements AsynchReadHandler, AsynchWriteHandler, AsynchChannelHandler {

    /** Default buffer size - 16 KB */
    private final static int BUFFER_SIZE = 4096;  //*4;  // 1 << 14;

    ByteBuffer buffer1 = ByteBuffer.allocate(BUFFER_SIZE);
    //ByteBuffer buffer2  = ByteBuffer.allocateDirect( BUFFER_SIZE );

    AsynchChannel achannel = null;
    PseudoHttpProtocolFactory factory = null;

    /**
     * Creates a server instance for the given socket channel and multiplexor.
     *
     * @param m - <code>Multiplexor</code> instance to use as IO processor.
     * @param channel - protocol's socket channel
     */
    PesudoHttpProtocol(PseudoHttpProtocolFactory f) {
        this.factory = f;
    }

    /**
     * Start this protocol state machine.
     * <p>
     * On accept of a new connectio a new protocol instance is created,
     * then <code>Acceptor</code> instance executes this method.
     *
     * @see ProtocolAdaptor#start()
     */

    public void channelAttached(AsynchChannel channel) throws Exception {
        achannel = channel;
        //System.out.println( Thread.currentThread().getName() + ": EchoServer protocol started" );

        achannel.read(buffer1, this);
    }

    public void channelClosed(AsynchChannel channel) throws Exception {
        IOStatistic rdInfo = achannel.m_readInfo;
        IOStatistic wrInfo = achannel.m_writeInfo;

        System.out.printf("Server Connection Closed : Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
            rdInfo.m_xferBytes,
            rdInfo.m_opStarted,
            rdInfo.m_opFinished,
            rdInfo.m_opCancelled,
            wrInfo.m_xferBytes,
            wrInfo.m_opStarted,
            wrInfo.m_opFinished,
            wrInfo.m_opCancelled);
        factory.onProtocolFinished(rdInfo, wrInfo);

    }

    /**
     * Notifies that read operation was completed.
     *
     * @param opRead - read operation
     * @see IOHandler#onReadFinished(OpRead)
     */
    public void readCompleted(OpRead opRead) throws Exception {
        if (opRead.getError() != null) {
            //System.out.println( "EchoServer::readCompleted: " +
            //                 opRead.getError().toString());

            achannel.close();
            return;
        }

        if (opRead.getBytesCompleted() <= 0) {
            //System.out.println( "EchoServer::readCompleted: Peer closed " +
            //                   opRead.getBytesCompleted() );
            achannel.close();
            return;
        }

        ByteBuffer inBuffer = opRead.getBuffer();
        inBuffer.flip();

        String strBodyBegin = new String
            ("<HTML><BODY BGCOLOR=\"#ffffff\">\r\n" +
                "<H1> This is demo Java Pseudo Http Server." +
                "</H1>" +
                "<H2> I am based on J5Proactor framework." +
                "</H2>" +
                "<H2> I can print input request data." +
                "</H2>" +
                "<P>=======Begin Request=========<P>");

        String strBodyEnd = new String
            ("<P>=======End Request=========<P>" +
                "</BODY></HTML>\r\n");

        int contentLength = strBodyBegin.length()
            + strBodyEnd.length()
            + inBuffer.remaining();

        // Set Response Line
        String strResponse =
            new String("HTTP/1.1 200 ok\r\n" +
                "Content-type: text/html\r\n" +
                //"Connection: close\r\n" +
                "Content-length: " + contentLength + "\r\n" +
                "Date: Thu, 2 Mar 2006 12:00:00 GMT\r\n" +
                "\r\n");

        ByteBuffer outBuffer = ByteBuffer.allocate(contentLength + strResponse.length());

        outBuffer.put(strResponse.getBytes());
        outBuffer.put(strBodyBegin.getBytes());
        outBuffer.put(inBuffer);
        outBuffer.put(strBodyEnd.getBytes());

        outBuffer.flip();
        achannel.write(outBuffer, this);
    }

    /**
     * Notifies that write operation was completed.
     *
     * @param opWrite - write operation
     * @see IOHandler#onWriteFinished(OpWrite)
     */
    public void writeCompleted(OpWrite opWrite) throws Exception {
        if (opWrite.getError() != null) {
            System.out.println("EchoServer::writeCompleted: " +
                opWrite.getError().toString());
            achannel.close();
            return;
        }

        if (opWrite.getBytesCompleted() <= 0) {
            System.out.println("EchoServer::writeCompleted: Peer closed " +
                opWrite.getBytesCompleted());
            achannel.close();
            return;
        }

        ByteBuffer buffer = opWrite.getBuffer();
        buffer.clear();

        achannel.read(buffer, this);
    }

}
