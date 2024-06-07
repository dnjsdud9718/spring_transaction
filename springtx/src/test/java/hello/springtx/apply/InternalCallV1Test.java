package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Call;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
    }

    @Test
    void internalCall() {
        callService.internal();
    }

    /*
        여기가 중요
        external() 내에서 internal() 호출했을 때, 트랜잭션이 적용되지 않았다
        external()은 트랜잭션이 없지만, internal()은 있다. 근데 적용이 안된다.

        프록시와 내부 호출 !x100
        1. 클라이언트가 프록시 호출
        2. 트랙잭션 프록시 호출
        3. 트랜잭션 적용 X(external은 Non-Transaction)
        4. 실제 external()호출
        5. this.internal() 호출(프록시를 거치지 않고 대상 객체를 바로 호출하는 문제)
        ** 5번이 문제
        java 문법 : 앞에 무언가 생략되 있으면 this가 생략되어 있다.

        가장 단순하고 실무에서 자주 사용하는 방법
        -> internal()를 클래스로 추출하기
     */
    @Test
    void externalCall() {
        callService.external();
    }
    @TestConfiguration
    static class InternalCallV1TestConfig {

        @Bean
        CallService callService() {
            return new CallService();
        }

    }
    @Slf4j
    static class CallService {

        public void external() {
            log.info("call external");
            printTxInfo();
            internal(); //@Transactional self-invocation (in effect, a method within the target object calling another method of the target object) does not lead to an actual transaction at runtime
        }

        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly={}", readOnly);

        }

    }

}
