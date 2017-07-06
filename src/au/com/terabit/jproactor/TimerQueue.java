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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * The <code>TimerQueue</code> class contains an ordered list of the
 * <code>OpTimer</code> instances.
 * <p>
 * Timer instances are ordered by the the
 * {@link au.com.terabit.jproactor.OpTimer#m_expiryTime} with lowest
 * expiry time in the front.
 *
 * @author Alexander Libman
 * @author Yevgeny Libman
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
final class TimerQueue {

    //定时器队列
    private LinkedList<OpTimer> m_queue = new LinkedList<OpTimer>();

    // TODO: 17/5/15 by zmyer
    public TimerQueue() {
    }

    /**
     * Adds specifed timer to the queue.
     *
     * @param timer timer opretion to put into queue
     * @return <tt>true</tt> if this timer operation is first in the queue, <tt>false</tt> otherwise
     */

    // TODO: 17/5/15 by zmyer
    public boolean add(OpTimer timer) {
        int i = 0;

        // Insert OpTimer into the queue in the order they will expire.
        // If two timers expire at the same time, put the newer entry
        // later so they expire in the order they came in.
        for (OpTimer t : m_queue) {
            if (timer.m_expiryTime < t.m_expiryTime) {
                break;
            }
            i++;
        }
        m_queue.add(i, timer);
        timer.setState(OpTimer.State.QUEUED);

        // if it's a first element in the queue return true
        if (i == 0)
            return true;
        else
            return false;
    }

    /**
     * Requested by demultiplexor to return timers that expired for execution.
     * <p>
     * If a timer operation was cancelled by that time it will be discarded.
     *
     * @return collection of timers that expired by now.
     */

    // TODO: 17/5/15 by zmyer
    Collection<OpTimer> getExpiredTimers() {
        LinkedList<OpTimer> timers = new LinkedList<OpTimer>();
        long now = System.currentTimeMillis();

        for (Iterator<OpTimer> i = m_queue.iterator(); i.hasNext(); ) {
            OpTimer t = i.next();

            if (t.m_expiryTime <= now) {
                i.remove();

                if (!t.isCancelled())
                    timers.addLast(t);
            }
        }
        return timers;
    }

    /**
     * Return the delay, in milliseconds, from now to the time of the first
     * (head) timer in the queue.
     *
     * @return <li>Positive value - delay from now to the expiry time of the first timer in the
     * queue <li>Value of 0 denotes that queue is empty <li>Negative value denotes that elements
     * expiry time has passed.
     */

    // TODO: 17/5/15 by zmyer
    public long getFirstDelay() {
        if (m_queue.isEmpty())
            return 0;

        return m_queue.getFirst().m_expiryTime - System.currentTimeMillis();
    }
}
