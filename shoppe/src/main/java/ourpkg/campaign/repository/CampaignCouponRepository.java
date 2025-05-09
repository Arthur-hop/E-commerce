package ourpkg.campaign.repository;

import ourpkg.campaign.entity.CampaignCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignCouponRepository extends JpaRepository<CampaignCoupon, Integer> {
    
    // 查詢特定活動中的所有優惠券
    List<CampaignCoupon> findByCampaignId(Integer campaignId);
    
    // 查詢特定優惠券在哪些活動中
    List<CampaignCoupon> findByCouponId(Integer couponId);
    
 // 查詢特定活動中特定優惠券 - 檢查是否正確使用關係
    Optional<CampaignCoupon> findByCampaignIdAndCouponId(Integer campaignId, Integer couponId);
    
    // 查詢所有還有剩餘數量的優惠券
    @Query("SELECT cc FROM CampaignCoupon cc WHERE cc.campaignId = :campaignId AND cc.remainingQuantity > 0")
    List<CampaignCoupon> findAvailableCouponsInCampaign(@Param("campaignId") Integer campaignId);
    
 // 查詢特定活動中未被標記為已領取的優惠券 - 可能需要修改
    @Query("SELECT cc FROM CampaignCoupon cc JOIN cc.coupon c WHERE cc.campaignId = :campaignId AND c.redeemed = false AND cc.remainingQuantity > 0")
    List<CampaignCoupon> findUnredeemedCouponsInCampaign(@Param("campaignId") Integer campaignId);
    
    // 使用優惠券（減少剩餘數量）
    @Modifying
    @Transactional
    @Query("UPDATE CampaignCoupon cc SET cc.remainingQuantity = cc.remainingQuantity - 1, cc.updatedAt = CURRENT_TIMESTAMP WHERE cc.campaign.campaignId = :campaignId AND cc.coupon.couponId = :couponId AND cc.remainingQuantity > 0")
    int decreaseCouponQuantity(@Param("campaignId") Integer campaignId, @Param("couponId") Integer couponId);
    
    // 批量更新優惠券數量
    @Modifying
    @Transactional
    @Query("UPDATE CampaignCoupon cc SET cc.totalQuantity = :totalQuantity, cc.remainingQuantity = :remainingQuantity, cc.updatedAt = CURRENT_TIMESTAMP WHERE cc.campaignCouponId = :campaignCouponId")
    void updateCouponQuantity(@Param("campaignCouponId") Integer campaignCouponId, @Param("totalQuantity") Integer totalQuantity, @Param("remainingQuantity") Integer remainingQuantity);
    
    // 查詢活動中剩餘數量最多的優惠券
    @Query("SELECT cc FROM CampaignCoupon cc WHERE cc.campaign.campaignId = :campaignId ORDER BY cc.remainingQuantity DESC")
    List<CampaignCoupon> findCouponsOrderByRemainingQuantityDesc(@Param("campaignId") Integer campaignId);
    
    // 檢查優惠券是否可以領取
    @Query("SELECT CASE WHEN COUNT(cc) > 0 THEN true ELSE false END FROM CampaignCoupon cc JOIN cc.coupon c WHERE cc.campaign.campaignId = :campaignId AND cc.coupon.couponId = :couponId AND cc.remainingQuantity > 0 AND c.redeemed = false")
    boolean isCouponAvailableForRedemption(@Param("campaignId") Integer campaignId, @Param("couponId") Integer couponId);
    
    // 刪除活動中的優惠券
    @Modifying
    @Transactional
    void deleteByCampaignIdAndCouponId(Integer campaignId, Integer couponId);
}