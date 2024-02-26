package com.concurrent.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BookCountServiceTest {

    @Autowired
    BookCountService bookCountService;

    @Test
    void decreaseTest() throws InterruptedException {
        bookCountService.setCount(100);

        Runnable decreaseCount = () -> bookCountService.decreaseCount();

        concurrentTest(100, decreaseCount);

        assertThat(bookCountService.getCount()).isEqualTo(0);
    }

    @Test
    void increaseTest() throws InterruptedException {
        Runnable increaseCount = () -> bookCountService.increaseCount();

        concurrentTest(100, increaseCount);

        assertThat(bookCountService.getCount()).isEqualTo(100);
    }

    void concurrentTest(int executeCount, Runnable methodToTest) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch countDownLatch = new CountDownLatch(executeCount);

        for (int i = 0; i < executeCount; i++) {
            executorService.submit(() -> {
                methodToTest.run();
                countDownLatch.countDown();
            });
        }

        countDownLatch.await();
    }
}
