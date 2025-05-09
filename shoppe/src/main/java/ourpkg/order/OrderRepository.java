package ourpkg.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.payment.Payment;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {


	// 根據訂單總金額與狀態 ID 查詢訂單（例如未付款狀態為 ID=1）
//	List<Order> findByTotalPriceAndOrderStatusCorrespond_Id(BigDecimal bigDecimal, Integer statusId);

	List<Order> findByTotalPriceAndOrderStatusCorrespond_Id(BigDecimal totalPrice, Integer statusId);

	
	
	// 買家查詢自己的訂單
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItem oi " +
           "LEFT JOIN FETCH oi.sku s " +
           "LEFT JOIN FETCH s.product p " +  // 確保加載產品資訊
           "LEFT JOIN FETCH o.orderAddressBilling " +
           "LEFT JOIN FETCH o.orderAddressShipping " +
           "WHERE o.user.userId = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findOrdersByUserId(@Param("userId") Integer userId);
    
    @Query("SELECT DISTINCT o FROM Order o " +
    		"LEFT JOIN FETCH o.user u " +
    		"LEFT JOIN FETCH o.orderItem oi " + 
    		"LEFT JOIN FETCH oi.sku s " + 
    		"LEFT JOIN FETCH s.product p " + 
    		"LEFT JOIN FETCH p.shop sh " + 
    		"LEFT JOIN FETCH o.orderAddressBilling " +
    		"LEFT JOIN FETCH o.orderAddressShipping " +
    		"WHERE EXISTS (SELECT 1 FROM OrderItem oi2 WHERE oi2.order = o AND oi2.shop.user.userId = :sellerId) " +
    		"ORDER BY o.createdAt DESC")
    		List<Order> findOrdersByShopOwner(@Param("sellerId") Integer sellerId);
    
    // 管理員查詢所有訂單 (已有產品查詢)
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.user u " +
           "LEFT JOIN FETCH o.orderItem oi " +
           "LEFT JOIN FETCH oi.sku s " +
           "LEFT JOIN FETCH s.product p " +  // 已有這行，確保加載產品資訊
           "LEFT JOIN FETCH o.orderAddressBilling " +
           "LEFT JOIN FETCH o.orderAddressShipping " +
           "ORDER BY o.createdAt DESC")
    List<Order> findAllOrdersWithItems();
    
    //訂單評價
    @Query("SELECT DISTINCT o FROM Order o " +
    	       "JOIN FETCH o.orderItem oi " +
    	       "JOIN FETCH oi.sku s " +
    	       "JOIN FETCH s.product p " +
    	       "WHERE o.user.userId = :userId AND o.orderStatusCorrespond.name = :status")
    	List<Order> findByUserUserIdAndStatus(@Param("userId") Integer userId, @Param("status") String status);

    
    // 🔔 賣家：查詢待出貨訂單（狀態為「待付款」、「已付款」、「備貨中」）
    @Query("SELECT COUNT(o) FROM Order o " +
           "JOIN o.orderItem oi " +
           "JOIN oi.sku s " +
           "JOIN s.product p " +
           "JOIN p.shop sh " +
           "WHERE sh.user.userId = :sellerId AND o.orderStatusCorrespond.name IN ('待付款', '已付款', '備貨中')")
    int countPendingOrdersForSeller(@Param("sellerId") Integer sellerId);

    // 🔔 買家：查詢已出貨訂單（狀態為「配送中」）
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.user.userId = :userId AND o.orderStatusCorrespond.name = '配送中'")
    int countShippedOrdersForUser(@Param("userId") Integer userId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItem WHERE o.orderId = :orderId")
	Optional<Order> findOrderWithItems(@Param("orderId") Integer orderId);
	
	@Query("SELECT o FROM Order o " +
		       "LEFT JOIN FETCH o.orderItem " +
		       "LEFT JOIN FETCH o.orderAddressBilling " +
		       "LEFT JOIN FETCH o.orderAddressShipping " +
		       "LEFT JOIN FETCH o.payment " +
		       "WHERE o.orderId = :orderId")
		Optional<Order> findFullOrderById(@Param("orderId") Integer orderId);


}