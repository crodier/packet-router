package other;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A high-performance, lock-free, multi-threaded ring buffer implementation.
 * Uses the Disruptor-style approach with sequence numbers and padding to avoid false sharing.
 */
public class RingBuffer<T> {

    // Padding to prevent false sharing between fields
    private static class PaddedAtomicLong extends AtomicLong {
        // 64 bytes padding (8 longs) to ensure each atomic long is on its own cache line
        private long p1, p2, p3, p4, p5, p6, p7;

        PaddedAtomicLong(long initialValue) {
            super(initialValue);
        }

        // Prevent padding optimization
        public long preventOptimization() {
            return p1 + p2 + p3 + p4 + p5 + p6 + p7;
        }
    }

    private static class Cell<T> {
        // Sequence number for this cell
        private final PaddedAtomicLong sequence;
        // The actual data - volatile for visibility
        private volatile T data;

        Cell(long sequence) {
            this.sequence = new PaddedAtomicLong(sequence);
        }
    }

    // VarHandle for efficient volatile access
    private static final VarHandle DATA_HANDLE;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            DATA_HANDLE = lookup.findVarHandle(Cell.class, "data", Object.class);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    private final int capacity;
    private final int mask;
    private final Cell<T>[] buffer;
    private final PaddedAtomicLong head;  // Producer position
    private final PaddedAtomicLong tail;  // Consumer position

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }

        // Round up to next power of 2
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;

        // Initialize buffer
        this.buffer = new Cell[this.capacity];
        for (int i = 0; i < this.capacity; i++) {
            buffer[i] = new Cell<>(i);
        }

        this.head = new PaddedAtomicLong(0);
        this.tail = new PaddedAtomicLong(0);
    }

    /**
     * Non-blocking push operation.
     * @return true if successful, false if buffer is full
     */
    public boolean offer(T value) {
        if (value == null) {
            throw new NullPointerException("Null values not allowed");
        }

        long currentHead = head.get();

        for (;;) {
            Cell<T> cell = buffer[(int)(currentHead & mask)];
            long seq = cell.sequence.get();
            long diff = seq - currentHead;

            if (diff == 0) {
                // Cell is available for writing
                if (head.compareAndSet(currentHead, currentHead + 1)) {
                    // We got the slot
                    DATA_HANDLE.setVolatile(cell, value);
                    cell.sequence.lazySet(currentHead + 1);
                    return true;
                }
                // CAS failed, retry with new head
                currentHead = head.get();
            } else if (diff < 0) {
                // Buffer is full
                return false;
            } else {
                // Another thread already claimed this slot
                currentHead = head.get();
            }
        }
    }

    /**
     * Non-blocking pop operation.
     * @return the value, or null if buffer is empty
     */
    public T poll() {
        long currentTail = tail.get();

        for (;;) {
            Cell<T> cell = buffer[(int)(currentTail & mask)];
            long seq = cell.sequence.get();
            long diff = seq - (currentTail + 1);

            if (diff == 0) {
                // Cell has data available
                if (tail.compareAndSet(currentTail, currentTail + 1)) {
                    // We got the slot
                    @SuppressWarnings("unchecked")
                    T value = (T) DATA_HANDLE.getVolatile(cell);
                    DATA_HANDLE.setVolatile(cell, null);  // Help GC
                    cell.sequence.lazySet(currentTail + capacity);
                    return value;
                }
                // CAS failed, retry with new tail
                currentTail = tail.get();
            } else if (diff < 0) {
                // Buffer is empty
                return null;
            } else {
                // Another thread already consumed this slot
                currentTail = tail.get();
            }
        }
    }

    /**
     * Check if buffer is empty (snapshot, may change immediately).
     */
    public boolean isEmpty() {
        return head.get() == tail.get();
    }

    /**
     * Get current size (snapshot, may change immediately).
     */
    public int size() {
        long currentHead = head.get();
        long currentTail = tail.get();
        return (int)(currentHead - currentTail);
    }

    /**
     * Get buffer capacity.
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Wait-free drain operation for single consumer.
     * Processes all available items with the given consumer.
     */
    public int drain(java.util.function.Consumer<T> consumer) {
        int count = 0;
        T item;
        while ((item = poll()) != null) {
            consumer.accept(item);
            count++;
        }
        return count;
    }

    /**
     * Batch offer operation for better throughput.
     */
    public int offerBatch(T[] items, int offset, int length) {
        int offered = 0;
        for (int i = 0; i < length; i++) {
            if (!offer(items[offset + i])) {
                break;
            }
            offered++;
        }
        return offered;
    }

    private static int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Example usage and benchmarking.
     */
    public static void main(String[] args) throws InterruptedException {
        final int BUFFER_SIZE = 1024 * 1024;
        final int NUM_ITEMS = 10_000_000;
        final int NUM_PRODUCERS = 4;
        final int NUM_CONSUMERS = 4;

        RingBuffer<Integer> buffer = new RingBuffer<>(BUFFER_SIZE);
        AtomicLong itemsProduced = new AtomicLong(0);
        AtomicLong itemsConsumed = new AtomicLong(0);

        long startTime = System.nanoTime();

        // Create producer threads
        Thread[] producers = new Thread[NUM_PRODUCERS];
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                int produced = 0;
                while (itemsProduced.incrementAndGet() <= NUM_ITEMS) {
                    while (!buffer.offer(producerId)) {
                        Thread.yield();
                    }
                    produced++;
                }
                System.out.printf("Producer %d produced %d items%n", producerId, produced);
            });
            producers[i].start();
        }

        // Create consumer threads
        Thread[] consumers = new Thread[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            final int consumerId = i;
            consumers[i] = new Thread(() -> {
                int consumed = 0;
                long sum = 0;
                while (itemsConsumed.get() < NUM_ITEMS) {
                    Integer value = buffer.poll();
                    if (value != null) {
                        itemsConsumed.incrementAndGet();
                        sum += value;
                        consumed++;
                    } else {
                        Thread.yield();
                    }
                }
                System.out.printf("Consumer %d consumed %d items, sum=%d%n",
                        consumerId, consumed, sum);
            });
            consumers[i].start();
        }

        // Wait for completion
        for (Thread t : producers) t.join();
        for (Thread t : consumers) t.join();

        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1_000_000_000.0;
        double throughput = NUM_ITEMS / seconds;

        System.out.printf("%nProcessed %d items in %.2f seconds%n", NUM_ITEMS, seconds);
        System.out.printf("Throughput: %.2f million ops/sec%n", throughput / 1_000_000);
        System.out.printf("Final buffer size: %d%n", buffer.size());
    }
}

/**
 * Alternative implementation using AtomicReferenceArray for simpler design.
 * This version trades some performance for simplicity and doesn't require padding.
 */
class SimpleRingBuffer<T> {
    private final AtomicReferenceArray<T> buffer;
    private final AtomicLong head = new AtomicLong(0);
    private final AtomicLong tail = new AtomicLong(0);
    private final int capacity;
    private final int mask;

    public SimpleRingBuffer(int capacity) {
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
        this.buffer = new AtomicReferenceArray<>(this.capacity);
    }

    public boolean offer(T value) {
        long currentHead = head.get();
        long currentTail = tail.get();

        if (currentHead - currentTail >= capacity) {
            return false; // Full
        }

        int index = (int)(currentHead & mask);
        if (buffer.compareAndSet(index, null, value)) {
            head.lazySet(currentHead + 1);
            return true;
        }
        return false;
    }

    public T poll() {
        long currentTail = tail.get();
        long currentHead = head.get();

        if (currentTail >= currentHead) {
            return null; // Empty
        }

        int index = (int)(currentTail & mask);
        T value = buffer.getAndSet(index, null);
        if (value != null) {
            tail.lazySet(currentTail + 1);
        }
        return value;
    }

    private static int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}

