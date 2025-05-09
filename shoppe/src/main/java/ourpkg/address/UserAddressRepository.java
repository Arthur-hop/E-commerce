package ourpkg.address;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Integer>{
	List<UserAddress> findByUserUserId(Integer userId);
    Optional<UserAddress> findByUserUserIdAndUserAddressId(Integer userId, Integer userAddressId);
    List<UserAddress> findByUserUserIdAndAddressTypeCorrespondId(Integer userId, Integer addressTypeId);
	Optional<UserAddress> findByUserAddressId(Integer addressId);
}
