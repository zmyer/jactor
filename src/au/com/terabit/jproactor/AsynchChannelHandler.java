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

/**
 * The <code>AsynchHandler</code> provides gateway into <i>JavaProactor</i>.
 * Any class that would like to take advantage of <i>JavaProactor</i> needs to
 * implemet this interface.
 * <p>
 * The implementing classes usually will need to define protocol as a some kind
 * m_state machine. That m_state machine will interact with <code>Demultiplexor</code>
 * by implementing <code>AsynchHandler</code> interface.
 * <p>
 * Copyright &copy; 2003 Terabit Pty Ltd. All rights reserved.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 * @see au.com.terabit.multiplexor.EchoServerProtocol
 */

// TODO: 17/5/15 by zmyer
public interface AsynchChannelHandler {

    /** Start's protocol m_state machine */
    //public void start(AsynchChannel channel) throws Exception;

    /**
     * Notifies that this handler is associated with the <code>TCPAsynchChannel</code>
     * instance.
     * <p>One <code>AsynchHandler</code> can be shared with multiple socket
     * channels, e.g. chat server.
     * This mehtod is called by <code>TCPAsynchChannel</code>.
     *
     * @param channel a new channel that is associated with this protocol.
     */
    public void channelAttached(AsynchChannel channel) throws Exception;

    /**
     * Notifies that cnannel is closed.
     *
     * @param channel asynchronous channel that was closed
     */
    public void channelClosed(AsynchChannel channel) throws Exception;

    /** Notifies that IO error happened    */
    //public void onError( Throwable cause );

}
