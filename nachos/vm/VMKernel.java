package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
        super.initialize(args);


    }

    /**
     * Test this kernel.
     */
    public void selfTest() {
        super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
        super.run();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
        super.terminate();
    }

    /** START */
    protected static MMU mmu = new MMU();

    public static void handleTLBMiss(int pid, int vpn) {
        mmu.fetchEntryFromTable(pid, vpn);
    }

    public static void handlePageFault(int pid, int vpn) {
        System.out.println("UNHADLED PAGE FAULT");
    }

    public static void addPageTableEntry(int pid, TranslationEntry entry) {
        mmu.addEntry(pid, entry);
    }

    public static TranslationEntry removePageTableEntry(int pid, int vpn) {
        return mmu.removeEntry(pid, vpn);
    }

    public static void releasePage (int ppn) {
        Lib.assertTrue(ppn >= 0 && ppn < Machine.processor().getNumPhysPages());
        Machine.interrupt().disable();
        pageList.add(ppn);
        Machine.interrupt().enable();
    }

    public static int [] allocatePages (int numPages) {
        Machine.interrupt().disable();
        int [] allocated = new int[numPages];
        if (numPages > pageList.size()) {
            Machine.interrupt().enable();
            return null;
        }
        for (int i = 0; i < numPages; i++) {
            allocated[i] = pageList.removeFirst();
        }
        Machine.interrupt().enable();
        return allocated;
    }
    /** FINISH */

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';
}
