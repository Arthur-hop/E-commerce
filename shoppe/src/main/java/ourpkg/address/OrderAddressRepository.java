package ourpkg.address;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderAddressRepository extends JpaRepository<OrderAddress, Integer> {
    // 基本的 CRUD 操作由 JpaRepository 提供
}