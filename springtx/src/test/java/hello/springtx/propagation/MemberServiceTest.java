package hello.springtx.propagation;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LogRepository logRepository;

    /**
     * memberService        @Transactional: OFF
     * memberRepository     @Transactional: ON  commit
     * logRepository        @Transactional: ON  commit
     *
     * memberRepository 와 logRepository 의 @Transactional
     */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";
        // when
        memberService.joinV1(username);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }

    /**
     * memberService        @Transactional: OFF
     * memberRepository     @Transactional: ON  commit
     * logRepository        @Transactional: ON  rollback
     *
     * memberRepository 와 logRepository 의 @Transactional
     *
     * 회원은 저장되지만 회원 이력 로그는 롤백된다. 따라서 데이터 정합성에 문제가 발생할 수 있다.(해결 -> 하나의 트랜잭션으로 묶기)
     */
    @Test
    void outerTxOff_fail() {
        // given
        String username = " 로그예외_outerTxOff_fail";
        // when
        assertThatThrownBy(() -> memberService.joinV1(username))
            .isInstanceOf(RuntimeException.class);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }

    /**
     * memberService        @Transactional: ON
     * memberRepository     @Transactional: OFF
     * logRepository        @Transactional: OFF
     *
     * 정합성 문제 해결 ( 트랜잭션 하나로 묶기)
     * memberRepository 와 logRepository 의 @Transactional 주석처리
     * memberService 의 joinV1() @Transactional 추가
     */
    @Test
    void singleTx_success() {
        // given
        String username = "singleTx_success";
        // when
        memberService.joinV1(username);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }
    /**
     * memberService        @Transactional: ON
     * memberRepository     @Transactional: ON
     * logRepository        @Transactional: ON
     *
     * 트랜잭션 전파 default : REQUIRED
     */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";
        // when
        memberService.joinV1(username);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isPresent());
    }
    /**
     * memberService        @Transactional: ON
     * memberRepository     @Transactional: ON
     * logRepository        @Transactional: ON Exception
     *
     * 트랜잭션 전파 default : REQUIRED
     * logRepository.save(...) -> 예외 발생 : 신규 트랜잭션 X, rollbackOnly 표시
     */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        // when
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> memberService.joinV1(username))
            .isInstanceOf(RuntimeException.class);

        // then
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
    /**
     * memberService        @Transactional: ON
     * memberRepository     @Transactional: ON
     * logRepository        @Transactional: ON Exception
     *
     * 트랜잭션 전파 default : REQUIRED
     * logRepository.save(...) -> 예외 발생 : 신규 트랜잭션 X, rollbackOnly 표시
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        // when
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> memberService.joinV2(username))
            .isInstanceOf(UnexpectedRollbackException.class);

        // then
        assertTrue(memberRepository.find(username).isEmpty());
        assertTrue(logRepository.find(username).isEmpty());
    }
    /**
     * memberService        @Transactional: ON
     * memberRepository     @Transactional: ON
     * logRepository        @Transactional: ON Exception
     *
     * logRepository : REQUIRED_NEW
     */
    @Test
    void recoverException_success() {
        // given
        String username = "로그예외_recoverException_success";

        // when
        memberService.joinV2(username);

        // then
        assertTrue(memberRepository.find(username).isPresent());
        assertTrue(logRepository.find(username).isEmpty());
    }




}