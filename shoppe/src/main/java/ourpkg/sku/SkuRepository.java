package ourpkg.sku;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkuRepository extends JpaRepository<Sku, Integer> {

	List<Sku> findAllByProduct_ProductId(Integer productId);

	Optional<Sku> findByProduct_ProductIdAndSkuId(Integer productId, Integer skuId);

	List<Sku> findByProduct_ProductId(Integer productId);

	Optional<Sku> findBySkuId(Integer id);

	boolean existsBySkuId(Integer id);

	void deleteBySkuId(Integer id);

	@Modifying
	@Query("DELETE FROM Sku s WHERE s.skuId = :id")
	void deleteSkuById(@Param("id") Integer id);

	// 新增的軟刪除相關方法
	List<Sku> findByIsDeletedFalse();

	Optional<Sku> findBySkuIdAndIsDeletedFalse(Integer id);

	boolean existsBySkuIdAndIsDeletedFalse(Integer id);

	boolean existsByProduct_ProductIdAndIsDeletedFalse(Integer id);

	/**
	 * 檢查商品是否存在SKU
	 */
	boolean existsByProduct_ProductId(Integer productId);

	/**
	 * 檢查商品下是否存在指定規格的SKU
	 */
	boolean existsByProduct_ProductIdAndSpecPairsAndIsDeletedFalse(Integer productId, String specPairs);

	/**
	 * 檢查商品下是否存在指定規格的其他SKU (排除指定ID)
	 */
	boolean existsByProduct_ProductIdAndSpecPairsAndSkuIdNotAndIsDeletedFalse(Integer productId, String specPairs,
			Integer skuId);

	/**
	 * 查詢商品的所有未刪除SKU
	 */
	List<Sku> findByProduct_ProductIdAndIsDeletedFalse(Integer productId);

	/**
	 * 分頁查詢商品的所有未刪除SKU
	 */
	Page<Sku> findByProduct_ProductIdAndIsDeletedFalse(Integer productId, Pageable pageable);

	/**
	 * 查詢指定價格範圍的未刪除SKU
	 */
	List<Sku> findByPriceBetweenAndIsDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice);

	/**
	 * 查詢庫存低於或等於指定閾值的未刪除SKU
	 */
	List<Sku> findByStockLessThanEqualAndIsDeletedFalse(Integer threshold);

	/**
	 * 查詢指定價格範圍內的商品ID
	 */
	@Query("SELECT DISTINCT s.product.productId FROM Sku s WHERE s.price BETWEEN :minPrice AND :maxPrice AND s.isDeleted = false")
	List<Integer> findProductIdsByPriceRange(@Param("minPrice") BigDecimal minPrice,
			@Param("maxPrice") BigDecimal maxPrice);


	List<Sku> findByProduct_Shop_ShopId(Integer shopId);

	
}
