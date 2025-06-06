package other;

import javax.xml.crypto.Data;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;

public class SeqLock {
    private final AtomicLong sequence = new AtomicLong(0);
    private volatile Data data;

    void write(Data newData) {
        long seq = sequence.incrementAndGet();  // Odd = writing
        VarHandle.storeStoreFence();           // Ensure sequence visible

        data = newData;                        // Update data

        VarHandle.storeStoreFence();           // Ensure data visible
        sequence.incrementAndGet();            // Even = done writing
    }

    Data read() {
        while (true) {
            long seq1 = sequence.get();
            if ((seq1 & 1) != 0) continue;    // Skip if writing

            VarHandle.loadLoadFence();         // Ensure sequence read first
            Data copy = data;                  // Read data
            VarHandle.loadLoadFence();         // Ensure data read before seq2

            long seq2 = sequence.get();
            if (seq1 == seq2) return copy;    // No concurrent write
        }
    }
}
