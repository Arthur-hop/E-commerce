package ourpkg.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.product.Product;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // 查詢是否已對某個商品留言過（同一訂單項目）
//    boolean existsByUserAndOrderItem(User user, OrderItem orderItem);
	boolean existsByUserUserIdAndOrderItemItemId(Integer userId, Integer itemId);

    // 依商品 ID 查詢評價
    List<Review> findByProduct_ProductId(Integer productId);

    // 依店家 ID 查詢評價
    List<Review> findByShop_ShopId(Integer shopId);

    // 依使用者 ID 查詢評價
    List<Review> findByUser_UserId(Integer userId);

    // 依評價狀態查詢
    List<Review> findByStatus(String status);

    // 依商品名稱查詢評價
    List<Review> findByProductName(String productName);

    // 依商品查詢評價
    List<Review> findByProduct(Product product);
    
    @Query("SELECT r FROM Review r WHERE r.product.productId = :productId")
    List<Review> findByProductId(Integer productId);
    
    @Query("SELECT r FROM Review r WHERE r.shop.shopId = :shopId")
    List<Review> findByShopId(@Param("shopId") Integer shopId);
    
    @Query("SELECT r FROM Review r WHERE r.user.userId = :userId")
    List<Review> findByUserId(Integer productId);

    //查詢平均評分與評論數量
    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.product.id = :productId")
    List<Object[]> getRatingInfo(@Param("productId") Integer productId);
}
