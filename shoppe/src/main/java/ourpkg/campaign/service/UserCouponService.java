package ourpkg.campaign.service;

import java.util.List;

import ourpkg.campaign.dto.CouponWithBannerDTO;

public interface UserCouponService {

	List<CouponWithBannerDTO> getCouponsWithBannerByUserId(Integer userId);

	List<CouponWithBannerDTO> getCouponsWithBannerByUser(Integer userId, String status);

}
