package hello.springtx.apply;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.Basic;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/*
    @Transactional : 선언적 트랜잭션을 사용하면 DB 트랜잭션을 처리할 수 있다.
    템플릿 콜백 패턴이 적용된다 : 트랜잭션 프록시 -> 실제 서비스 호출 -> 데이터 접근 로직

    트랜잭션 프록시에서 트랜잭션 관련 코드를 처리한다 : 트랜잭션 매니저를 통해 커넥션을 생성하고 관리한다.
    트랜잭션을 적용하기 위해서는 트랜잭션 내 동일한 커넥션을 사용해야 하는데 이를 위해 트랜잭션 동기화 매니저가 커넥션을 보관한다.
    커넥션은 트랜잭션 동기화 매니저를 통해 보관되어 DAO는 동기화 매니저로부터 커넥션을 획득하고 반환한다.

    트랜잭션 매니저는 데이터소스에 의존 한다 == 트랜잭션 매니저는 데이터소스를 주입 받는다

    AOP 기반이기 때문에 트랜잭션이 눈에 보이지 않아 적용 여부를 확인하기 어렵다. 실제 적용되고 있는지 확인해보는 실습

                <------------의존 관계 주입          ---------------> 상속 관계
    <<Client>>                      <<proxy>>                                   <<real>>
                 프록시 참조----->                            실제 참조----->
    txBasicTest                     basicService$$CGLIB                         basiecService

    스프링 컨테이너에는 실제 객체 대신 프록시가 스프링 빈으로 등록되었기 때문에 프록시를 주입한다.
 */
@Slf4j
@SpringBootTest
public class TxBasicTest {

    @Autowired
    BasicService basicService;

    @Test
    void proxyCheck() {
        //BasicService$$EnhancerBySpringCGLIB...
        log.info("aop class={}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Test
    void txTest() {
        // proxy의 메서드가 transaction 대상인지 판단하고, 적용 여부 결정된다(@Transactional)
        basicService.tx();

        basicService.nonTx();

    }

    @TestConfiguration
    static class TxApplyBasicConfig {

        @Bean
        BasicService basicService() {
            return new BasicService();
        }

    }

    @Slf4j
    static class BasicService {

        @Transactional // BasicService에 proxy AOP를 적용되게 만든다.
        public void tx() {
            log.info("call tx");
            // 트랜잭션이 적용되어 있는것인지 맞는지 확인
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }
    }
}
