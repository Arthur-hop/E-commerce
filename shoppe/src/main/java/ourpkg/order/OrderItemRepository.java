package ourpkg.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
	// 根據單價查詢訂單項目
	@Query("SELECT oi FROM OrderItem oi WHERE oi.unitPrice = :unitPrice")
	List<OrderItem> findByUnitPrice(@Param("unitPrice") int unitPrice);

	// 如果需要更精確的查詢，可以加上其他條件
	@Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE oi.unitPrice = :unitPrice AND o.orderStatusCorrespond.id = 1")
	List<OrderItem> findByUnitPriceAndStatusPending(@Param("unitPrice") int unitPrice);

	// ============================================by chien
	/**
     * 計算特定商品的總銷售數量
     * 
     * @param productId 商品ID
     * @return 該商品的總銷售數量
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.sku.product.productId = :productId")
    Integer countSoldQuantityByProductId(@Param("productId") Integer productId);
	
    /**
     * 計算特定SKU的總銷售數量
     * 
     * @param skuId SKU ID
     * @return 該SKU的總銷售數量
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.sku.skuId = :skuId")
    Integer countSoldQuantityBySkuId(@Param("skuId") Integer skuId);
    
    /**
     * 計算特定商品的總銷售金額
     * 
     * @param productId 商品ID
     * @return 該商品的總銷售金額
     */
    @Query("SELECT SUM(oi.unitPrice * oi.quantity) FROM OrderItem oi WHERE oi.sku.product.productId = :productId")
    BigDecimal sumSalesAmountByProductId(@Param("productId") Integer productId);
    
    /**
     * 計算特定SKU的總銷售金額
     * 
     * @param skuId SKU ID
     * @return 該SKU的總銷售金額
     */
    @Query("SELECT SUM(oi.unitPrice * oi.quantity) FROM OrderItem oi WHERE oi.sku.skuId = :skuId")
    BigDecimal sumSalesAmountBySkuId(@Param("skuId") Integer skuId);
    
    /**
     * 獲取最近30天的每日銷售數量
     * 
     * @param productId 商品ID
     * @return 日期和銷售數量的映射表
     */
    @Query(value = "SELECT CONVERT(VARCHAR(10), o.created_at, 120) AS sale_date, SUM(oi.quantity) AS daily_count " +
                  "FROM OrderItem oi " +
                  "JOIN [Order] o ON oi.order_id = o.order_id " +
                  "WHERE oi.sku_id IN (SELECT sku_id FROM SKU WHERE product_id = :productId) " +
                  "AND o.created_at >= DATEADD(day, -30, GETDATE()) " +
                  "GROUP BY CONVERT(VARCHAR(10), o.created_at, 120) " +
                  "ORDER BY sale_date ASC", 
           nativeQuery = true)
    Map<String, Integer> getDailySalesCountLast30Days(@Param("productId") Integer productId);
	// ============================================by chien
	
	
}
