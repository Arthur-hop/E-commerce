//package ourpkg.product.oldEntityAndRepo;
//
//import java.math.BigDecimal;
//import java.util.List;
//import java.util.Optional;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import ourpkg.shop.Shop;
//
//public interface ProductRepository extends JpaRepository<Product, Integer> {
//
//	Optional<Product> findByProductId(Integer productId);
//
//	boolean existsByCategory2_Id(Integer id);
//
//	boolean existsByProductId(Integer productId);
//
//	List<Product> findByShop_ShopId(Integer shopId);
//
//	boolean existsByCategory1_IdAndCategory2_Id(Integer category1Id, Integer category2Id);
//
//	List<Product> findByCategory1_IdAndCategory2_Id(Integer category1Id, Integer category2Id);
//
//	List<Product> findByShop_ShopIdAndCategory1_Id(Integer shopId, Integer category1Id);
//
//	List<Product> findByShop_ShopIdAndCategory2_Id(Integer shopId, Integer category2Id);
//
//	List<Product> findByShop_ShopIdAndCategory1_IdAndCategory2_Id(Integer shopId, Integer category1Id,
//			Integer category2Id);
//
//	List<Product> findByProductNameContaining(String keyword);
//
//	List<Product> findByDescriptionContaining(String keyword);
//
//	List<Product> findByProductIdIn(List<Integer> productIds);
//	
//	//商店頁面 計算商店有幾筆商品
//		Integer countByShop(Shop shop);
//
//}
