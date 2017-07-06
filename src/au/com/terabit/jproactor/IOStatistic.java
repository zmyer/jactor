/*
 * Created on 22/09/2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.jproactor;

// TODO: 17/5/15 by zmyer
public class IOStatistic {
    public long m_xferBytes = 0;
    public long m_opStarted = 0;
    public long m_opFinished = 0;
    public long m_opCancelled = 0;

    public IOStatistic() {
    }

    synchronized public void append(IOStatistic other) {
        m_xferBytes += other.m_xferBytes;
        m_opStarted += other.m_opStarted;
        m_opFinished += other.m_opFinished;
        m_opCancelled += other.m_opCancelled;
    }

}
