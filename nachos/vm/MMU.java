package nachos.vm;

import java.util.Random;

import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

public class MMU {
    public static InvPageTable pageTable = new InvPageTable();
    private Random rand = new Random();
    public MMU() {

    }

    public TranslationEntry fetchEntryFromTable(int pid, int vpn) {
        TranslationEntry entry = pageTable.get(pid, vpn);
        if (entry == null) {
            System.out.println("NO ENTRY FOUND IN PAGE-TABLE.");
        }
        int indx = rand.nextInt(Machine.processor().getTLBSize());
        Machine.processor().writeTLBEntry(indx, entry);
        return entry;
    }

    public void addEntry(int pid, TranslationEntry entry) {
        pageTable.put(pid, entry);
    }

    public TranslationEntry removeEntry(int pid, int vpn) {
        return pageTable.remove(pid, vpn);
    }

    public TranslationEntry getTranslationEntry(int pid, int vpn) {
        return pageTable.get(pid, vpn);
    }
}
