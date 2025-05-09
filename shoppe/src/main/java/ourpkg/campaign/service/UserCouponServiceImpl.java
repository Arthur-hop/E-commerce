package ourpkg.campaign.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.campaign.dto.CouponWithBannerDTO;
import ourpkg.campaign.entity.UserCoupon;
import ourpkg.campaign.repository.UserCouponRepository;

@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final UserCouponRepository userCouponRepository;

    @Override
    public List<CouponWithBannerDTO> getCouponsWithBannerByUserId(Integer userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);
        return mapToDTO(userCoupons);
    }

    @Override
    public List<CouponWithBannerDTO> getCouponsWithBannerByUser(Integer userId, String status) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserIdAndStatus(userId, status);
        return mapToDTO(userCoupons);
    }

    private List<CouponWithBannerDTO> mapToDTO(List<UserCoupon> userCoupons) {
        return userCoupons.stream().map(uc -> {
            var coupon = uc.getCoupon();
            var campaign = uc.getCampaign();
            return new CouponWithBannerDTO(
                coupon.getCouponId(),
                coupon.getCouponName(),
                coupon.getCouponCode(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getEndDate(),
                coupon.getDescription(),
                uc.getStatus(),
                campaign != null ? campaign.getBannerImage() : null
            );
        }).collect(Collectors.toList());
    }
}
