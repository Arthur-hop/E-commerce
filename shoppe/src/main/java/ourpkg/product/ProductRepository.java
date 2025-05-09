package ourpkg.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ourpkg.shop.Shop;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

	Optional<Product> findByProductId(Integer productId);

	boolean existsByCategory2_Id(Integer id);

	boolean existsByProductId(Integer productId);

	List<Product> findByShop_ShopId(Integer shopId);

	boolean existsByCategory1_IdAndCategory2_Id(Integer category1Id, Integer category2Id);

	List<Product> findByCategory1_IdAndCategory2_Id(Integer category1Id, Integer category2Id);

	List<Product> findByShop_ShopIdAndCategory1_Id(Integer shopId, Integer category1Id);

	List<Product> findByShop_ShopIdAndCategory2_Id(Integer shopId, Integer category2Id);

	List<Product> findByShop_ShopIdAndCategory1_IdAndCategory2_Id(Integer shopId, Integer category1Id,
			Integer category2Id);

	List<Product> findByProductNameContaining(String keyword);

	List<Product> findByDescriptionContaining(String keyword);

	List<Product> findByProductIdIn(List<Integer> productIds);

	// 商店頁面 計算商店有幾筆商品
	Integer countByShop(Shop shop);

	/**
	 * 查詢未刪除的商品
	 */
	Optional<Product> findByProductIdAndIsDeletedFalse(Integer productId);

	/**
	 * 檢查未刪除的商品是否存在
	 */
	boolean existsByProductIdAndIsDeletedFalse(Integer productId);

	
	// 商品前端公開查詢：包含預加載的關聯實體
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findAllPublicProducts();
    
    // 按店鋪ID查詢商品
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findPublicProductsByShopId(@Param("shopId") Integer shopId);
    
    // 按一級分類查詢商品
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.category1.id = :category1Id AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findPublicProductsByCategory1Id(@Param("category1Id") Integer category1Id);
    
    // 按二級分類查詢商品
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.category2.id = :category2Id AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findPublicProductsByCategory2Id(@Param("category2Id") Integer category2Id);
    
    // 按店鋪ID和一級分類查詢商品
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category1.id = :category1Id AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findPublicProductsByShopIdAndCategory1Id(
        @Param("shopId") Integer shopId, 
        @Param("category1Id") Integer category1Id
    );
    
    // 按店鋪ID和二級分類查詢商品
    @EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category2.id = :category2Id AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    List<Product> findPublicProductsByShopIdAndCategory2Id(
        @Param("shopId") Integer shopId, 
        @Param("category2Id") Integer category2Id
    );
    
    // 分頁查詢所有商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    
    // 分頁查詢店鋪商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    Page<Product> findByShop_ShopIdAndIsDeletedFalse(Integer shopId, Pageable pageable);
    
    // 分頁查詢分類商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    Page<Product> findByCategory1_IdAndIsDeletedFalse(Integer category1Id, Pageable pageable);
    
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    Page<Product> findByCategory2_IdAndIsDeletedFalse(Integer category2Id, Pageable pageable);
    
    // 模糊查詢商品名稱
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.productName LIKE %:keyword% AND p.active = true AND p.reviewStatus = true AND p.isDeleted = false")
    Page<Product> findByProductNameContainingAndIsDeletedFalse(@Param("keyword") String keyword, Pageable pageable);

	
		boolean existsByShop_ShopId(Integer shopId);
		
		
		 // 根據 shopId 查找所有產品
	    List<Product> findByShopShopId(Integer shopId);

	    // 使用 JPQL 查詢獲取產品和相關的 SKU、圖片等
	    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.skuList LEFT JOIN FETCH p.productImages WHERE p.shop.shopId = :shopId")
	    List<Product> findProductsWithDetailsByShopId(@Param("shopId") Integer shopId);

}
