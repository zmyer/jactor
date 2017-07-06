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
 * The <code>AsynchHandlerFactory</code> interface is responsible for creation of
 * a new instance of {@link ProtocolAdaptor} class.
 *
 * @author <a href="libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */

// TODO: 17/5/15 by zmyer
public interface AsynchChannelHandlerFactory {
    /**
     * Creates protocol instance for the given channel.
     *
     * @return a new <code>AsynchHandler</code> instance
     */
    public AsynchChannelHandler createChannelHandler();
}
