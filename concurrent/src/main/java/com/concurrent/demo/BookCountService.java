package com.concurrent.demo;

import org.springframework.stereotype.Service;

@Service
public class BookCountService {

    private int count;

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return this.count;
    }

    public synchronized void decreaseCount() {
        this.count -= 1;
    }

    public synchronized void increaseCount() {
        this.count += 1;
    }
}
