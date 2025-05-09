package ourpkg.coupon.controller;

import java.util.List;
import java.util.Optional;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ourpkg.campaign.dto.CouponWithBannerDTO;
import ourpkg.campaign.dto.UserCouponDTO;
import ourpkg.campaign.entity.UserCoupon;
import ourpkg.campaign.repository.UserCouponRepository;
import ourpkg.campaign.service.UserCouponService;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.repository.CouponRepository;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/user-coupons")
@RequiredArgsConstructor
public class UserCouponController {

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponService userCouponService;

    @GetMapping("/available")
    public ResponseEntity<List<UserCouponDTO>> getAvailableCoupons() {
        Integer userId = userService.getCurrentUserId();
        List<UserCoupon> coupons = userCouponRepository.findByUserIdAndStatus(userId, "ACTIVE");

        List<UserCouponDTO> dtos = coupons.stream()
                .map(uc -> new UserCouponDTO(
                        uc.getUserCouponId(),
                        uc.getCoupon().getCouponName(),
                        uc.getCoupon().getDiscountType(),
                        uc.getCoupon().getDiscountValue(),
                        uc.getCoupon().getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        uc.getCoupon().getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/available/shop/{shopId}")
    public ResponseEntity<List<UserCouponDTO>> getAvailableCouponsForShop(@PathVariable Integer shopId) {
        Integer userId = userService.getCurrentUserId();
        List<UserCoupon> coupons = userCouponRepository.findUserCouponsForShop(userId, shopId);

        List<UserCouponDTO> dtos = coupons.stream()
                .map(uc -> new UserCouponDTO(
                        uc.getUserCouponId(),
                        uc.getCoupon().getCouponName(),
                        uc.getCoupon().getDiscountType(),
                        uc.getCoupon().getDiscountValue(),
                        uc.getCoupon().getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        uc.getCoupon().getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                ))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/redeem")
    public ResponseEntity<?> redeemCoupon(
            @RequestParam Integer userId,
            @RequestParam String code,
            @RequestParam Integer shopId) {

        Coupon coupon = couponRepository.findByCouponCode(code)
                .orElseThrow(() -> new RuntimeException("❌ 無效的優惠碼！"));

        if (!coupon.getShop().getShopId().equals(shopId)) {
            throw new RuntimeException("❌ 優惠碼不適用此商店！");
        }

        Optional<UserCoupon> ucOpt = userCouponRepository.findByUserIdAndCouponId(userId, coupon.getCouponId());

        if (ucOpt.isEmpty()) {
            throw new RuntimeException("❌ 尚未領取此優惠券！");
        }

        UserCoupon uc = ucOpt.get();

        if (!"ACTIVE".equalsIgnoreCase(uc.getStatus())) {
            throw new RuntimeException("❌ 優惠券已使用或失效！");
        }

        UserCouponDTO dto = new UserCouponDTO(
                uc.getUserCouponId(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                coupon.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        );

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/with-banner/{userId}")
    public ResponseEntity<List<CouponWithBannerDTO>> getUserCoupons(
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "ALL") String status
    ) {
        List<CouponWithBannerDTO> coupons = userCouponService.getCouponsWithBannerByUser(userId, status);
        return ResponseEntity.ok(coupons);
    }
}
