/*
 *********************************************************************
 * Created on 6/01/2006
 *
 * Copyright (C) 2003-06 Terabit Pty Ltd.  All rights reserved.
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

/**
 * This class provides implementation of the timer operations.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public final class OpTimer {

    /** Timer state values */
    //定时器状态
    public enum State {
        CREATED, QUEUED, RUNNING, CANCELLED, EXPIRED
    }

    /**
     * Timer expiration time: number of milliseconds since 1 Jan 1970 00:00:00 GMT
     */
    //定时器超时时间
    public final long m_expiryTime;

    /** Timer's handler instance to dispatch timer expiry event. */
    //异步定时器处理器
    final private AsynchTimerHandler m_handler;

    ///** IOOperation for which this timee was created, optional */
    //private IOOperation m_operation = null;

    /** Operation's state */
    private State m_state = State.CREATED;

    private volatile Object attachment = null;

    // ========================================================================
    //

    /**
     * @param handler the timer notification receiver
     * @param delay time delay from now when to expire the timer, milliseconds
     */
    // TODO: 17/5/15 by zmyer
    public OpTimer(long delay, AsynchTimerHandler handler) {
        this.m_handler = handler;
        this.m_expiryTime = System.currentTimeMillis() + delay;
    }

    /**
     * Called when timer has expired - notifies protocol instance.
     *
     * @see IOOperation#onComplete()
     */

    // TODO: 17/5/15 by zmyer
    public void onComplete() {
        //????
        if (m_state == State.CANCELLED)
            return;
        //设置定时器运行状态
        m_state = State.RUNNING;
        try {
            if (m_handler != null) {
                //开始处理超时流程
                m_handler.timerExpired(this);
            }
        } catch (Exception e) {
            System.out.println("OpTimer::onComplete: " + e.getMessage());
        }
        //设置定时器超时状态
        m_state = State.EXPIRED;
    }

    /** Cancels this timer */
    // TODO: 17/5/15 by zmyer
    public void cancel() {
        m_state = State.CANCELLED;
    }

    // TODO: 17/5/15 by zmyer
    public boolean isCancelled() {
        return m_state == State.CANCELLED;
    }

    // TODO: 17/5/15 by zmyer
    void setState(OpTimer.State state) {
        m_state = state;
    }

    /**
     * Attaches an object to this operation.
     * <p>
     * Attachments can be useful when executig {@link #onComplete()} operation
     * and be retrieved using {@link #attachment()} operation.
     *
     * @param value object to be attached, can be a <tt>null</tt> value
     * @return The previously-attached object, if any, otherwise <tt>null</tt>
     */
    // TODO: 17/5/15 by zmyer
    public final Object attach(Object value) {
        Object old = this.attachment;
        this.attachment = value;

        return old;
    }

    /**
     * Returns currently attached object.
     *
     * @return attachement
     */
    // TODO: 17/5/15 by zmyer
    public final Object attachment() {
        return this.attachment;
    }

}
