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
package au.com.terabit.testjpssl;

import au.com.terabit.jproactor.Acceptor;
import au.com.terabit.jproactor.AsynchTimerHandler;
import au.com.terabit.jproactor.Connector;
import au.com.terabit.jproactor.Demultiplexor;
import au.com.terabit.jproactor.OpTimer;
import au.com.terabit.ssl.SSLAsynchChannelFactory;
import au.com.terabit.testcmn.AutoClientProtocolFactory;
import au.com.terabit.testcmn.EchoServerProtocolFactory;
import au.com.terabit.testcmn.ManualClientProtocol;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
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
public class TestJProactorSSL {

    /**
     *
     */
    int m_listenPort = 12345;
    String m_listenHost = new String("localhost");
    int m_connectPort = 12345;
    String m_connectHost = new String("localhost");

    /** denotes whether test shall start EchoServer */
    boolean m_serverMode = true;
    boolean m_autoClientMode = true;
    boolean m_manualClientMode = false;

    int m_threads = 1;
    int m_sessions = 1;
    int m_timeRun = 0; // in seconds
    // if set to true and test is base on time then multiplexor's timer
    // operations will be used to terminate the test
    boolean m_useTimers = false;

    CountDownLatch m_latch = null;

    long m_actualRunTime = 0;

    Demultiplexor m_demultiplexor = new Demultiplexor();

    EchoServerProtocolFactory m_srvFactory = new EchoServerProtocolFactory();
    AutoClientProtocolFactory m_clnFactory = new AutoClientProtocolFactory();
    ManualClientProtocol m_manualClient = new ManualClientProtocol();

    SSLContext m_sslContext = SSLContext.getInstance("TLS");

    SSLAsynchChannelFactory m_sslSrvFactory =
        new SSLAsynchChannelFactory(false, m_sslContext, m_srvFactory);

    SSLAsynchChannelFactory m_sslClnFactory =
        new SSLAsynchChannelFactory(true, m_sslContext, m_clnFactory);

    SSLAsynchChannelFactory m_sslManualFactory =
        new SSLAsynchChannelFactory(true, m_sslContext, m_manualClient);

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

    public TestJProactorSSL() throws Exception {
    }

    public void startAcceptor() throws Exception {
        InetSocketAddress addr =
            new InetSocketAddress(m_listenHost, m_listenPort);

        System.out.println("Start Acceptor on " + addr);
        Acceptor a = new Acceptor(m_demultiplexor, addr, m_sslSrvFactory);
        a.start(10); //  start 10 pending accepts in queue
    }

    public void startAutoClients() throws Exception {
        InetSocketAddress addr =
            new InetSocketAddress(m_connectHost, m_connectPort);

        System.out.println("Number Auto Sessions is " + m_sessions);
        System.out.println("Address to connect " + addr);

        if (m_useTimers && m_timeRun != 0) {
            m_clnFactory.setup(m_timeRun, m_latch);
        } else {
            m_clnFactory.setup(0, null);
        }

        Connector c = new Connector(m_demultiplexor, m_sslClnFactory);

        for (int i = 0; i < m_sessions; ++i) {
            c.start(addr);
        }
    }

    public void startManualClient() throws Exception {
        InetSocketAddress addr =
            new InetSocketAddress(m_connectHost, m_connectPort);

        m_manualClient.setTraceLevel(0); //  set 1 to see trace in console

        Connector c = new Connector(m_demultiplexor, m_sslManualFactory);

        c.start(addr);

        System.out.println("Manual Session");
        System.out.println("Type any data to send or 'q' to quit==>");

        byte[] inBuf = new byte[1024];

        for (; ; ) {
            int num = System.in.read(inBuf, 0, inBuf.length);

            if (num <= 0)
                break;
            if (inBuf[0] == 'q')
                break;
            if (inBuf[0] == 'c') {
                m_manualClient.close();
            } else {
                m_manualClient.write(inBuf, 0, num);
            }
        }

        m_manualClient.close();
        System.out.println("-----------Begin sleeping--------");
        Thread.sleep(2000);
        System.out.println("-----------End sleeping--------");
    }

    public void run() throws Exception {
        int latchCount = 0;

        if (m_serverMode)
            latchCount++;
        if (m_autoClientMode)
            latchCount += m_sessions;

        m_latch = new CountDownLatch(latchCount);

        System.out.println("Demultiplexor thread pool size is " + m_threads);
        m_demultiplexor.start(m_threads);

        long startTime = System.currentTimeMillis();

        if (m_serverMode)
            startAcceptor();
        if (m_autoClientMode)
            startAutoClients();

        if (m_manualClientMode) {
            startManualClient();
        } else {
            if (m_timeRun == 0) {
                System.out.println("Press any key to stop ==>");
                System.in.read();
            } else {
                startTimers();
            }
        }
        m_demultiplexor.shutdown();
        long endTime = System.currentTimeMillis();

        m_actualRunTime = endTime - startTime;
        printStatistics();
        System.out.println("TestMpx finished");
    }

