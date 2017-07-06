/*
 * ====================================================================
 * Copyright (c) 2003-04 Terabit P/L.
 * All rights reserved.
 * ====================================================================
 *
 * Created on 14/01/2006
 * $Id$
 */
package au.com.terabit.testjp;

import au.com.terabit.jproactor.AsynchTimerHandler;
import au.com.terabit.jproactor.Demultiplexor;
import au.com.terabit.jproactor.OpTimer;
import java.io.IOException;

public class TestTimer implements AsynchTimerHandler {

    private Demultiplexor mDemultiplexor = null;

    private OpTimer timer1 = null;
    private OpTimer timer2 = null;
    private OpTimer timer3 = null;
    private OpTimer timer4 = null;
    private OpTimer timer5 = null;
    private OpTimer timer6 = null;
    private OpTimer timer7 = null;
    private OpTimer timer8 = null;
    private OpTimer timer9 = null;

    private int invocationCount = 0;

    public TestTimer() throws IOException {
        mDemultiplexor = new Demultiplexor();
        mDemultiplexor.start(2);
    }

    public void start() {
        invocationCount = 0;
        timer1 = new OpTimer((long) (4 * 1000), this);
        mDemultiplexor.startTimer(timer1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer2 = new OpTimer((long) (1 * 1000), this);
        mDemultiplexor.startTimer(timer2);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer3 = new OpTimer((long) (2 * 1000), this);
        mDemultiplexor.startTimer(timer3);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer4 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer4);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer5 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer5);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer6 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer6);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer7 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer7);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer8 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer8);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        timer9 = new OpTimer((long) (3 * 1000), this);
        mDemultiplexor.startTimer(timer9);

    }

    public void timerExpired(OpTimer opTimer) throws Exception {
        long now = System.currentTimeMillis();
        System.out.println("[" + Thread.currentThread().getName() +
            "] Timer precision is " + (now - opTimer.m_expiryTime) + " millisecond(s)");

        OpTimer curTimer = null;

        switch (++invocationCount) {
            case 1:
                curTimer = timer1;
                break;
            case 2:
                curTimer = timer2;
                break;
            case 3:
                curTimer = timer3;
                break;
            case 4:
                curTimer = timer4;
                break;
            case 5:
                curTimer = timer5;
                break;
            case 6:
                curTimer = timer6;
                break;
            case 7:
                curTimer = timer7;
                break;
            case 8:
                curTimer = timer8;
                break;
            case 9:
                curTimer = timer9;
                break;
        }
        if (curTimer != opTimer)
            System.out.println("Unknow timer instance is passed as action parameter");

        if (curTimer == timer9)
            mDemultiplexor.shutdown();
    }

    public static void main(String[] argv) throws Exception {
        TestTimer test = new TestTimer();
        test.start();
    }

}
