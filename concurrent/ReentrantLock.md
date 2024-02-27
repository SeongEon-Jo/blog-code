자바에서는 스레드 간 동기화 작업을 지원하기 위해 `synchronized`와 `ReentrantLock`을 지원한다.

그 중 `ReentrantLock`은 `synchronized`와 비교해 조금 더 유연한 락 획득 및 관리 방식을 지원한다.

하나씩 살펴보면서 사용법을 익혀보자.

`ReentrantLock` 인스턴스를 생성할 때는 `fair` 인자를 설정해 락 획득을 "공정"하게 혹은 그렇지 않게 설정할 수 있다.

```java
ReentrantLock fairReentrantLock = new ReentrantLock(true);
ReentrantLock unfairReentrantLock = new ReentrantLock(false);
```
여기서 "공정"하다라는 의미는, 락 획득을 대기 중인 스레드들에 대하여 가장 오래 기다린 스레드에게 먼저 락을 점유하도록 한다는 것을 의미한다.
![스크린샷 2024-02-27 오후 10.39.26.png](..%2F..%2F..%2F%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7%202024-02-27%20%EC%98%A4%ED%9B%84%2010.39.26.png)
`fair`인자 여부에 따라 아예 다른 인스턴스를 생성해주는 것을 확인할 수 있다.

