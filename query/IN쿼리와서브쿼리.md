# 문제상황
개발 중 아래와 같은 요구사항이 들어왔다.

"1:N(N >=0)관계의 테이블 A와 B에 대하여, B가 하나라도 존재하는 모든 A의 데이터를 조회할 수 있게 해달라"

해당 조건을 만족하는 쿼리는 간단했다.

A와 B를 `INNER JOIN` 해주고, 1:N 관계에서 발생하는 A 데이터의 중복을 제거하기 위해 `DISTINCT`를 적용해주면 됐다.

이렇게 완성된 쿼리는 아래와 같았다.

```sql
SELECT DISTINCT a.id
FROM A a
INNER JOIN B b on a.id = b.a_id
WHERE ...;
```
하지만 `JOIN`을 사용하지 않고 위 쿼리와 동일한 결과를 가져올 수 있는 쿼리가 하나 더 있다.

바로 서브 쿼리를 사용하는 것이다.

`JOIN` 절을 이용해 B 테이블 데이터를 가져오는 쿼리를 별도 `SELECT` 쿼리 가져와, 결과값을 A 테이블의 `IN`절에 넣는 방식이다.

해당 쿼리는 아래와 같이 짜여질 수 있다.

```sql
SELECT a.id
FROM A a
WHERE a.id IN (SELECT DISTINCT b.a_id FROM B b)
```
다만 일반적으로 서브쿼리는 안티패턴으로 취급받기도 하고 성능상 문제가 있을 수 있다는 얘기를 많이 들었어서(물론 5.6 버전부터 개선되었다곤 하다만), 최대한 `JOIN`으로 처리하고 하였다.

하지만 두 가지 방식에 대해 각각의 TPS를 확인한 결과 예상치 못한 결과를 확인할 수 있었다.
![tps헤더.png](..%2F..%2F..%2F..%2FDownloads%2Ftps%ED%97%A4%EB%8D%94.png)
![스크린샷 2024-02-29 오전 8.29.12.png](..%2F..%2F..%2F..%2FDownloads%2F%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7%202024-02-29%20%EC%98%A4%EC%A0%84%208.29.12.png)
![스크린샷 2024-03-03 오후 3.47.24.png](..%2F..%2F..%2F..%2FDownloads%2F%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7%202024-03-03%20%EC%98%A4%ED%9B%84%203.47.24.png)
`JOIN`을 사용한 쿼리에서 TPS 282.7을 기록하였으나, 서브 쿼리를 사용한 경우 349.3으로 **약 60**이나 개선된 결과가 나왔다.

서브쿼리를 사용하는 것이 성능상 더 좋지 않다는 것을 증명하기 위한 실험이었는데 결과가 오히려 반대로 나와서 다소 당황스러웠다.

# 디깅

그렇다면 무엇이 위와 같은 결과를 초래했는지 확인해봐야했다.

서브쿼리를 사용하는 것이 `JOIN`을 사용하는 것보다 거의 대부분 느릴 것이라는 것은 유명한 [블로그 글](https://jason-heo.github.io/mysql/2014/05/22/avoid-mysql-in.html)에서 기인했다.

해당 글을 간단히 요약하자면, `IN`절에 서브쿼리가 포함되어있을 때 실제로는 `IN`절 -> `FROM`절의 순서로 쿼리가 실행되는 것이 아니라 `FROM`절이 먼저 실행된다는 것이다.

따라서, `IN`절의 서브쿼리 결과값을 기반으로 A 테이블 값이 필터링되어서 가져와지는 것이 아닌 모든 A 테이블 값들을 불러와 해당 값들을 하나씩 돌면서 서브쿼리로 만들어진 `IN`절에 부합하는 값들을 필터링한다.

따라서 사실상 A 테이블에 대한 풀 스캔이 이루어지므로 성능이 좋지 못하다는 것이다.

그러나 해당 내용은 MySQL 5.5 버전을 기준으로 작성되었으며, 이후 버전들부터 서브쿼리에 대한 최적화 작업이 많이 진행되었기에 해당 내용이 반영된 서브쿼리 수행 방식을 확인해볼 필요가 있었다.

