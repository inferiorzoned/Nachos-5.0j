package nachos.vm;

import nachos.machine.*;
import java.util.Hashtable;

public class InvPageTable {

    private Hashtable<InvPageTable.Key, InvPageTable.Entry> pageTable;

    public InvPageTable() {
        pageTable = new Hashtable<>();
    }

    public void put(int pid, TranslationEntry entry) {
        pageTable.put(new Key(pid, entry.vpn), new Entry(pid, entry));
    }

    public TranslationEntry get(int pid, int vpn) {
        return pageTable.get(new Key(pid, vpn)).getTranslationEntry();
    }

    public TranslationEntry remove (int pid, int vpn) {
        return pageTable.remove(new Key(pid, vpn)).getTranslationEntry();
    }

    protected class Key {
        int pid;
        int vpn;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + pid;
            result = prime * result + vpn;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Key other = (Key) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (pid != other.pid)
                return false;
            if (vpn != other.vpn)
                return false;
            return true;
        }

        private InvPageTable getEnclosingInstance() {
            return InvPageTable.this;
        }

        public Key(int pid, int vpn) {
            this.pid = pid;
            this.vpn = vpn;
        }
    }

    protected class Entry {
        /**
         * Allocate a new translation entry with the specified initial state.
         *
         * @param vpn      the virtual page numben.
         * @param ppn      the physical page number.
         * @param valid    the valid bit.
         * @param readOnly the read-only bit.
         * @param used     the used bit.
         * @param dirty    the dirty bit.
         */
        public Entry(int pid, int vpn, int ppn, boolean valid, boolean readOnly, boolean used, boolean dirty) {
            this.pid = pid;
            this.vpn = vpn;
            this.ppn = ppn;
            this.valid = valid;
            this.readOnly = readOnly;
            this.used = used;
            this.dirty = dirty;
        }

        /**
         * Allocate a new translation entry, copying the contents of an existing one.
         *
         * @param entry the translation entry to copy.
         */
        public Entry(int pid, TranslationEntry entry) {
            this.pid = pid;
            vpn = entry.vpn;
            ppn = entry.ppn;
            valid = entry.valid;
            readOnly = entry.readOnly;
            used = entry.used;
            dirty = entry.dirty;
        }

        public TranslationEntry getTranslationEntry() {
            return new TranslationEntry(vpn, ppn, valid, readOnly, used, dirty);
        }

        /** The process id. */
        public int pid;

        /** The virtual page number. */
        public int vpn;

        /** The physical page number. */
        public int ppn;

        /**
         * If this flag is <tt>false</tt>, this translation entry is ignored.
         */
        public boolean valid;

        /**
         * If this flag is <tt>true</tt>, the user pprogram is not allowed to modify the
         * contents of this virtual page.
         */
        public boolean readOnly;

        /**
         * This flag is set to <tt>true</tt> every time the page is read or written by a
         * user program.
         */
        public boolean used;

        /**
         * This flag is set to <tt>true</tt> every time the page is written by a user
         * program.
         */
        public boolean dirty;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getEnclosingInstance().hashCode();
            result = prime * result + (dirty ? 1231 : 1237);
            result = prime * result + pid;
            result = prime * result + ppn;
            result = prime * result + (readOnly ? 1231 : 1237);
            result = prime * result + (used ? 1231 : 1237);
            result = prime * result + (valid ? 1231 : 1237);
            result = prime * result + vpn;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Entry other = (Entry) obj;
            if (!getEnclosingInstance().equals(other.getEnclosingInstance()))
                return false;
            if (dirty != other.dirty)
                return false;
            if (pid != other.pid)
                return false;
            if (ppn != other.ppn)
                return false;
            if (readOnly != other.readOnly)
                return false;
            if (used != other.used)
                return false;
            if (valid != other.valid)
                return false;
            if (vpn != other.vpn)
                return false;
            return true;
        }

        private InvPageTable getEnclosingInstance() {
            return InvPageTable.this;
        }

        
    }
}