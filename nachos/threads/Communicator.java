package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock();
        speakVar = new Condition2(lock);
        listenVar = new Condition2(lock);
        msgCount = 0;
        speakerCount = 0;
        listenerCount = 0;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        lock.acquire();

        speakerCount++;

        while (listenerCount == 0 || msgCount == 1) {
            speakVar.sleep();
        }

        w = word;
        msgCount = 1;
        listenVar.wake();
        speakerCount--;
        lock.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return the
     * <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        lock.acquire();
        speakVar.wake();
        listenerCount++;

        while (listenerCount > 1 || msgCount == 0) {
            listenVar.sleep();
        }
        int listened = w;
        msgCount = 0;
        listenerCount--;
        // speakVar.wakeAll();
        speakVar.wake();
        lock.release();
        return listened;
    }

    public static void selfTest() {
        /* Speaker in first, standard transfer */
        System.out.println("------------Communicator SelfTest Output-----------");
        System.out.println("<---Test Speaker First #1--->");
        Communicator tester = new Communicator();
        KThread say = new KThread(new Talk(tester, 70)).setName("Speaker 70");
        say.fork();
        System.out.println(" No output till here indicates test thread is waiting for listner");
        KThread list = new KThread(new Hear(tester)).setName("Listner 1");
        list.fork();
        list.join();
        // List1 should now pick up say1, both should output the transferred message.

        /* Listner in first */
        System.out.println("<--Test Listner First #2--->");
        KThread listF = new KThread(new Hear(tester)).setName("Listner 2"); // Listener comes in first
        listF.fork();
        System.out.println("Should be no new output since header, listner should be waiting");
        KThread sayL = new KThread(new Talk(tester, 75)).setName("Speaker 75");
        sayL.fork();
        sayL.join();

        /* Multi thread waiting */

        System.out.println("<---Test Many Speakers #3--->");
        Communicator multiTest = new Communicator();
        KThread say1 = new KThread(new Talk(multiTest, 10)).setName("Speaker 10");
        KThread say2 = new KThread(new Talk(multiTest, 79)).setName("Speaker 79");
        KThread say3 = new KThread(new Talk(multiTest, 30)).setName("Speaker 30");

        say1.fork();
        say2.fork(); // Not joined as never expected to be heard. Alright if main nachos forgets
                     // these threads.
        say3.fork();
        KThread list1 = new KThread(new Hear(multiTest)).setName("Listner 3");
        list1.fork();
        list1.join();
        System.out.println("<---Test Many Speakers and Matching listeners #4--->");
        Communicator multiTest2 = new Communicator(); // New communicator used as old multiTest still has speak threads
                                                      // on it. Forever left waiting.
        KThread say4 = new KThread(new Talk(multiTest2, 11)).setName("Speaker 11");
        KThread say5 = new KThread(new Talk(multiTest2, 69)).setName("Speaker 69");
        KThread say6 = new KThread(new Talk(multiTest2, 45)).setName("Speaker 45");

        say4.fork();
        say5.fork();
        say6.fork();
        KThread list4 = new KThread(new Hear(multiTest2)).setName("Listner 4");
        list4.fork();
        list4.join();
        KThread list5 = new KThread(new Hear(multiTest2)).setName("Listner 5");
        list5.fork();
        list5.join();
        KThread list6 = new KThread(new Hear(multiTest2)).setName("Listner 6");
        list6.fork();
        list6.join();

        System.out.println("There should have been 3 Words Spoken and 3 Words Heard in test #4. End."); // Should be
                                                                                                        // seen at the
                                                                                                        // conclusion of
                                                                                                        // all test
                                                                                                        // threads.

    }

    private static class Talk implements Runnable {
        int MESG;
        Communicator communicator;

        public Talk(Communicator comm, int word) {
            MESG = word;
            communicator = comm;
        }

        public void run() {

            communicator.speak(MESG);
            System.out.println("Said: " + MESG);
        }
    }

    private static class Hear implements Runnable {
        int message;
        Communicator communicator;

        public Hear(Communicator comm) {
            message = 0;
            communicator = comm;
        }

        public void run() {
            message = communicator.listen();
            System.out.println("Heard: " + message);
        }
    }

    int w;
    Lock lock;
    Condition2 speakVar;
    Condition2 listenVar;
    int msgCount;
    int speakerCount;
	int listenerCount;
}
