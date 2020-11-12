package nachos.threads;

import nachos.machine.*;

import java.util.LinkedList;

/**
 * An implementation of condition variables built upon semaphores.
 *
 * <p>
 * A condition variable is a synchronization primitive that does not have a
 * value (unlike a semaphore or a lock), but threads may still be queued.
 *
 * <p>
 * <ul>
 *
 * <li><tt>sleep()</tt>: atomically release the lock and relinkquish the CPU
 * until woken; then reacquire the lock.
 *
 * <li><tt>wake()</tt>: wake up a single thread sleeping in this condition
 * variable, if possible.
 *
 * <li><tt>wakeAll()</tt>: wake up all threads sleeping inn this condition
 * variable.
 *
 * </ul>
 *
 * <p>
 * Every condition variable is associated with some lock. Multiple condition
 * variables may be associated with the same lock. All three condition variable
 * operations can only be used while holding the associated lock.
 *
 * <p>
 * In Nachos, condition variables are summed to obey <i>Mesa-style</i>
 * semantics. When a <tt>wake()</tt> or <tt>wakeAll()</tt> wakes up another
 * thread, the woken thread is simply put on the ready list, and it is the
 * responsibility of the woken thread to reacquire the lock (this reacquire is
 * taken core of in <tt>sleep()</tt>).
 *
 * <p>
 * By contrast, some implementations of condition variables obey
 * <i>Hoare-style</i> semantics, where the thread that calls <tt>wake()</tt>
 * gives up the lock and the CPU to the woken thread, which runs immediately and
 * gives the lock and CPU back to the waker when the woken thread exits the
 * critical section.
 *
 * <p>
 * The consequence of using Mesa-style semantics is that some other thread can
 * acquire the lock and change data structures, before the woken thread gets a
 * chance to run. The advance to Mesa-style semantics is that it is a lot easier
 * to implement.
 */

public class Condition {
    /**
     * Allocate a new condition variable.
     *
     * @param conditionLock the lock associated with this condition variable. The
     *                      current thread must hold this lock whenever it uses
     *                      <tt>sleep()</tt>, <tt>wake()</tt>, or
     *                      <tt>wakeAll()</tt>.
     */
    public Condition(Lock conditionLock) {
        this.conditionLock = conditionLock;

        waitQueue = new LinkedList<Semaphore>();
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The current
     * thread must hold the associated lock. The thread will automatically reacquire
     * the lock before <tt>sleep()</tt> returns.
     *
     * <p>
     * This implementation uses semaphores to implement this, by allocating a
     * semaphore for each waiting thread. The waker will <tt>V()</tt> this
     * semaphore, so thre is no chance the sleeper will miss the wake-up, even
     * though the lock is released before caling <tt>P()</tt>.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        Semaphore waiter = new Semaphore(0);
        waitQueue.add(waiter);
        
        conditionLock.release();
        waiter.P();
        //blocked
        conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        // System.out.println("in wake " + waitQueue);
        if (!waitQueue.isEmpty())
            ((Semaphore) waitQueue.removeFirst()).V();
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current thread
     * must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        while (!waitQueue.isEmpty())
            wake();
    }

    // private static class PingTest implements Runnable {
    // PingTest(int which) {
    // this.which = which;
    // // this.lock = lock;
    // // this.condition = condition;
    // }

    // public void run() {
    // // currentThread.yield();
    // for (int i = 0; i < 5; i++) {
    // System.out.println("*** thread " + which + " looped " + i + " times");
    // lock.acquire();
    // if(which == 1 && i == 2)
    // condition.sleep();
    // if(which == 2 && i == 4)
    // condition.wake();
    // lock.release();
    // KThread.yield();
    // }
    // }

    // private int which;
    // // private Lock lock;
    // // private Condition condition;
    // }

    public static void selfTest() { 
        // Lock lock = new Lock();
        // Condition condition = new Condition(lock);

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
    }

    private Lock conditionLock;
    private LinkedList<Semaphore> waitQueue;
}
