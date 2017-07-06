/*
 *********************************************************************
 * $Id$
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
package au.com.terabit.testhttp;

import au.com.terabit.jproactor.Acceptor;
import au.com.terabit.jproactor.Demultiplexor;
import au.com.terabit.ssl.SSLAsynchChannelFactory;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * A simple <code>TestServer</code> that starts <code>EchoServerProtocol<code>.
 * <p>
 * This class is an example of how to use <i>Java Proator</i> classes to
 * start <code>ProtocolHandler</code> instance.
 * <p>
 * Usage:
 * <p>au.com.terabit.echoserver.TestServer [listen port number] [thread pool size]
 * Copyright &copy; 2003 Terabit Pty Ltd. All rights reserved.
 *
 * @author <a href="mailto:libman@terabit.com.au">Yevgeny Libman</a>
 * @version <code>$Revision$ $Date$</code>
 */
public class TestSSL {

    /**
     *
     */
    int m_listenPort = 12345;
    String m_listenHost = new String("localhost");

    int m_listenPortSSL = 12346;
    String m_listenHostSSL = new String("localhost");

    int m_threads = 1;
    long m_actualRunTime = 0;

    Demultiplexor m_demultiplexor = new Demultiplexor();

    SSLContext m_sslContext = SSLContext.getInstance("TLS");

    PseudoHttpProtocolFactory m_srvFactory =
        new PseudoHttpProtocolFactory();

    SSLAsynchChannelFactory m_sslFactory =
        new SSLAsynchChannelFactory(false, m_sslContext, m_srvFactory);

    public TestSSL() throws Exception {
    }

    public void initSSL(String ksFile,
        String tsFile,
        String password) throws Exception {
        char[] passphrase = password.toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");

        ks.load(new FileInputStream(ksFile), passphrase);
        ts.load(new FileInputStream(tsFile), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ts);

        // m_sslContext = SSLContext.getInstance("TLS");

        m_sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    }

    public void startAcceptors() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(m_listenHost, m_listenPort);

        System.out.println("Start Acceptor on " + addr);
        Acceptor acceptor = new Acceptor(m_demultiplexor, addr, m_srvFactory);
        acceptor.start(10); //  start 10 pending accepts in queue

        addr = new InetSocketAddress(m_listenHostSSL, m_listenPortSSL);
        System.out.println("Start Acceptor on " + addr);
        Acceptor acceptorSSL = new Acceptor(m_demultiplexor, addr, m_sslFactory);
        acceptorSSL.start(10); //  start 10 pending accepts in queue
    }

    public void run() throws Exception {
        System.out.println("Demultiplexor thread pool size is " + m_threads);
        m_demultiplexor.start(m_threads);

        long startTime = System.currentTimeMillis();

        startAcceptors();

        System.out.println("Press any key to stop ==>");
        System.in.read();
        m_demultiplexor.shutdown();
        long endTime = System.currentTimeMillis();

        m_actualRunTime = endTime - startTime;
        printStatistics();
        System.out.println("TestMpx finished");
    }

    public void printStatistics() {
        m_srvFactory.printStats();

        long rBytes = m_srvFactory.m_readInfo.m_xferBytes;
        long rOps = m_srvFactory.m_readInfo.m_opFinished;

        long wBytes = m_srvFactory.m_writeInfo.m_xferBytes;

        long wOps = m_srvFactory.m_writeInfo.m_opFinished;

        System.out.printf
            ("Total : Reads=%1$d(%2$d) Writes=%3$d(%4$d)\n",
                rBytes,
                rOps,
                wBytes,
                wOps);

        double time = m_actualRunTime / 1000;
        double avgBytes = (rBytes + wBytes) / time;
        double avgOps = (rOps + wOps) / time;

        System.out.printf
            ("RunTime = %1$f Threads=%2$d\n",
                time,
                m_threads);

        System.out.printf
            ("Throughtput Bytes/sec=%1$f Op/sec=%2$f\n",
                avgBytes,
                avgOps);

    }

    public void parseArgs(String[] argv) throws Exception {
        int argc = argv.length;

        for (int i = 0; i < argc; i++) {
            if (argv[i].compareTo("--threads") == 0 ||
                argv[i].compareTo("-t") == 0) {
                m_threads = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--port_listen") == 0 ||
                argv[i].compareTo("-pl") == 0) {
                m_listenPort = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--ssl_host_listen") == 0 ||
                argv[i].compareTo("-hl") == 0) {
                m_listenHost = argv[++i];
            } else if (argv[i].compareTo("--ssl_port_listen") == 0 ||
                argv[i].compareTo("-pc") == 0) {
                m_listenPortSSL = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--ssl_host_listen") == 0 ||
                argv[i].compareTo("-hc") == 0) {
                m_listenHostSSL = argv[++i];
            } else {
                usage();
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        TestSSL test = new TestSSL();
        test.parseArgs(argv);
        test.initSSL("./.keystore", "./.truststore", "password");
        test.run();
    }

    private static void usage() {
        System.out.println("Usage: " + TestSSL.class.getName() +
            "\n [--threads      number]" +
            "\n [--port_listen  port]" +
            "\n [--host_listen  host]" +
            "\n [--ssl_port_listen  port]" +
            "\n [--ssl_host_listen  host]" +
            "\n"
        );
        System.exit(-1);
    }

}
