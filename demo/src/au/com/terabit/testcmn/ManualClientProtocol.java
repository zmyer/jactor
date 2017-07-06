/*
 *********************************************************************
 * Created on 08.02.2004
 *
 * Copyright (C) 2003 Terabit Pty Ltd.  All rights reserved.
 *
 * This file may be distributed and used only under the terms of the  
 * Terabit Public License as defined by Terabit Pty Ltd of Australia   
 * and appearing in the file tlicense.txt included in the packaging of
 * this module and available at http://www.terabit.com.au/license.php.
 *
 * Contact support@terabit.com.au for any information
 **********************************************************************
 */
package au.com.terabit.testcmn;

import au.com.terabit.jproactor.AsynchChannel;
import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchChannelHandlerFactory;
import au.com.terabit.jproactor.AsynchReadHandler;
import au.com.terabit.jproactor.AsynchWriteHandler;
import au.com.terabit.jproactor.IOStatistic;
import au.com.terabit.jproactor.OpRead;
import au.com.terabit.jproactor.OpWrite;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Debug version of simple client protocol.
 * <p>
 * All IO operations are controlled by <code>TestClient</code>.
 *
 * @author <a href="mailto:libman@terabit.com.au">Alexander Libman</a>
 * @version <code>$Revision$ $Date$</code>
 * @see au.com.terabit.echoserver.EchoProtocol
 */
public class ManualClientProtocol
    implements AsynchChannelHandlerFactory,
    AsynchReadHandler,
    AsynchWriteHandler,
    AsynchChannelHandler

{
    /** Default buffer size - 16 KB */
    private final static int BUFFER_SIZE = 4096;  //*4;  // 1 << 14;

    ByteBuffer rdBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    AsynchChannel achannel = null;

    int traceLevel = 0;

    public IOStatistic m_readInfo = new IOStatistic();
    public IOStatistic m_writeInfo = new IOStatistic();
    public AtomicInteger m_created = new AtomicInteger(0);
    public AtomicInteger m_finished = new AtomicInteger(0);

    public ManualClientProtocol() {
    }

    public void setTraceLevel(int level) {
        traceLevel = level;
    }

    //
    // Interface AsynchChannelHandlerFactory
    //  AsynchChannelHandler and Factory itself
    //
    public AsynchChannelHandler createChannelHandler() {
        m_created.incrementAndGet();
        return this;
    }

    //
    // Interface AsynchChannelHandler
    //
    // TODO: 17/5/15 by zmyer
    public void channelClosed(AsynchChannel channel) throws Exception {
        IOStatistic rdInfo = achannel.m_readInfo;
        IOStatistic wrInfo = achannel.m_writeInfo;
        m_finished.incrementAndGet();

        System.out.printf("Manual Client Closed : Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
            rdInfo.m_xferBytes,
            rdInfo.m_opStarted,
            rdInfo.m_opFinished,
            rdInfo.m_opCancelled,
            wrInfo.m_xferBytes,
            wrInfo.m_opStarted,
            wrInfo.m_opFinished,
            wrInfo.m_opCancelled);

        m_readInfo.append(rdInfo);
        m_writeInfo.append(wrInfo);

    }

    public void printStats() {
        System.out.printf
            ("Total Manual Clients (%9$d,%10$d): Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
                m_readInfo.m_xferBytes,
                m_readInfo.m_opStarted,
                m_readInfo.m_opFinished,
                m_readInfo.m_opCancelled,
                m_writeInfo.m_xferBytes,
                m_writeInfo.m_opStarted,
                m_writeInfo.m_opFinished,
                m_writeInfo.m_opCancelled,
                m_created.intValue(),
                m_finished.intValue());

    }

    public void channelAttached(AsynchChannel channel) throws Exception {
        achannel = channel;

        rdBuffer.clear();
        achannel.read(rdBuffer, this);
    }

    /* (non-Javadoc)
     * @see IOHandler#onReadFinished(OpRead)
     */
    public void readCompleted(OpRead opRead) throws Exception {

        //System.out.println("Read finished: " + total_op_r);

        if (opRead.getError() != null) {
            System.out.println("Client::readCompleted: " +
                opRead.getError().toString());

            achannel.close();
            return;
        }

        if (opRead.getBytesCompleted() <= 0) {
            System.out.println("Client::readCompleted: Peer closed " +
                opRead.getBytesCompleted());
            achannel.close();
            return;
        }

        ByteBuffer buffer = opRead.getBuffer();

        buffer.flip();

        byte[] array = buffer.array();

        System.out.print("Recv "
            + opRead.getBytesCompleted()
            + " bytes:");

        System.out.write(array, 0, opRead.getBytesCompleted());

        // start read again
        buffer.clear();
        achannel.read(buffer, this);
    }

    /* (non-Javadoc)
     * @see IOHandler#onWriteFinished(OpWrite)
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

        buffer.flip();

        byte[] array = buffer.array();

        System.out.print("Sent "
            + opWrite.getBytesCompleted()
            + " bytes:");

        System.out.write(array, 0, opWrite.getBytesCompleted());

        buffer = null;
    }

    public void write(byte[] data, int off, int len) throws Exception {
        if (data == null || len == 0)
            return;

        ByteBuffer buffer = ByteBuffer.allocate(len);

        buffer.put(data, off, len);
        buffer.flip();

        achannel.write_n(buffer, this);
    }

    public void write(String str) throws Exception {
        if (str == null || str.length() == 0)
            return;

        ByteBuffer buffer = ByteBuffer.allocate(str.length() * 2);

        CharBuffer chBuf = buffer.asCharBuffer();
        chBuf.put(str);

        achannel.write_n(buffer, this);
    }

    public void write(char ch) throws Exception {

        ByteBuffer buffer = ByteBuffer.allocate(2);

        buffer.putChar(ch);
        buffer.flip();

        achannel.write_n(buffer, this);
    }

    public void close() {
        if (achannel != null)
            achannel.close();
    }
}
