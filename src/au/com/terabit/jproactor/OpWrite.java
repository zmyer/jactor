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

import java.nio.ByteBuffer;

/**
 * The <tt>write</tt> operation implementation.
 *
 * @author <a href="mailto:libman@terabit.comg">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class OpWrite extends IOOperation {

    /** data buffer */
    //数据缓冲区
    protected ByteBuffer m_buffer = null;

    /**
     * The number of bytes processed by IO operation.
     * <p> <tt>-1</tt> if error occured or channel is closed
     */
    //已经写入的字节数
    protected int m_bytesCompleted = 0;

    /**
     * This flag denotes that data needs to be written until buffer is empty.
     *
     * @see java.nio.ByteBuffer#remaning()
     */
    //
    protected boolean m_flgExactly = false;

    /** Operation's handler */
    //异步写入处理器
    private AsynchWriteHandler m_handler = null;

    
    /* ---------------------------------------------------------------------- */

    /**
     * Creates write operation given specified m_asynchChannel, which has io channel,
     * data buffer to be written.
     *
     * @param m_asynchChannel
     * @param m_buffer write buffer
     * @param exactly specifies whether operation must ensure that all data are written before it's
     * considered to be completed
     */
    // TODO: 17/5/15 by zmyer
    OpWrite(AsynchChannel achannel,
        AsynchWriteHandler handler,
        ByteBuffer wrBuf,
        boolean exactly) throws Exception {
        super(IOOperation.OP_WRITE, achannel);

        if (wrBuf == null)
            throw new NullPointerException("ByteBuffer is null");
        if (wrBuf.remaining() == 0)
            throw new Exception("Requested 0 bytes to write");

        this.m_handler = handler;
        this.m_buffer = wrBuf;
        this.m_flgExactly = exactly;
        this.m_state = IOOperation.STATE_STARTED;
    }

    /**
     * @return a number of bytes available after io operation was completed; <tt>-1</tt> if
     * exception or end-of-channel happened
     */
    // TODO: 17/5/15 by zmyer
    public int getBytesCompleted() {
        return m_bytesCompleted;
    }

    /**
     * Returns operations' data buffer.
     *
     * @return data buffer
     */
    // TODO: 17/5/15 by zmyer
    public ByteBuffer getBuffer() {
        return m_buffer;
    }

    /**
     * Tells the difference between a number of requested and processed bytes
     * at present time.
     * <p>Example 1: Requested to read <i>n</i> bytes, actually read was
     * <i>m</i>, so bytesRemaining is <i>n-m</i>
     * <p>Example 2: Requested to write <i>n</i> bytes, actually written was
     * <i>m</i>, so bytesRemaining is <i>n-m</i>
     *
     * @see java.nio.ByteBuffer#remaning()
     */
    // TODO: 17/5/15 by zmyer
    public int remaining() {
        return m_buffer.remaining();
    }

    // TODO: 17/5/15 by zmyer
    protected boolean execute() {
        try {
            //开始写入数据
            int n = m_asynchChannel.finishWrite(this);

            if (n < 0) {
                m_state = IOOperation.STATE_FINISHED;
            } else if (n > 0) {
                m_bytesCompleted += n;
                m_asynchChannel.m_writeInfo.m_xferBytes += n;

                if (!m_flgExactly || m_buffer.remaining() == 0) {
                    m_state = IOOperation.STATE_FINISHED;
                }
            }
        } catch (Exception e) {
            m_state = IOOperation.STATE_FINISHED;
            m_errorCause = e;
        }

        if (m_state == IOOperation.STATE_FINISHED) {
            ++m_asynchChannel.m_writeInfo.m_opFinished;
            return true;
        }
        return false;

    }

    /**
     * Called when IO operation has completed - notifies protocol instance
     * that write operation has completed.
     *
     * @see IOOperation#onComplete()
     */

    // TODO: 17/5/15 by zmyer
    public void onComplete() {
        try {
            if (m_handler != null) {
                m_handler.writeCompleted(this);
            }
        } catch (Exception e) {
            System.out.println("OpWrite::onComplete:" + e.getMessage());
        }
    }

}