    /**
     * Handles test running time and timers scenarious.
     *
     * @throws InterruptedException
     */
    private void startTimers() throws InterruptedException {
        // if we are using timers and acceptor is running then set timer
        // to terminate acceptor as well 
        if (m_useTimers && m_serverMode) {
            AsynchTimerHandler timerHandler = new AsynchTimerHandler() {
                public void timerExpired(OpTimer opTimer) throws Exception {
                    if (m_latch != null)
                        m_latch.countDown();
                }

            };
            OpTimer timer = new OpTimer(m_timeRun * 1000, timerHandler);
            m_demultiplexor.startTimer(timer);
        }
        System.out.println("Test runtime " + m_timeRun + " seconds");

        if (m_useTimers) {
            System.out.println("Using timers");
            m_latch.await();
        } else {
            Thread.sleep(m_timeRun * 1000);
        }
    }

    public void printStatistics() {
        m_manualClient.printStats();
        m_clnFactory.printStats();
        m_srvFactory.printStats();

        long rBytes = m_clnFactory.m_readInfo.m_xferBytes
            + m_srvFactory.m_readInfo.m_xferBytes
            + m_manualClient.m_readInfo.m_xferBytes;

        long rOps = m_clnFactory.m_readInfo.m_opFinished
            + m_srvFactory.m_readInfo.m_opFinished
            + m_manualClient.m_readInfo.m_opFinished;

        long wBytes = m_clnFactory.m_writeInfo.m_xferBytes
            + m_srvFactory.m_writeInfo.m_xferBytes
            + m_manualClient.m_writeInfo.m_xferBytes;

        long wOps = m_clnFactory.m_writeInfo.m_opFinished
            + m_srvFactory.m_writeInfo.m_opFinished
            + m_manualClient.m_writeInfo.m_xferBytes;

        System.out.printf
            ("Total : Reads=%1$d(%2$d) Writes=%3$d(%4$d)\n",
                rBytes,
                rOps,
                wBytes,
                wOps);

        double time = m_actualRunTime / 1000;
        double avgBytes = (rBytes + wBytes) / time;
        double avgOps = (rOps + wOps) / time;

        System.out.printf("RunTime = %1$f Threads=%2$d\n",
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
            if (argv[i].compareTo("--mode") == 0 ||
                argv[i].compareTo("-m") == 0) {
                m_serverMode = false;
                m_autoClientMode = false;
                m_manualClientMode = false;
                i++;

                for (int j = 0; j < argv[i].length(); ++j) {
                    char c = Character.toLowerCase(argv[i].charAt(j));
                    switch (c) {
                        case 's':
                            m_serverMode = true;
                            break;
                        case 'a':
                            m_autoClientMode = true;
                            break;
                        case 'm':
                            m_manualClientMode = true;
                            break;
                        default:
                            System.out.printf("Invalid paramter mode\n");
                    }
                }
            } else if (argv[i].compareTo("--threads") == 0 ||
                argv[i].compareTo("-t") == 0) {
                m_threads = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--sessions") == 0 ||
                argv[i].compareTo("-s") == 0) {
                m_sessions = Integer.parseInt(argv[++i]);

            } else if (argv[i].compareTo("--port_listen") == 0 ||
                argv[i].compareTo("-pl") == 0) {
                m_listenPort = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--port_connect") == 0 ||
                argv[i].compareTo("-pc") == 0) {
                m_connectPort = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--host_listen") == 0 ||
                argv[i].compareTo("-hl") == 0) {
                m_listenHost = argv[++i];
            } else if (argv[i].compareTo("--host_connect") == 0 ||
                argv[i].compareTo("-hc") == 0) {
                m_connectHost = argv[++i];
            } else if (argv[i].compareTo("--time") == 0 ||
                argv[i].compareTo("-i") == 0) {
                m_timeRun = Integer.parseInt(argv[++i]);
            } else if (argv[i].compareTo("--use_timers") == 0 ||
                argv[i].compareTo("-ut") == 0) {
                m_useTimers = true;
            } else {
                usage();
            }
        }
    }

    public static void main(String[] argv) throws Exception {
        TestJProactorSSL test = new TestJProactorSSL();
        test.parseArgs(argv);
        test.initSSL("./.keystore", "./.truststore", "password");
        test.run();
    }

    private static void usage() {
        System.out.println("Usage: " + TestJProactorSSL.class.getName() +
            "\n [--mode         [S][A][M]]" +
            "\n [--threads      number]" +
            "\n [--sessions     number]" +
            "\n [--time         seconds]" +
            "\n [--use_timers] " +
            "\n [--port_listen  port]" +
            "\n [--host_listen  host]" +
            "\n [--port_connect port]" +
            "\n [--host_connect host]" +
            "\n"
        );
        System.exit(-1);
    }

}
