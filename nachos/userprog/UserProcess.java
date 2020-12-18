package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];
        for (int i = 0; i < numPhysPages; i++)
            pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

        pid = processCounter++;
    }

    /**
     * Allocate and return a new process of the correct class. The class name is
     * specified by the <tt>nachos.conf</tt> key <tt>Kernel.processClassName</tt>.
     *
     * @return a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
        return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to load
     * the program, and then forks a thread to run it.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
        if (!load(name, args))
            return false;

        userThread = (UThread) (new UThread(this).setName(name));
        userThread.fork();

        return true;
    }

    /**
     * Save the state of this process in preparation for a context switch. Called by
     * <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
        Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read at
     * most <tt>maxLength + 1</tt> bytes from the specified address, search for the
     * null terminator, and convert it to a <tt>java.lang.String</tt>, without
     * including the null terminator. If no null terminator is found, returns
     * <tt>null</tt>.
     *
     * @param vaddr     the starting virtual address of the null-terminated string.
     * @param maxLength the maximum number of characters in the string, not
     *                  including the null terminator.
     * @return the string read, or <tt>null</tt> if no null terminator was found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
        Lib.assertTrue(maxLength >= 0);

        byte[] bytes = new byte[maxLength + 1];

        int bytesRead = readVirtualMemory(vaddr, bytes);

        for (int length = 0; length < bytesRead; length++) {
            if (bytes[length] == 0)
                return new String(bytes, 0, length);
        }

        return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to read.
     * @param data  the array where the data will be stored.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
        return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to read.
     * @param data   the array where the data will be stored.
     * @param offset the first byte to write in the array.
     * @param length the number of bytes to transfer from virtual memory to the
     *               array.
     * @return the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        // Retrieve memory from processor
        byte[] memory = Machine.processor().getMemory();

        // First virtual page number calculated from virtual address
        int firstVPN = Processor.pageFromAddress(vaddr);
        // First offset calculated from virtual address
        int firstOffset = Processor.offsetFromAddress(vaddr);
        // Last virtual page number calculated from the last virtual address
        int lastVPN = Processor.pageFromAddress(vaddr + length);

        // Retrieve and set up translation entry of the page for read
        TranslationEntry page = getTranslationEntry(firstVPN, false);

        // No page found
        if (page == null) return 0;

        // The amount of transferred byte should be the minimum of data size 
        //      and the number of bytes in the page
        int amount = Math.min(length, pageSize - firstOffset);
        // copy bytes from memory to data, started from first offset
        System.arraycopy(memory, Processor.makeAddress(page.ppn, firstOffset), 
                        data, offset, amount);
        
        // Forward the offset
        offset += amount;

        // Processing all virtual pages from first vpn to last vpn
        for (int vpn = firstVPN + 1; vpn < lastVPN + 1; vpn++) {
            page = getTranslationEntry(vpn, false);
            // No translation for the page is found.
            //      So, return the amount of bytes calculated upto now
            if (page == null) break;

            // The amount of transferred byte for this page should be 
            //      the minimum of bytes yet to transfer and page size
            int len = Math.min(length - amount, pageSize);
            // copy bytes from memory to data, started from 0
            System.arraycopy(memory, Processor.makeAddress(page.ppn, 0), 
                        data, offset, len);

            // Update amount of byte transferred
            amount += len;
            // Forward the offset
            offset += len;
        }

        return amount;
    }

    /**
     * Transfer all data from the specified array to this process's virtual memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param vaddr the first byte of virtual memory to write.
     * @param data  the array containing the data to transfer.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
        return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory. This
     * method handles address translation details. This method must <i>not</i>
     * destroy the current process if an error occurs, but instead should return the
     * number of bytes successfully copied (or zero if no data could be copied).
     *
     * @param vaddr  the first byte of virtual memory to write.
     * @param data   the array containing the data to transfer.
     * @param offset the first byte to transfer from the array.
     * @param length the number of bytes to transfer from the array to virtual
     *               memory.
     * @return the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);
        // Retrieve memory from processor
        byte[] memory = Machine.processor().getMemory();

        // First virtual page number calculated from virtual address
        int firstVPN = Processor.pageFromAddress(vaddr);
        // First offset calculated from virtual address
        int firstOffset = Processor.offsetFromAddress(vaddr);
        // Last virtual page number calculated from the last virtual address
        int lastVPN = Processor.pageFromAddress(vaddr + length);

        // Retrieve and set up translation entry of the page for write
        TranslationEntry page = getTranslationEntry(firstVPN, true);

        // No page found
        if (page == null) return 0;

        // The amount of transferred byte should be the minimum of data size 
        //      and the number of bytes in the page
        int amount = Math.min(length, pageSize - firstOffset);
        // copy bytes from data to memory, started from first offset
        System.arraycopy(data, offset, memory, 
                        Processor.makeAddress(page.ppn, firstOffset), amount);

        // Forward the offset
        offset += amount;

        // Processing all virtual pages from first vpn to last vpn
        for (int vpn = firstVPN + 1; vpn < lastVPN + 1; vpn++) {
            page = getTranslationEntry(vpn, true);
            // No translation for the page is found.
            //      So, return the amount of bytes calculated upto now
            if (page == null) break;

            // The amount of transferred byte for this page should be 
            //      the minimum of bytes yet to transfer and page size
            int len = Math.min(length - amount, pageSize);
            // copy bytes from data to memory, started from 0
            System.arraycopy(data, offset, memory, 
                            Processor.makeAddress(page.ppn, 0), len);
            
            // Update amount of byte transferred
            amount += len;
            // Forward the offset
            offset += len;
        }

        return amount;
    }

    /**
     * Load the executable with the specified name into this process, and prepare to
     * pass it the specified arguments. Opens the executable, reads its header
     * information, and copies sections and arguments into this process's virtual
     * memory.
     *
     * @param name the name of the file containing the executable.
     * @param args the arguments to pass to the executable.
     * @return <tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        } catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s = 0; s < coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }

        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i = 0; i < args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
        numPages += stackPages;
        initialSP = numPages * pageSize;

        // and finally reserve 1 page for arguments
        numPages++;

        if (!loadSections())
            return false;

        // store arguments in last page
        int entryOffset = (numPages - 1) * pageSize;
        int stringOffset = entryOffset + args.length * 4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i = 0; i < argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
            stringOffset += 1;
        }

        return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into memory.
     * If this returns successfully, the process will definitely be run (this is the
     * last step in process initialization that can fail).
     *
     * @return <tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
        // Allocate physical pages from Kernel. 
        //      The requested number of pages = Number of pages occupied by this process
        int [] ppns = UserKernel.allocatePages(numPages);

        // Unable to allocate physical pages. Terminate user program(.coff)
        if (ppns == null) {
            coff.close();
            Lib.debug(dbgProcess, "\tinsufficient physical memory");
            return false;
        }

        // Initialize page table for this process
        pageTable = new TranslationEntry[numPages];

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

                pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);

                section.loadPage(i, ppn);
            }
        }

        // allocate free pages for stack and argv
        for (int i = numPages - stackPages - 1; i < numPages; i++) {
			pageTable[i] = new TranslationEntry(i, ppns[i], true, false, false, false);
		}

        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
        coff.close();
        for (int i = 0; i < numPages; i++) {
            UserKernel.releasePage(pageTable[i].ppn);
        }
        pageTable = null;
    }

    /**
     * Retrieve translation entry for the virtual page number.
     * It also sets necessary bits based on whether the page is writable.
     * @param vpn   Virtual Page Number
     * @param isWrite   Is this page for write?
     * @return  The TranslationEntry for the vpn. If not found, returns null
     */
    protected TranslationEntry getTranslationEntry(int vpn, boolean isWrite) {
        // Virtual page number must be non-negative and less than number of pages occupied by this process
        if (vpn < 0 || vpn >= numPages) return null;
        // Retrieve translation entry for vpn
        TranslationEntry page = pageTable[vpn];
        // No entry found for this vpn
        if (page == null) return null;
        // A read-only page cannot be used for write
		if (page.readOnly && isWrite) return null;
        // This page is in use
        page.used = true;
        // Set the dirty bit as this page is writable
        if (isWrite)
            page.dirty = true;
        
        return page;
    }

    /**
     * Initialize the processor's registers in preparation for running the program
     * loaded into this process. Set the PC register to point at the start function,
     * set the stack pointer register to point at the top of the stack, set the A0
     * and A1 registers to argc and argv, respectively, and initialize all other
     * registers to 0.
     */
    public void initRegisters() {
        Processor processor = Machine.processor();

        // by default, everything's 0
        for (int i = 0; i < Processor.numUserRegisters; i++)
            processor.writeRegister(i, 0);

        // initialize PC and SP according
        processor.writeRegister(Processor.regPC, initialPC);
        processor.writeRegister(Processor.regSP, initialSP);

        // initialize the first two argument registers to argc and argv
        processor.writeRegister(Processor.regA0, argc);
        processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call.
     */
    private int handleHalt() {

        Machine.halt();

        Lib.assertNotReached("Machine.halt() did not halt machine!");
        return 0;
    }

    //added by Shahrar
	private int handleRead(int fileDescriptor, int address, int count){
    	int result = -1;
    	if(fileDescriptor != 0 || count < 0){
    		return result;
		}
    	OpenFile openFile = UserKernel.console.openForReading();
		if(openFile == null){
			return result;
		}
		byte[] buff = new byte[count];
		int size = openFile.read(buff,0, count);

		if(size < 0){
			return result;
		}

		return writeVirtualMemory(address, buff, 0, size);
	}

	private int handleWrite(int fileDescriptor, int address, int count){
    	int result = -1;
    	if(fileDescriptor != 1 || count < 0){
			return result;
		}

    	OpenFile openFile = UserKernel.console.openForWriting();
		if(openFile == null){
			return result;
		}
		byte[] buff = new byte[count];
		int size = readVirtualMemory(address, buff);
		if(size < count){
			return result;
		}
		else {
			return openFile.write(buff, 0, count);
		}
    }
    
    private int handleExec(int fileVAddr, int argc, int argvAddr) {

        String fileName = readVirtualMemoryString(fileVAddr, FILE_NAME_MAX_LEN);

        if (fileName == null || !fileName.endsWith(".coff")) {
            Lib.debug(dbgProcess, "handleExec(): Invalid file name.");
            return -1;
        }

        if (argc < 0) {
            Lib.debug(dbgProcess, "handleExec(): argc is negative.");
            return -1;
        }

        String [] argv = new String[argc];
        byte [] buffer = new byte[4];

        for (int i = 0; i < argc; i++) {
            if (readVirtualMemory(argvAddr + i*4, buffer) != 4)
                return -1;

            int vaddr = Lib.bytesToInt(buffer, 0);
            argv[i] = readVirtualMemoryString(vaddr, FILE_NAME_MAX_LEN);

            if (argv[i] == null) return -1;
        }

        UserProcess child = newUserProcess();

        if (!child.execute(fileName, argv)) {
            Lib.debug(dbgProcess, "handleExec(): Failed to execute child process.");
            return -1;
        }

        child.parent = this;
        childProcesses.add(child);

        return child.pid;
    }

    private int handleJoin(int processId, int statusVAddr) {
        if (processId < 0) {
            Lib.debug(dbgProcess, "handleJoin(): processId is negative.");
            return -1;
        }
        if (statusVAddr < 0) {
            Lib.debug(dbgProcess, "handleJoin(): virtual address of status is negative.");
            return -1;
        }

        UserProcess child = null;
        for (UserProcess process : childProcesses) {
            if (process.pid == processId) {
                child = process;
                break;
            }
        }

        if (child == null) {
            Lib.debug(dbgProcess, "handleJoin(): processId is not a child process of the current process.");
            return -1;
        }

        child.userThread.join();
        child.parent = null;

        childProcesses.remove(child);

        lock.acquire();
        Integer status = childStatus.get(child.pid);
        lock.release();

        if (status == null) {
            Lib.debug(dbgProcess, "handleJoin(): Exit status not found.");
            return 0;
        } else {
            byte [] buffer = new byte[4];

            buffer = Lib.bytesFromInt(status);

            if (writeVirtualMemory(statusVAddr, buffer) == 4) return 1;
            else {
                Lib.debug(dbgProcess, "handleJoin(): Failure to write status.");
                return 0;
            }
        }
    }

    private int handleExit(int status) {
        if (this.parent != null) {
            lock.acquire();
            parent.childStatus.put(pid, status);
            lock.release();
        }

        unloadSections();

        childProcesses.forEach(process -> process.parent = null);
        childProcesses.clear();

        if (pid == 0) Kernel.kernel.terminate();
        else UThread.finish();

        return 0;
    }

    private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2, syscallJoin = 3, syscallCreate = 4,
            syscallOpen = 5, syscallRead = 6, syscallWrite = 7, syscallClose = 8, syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr>
     * <td>syscall#</td>
     * <td>syscall prototype</td>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td><tt>void halt();</tt></td>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td><tt>void exit(int status);</tt></td>
     * </tr>
     * <tr>
     * <td>2</td>
     * <td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td><tt>int  join(int pid, int *status);</tt></td>
     * </tr>
     * <tr>
     * <td>4</td>
     * <td><tt>int  creat(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>5</td>
     * <td><tt>int  open(char *name);</tt></td>
     * </tr>
     * <tr>
     * <td>6</td>
     * <td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>7</td>
     * <td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td>
     * </tr>
     * <tr>
     * <td>8</td>
     * <td><tt>int  close(int fd);</tt></td>
     * </tr>
     * <tr>
     * <td>9</td>
     * <td><tt>int  unlink(char *name);</tt></td>
     * </tr>
     * </table>
     * 
     * @param syscall the syscall number.
     * @param a0      the first syscall argument.
     * @param a1      the second syscall argument.
     * @param a2      the third syscall argument.
     * @param a3      the fourth syscall argument.
     * @return the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
        switch (syscall) {
            case syscallHalt:
                return handleHalt();
            case syscallRead:
                return handleRead(a0,a1,a2);
            case syscallWrite:
                return handleWrite(a0,a1,a2);
            case syscallExec:
                return handleExec(a0, a1, a2);
            case syscallJoin:
                return handleJoin(a0, a1);
            case syscallExit:
                return handleExit(a0);

            default:
                Lib.debug(dbgProcess, "Unknown syscall " + syscall);
                Lib.assertNotReached("Unknown system call!");
        }
        return 0;
    }

    /**
     * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>.
     * The <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param cause the user exception that occurred.
     */
    public void handleException(int cause) {
        Processor processor = Machine.processor();

        switch (cause) {
            case Processor.exceptionSyscall:
                int result = handleSyscall(processor.readRegister(Processor.regV0),
                        processor.readRegister(Processor.regA0), processor.readRegister(Processor.regA1),
                        processor.readRegister(Processor.regA2), processor.readRegister(Processor.regA3));
                processor.writeRegister(Processor.regV0, result);
                processor.advancePC();
                break;

            default:
                Lib.debug(dbgProcess, "Unexpected exception: " + Processor.exceptionNames[cause]);
                Lib.assertNotReached("Unexpected exception");
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = Config.getInteger("Processor.numStackPages", 8);

    private int initialPC, initialSP;
    private int argc, argv;

    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';


    private static final int FILE_NAME_MAX_LEN = 256;
    private LinkedList<UserProcess> childProcesses = new LinkedList<>();

    protected int pid;
    protected static int processCounter = 0;

    protected UThread userThread;
    protected UserProcess parent = null; 

    protected HashMap<Integer, Integer> childStatus = new HashMap<>();
    private static Lock lock = new Lock();
}
