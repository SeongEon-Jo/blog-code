자바에서는 스레드 간 동기화 작업을 지원하기 위해 `synchronized`와 `ReentrantLock`을 지원한다.

그 중 `ReentrantLock`은 `synchronized`와 비교해 조금 더 유연한 락 획득 및 관리 방식을 지원한다.

하나씩 살펴보면서 사용법을 익혀보자.

# 공정한 락과 비공정한 락

`ReentrantLock` 인스턴스를 생성할 때는 `fair` 인자를 설정해 락 획득을 "공정"하게 혹은 그렇지 않게 설정할 수 있다.

```java
ReentrantLock fairReentrantLock = new ReentrantLock(true);
ReentrantLock unfairReentrantLock = new ReentrantLock(false);
```
여기서 "공정"하다라는 의미는, 락 획득을 대기 중인 스레드들에 대하여 가장 오래 기다린 스레드에게 먼저 락을 점유하도록 한다는 것을 의미한다.

<img width="806" alt="스크린샷 2024-02-27 오후 10 39 26" src="https://github.com/SeongEon-Jo/blog-code/assets/62459414/c55a9e5c-94ad-4354-bd2d-3e1056ae2924">

`fair`인자 여부에 따라 아예 다른 인스턴스를 생성해주는 것을 확인할 수 있다.

# 잠금 방식
`ReentrantLock`은 잠금을 위해 크게 두 가지 메서드를 지원한다.

### 1. `lock()`
기본적인 잠금 방식이다.

만약 `lock()` 호출 시 다른 스레드에서 락을 소유 중이라면 현재 스레드는 잠시 블락되고 락을 획득할 수 있을 때까지 대기한다.

`synchronized`를 사용했을 때와 유사한 형태라고 이해하면 된다.

사용법은 아래를 참고할 수 있다.
```java
public class ReentrantLockBookCountService {

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private int count;
    
    public void decreaseCount() {
        try {
            reentrantLock.lock();
            this.count -=1;
        } finally {
            reentrantLock.unlock();
        }
    }
}
```

### 2. `tryLock()`
`tryLock()`은 `lock()`과 다르게 만약 다른 스레드에 의해 락이 점유되고 있다면 락 획득을 실패처리한다.

<img width="590" alt="스크린샷 2024-02-28 오후 8 42 01" src="https://github.com/SeongEon-Jo/blog-code/assets/62459414/fbab1cd7-f8c8-4f71-a071-1a867f1d93b7">


이에 따라, 리턴값에서도 확인할 수 있듯 락 획득 성공 혹은 실패 여부를 `boolean`타입으로 반환한다.

사용법은 아래와 같다.
```java
public class ReentrantLockBookCountService {

    private final ReentrantLock reentrantLock = new ReentrantLock();
    private int count;
    
    public void decreaseCount() {
        if (reentrantLock.tryLock()) {
            try {
                this.count += 1;
            } finally {
                reentrantLock.unlock();
            }
        } else {
            // 락 획득 실패 시 처리 작업
        }
    }
}
```
따라서, 동일한 테스트 코드를 돌렸을 때 `tryLock()`을 사용하면 `lock()`을 사용했을때와 다르게 간헐적으로 테스트에 실패하는 케이스가 발생한다.
```java
@Test
void decreaseTest() throws InterruptedException {
    bookCountService.setCount(100);

    Runnable decreaseCount = () -> bookCountService.decreaseCount();

    concurrentTest(100, decreaseCount);

    assertThat(bookCountService.getCount()).isEqualTo(0);
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
```

<img width="517" alt="스크린샷 2024-02-28 오후 8 53 07" src="https://github.com/SeongEon-Jo/blog-code/assets/62459414/c048d34d-0cc8-41ea-8441-cbbc72721f5a">

아래 코드처럼 `tryLock()`에 타임아웃을 설정하는 것 또한 가능하다.

이를 사용하면 지정된 타임아웃 시간동안 락 획득을 시도할 것이고, 타임아웃이 경과하면 그때서야 실패 처리시킬 것이다.

```java
public void decreaseCount() {
        if (reentrantLock.tryLock(5, TimeUnit.SECONDS)) { // 5초만 동안 락 획득 시도
            try {
                this.count += 1;
            } finally {
                reentrantLock.unlock();
            }
        } else {
            // 락 획득 실패 시 처리 작업
        }
    }
```

이와 같이 만약 스레드가 락 획득을 무한정 기다리게 하고 싶지 않다면 `tryLock()`을 통해 조금 더 유연한 처리가 가능하도록 할 수 있다.

### `tryLock()` 주의점
`tryLock()` 메서드에 적한 자바독을 확인해보면 다음과 같은 문구를 확인할 수 있다.

```java
     * Even when this lock has been set to use a
     * fair ordering policy, a call to {@code tryLock()} <em>will</em>
     * immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting for this lock, then use
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS)}
     * which is almost equivalent (it also detects interruption).
```
간단하게 해석해보자면, `tryLock()`은 락이 공정한 순서로 획득되도록(fair ordering policy) 설정되었더라도 다른 스레드들이 락을 기다리는 여부와 관계없이 락 획득이 가능하면 바로 락을 획득한다.

이러한 방식은 공정성을 해치는 일이지만 특정 상황에서는 유용할 수 있다.

만약 `tryLock()`을 사용하면서 공정성도 유지하고자 한다면 임의로 타임아웃을 설정해줘야한다. (0초로도 설정이 가능하다!)
