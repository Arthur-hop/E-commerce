package ourpkg.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ourpkg.shop.Shop;

public interface ProductShopRepository extends JpaRepository<Shop, Integer> {
	
//	Shop findByUser_UserId(Integer userId);

	 Optional<Shop> findByUser_UserId(Integer userId);
	// Optional<Shop> findByEmail(String email);

	boolean existsByShopId(Integer shopId);

	Optional<Shop> findByShopId(Integer shopId);

	Optional<Shop> findByUserUserId(Integer userId);

	@Query("SELECT s FROM Shop s JOIN FETCH s.user")
	List<Shop> findAllWithUser();

}
