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

import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchChannelHandlerFactory;
import au.com.terabit.jproactor.IOStatistic;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Describe your <code>EchoServerProtocolFactory</code> class here.
 *
 * <p>Detailed description goes here.
 *
 * @author <a href="mailto:abc@xyz.org">zhenya</a>
 * @version <code>$Revision$ $Date$</code>
 */
public class EchoServerProtocolFactory implements AsynchChannelHandlerFactory {

    public IOStatistic m_readInfo = new IOStatistic();
    public IOStatistic m_writeInfo = new IOStatistic();
    public AtomicInteger m_created = new AtomicInteger(0);
    public AtomicInteger m_finished = new AtomicInteger(0);

    /**
     * Creates <code>EchoServer</code> protocol instance.
     *
     * @param m - <code>Multiplexor</code> instance to use as IO processor.
     * @param channel - protocol's socket channel
     * @see ProtocolFactory#createProtocol(java.nio.channels.SelectableChannel)
     */
    public AsynchChannelHandler createChannelHandler() {
        try {
            m_created.incrementAndGet();
            return new EchoServerProtocol(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void onProtocolFinished(IOStatistic readInfo, IOStatistic writeInfo) {
        m_finished.incrementAndGet();
        m_readInfo.append(readInfo);
        m_writeInfo.append(writeInfo);
    }

    public void printStats() {
        System.out.printf
            ("Total Servers (%9$d,%10$d): Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
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
}
