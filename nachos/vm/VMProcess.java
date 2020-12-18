package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        // super();
        pid = processCounter++;
    }

    /**
     * Save the state of this process in preparation for a context switch. Called by
     * <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
        super.saveState();
        // invalidate current TLB
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
            TranslationEntry entry = new TranslationEntry();
            entry.valid = false;
            Machine.processor().writeTLBEntry(i, entry);
        }
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        // super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return <tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        // Allocate physical pages from Kernel. 
        //      The requested number of pages = Number of pages occupied by this process
        int [] ppns = VMKernel.allocatePages(numPages);

        // Unable to allocate physical pages. Terminate user program(.coff)
        if (ppns == null) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // load sections
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);

            Lib.debug(dbgProcess,
                    "\tinitializing " + section.getName() + " section (" + section.getLength() + " pages)");

            // For contiguous vpns
            for (int i = 0; i < section.getLength(); i++) {
                int vpn = section.getFirstVPN() + i;
                // Retrieve physical page number
                int ppn = ppns[vpn];

                VMKernel.addPageTableEntry(pid, new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false));

                section.loadPage(i, ppn);
            }
        }

        // allocate free pages for stack and argv
        for (int i = numPages - stackPages - 1; i < numPages; i++) {
			VMKernel.addPageTableEntry(pid, new TranslationEntry(i, ppns[i], true, false, false, false));
		}

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        coff.close();
        for (int i = 0; i < numPages; i++) {
            TranslationEntry entry = VMKernel.removePageTableEntry(pid, i);
            VMKernel.releasePage(entry.ppn);
        }
    }

    /**
     * Retrieve translation entry for the virtual page number. First
     * It also sets necessary bits based on whether the page is writable.
     * @param vpn   Virtual Page Number
     * @param isWrite   Is this page for write?
     * @return  The TranslationEntry for the vpn. If not found, returns null
     */
    protected TranslationEntry getTranslationEntry(int vpn, boolean isWrite) {
        // Virtual page number must be non-negative and less than number of pages occupied by this process
        if (vpn < 0 || vpn >= numPages) return null;

        TranslationEntry entry = null;
        for (int i = 0; i < Machine.processor().getTLBSize(); i++) {
            TranslationEntry tlbEntry = Machine.processor().readTLBEntry(i);
            if (tlbEntry.valid && tlbEntry.vpn == vpn) {
                entry = tlbEntry;
                break;
            }
        }

        if (entry == null) {
            // FIXME - workaround to generate tlbMissException?
            entry = VMKernel.mmu.fetchEntryFromTable(pid, vpn);
        }

        // No entry found for this vpn
        if (entry == null) return null;
        // A read-only page cannot be used for write
		if (entry.readOnly && isWrite) return null;
        // This page is in use
        entry.used = true;
        // Set the dirty bit as this page is writable
        if (isWrite)
            entry.dirty = true;
        
        return entry;
    }

    private void handleTLBMiss(int badVAddr) {
        int vpn = Processor.pageFromAddress(badVAddr);
        
        VMKernel.handleTLBMiss(pid, vpn);
    }

    private void handlePageFault(int badVAddr) {
        int vpn = Processor.pageFromAddress(badVAddr);
        
        VMKernel.handlePageFault(pid, vpn);
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>.
     * The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        int vaddr;
        switch (cause) {
            case Processor.exceptionTLBMiss:
                vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
                handleTLBMiss(vaddr);
                break;
            
            case Processor.exceptionPageFault:
                vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
                handlePageFault(vaddr);
                break;
            default:
                super.handleException(cause);
                break;
        }
    }

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
}
