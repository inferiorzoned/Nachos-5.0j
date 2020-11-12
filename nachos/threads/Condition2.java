package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param conditionLock the lock associated with this condition variable. The
     *                      current thread must hold this lock whenever it uses
     *                      <tt>sleep()</tt>, <tt>wake()</tt>, or
     *                      <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
        
        // waitQueue = new LinkedList<KThread>();
        this.waitingQueue = ThreadedKernel.scheduler.newThreadQueue(false);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The current
     * thread must hold the associated lock. The thread will automatically reacquire
     * the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();

        // waitQueue.add(KThread.currentThread());
        conditionLock.release();
        // does the same work as of semaphore P()
        // KThread.sleep();
        waitingQueue.waitForAccess(KThread.currentThread());
        KThread.sleep();
        conditionLock.acquire();

        Machine.interrupt().restore(intStatus);
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        // does the same work as of semaphore V()
        // if (!waitQueue.isEmpty())
            // (waitQueue.removeFirst()).ready();
        KThread k = waitingQueue.nextThread();
        if (k != null) {
            k.ready();
        }
        Machine.interrupt().restore(intStatus);        
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current thread
     * must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        boolean intStatus = Machine.interrupt().disable();
        
        // while(!waitQueue.isEmpty())
        KThread k = waitingQueue.nextThread();
        while(k!= null){
            k.ready();
            k = waitingQueue.nextThread();
        }
            

        Machine.interrupt().restore(intStatus);
    }

    public static void selfTest() { 
        // Lock lock = new Lock();
        // Condition2 condition = new Condition2(lock);

        // KThread t1 = new KThread(new Runnable() {
        //     int which = 1;

        //     public void run() {
        //         // currentThread.yield();
        //         for (int i = 0; i < 5; i++) {
        //             System.out.println("*** thread " + which + " looped " + i + " times");
        //             lock.acquire();
        //             if (which == 1 && i == 2)
        //                 condition.sleep();
        //             if (which == 2 && i == 4)
        //                 condition.wake();
        //             lock.release();
        //             // KThread.yield();
        //         }
        //         System.out.println("thread " + which + " returning");
        //     }
        // }).setName("abc");

        // t1.fork();
        // // t1.join();
        // KThread t2 = new KThread(new Runnable() {
        //     int which = 2;

        //     public void run() {
        //         // currentThread.yield();
        //         for (int i = 0; i < 7; i++) {
        //             System.out.println("*** thread " + which + " looped " + i + " times");
        //             lock.acquire();
        //             if (which == 1 && i == 2)
        //                 condition.sleep();
        //             if (which == 2 && i == 4)
        //                 condition.wake();
        //             lock.release();
        //             // KThread.yield();
        //         }
        //     }
        // }).setName("def");
        // t2.fork();
        // t1.join();
        // // t2.join();
        // KThread t3 = new KThread(new Runnable() {
        //     int which = 0;

        //     public void run() {
        //         // currentThread.yield();
        //         for (int i = 0; i < 5; i++) {
        //             System.out.println("*** thread " + which + " looped " + i + " times");
        //             lock.acquire();
        //             if (which == 1 && i == 2)
        //                 condition.sleep();
        //             if (which == 2 && i == 4)
        //                 condition.wake();
        //             lock.release();
        //             // KThread.yield();
        //         }
        //     }
        // }).setName("main");
        // t3.fork();
        // t3.join();
        final Lock testLock = new Lock();
		final Condition2 testCond = new Condition2(testLock);
		// Test Case 1: Put a running thread to sleep 
		KThread sleep1 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
				System.out.println("--------Starting Condition2 selft test -----------------");
                System.out.println("Test Case 1: Taking a nap");
                testCond.sleep();     
                System.out.println("Test Case 1: Thread woke up!"); 
				System.out.println("Test Case 1: Complete");
				testLock.release();
				
        } } ).setName("Test 1");
		sleep1.fork();
		//KThread sleep0 = new KThread(new Runnable);
		
		KThread sleep2 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 1.1: Taking a nap");
                testCond.sleep();     
                System.out.println("Test Case 1.1: Thread woke up!");
				System.out.println("Test Case 1.1: Complete");
				testLock.release();		
				
        } } ).setName("Test 1.1");
		
		sleep2.fork();
		
		KThread sleep3 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 1.2: Taking a nap");
                testCond.sleep();     
                System.out.println("Test Case 1.2: Thread woke up!"); 
				System.out.println("Test Case 1.2: Complete");
				testLock.release();
        } } ).setName("Test 1.2");
		sleep3.fork();
		
		// Test Case 2: Wake a sleeping thread
		KThread wake1 =	new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 2: Waking a thread...");
                testCond.wake();      
				System.out.println("Test Case 2: Complete");
				testLock.release();
        } } ).setName("Test 2");
		wake1.fork();
		sleep1.join();
		
		// Test Case 3: Waking all threads on the sleepingQueue
		KThread wakeAll1 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 3: Waking everyone up...");
                testCond.wakeAll();     
                System.out.println("Test Case 3: Everyone's awake now!!"); 
				System.out.println("Test Case 3: Complete");
				testLock.release();
        } } ).setName("Test 3");
		wakeAll1.fork();
		sleep2.join();
		sleep3.join();
		
		// Test Case 4: Wake a thread when sleepingQueue is empty
		KThread wake2 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 4: Waking someone who's not there...");
                testCond.wake();      
				System.out.println("Test Case 4: No one home to wake... ");
				System.out.println("Test Case 4: Complete");
				testLock.release();
        } } ).setName("Test 4");
		wake2.fork();
		
		// Test Case 5: Wake all when sleepingQueue is empty
		KThread wakeAll2 = new KThread(new Runnable(){

            public void run(){
                testLock.acquire();
                System.out.println("Test Case 5: Waking an empty nest...");
                testCond.wakeAll();     
                System.out.println("Test Case 5: No one home to wake..."); 
				System.out.println("Test Case 5: Complete");
				testLock.release();
        } } ).setName("Test 5");
        wakeAll2.fork();
        wakeAll2.join();
        wake2.join();

    }
    private Lock conditionLock;
    // waitQueue works in the same fashion as the waitQueue in Condition
    // private LinkedList<KThread> waitQueue;
    private ThreadQueue waitingQueue;
}
