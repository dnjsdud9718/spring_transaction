package hello.springtx.propagation;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final LogRepository logRepository;

    public void joinV1(String username) {
        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member);
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        logRepository.save(logMessage);
        log.info("== logRepository 호출 종료 ==");
    }

    // 로그 예외 처리
    public void joinV2(String username) {
        // 로그 저장 실패로 인한 비즈니스 로직이 실패하는 경우가 싫다. -> 예외 잡자

        Member member = new Member(username);
        Log logMessage = new Log(username);

        log.info("== memberRepository 호출 시작 ==");
        memberRepository.save(member); // 트랜잭션 각각 사용하는 예제
        log.info("== memberRepository 호출 종료 ==");

        log.info("== logRepository 호출 시작 ==");
        try {
            logRepository.save(logMessage);
        } catch (RuntimeException e) { // unchecked 예외를 잡아서 처리 -> 정상 흐름 commit 호출 -> but rollback-only 마크 표시 존재-> UnexpectedRollbackException
            log.info("log 저장에 실패했습니다. logMessage={}", logMessage);
            log.info("정상 흐름 반환");
        }
        log.info("== logRepository 호출 종료 ==");
    }

}
