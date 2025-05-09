package ourpkg.campaign.repository;

import ourpkg.campaign.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Integer> {
    
    // 查詢用戶的所有優惠券
    List<UserCoupon> findByUserId(Integer userId);
    
    // 查詢用戶的有效優惠券
    List<UserCoupon> findByUserIdAndStatus(Integer userId, String status);
    
    // 查詢用戶從特定活動中領取的優惠券
    List<UserCoupon> findByUserIdAndCampaignId(Integer userId, Integer campaignId);
    
    // 查詢用戶是否領取了特定優惠券
    Optional<UserCoupon> findByUserIdAndCouponId(Integer userId, Integer couponId);
    
    // 使用優惠券
    @Modifying
    @Transactional
    @Query("UPDATE UserCoupon uc SET uc.status = 'USED', uc.usedDate = :usedDate, uc.order.orderId = :orderId WHERE uc.userCouponId = :userCouponId AND uc.status = 'ACTIVE'")
    int useCoupon(@Param("userCouponId") Integer userCouponId, @Param("usedDate") Date usedDate, @Param("orderId") Integer orderId);
    
    // 查詢特定訂單使用的優惠券
    List<UserCoupon> findByOrderId(Integer orderId);
    
    // 查詢用戶的可用優惠券（按照獲取時間排序）
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.user.userId = :userId AND uc.status = 'ACTIVE' ORDER BY uc.acquiredDate DESC")
    List<UserCoupon> findActiveUserCouponsSortedByDate(@Param("userId") Integer userId);
    
 // 查詢用戶可用於特定商店的優惠券 - 可能需要修改
    @Query("SELECT uc FROM UserCoupon uc JOIN uc.coupon c WHERE uc.userId = :userId AND uc.status = 'ACTIVE' AND c.shop.shopId = :shopId")
    List<UserCoupon> findUserCouponsForShop(@Param("userId") Integer userId, @Param("shopId") Integer shopId);
    
    // 查詢用戶在特定活動中已領取的優惠券數量
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.user.userId = :userId AND uc.campaign.campaignId = :campaignId")
    int countUserCouponsFromCampaign(@Param("userId") Integer userId, @Param("campaignId") Integer campaignId);
    
    // 使優惠券過期
    @Modifying
    @Transactional
    @Query("UPDATE UserCoupon uc SET uc.status = 'EXPIRED' WHERE uc.status = 'ACTIVE' AND uc.coupon.couponId IN (SELECT c.couponId FROM Coupon c WHERE c.endDate < :now)")
    void expireUserCoupons(@Param("now") Date now);
    
    // 判斷用戶是否已經領取了該活動中的優惠券
    @Query("SELECT CASE WHEN COUNT(uc) > 0 THEN true ELSE false END FROM UserCoupon uc WHERE uc.user.userId = :userId AND uc.campaign.campaignId = :campaignId AND uc.coupon.couponId = :couponId")
    boolean hasUserRedeemedCampaignCoupon(@Param("userId") Integer userId, @Param("campaignId") Integer campaignId, @Param("couponId") Integer couponId);
    
    /**
     * 計算特定活動和優惠券的已領取數量
     */
    @Query("SELECT COUNT(uc) FROM UserCoupon uc WHERE uc.campaignId = :campaignId AND uc.couponId = :couponId")
    long countByCampaignIdAndCouponId(@Param("campaignId") Integer campaignId, @Param("couponId") Integer couponId);
    
    boolean existsByUserIdAndCouponIdAndStatus(Integer userId, Integer couponId, String status);

    @Modifying
    @Transactional
    @Query("UPDATE UserCoupon uc SET uc.status = 'USED', uc.usedDate = :usedDate, uc.order.orderId = :orderId WHERE uc.userCouponId = :userCouponId AND uc.status = 'ACTIVE'")
    int useCoupon(@Param("userCouponId") Integer userCouponId, @Param("usedDate") LocalDateTime usedDate, @Param("orderId") Integer orderId);
    
}