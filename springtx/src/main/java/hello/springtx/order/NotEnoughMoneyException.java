package hello.springtx.order;


// Exception을 상속 받았기 때문에 checkedException
// 체크드 예외 발생 시 스프링 트랜잭션 AOP 기본 설정에 의해 커밋이 호출된다.
public class NotEnoughMoneyException extends Exception {

    public NotEnoughMoneyException(String message) {
        super(message);
    }
}
