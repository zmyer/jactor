package au.com.terabit.testcmn;

import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchChannelHandlerFactory;
import au.com.terabit.jproactor.IOStatistic;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AutoClientProtocolFactory implements AsynchChannelHandlerFactory {
    public IOStatistic m_readInfo = new IOStatistic();
    public IOStatistic m_writeInfo = new IOStatistic();
    public AtomicInteger m_created = new AtomicInteger(0);
    public AtomicInteger m_finished = new AtomicInteger(0);

    long m_finishTime = 0;

    /** Decrements latch when protocol has finished */
    CountDownLatch m_latch = null;

    public AutoClientProtocolFactory() {
    }

    public void setup(int runtime, CountDownLatch latch) {
        if (runtime != 0) {
            m_finishTime = runtime * 1000 + System.currentTimeMillis();
        }
        m_latch = latch;
    }

    public AsynchChannelHandler createChannelHandler() {
        try {
            m_created.incrementAndGet();
            return new AutoClientProtocol(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void onProtocolFinished(IOStatistic readInfo, IOStatistic writeInfo) {
        m_finished.incrementAndGet();
        m_readInfo.append(readInfo);
        m_writeInfo.append(writeInfo);

        if (m_latch != null) {
            m_latch.countDown();
        }
    }

    public void printStats() {
        System.out.printf
            ("Total Auto Clients (%9$d,%10$d): Reads=%1$d(%2$d,%3$d,%4$d) Writes=%5$d(%6$d,%7$d,%8$d)\n",
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
