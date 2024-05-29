package hello.springtx.order;

import org.springframework.data.jpa.repository.JpaRepository;

// 스프링 데이터 JPA
public interface OrderRepository extends JpaRepository<Order, Long> {

}
