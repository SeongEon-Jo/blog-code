package com.concurrent.demo;

import org.springframework.stereotype.Service;

@Service
public interface BookCountService {
    void setCount(int count);
    int getCount();
    void decreaseCount();
    void increaseCount();
}
