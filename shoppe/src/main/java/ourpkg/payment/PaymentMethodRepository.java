package ourpkg.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer>{
    Optional<PaymentMethod> findByNameIgnoreCase(String name); // ✅ 新增這一行

}
