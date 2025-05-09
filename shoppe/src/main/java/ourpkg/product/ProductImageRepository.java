package ourpkg.product;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Integer> {

	/**
	 * 根據產品ID查詢所有相關圖片
	 */
	List<ProductImage> findByProduct_ProductId(Integer id);

	/**
	 * 根據產品ID查詢主圖片
	 */
	Optional<ProductImage> findByProduct_ProductIdAndIsPrimaryTrue(Integer productId);

	/**
	 * 刪除產品的所有圖片
	 */
	void deleteByProduct_ProductId(Integer id);

	List<ProductImage> findByProduct_ProductIdAndImageIdNotIn(Integer id, List<Integer> deleteImageIds);

	// ============================================================================

	/**
	 * 根據產品ID查詢主圖片路徑
	 */
	@Query("SELECT pi.imagePath FROM ProductImage pi WHERE pi.product.productId = :productId AND pi.isPrimary = true")
	Optional<String> findPrimaryImageByProduct_ProductId(@Param("productId") Integer productId);

	/**
	 * 根據商品ID查詢所有圖片路徑，按顯示順序排序
	 */
	@Query("SELECT pi.imagePath FROM ProductImage pi WHERE pi.product.productId = :productId ORDER BY pi.displayOrder")
	List<String> findAllImagePathsByProduct_ProductId(@Param("productId") Integer productId);

	/**
	 * 根據商品ID查詢第一張圖片路徑 如果有主圖片則返回主圖片，否則返回排序最小的圖片
	 */
	@Query(value = "SELECT TOP 1 pi.[image_path] FROM ProductImage pi " + "WHERE pi.[product_id] = :productId "
			+ "ORDER BY pi.[is_primary] DESC, pi.[display_order]", nativeQuery = true)
	Optional<String> findFirstImageByProduct_ProductId(@Param("productId") Integer productId);

	/**
	 * 檢查產品是否有圖片
	 */
	boolean existsByProduct_ProductId(Integer productId);

	@Modifying
	@Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.productId = :productId")
	void resetAllPrimaryImages(@Param("productId") Integer productId);

}
