package ourpkg.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderStatusRepository extends JpaRepository<OrderStatusCorrespond, Integer> {

    /**
     * 根據狀態名稱查找訂單狀態
     *
     * @param name 狀態名稱
     * @return 對應的訂單狀態
     */
    Optional<OrderStatusCorrespond> findByName(String name);

}