package nachos.threads;

import java.util.Comparator;
import java.util.PriorityQueue;


import nachos.machine.Machine;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p>
     * <b>Note</b>: Nachos will not function correctly with more than one alarm.
     */
    public Alarm() {
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current thread
     * to yield, forcing a context switch if there is another thread that should be
     * run.
     */
    public void timerInterrupt() {
        // System.out.println("Clock time: " + Machine.timer().getTime() + ", waitingQueue size:" + pq.size());

        boolean intStatus = Machine.interrupt().disable();

        while(!pq.isEmpty() && pq.peek().wakeupTime <= Machine.timer().getTime()) {
            KThread k = pq.poll().t;
           //  System.out.println("Making " + k.getName() + " ready at " + Machine.timer().getTime());
            k.ready();
        }
        KThread.yield();

        Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks, waking it up in
     * the timer interrupt handler. The thread must be woken up (placed in the
     * scheduler ready set) during the first timer interrupt where
     *
     * <p>
     * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     *
     * @see nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        
        long wakeupTime = Machine.timer().getTime() + x;
        
        // System.out.println(KThread.currentThread().getName() + " is waiting until " + wakeupTime);
        waitingThread thread = new waitingThread(KThread.currentThread(), wakeupTime);
        pq.add(thread);

        boolean intStatus = Machine.interrupt().disable();
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
    }

    
    public class waitingThread {
        public KThread t;
        public long wakeupTime;

        public waitingThread(KThread t, long wakeupTime) {
            this.t = t;
            this.wakeupTime = wakeupTime;
        }        
    }

    private static class PingTest implements Runnable {
        @Override
        public void run() {
            int t = (int)((Math.random() * 1000));
            // System.out.println(KThread.currentThread().getName() + " started, time is " + Machine.timer().getTime());
            ThreadedKernel.alarm.waitUntil(t);
            // System.out.println(KThread.currentThread().getName() + " finishing, time is " + Machine.timer().getTime());
        }
    }

    public static void selfTest() {
        final int no = 7; 
        PingTest p = new PingTest();
        for (int i = 0; i < no; i++) {
            new KThread(p).setName("thread " + i + "0" + i).fork();
        }
        
        KThread.yield();
        while(!ThreadedKernel.alarm.pq.isEmpty()) {
            KThread.yield();
        }
    }
    
    PriorityQueue <waitingThread> pq = new PriorityQueue<>(
        new Comparator<waitingThread>() {
            public int compare(waitingThread a, waitingThread b) {
                return Long.valueOf(a.wakeupTime).compareTo(b.wakeupTime);
            }
        }
    );
}
