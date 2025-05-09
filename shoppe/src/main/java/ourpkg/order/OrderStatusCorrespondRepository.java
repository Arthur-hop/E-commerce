package ourpkg.order;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusCorrespondRepository extends JpaRepository<OrderStatusCorrespond, Integer> {
    Optional<OrderStatusCorrespond> findByName(String name);

}
