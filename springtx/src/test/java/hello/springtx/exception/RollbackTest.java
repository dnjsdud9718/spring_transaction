package hello.springtx.exception;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

/*
    @Transactional 예외 발생 시 롤백/커밋
    스프링 트랜잭션 AOP는 기본적으로 에외의 종류에 따라 롤백하거나 커밋한다.
    기본설정
    checked(Exception 하위) 예외인 경우 -> commit
    unchecked(RuntimeException 하위) 예외인 경우 -> rollback
    만약, checked 예외 발생 시 롤백을 하고 싶다면 rollbackFor를 지정하면 된다.

    스프링은 기본적으로
    체크예외는 비즈니스 의미가 있을 때 사용(기본 설정 커밋),
    언체크에외는 복구가 불가능한 예외로 간주한다(기본 설정 롤백).

    비지니스 의미 : 주문 결제 시, 잔고가 부족하면 주문 데이터 저장하고, 결제 상태를 '대기'로 처리
    복구 불가능한 예외 : 시스템에서 발생하는 예외(데이터베이스 접근,  sql 문법, 네트워크 통신)
 */

@Slf4j
@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService rollbackService;

    @Test
    void isProxy() {
        log.info("rollbackService = {}", rollbackService.getClass());
    }

    @Test
    void runtimeException() {
        Assertions.assertThatThrownBy(() -> rollbackService.runtimeException())
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {
        Assertions.assertThatThrownBy(() -> rollbackService.checkedException())
            .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor() {
        Assertions.assertThatThrownBy(() -> rollbackService.rollbackFor())
            .isInstanceOf(MyException.class);
    }


    @TestConfiguration
    static class RollbackTestConfig {

        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }

    }

    @Slf4j
    static class RollbackService {

        // 런타임 예외 발생: 롤백
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        // 체크 예외 발생 : 커밋
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        // 체크 예외 rollbackFor 지정 : 롤백
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }
    }

    // checked Exception
    static class MyException extends Exception {


    }
}
