package other;

import java.lang.invoke.VarHandle;

class ProducerConsumer {
    volatile boolean flag = false;
    int data;

    // Producer
    void produce() {
        data = 42;                    // Plain store
        VarHandle.storeStoreFence();  // Ensure data is written first
        flag = true;                  // Release store
    }

    // Consumer
    void consume() {
        while (!flag);                // Acquire load
        VarHandle.loadLoadFence();    // Ensure flag is read first
        int value = data;             // Plain load (guaranteed to see 42)
    }
}
