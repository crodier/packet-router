package other;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionVariable<T> {
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();

    private final int capacity;
    private final List<T> collection;  // Make final

    public ConditionVariable(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.collection = new ArrayList<>(capacity);
    }

    // Must be called with lock held
    private boolean isFull() {
        return collection.size() == capacity;
    }

    // Must be called with lock held
    private boolean isEmpty() {
        return collection.isEmpty();
    }

    public void put(T item) throws InterruptedException {
        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }

        lock.lock();
        try {
            while (isFull()) {
                notFull.await();
            }
            collection.add(item);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (isEmpty()) {
                notEmpty.await();
            }
            T item = collection.remove(0);  // Remove from front (FIFO)
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    // Optional: non-blocking versions
    public boolean offer(T item) {
        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }

        lock.lock();
        try {
            if (isFull()) {
                return false;
            }
            collection.add(item);
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public T poll() {
        lock.lock();
        try {
            if (isEmpty()) {
                return null;
            }
            T item = collection.remove(0);
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return collection.size();
        } finally {
            lock.unlock();
        }
    }
}