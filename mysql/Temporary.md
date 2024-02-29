# 서론
쿼리 튜닝을 위해 MySQL 실행 계획을 살피던 중 처음보는 값을 발견했다.

바로 `Start Temporary`와 `End Temporary`가 그 주인공이다.

기존에 `INNER JOIN`을 사용하던 쿼리를 서브쿼리로 변경하던 중 발견했는데, 아마 쿼리 내부에서 사용한 `distinct`와도 연관성이 있어보이는데, 무엇인지 정확하게 알아보자!

## 기존 쿼리
구체적인 요구사항은 1:N(N >= 0) 관계인 A, B 테이블에서 B의 개수가 1개 이상인 A의 ID값을 추출하는 것이었다.

기존엔 INNER JOIN을 통해 아래와 같은 쿼리를 작성하였다.
```sql
SELECT DISTINCT a.id
FROM A a
INNER JOIN B b on a.id = b.a_id
WHERE ...;
```
해당 쿼리의 실행 계획을 살펴보면, A 테이블에서 `Using Temporary`가 발생한다.

위의 쿼리는 쿼리 실행 순서를 고려했을 때, 아래와 같이 실행된다고 예측할 수 있다.

1. A와 B의 `JOIN` 결과를 임시테이블에 생성하여 `FROM`절을 실행
2. `WHERE`절 실행
3. `SELECT`에서 a.id에 대한 `DISTINCT`
