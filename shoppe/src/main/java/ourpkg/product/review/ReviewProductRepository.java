package ourpkg.product.review;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ourpkg.product.Product;

public interface ReviewProductRepository extends JpaRepository<Product, Integer> {

	// 管理員查詢所有商品：包含預加載的關聯實體
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.isDeleted = false")
	List<Product> findAllProductsForAdmin();

	// 管理員按店鋪ID查詢商品
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.isDeleted = false")
	List<Product> findProductsByShopIdForAdmin(@Param("shopId") Integer shopId);

	// 管理員按一級分類查詢商品
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.category1.id = :category1Id AND p.isDeleted = false")
	List<Product> findProductsByCategory1IdForAdmin(@Param("category1Id") Integer category1Id);

	// 管理員按二級分類查詢商品
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.category2.id = :category2Id AND p.isDeleted = false")
	List<Product> findProductsByCategory2IdForAdmin(@Param("category2Id") Integer category2Id);

	// 管理員按店鋪ID和一級分類查詢商品
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category1.id = :category1Id AND p.isDeleted = false")
	List<Product> findProductsByShopIdAndCategory1IdForAdmin(
	    @Param("shopId") Integer shopId, 
	    @Param("category1Id") Integer category1Id
	);

	// 管理員按店鋪ID和二級分類查詢商品
	@EntityGraph(attributePaths = { "shop", "shop.user", "productImages", "skuList", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category2.id = :category2Id AND p.isDeleted = false")
	List<Product> findProductsByShopIdAndCategory2IdForAdmin(
	    @Param("shopId") Integer shopId, 
	    @Param("category2Id") Integer category2Id
	);

	// 管理員分頁查詢所有商品
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	Page<Product> findByIsDeletedFalse(Pageable pageable);

	// 管理員分頁查詢店鋪商品
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	Page<Product> findByShop_ShopIdAndIsDeletedFalse(Integer shopId, Pageable pageable);

	// 管理員分頁查詢分類商品
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	Page<Product> findByCategory1_IdAndIsDeletedFalse(Integer category1Id, Pageable pageable);

	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	Page<Product> findByCategory2_IdAndIsDeletedFalse(Integer category2Id, Pageable pageable);

	// 管理員模糊查詢商品名稱
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.productName LIKE %:keyword% AND p.isDeleted = false")
	Page<Product> findByProductNameContainingForAdmin(@Param("keyword") String keyword, Pageable pageable);

	// 管理員按商品狀態查詢
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.active = :active AND p.isDeleted = false")
	Page<Product> findByActiveStatusForAdmin(@Param("active") Boolean active, Pageable pageable);

	// 管理員按審核狀態查詢
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.reviewStatus = :reviewStatus AND p.isDeleted = false")
	Page<Product> findByReviewStatusForAdmin(@Param("reviewStatus") Boolean reviewStatus, Pageable pageable);

	// 管理員按多條件查詢: 審核狀態 + 上架狀態
	@EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
	@Query("SELECT p FROM Product p WHERE p.active = :active AND p.reviewStatus = :reviewStatus AND p.isDeleted = false")
	Page<Product> findByActiveAndReviewStatusForAdmin(
	    @Param("active") Boolean active, 
	    @Param("reviewStatus") Boolean reviewStatus, 
	    Pageable pageable
	);
	
	 // 根據店鋪ID和一級分類分頁查詢商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category1.id = :category1Id AND p.isDeleted = false")
    Page<Product> findByShop_ShopIdAndCategory1_IdAndIsDeletedFalse(
        @Param("shopId") Integer shopId, 
        @Param("category1Id") Integer category1Id, 
        Pageable pageable
    );

    // 根據店鋪ID和二級分類分頁查詢商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.shop.shopId = :shopId AND p.category2.id = :category2Id AND p.isDeleted = false")
    Page<Product> findByShop_ShopIdAndCategory2_IdAndIsDeletedFalse(
        @Param("shopId") Integer shopId, 
        @Param("category2Id") Integer category2Id, 
        Pageable pageable
    );
    
 // 使用子查詢代替 JOIN，避免 MultipleBagFetchException

    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND " +
           "EXISTS (SELECT 1 FROM Sku s WHERE s.product = p AND s.price >= :minPrice AND s.price <= :maxPrice)")
    Page<Product> findByPriceRangeForAdmin(
        @Param("minPrice") BigDecimal minPrice, 
        @Param("maxPrice") BigDecimal maxPrice, 
        Pageable pageable
    );

    // 進階組合搜尋也同樣使用子查詢
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE " +
    		"(:keyword IS NULL OR p.productName LIKE CONCAT('%', :keyword, '%')) AND "+
           "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR p.createdAt <= :endDate) AND " +
           "(:shopId IS NULL OR p.shop.shopId = :shopId) AND " +
           "(:category1Id IS NULL OR p.category1.id = :category1Id) AND " +
           "(:category2Id IS NULL OR p.category2.id = :category2Id) AND " +
           "(:active IS NULL OR p.active = :active) AND " +
           "(:reviewStatus IS NULL OR p.reviewStatus = :reviewStatus) AND " +
           "(:minPrice IS NULL OR :maxPrice IS NULL OR " +
           "  EXISTS (SELECT 1 FROM Sku s WHERE s.product = p " +
           "          AND (:minPrice IS NULL OR s.price >= :minPrice) " +
           "          AND (:maxPrice IS NULL OR s.price <= :maxPrice)" +
           ")) AND " +
           "p.isDeleted = false")
    Page<Product> findByAdvancedCriteriaForAdmin(
        @Param("keyword") String keyword,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("startDate") Date startDate,
        @Param("endDate") Date endDate,
        @Param("shopId") Integer shopId,
        @Param("category1Id") Integer category1Id,
        @Param("category2Id") Integer category2Id,
        @Param("active") Boolean active,
        @Param("reviewStatus") Boolean reviewStatus,
        Pageable pageable
    );
    // 根據創建日期範圍搜尋商品
    @EntityGraph(attributePaths = { "shop", "productImages", "category1", "category2" })
    @Query("SELECT p FROM Product p WHERE " + 
           "p.createdAt >= :startDate AND p.createdAt <= :endDate AND p.isDeleted = false")
    Page<Product> findByCreatedDateRangeForAdmin(
        @Param("startDate") Date startDate, 
        @Param("endDate") Date endDate, 
        Pageable pageable
    );

    
    
}
