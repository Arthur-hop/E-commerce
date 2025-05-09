package ourpkg.coupon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ourpkg.coupon.entity.CouponApplication;

@Repository
public interface CouponApplicationRepository extends JpaRepository<CouponApplication, Integer> {
	/**
	 * 根據狀態查找優惠券申請，依照申請日期降冪排序。
	 * 
	 * @param status 狀態列表 (例如：List.of("PENDING_CREATE", "PENDING_UPDATE",
	 *               "PENDING_DELETE"))。
	 * @return 優惠券申請列表。
	 */
	List<CouponApplication> findByStatusInOrderByApplicationDateDesc(List<String> statuses); // Changed to use IN

	// 可選：查找特定商店或賣家的申請
	List<CouponApplication> findByRequestedShopIdAndStatusIn(Integer shopId, List<String> statuses);

	 /**
     * 計算符合指定狀態列表的申請數量
     * @param statuses 狀態列表 (例如 ["PENDING_CREATE", "PENDING_UPDATE", "PENDING_DELETE"])
     * @return 符合條件的申請數量
     */
    long countByStatusIn(List<String> statuses);
}
