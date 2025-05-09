package ourpkg.campaign.repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ourpkg.campaign.entity.MarketingCampaign;

@Repository
public interface MarketingCampaignRepository extends JpaRepository<MarketingCampaign, Integer> {
	// 查詢特定商店的所有行銷活動
	List<MarketingCampaign> findByShopId(Integer shopId);
	
	// 查詢特定商店的活躍行銷活動
	List<MarketingCampaign> findByShopIdAndStatus(Integer shopId, String status);
	
	// 查詢在指定時間範圍內活躍的行銷活動
	@Query("SELECT mc FROM MarketingCampaign mc WHERE mc.status = 'ACTIVE' AND mc.startDate <= :now AND mc.endDate >= :now")
	List<MarketingCampaign> findActiveRunningCampaigns(@Param("now") LocalDateTime now);
	
	// 查詢商店在指定時間範圍內活躍的行銷活動 (修正後的查詢)
	@Query("SELECT mc FROM MarketingCampaign mc WHERE mc.shopId = :shopId AND mc.status = 'ACTIVE' AND mc.startDate <= :now AND mc.endDate >= :now")
	List<MarketingCampaign> findShopActiveRunningCampaigns(@Param("shopId") Integer shopId, @Param("now") LocalDateTime now);
	
	// 查詢即將開始的行銷活動
	@Query("SELECT mc FROM MarketingCampaign mc WHERE mc.status = 'ACTIVE' AND mc.startDate > :now ORDER BY mc.startDate ASC")
	List<MarketingCampaign> findUpcomingCampaigns(@Param("now") LocalDateTime now);
	
	// 查詢已結束的行銷活動
	@Query("SELECT mc FROM MarketingCampaign mc WHERE mc.endDate < :now")
	List<MarketingCampaign> findEndedCampaigns(@Param("now") LocalDateTime now);
	
	// 根據活動名稱模糊查詢
	List<MarketingCampaign> findByCampaignNameContaining(String campaignName);
	
	// 更新活動狀態
	@Modifying
	@Query("UPDATE MarketingCampaign mc SET mc.status = :status, mc.updatedAt = CURRENT_TIMESTAMP WHERE mc.campaignId = :campaignId")
	void updateCampaignStatus(@Param("campaignId") Integer campaignId, @Param("status") String status);
	
	// 自動更新過期活動的狀態
	@Modifying
	@Query("UPDATE MarketingCampaign mc SET mc.status = 'ENDED', mc.updatedAt = CURRENT_TIMESTAMP WHERE mc.status = 'ACTIVE' AND mc.endDate < :now")
	void updateExpiredCampaigns(@Param("now") LocalDateTime now);
	
	Optional<MarketingCampaign> findByCampaignIdAndCreatedBy(Integer campaignId, Integer userId);

}