package ourpkg.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer>{

	boolean existsByPaymentMethod_Id(Integer id);

	boolean existsByPaymentStatus_Id(Integer id);

	
	 List<Payment> findByOrderOrderId(int orderId);
	 

}
