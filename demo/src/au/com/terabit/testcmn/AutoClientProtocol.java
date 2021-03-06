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
package au.com.terabit.testcmn;

import au.com.terabit.jproactor.AsynchChannel;
import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchReadHandler;
import au.com.terabit.jproactor.AsynchTimerHandler;
import au.com.terabit.jproactor.AsynchWriteHandler;
import au.com.terabit.jproactor.IOStatistic;
import au.com.terabit.jproactor.OpRead;
import au.com.terabit.jproactor.OpTimer;
import au.com.terabit.jproactor.OpWrite;
import java.nio.ByteBuffer;

//import java.nio.CharBuffer;

/**
 * A simple client protocol implementation.
 * <p>
 * This client simple connects to the server, i.e. <i>Echo</i> server,
 * sends data and then requests read operation.
 *
 * @author <a href="mailto:libman@terabit.com.au">Alexander Libman</a>
 * @version <code>$Revision$ $Date$</code>
 * @see au.com.terabit.echoserver.EchoProtocol
 */
public class AutoClientProtocol
    implements AsynchReadHandler,
    AsynchWriteHandler,
    AsynchTimerHandler,
    AsynchChannelHandler {
    /** Default buffer size - 16 KB */
    private final static int BUFFER_SIZE = 4096;  //*4;  // 1 << 14;

    ByteBuffer buffer1 = ByteBuffer.allocateDirect(BUFFER_SIZE);

    AsynchChannel achannel = null;
    AutoClientProtocolFactory factory = null;

    int traceLevel = 0;

    AutoClientProtocol(AutoClientProtocolFactory f) {
        this.factory = f;

    }

    void setTraceLevel(int level) {
        traceLevel = level;
    }

    //
    // Interface AsynchChannelHanlder
    //

    public void channelAttached(AsynchChannel channel) throws Exception {
        achannel = channel;

        if (factory.m_finishTime != 0) {
            long runTime = factory.m_finishTime - System.currentTimeMillis();
            if (runTime < 0) {
                runTime = 0;
            }
            achannel.schedulerTimer(runTime, this);
        }

        buffer1.clear();

        String demoStr = "123456789ABCDEF\n";

        byte[] array = demoStr.getBytes();

        buffer1.put(array);
        buffer1.position(0);
        buffer1.limit(1024);

        // buffer1.position (chBuf.position()*2);
        // buffer1.flip();

        achannel.write_n(buffer1, this);
    }

    public void channelClosed(AsynchChannel channel) throws Exception {
        IOStatistic rdInfo = achannel.m_readInfo;
        IOStatistic wrInfo = achannel.m_writeInfo;

        System.out.printf("Auto Client Connection Closed : Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
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

    /* (non-Javadoc)
     * @see TCPAsynchChannel#onReadFinished(OpRead)
     */
    public void readCompleted(OpRead opRead) throws Exception {
        //System.out.println("Read finished: " + total_op_r);

        if (opRead.getError() != null) {
            System.out.println("Client::readCompleted: " +
                opRead.getError().toString());

            opRead.getError().printStackTrace();

            achannel.close();
            return;
        }

        if (opRead.getBytesCompleted() <= 0) {
            //System.out.println( "Client::readCompleted: Peer closed " +
            //					opRead.getBytesCompleted() );
            achannel.close();
            return;
        }

        ByteBuffer buffer = opRead.getBuffer();

        buffer.flip();

        if (traceLevel > 0) {
            byte[] array = buffer.array();

            System.out.print("Recv "
                + opRead.getBytesCompleted()
                + " bytes:");

            System.out.write(array, 0, opRead.getBytesCompleted());
        }

        achannel.write_n(buffer, this);
    }

    /* (non-Javadoc)
     * @see
     */
    public void writeCompleted(OpWrite opWrite) throws Exception {

        //System.out.println("Write finished: " + total_op_w);

        if (opWrite.getError() != null) {
            System.out.println("Client::writeCompleted: " +
                opWrite.getError().toString());

            achannel.close();
            return;
        }

        if (opWrite.getBytesCompleted() <= 0) {
            System.out.println("Client::writeCompleted: Peer closed " +
                opWrite.getBytesCompleted());
            achannel.close();
            return;
        }

        ByteBuffer buffer = opWrite.getBuffer();

        if (traceLevel > 0) {
            buffer.flip();

            byte[] array = buffer.array();

            System.out.print("Sent "
                + opWrite.getBytesCompleted()
                + " bytes:");

            System.out.write(array, 0, opWrite.getBytesCompleted());

        }

        buffer.clear();
        achannel.read(buffer, this);
    }

    public void timerExpired(OpTimer opTimer) throws Exception {
        // stop protocol
        achannel.close();
    }

}
