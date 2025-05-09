// package ourpkg.shop;
//
// import java.util.Optional;
//
// import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;
// import org.springframework.stereotype.Repository;
//
// @Repository
// public interface ShopRepository extends JpaRepository<Shop, Integer> {
//
// Shop findByUserUserId(Integer userId);
//
// @Query("SELECT s FROM Shop s WHERE s.shopId = :shopId")
// Shop findByShopId2(@Param("shopId") Integer shopId);
//
// Optional<Shop> findByShopId(Integer shopId);
//
// public interface SellerShopRepository extends JpaRepository<Shop, Integer> {
// Optional<Shop> findByUserUserId(Integer userId);
// }
// 
//
// }
