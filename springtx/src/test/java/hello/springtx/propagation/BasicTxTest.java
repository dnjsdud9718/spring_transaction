package hello.springtx.propagation;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

@Slf4j
@SpringBootTest
public class BasicTxTest {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            log.info("dataSource = {}", dataSource.getClass()); // HikariDataSource
            return new DataSourceTransactionManager(dataSource);
        }

    }

    @Test
    void commit() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 커밋 시작");
        txManager.commit(status);
        log.info("트랜잭션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜잭션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션 롤백 시작");
        txManager.rollback(status);

        log.info("트랜잭션 롤백 완료");
    }

    @Test
    void double_commit() {
        // 트랜잭션 1과 2는 같은 connection 을 사용한다. 하지만 서로 다른 트랜잭션으로 이해해야 한다.
        // 히카리 커넥션 풀에서 커넥션을 획득하면 실제 커넥션을 그대로 반환하는 것이 아니라 내부 관리를 위해 히카리 프록시 커넥션을
        // 반환한다. 물론 내부에는 실제 커넥션이 포함되어 있다. 이 객체의 주소를 확인하면 같은 물리 커넥션을 사용하더라도 커넥션 풀에서
        // 획득한 커넥션을 구분할 수 있다.
        // HikariProxyConnection@1096030628 wrapping conn0
        // HikariProxyConnection@1706518410 wrapping conn0
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션2 커밋");
        txManager.commit(tx2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜잭션1 시작");
        TransactionStatus tx1 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션1 커밋");
        txManager.commit(tx1);

        log.info("트랜잭션2 시작");
        TransactionStatus tx2 = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜잭션2 롤백");
        txManager.rollback(tx2);
    }

    /*
    하나의 트랜잭션을 수행 중인데 내부에서 새로운 트랜잭션이 시작하는 경우,
    스프링은 이해를 돕기 위해 논리 트랜잭션과 물리 트랜잭션이라는 개념을 나눈다.
    논리 트랜잭션들은 하나의 물리 트랜잭션으로 묶인다.
    물리 트랜잭션은 우리가 이해하는 실제 데이터베이스에 적용되는 트랜잭션을 의미.
        실제 커넥션을 통해 트랜잭션을 시작하고, 실제 커넥션을 통해서 커밋, 롤백하는 단위
    논리 트랜잭션은 트랜잭션 매니저를 통해 트랜잭션 사용하는 단위

    manual commit -> set autocommit false

    -- 원칙
    모든 논리 트랜잭션이 커밋되어야 물리 트랜잭션이 커밋된다.
    하나의 논리 트랜잭션이라도 롤백되면 물리 트랜잭션은 롤백된다.
    */
    @Test
    void inner_commit() {
        // 내부 트랜잭션이 외부 트랜잭션에 참여 -> 외부 트랜백션의 범위가 내부 트랜잭션까지 넓어진다.
        // 처음 트랜잭션을 시작한 외부 트랜잭션이 실제 물리 트랜잭션을 관리하도록 한다.
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        // Participating in existing transaction : 기존에 존재하는 트랜잭션에 참여
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner); // 물리 데이터베이스 커밋하지 않는다. 아무 로직을 수행하지 않고 무시하고 넘어간다.

        log.info("외부 트랜잭션 커밋"); // 실제 커밋이 진행된다! DB 커넥션을 통한 커밋이 진행된다.
        txManager.commit(outer);
    }

    @Test
    void outer_rollback() {
        // 외부 트랜잭션 롤백, 내부 트랜잭션 커밋 -> 물리 트랜잭션 롤백(전체 롤백)
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜잭션 롤백");
        txManager.rollback(outer);
    }

    @Test
    void inner_rollback() {
        // 외부 트랜잭션 롤백, 내부 트랜잭션 커밋 -> 물리 트랜잭션 롤백(전체 롤백)
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("내부 트랜잭션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("내부 트랜잭션 롤백");
        // Participating transaction failed - marking existing transaction as rollback-only
        // Setting JDBC transaction [HikariProxyConnection@1863497271 wrapping conn0: url=jdbc:h2:mem:ad9d59d2-6c80-4bd9-b61d-3a354f37fd52 user=SA] rollback-only
        // 마킹 : 내부 트랜잭션을 롤백하면 실제 물리 트랜잭션은 롤백하지 않는다(못한다) -> 대신에 기존 트랜잭션을 롤백 전용(rollback-only)으로 표시
        // 아직 트랜잭션이 끝난 것이 아니기 때문에 실제 롤백을 호출하면 안된다.
        txManager.rollback(inner);

        log.info("외부 트랜잭션 커밋");
        // Global transaction is marked as rollback-only but transactional code requested commit -> 커밋을 호출했지만, 전체 트랜잭션이 롤백 전용으로 표시되어 있어 물리 트랜잭션을 롤백한다.
        // Initiating transaction rollback
        // Rolling back JDBC transaction on Connection [HikariProxyConnection@1863497271 wrapping conn0: url=jdbc:h2:mem:ad9d59d2-6c80-4bd9-b61d-3a354f37fd52 user=SA]
        // Releasing JDBC Connection [HikariProxyConnection@1863497271 wrapping conn0: url=jdbc:h2:mem:ad9d59d2-6c80-4bd9-b61d-3a354f37fd52 user=SA] after transaction
        // UnexpectedRollbackException
        log.info("inner.isRollbackOnly={}", inner.isRollbackOnly()); // true
        log.info("outer.isRollbackOnly={}", outer.isRollbackOnly()); // true
        // txManager.commit(outer); // UnexpectedRollbackException 예외 던진다. : 개발자 입장에서 커밋을 기대했는데 롤백이 되어 버렸다. 예외를 통해 문제를 알려야 한다.

        assertThatThrownBy(() -> txManager.commit(outer))
            .isInstanceOf(UnexpectedRollbackException.class);
    }

    /*
     외부 트랜잭션과 내부 트랜잭션을 완전히 분리해서 사용하는 방법 : REQUIRES_NEW
     별도의 물리 트랜잭션을 사용하는 방법. 커밋과 롤백이 각각 별도로 이뤄지게 된다.
     서로 영향을 주지 않는다.

     참고로 실무에서는 대부분 REQUIRED 사용 -> 아주 가끔 REQUIRED_NEW 사용, 나머지 전파 옵션(SUPPORT, NOT_SUPPORT, MANDATORY, NEVER 등)은 사용하지 않는다.
     */
    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜잭션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction() = {}", outer.isNewTransaction());

        log.info("내부 트랜잭션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW); // default: PROPAGATION_REQUIRED
        // Suspending current transaction, creating new transaction with name [null]
        // 현재 트랜잭션(외부 트랜잭션)을 잠시 미뤄두고 새로운 트랜잭션을 만든다.
        // 커넥션이 다르다.
        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction() = {}", inner.isNewTransaction()); // expected true
        assertThat(inner.isNewTransaction()).isTrue();

        log.info("내부 트랜잭션 롤백");
        txManager.rollback(inner); // expected rollback
        // Resuming suspended transaction after completion of inner transaction
        // 미뤄두었던 기존 트랜잭션을 다시 진행
        log.info("외부 트랜잭션 커밋");
        txManager.commit(outer); // expected commit

    }


    static class hello {
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void requires_new() {
            log.info("requires_new is called");
        }
    }

}
