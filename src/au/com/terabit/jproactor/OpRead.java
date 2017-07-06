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
 * The <tt>read</tt> operation implementation.
 *
 * @author <a href="mailto:libman@terabit.comg">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public class OpRead extends IOOperation {

    // ------------------------------------------------------------------------
    // Instance variables

    /** data buffer */
    //数据缓冲区
    protected ByteBuffer m_buffer = null;

    /**
     * The number of bytes processed by IO operation.
     * <p> <tt>-1</tt> if error occured or channel is closed
     */
    //已经读取的字节数
    protected int m_bytesCompleted = 0;

    /**
     * This flag denotes that data needs to be read until buffer is full.
     *
     * @see java.nio.ByteBuffer#remaning()
     */
    protected boolean m_flgExactly = false;

    /** Operation's handler */
    //异步读取处理器
    private AsynchReadHandler m_handler = null;
    

    /* ---------------------------------------------------------------------- */

    /**
     * Creates read operation given specified m_asynchChannel, which has io channel,
     * data buffer where to read data.
     *
     * @param asynchChannel
     * @param buffer read buffer
     * @param exactly specifies whether operation must ensure that input buffer must be full, i.e.
     * all available space in the buffer is filled in, before operation is considered to be
     * completed
     */
    // TODO: 17/5/15 by zmyer
    OpRead(AsynchChannel achannel,
        AsynchReadHandler handler,
        ByteBuffer rdBuf,
        boolean exactly) throws Exception {
        super(IOOperation.OP_READ, achannel);

        if (rdBuf == null)
            throw new NullPointerException("ByteBuffer is null");
        if (rdBuf.remaining() == 0)
            throw new Exception("Requested 0 bytes to read");
        //设置异步读取处理器
        this.m_handler = handler;
        //设置读取缓冲区
        this.m_buffer = rdBuf;
        //设置
        this.m_flgExactly = exactly;
        //设置读操作开始状态
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
     * Gets operations' data buffer.
     *
     * @return data buffer
     */
    // TODO: 17/5/15 by zmyer
    public ByteBuffer getBuffer() {
        return m_buffer;
    }

    /**
     * Tells the difference between a number of requested and processed bytes,
     * by IO operationm, at present time.
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
            //完成读操作
            int n = m_asynchChannel.finishRead(this);

            if (n < 0) {
                //设置读完成状态
                m_state = IOOperation.STATE_FINISHED;
            } else if (n > 0) {
                //更新读取的字节数
                m_bytesCompleted += n;
                //更新异步通道对象的读取字节数
                m_asynchChannel.m_readInfo.m_xferBytes += n;

                if (!m_flgExactly || m_buffer.remaining() == 0) {
                    //设置读取操作完成
                    m_state = IOOperation.STATE_FINISHED;
                }
            }
        } catch (Exception e) {
            m_state = IOOperation.STATE_FINISHED;
            m_errorCause = e;
        }

        if (m_state == IOOperation.STATE_FINISHED) {
            //递增异步通道对象的读取次数
            ++m_asynchChannel.m_readInfo.m_opFinished;
            return true;
        }
        return false;
    }

    /**
     * Called when IO operation has completed - notifies protocol instance
     * that read operation has completed.
     *
     * @see IOOperation#onComplete()
     */
    public void onComplete() {
        try {
            //m_buffer.flip(); // prepare buffer for get operations

            if (m_handler != null) {
                m_handler.readCompleted(this);
            }
        } catch (Exception e) {
            System.out.println("OpRead::onComplete() caught an error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
