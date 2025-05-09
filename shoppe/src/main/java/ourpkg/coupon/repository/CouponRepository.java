package ourpkg.coupon.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.service.CouponDAO;
import ourpkg.shop.Shop;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer>,CouponDAO{
	 List<Coupon> findByRedeemedFalse();
	 List<Coupon> findByRedeemedTrue();
	 boolean existsByCouponId(Integer couponId);
	 List<Coupon> findByShop_ShopId(Integer shopId);
	 List<Coupon> findByShop(Shop shop);
	 
	// 計算未領取 / 已領取的總數
	    int countByRedeemedFalse();
	    int countByRedeemedTrue();

	    // 依名稱搜尋 + 狀態
	    @Query(value = "SELECT COUNT(*) FROM coupon c WHERE c.redeemed = false AND c.coupon_name LIKE :name", nativeQuery = true)
	    int countByRedeemedFalseAndCouponNameLike(@Param("name") String name);

	    @Query(value = "SELECT COUNT(*) FROM coupon c WHERE c.redeemed = true AND c.coupon_name LIKE :name", nativeQuery = true)
	    int countByRedeemedTrueAndCouponNameLike(@Param("name") String name);

	    // 分頁查詢（未領取 + 無搜尋）
	    @Query(value = "SELECT * FROM coupon c WHERE c.redeemed = 0 ORDER BY c.coupon_id DESC OFFSET :offset ROWS FETCH NEXT :rows ROWS ONLY", nativeQuery = true)
	    List<Coupon> findUnclaimedPage(@Param("offset") int offset, @Param("rows") int rows);

	    // 分頁查詢（未領取 + 有搜尋）
	    @Query(value = "SELECT * FROM coupon c WHERE c.redeemed = 0 AND c.coupon_name LIKE :name ORDER BY c.coupon_id DESC :offset ROWS FETCH NEXT :rows ROWS ONLY", nativeQuery = true)
	    List<Coupon> findUnclaimedPageByName(@Param("name") String name, @Param("offset") int offset, @Param("rows") int rows);

	    // 分頁查詢（已領取 + 無搜尋）
	    @Query(value = "SELECT * FROM coupon c WHERE c.redeemed = 1 ORDER BY c.coupon_id DESC OFFSET :offset ROWS FETCH NEXT :rows ROWS ONLY", nativeQuery = true)
	    List<Coupon> findRedeemedPage(@Param("offset") int offset, @Param("rows") int rows);

	    // 分頁查詢（已領取 + 有搜尋）
	    @Query(value = "SELECT * FROM coupon c WHERE c.redeemed = 1 AND c.coupon_name LIKE :name ORDER BY c.coupon_id DESC OFFSET :offset ROWS FETCH NEXT :rows ROWS ONLY", nativeQuery = true)
	    List<Coupon> findRedeemedPageByName(@Param("name") String name, @Param("offset") int offset, @Param("rows") int rows);
	 
	 // --- Seller Duplicate Check ---
	    /**
	     * 檢查特定商店中是否存在具有相同代碼的有效優惠券。
	     * @param shopId 商店 ID。
	     * @param couponCode 優惠券代碼。
	     * @param currentDate 當前日期，用於檢查有效期限。
	     * @return 若存在則為 true，否則 false。
	     */
	    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
	           "FROM Coupon c " +
	           "WHERE c.shop.shopId = :shopId AND c.couponCode = :couponCode " +
	           "AND c.endDate >= :currentDate") // 僅檢查未過期的
	    boolean existsActiveByShopIdAndCouponCode(@Param("shopId") Integer shopId,
	                                              @Param("couponCode") String couponCode,
	                                              @Param("currentDate") java.util.Date currentDate);

	    /**
	     * 檢查特定商店中是否存在具有相同名稱的有效優惠券。
	     * @param shopId 商店 ID。
	     * @param couponName 優惠券名稱。
	     * @param currentDate 當前日期，用於檢查有效期限。
	     * @return 若存在則為 true，否則 false。
	     */
	    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
	           "FROM Coupon c " +
	           "WHERE c.shop.shopId = :shopId AND c.couponName = :couponName " +
	           "AND c.endDate >= :currentDate") // 僅檢查未過期的
	    boolean existsActiveByShopIdAndCouponName(@Param("shopId") Integer shopId,
	                                              @Param("couponName") String couponName,
	                                              @Param("currentDate") java.util.Date currentDate);


	    // --- Seller View ---
	    /**
	     * 查找特定商店的所有有效（未過期）優惠券。
	     * @param shopId 商店 ID。
	     * @param currentDate 當前日期。
	     * @return 優惠券列表。
	     */
	     List<Coupon> findByShop_ShopIdAndEndDateGreaterThanEqualOrderByEndDateAsc(Integer shopId, java.util.Date currentDate);

	     // === NEW: Monthly Stats Queries ===

	     /**
	      * 查詢指定日期範圍內，每個月新增的優惠券數量。
	      * 注意：這裡使用原生 SQL，因為 JPQL 對日期格式化和分組的支持可能有限或因資料庫而異。
	      * 此範例適用於 SQL Server 的 FORMAT 函數。若使用其他資料庫 (MySQL, PostgreSQL)，
	      * 日期格式化函數可能需要調整 (例如 DATE_FORMAT, TO_CHAR)。
	      * @param startDate 開始日期 (包含)。
	      * @param endDate 結束日期 (不包含，通常設為目標結束月份的下個月第一天)。
	      * @return List<Map<String, Object>>，每個 Map 包含 "monthYear" (yyyy-MM) 和 "count" (數量)。
	      */
	     @Query(value = "SELECT FORMAT(c.created_at, 'yyyy-MM') AS monthYear, COUNT(c.coupon_id) AS count " +
	                    "FROM Coupon c " +
	                    "WHERE c.created_at >= :startDate AND c.created_at < :endDate " +
	                    "GROUP BY FORMAT(c.created_at, 'yyyy-MM') " +
	                    "ORDER BY monthYear ASC", nativeQuery = true)
	     List<Map<String, Object>> findMonthlyCreationCounts(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	     /**
	      * 計算在指定日期仍然有效的優惠券總數。
	      * 有效定義：開始日期 <= 指定日期 AND 結束日期 >= 指定日期
	      * @param targetDate 要計算的目標日期。
	      * @return 在該日期有效的優惠券數量。
	      */
	     @Query("SELECT COUNT(c) FROM Coupon c WHERE c.startDate <= :targetDate AND c.endDate >= :targetDate")
	     long countActiveAtDate(@Param("targetDate") Date targetDate);
	     
	     Optional<Coupon> findByCouponCodeAndShop_ShopId(String couponCode, Integer shopId);

	     Optional<Coupon> findByCouponCode(String couponCode);

}
