/*
 * Created on 3/03/2006
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package au.com.terabit.ssl;

import au.com.terabit.jproactor.AsynchChannelHandler;
import au.com.terabit.jproactor.AsynchChannelHandlerFactory;
import javax.net.ssl.SSLContext;

/**
 * @author Alexander Libman
 * @author Yevgeny Libman
 *
 *         Copyright &copy; 2003 Terabit Pty Ltd. All rights reserved.
 */
public class SSLAsynchChannelFactory
    implements AsynchChannelHandlerFactory {
    private boolean m_mode = false;
    private SSLContext m_ctx = null;
    private AsynchChannelHandlerFactory m_usrFactory = null;

    public SSLAsynchChannelFactory(boolean mode,
        SSLContext ctx,
        AsynchChannelHandlerFactory usrFactory) {
        m_mode = mode;
        m_ctx = ctx;
        m_usrFactory = usrFactory;
    }

    public AsynchChannelHandler createChannelHandler() {
        return new SSLAsynchChannel(m_mode, m_ctx, m_usrFactory);
    }

}
