package ourpkg.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentStatusRepository extends JpaRepository<PaymentStatus, Integer>{
	 /**
     * 根據名稱查找付款狀態
     * 
     * @param name 付款狀態名稱
     * @return 對應的付款狀態
     */
    Optional<PaymentStatus> findByName(String name);
}
