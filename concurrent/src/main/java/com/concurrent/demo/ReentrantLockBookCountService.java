package com.concurrent.demo;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockBookCountService implements BookCountService {

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private int count;

    @Override
    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public int getCount() {
        return this.count;
    }

    @Override
    public void decreaseCount() {
        try {
            reentrantLock.lock();
            this.count -=1;
        } finally {
            reentrantLock.unlock();
        }
    }

    @Override
    public void increaseCount() {
        if (reentrantLock.tryLock()) {
            try {
                this.count += 1;
            } finally {
                reentrantLock.unlock();
            }
        } else {

        }
    }
}
