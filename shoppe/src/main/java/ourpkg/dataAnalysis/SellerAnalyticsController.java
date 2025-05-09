package ourpkg.dataAnalysis;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import ourpkg.dataAnalysis.DTOs.CustomerFrequencyDTO;
import ourpkg.dataAnalysis.DTOs.OrderStatusCountDTO;
import ourpkg.dataAnalysis.DTOs.PaymentMethodCountDTO;
import ourpkg.dataAnalysis.DTOs.SalesDataDTO;
import ourpkg.dataAnalysis.DTOs.SalesForecastDTO;
import ourpkg.dataAnalysis.DTOs.SellerStatisticsDTO;
import ourpkg.dataAnalysis.DTOs.TopProductDTO;

@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerAnalyticsController {

	private final SellerAnalyticsService sellerAnalyticsService;

	/**
	 * 獲取當前賣家的商店ID
	 */
	@GetMapping("/shop/current")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<Map<String, Integer>> getCurrentShopId(Principal principal) {
		Integer userId = Integer.parseInt(principal.getName());
		Integer shopId = sellerAnalyticsService.getShopIdByUserId(userId);
		return ResponseEntity.ok(Map.of("shopId", shopId));
	}

	/**
	 * 獲取賣家商店的統計數據
	 */
	@GetMapping("/shops/{shopId}/analytics/statistics")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<SellerStatisticsDTO> getSellerStatistics(@PathVariable Integer shopId, Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		SellerStatisticsDTO statistics = sellerAnalyticsService.getSellerStatistics(shopId);
		return ResponseEntity.ok(statistics);
	}

	/**
	 * 獲取銷售數據趨勢
	 */
	@GetMapping("/shops/{shopId}/analytics/sales")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<SalesDataDTO> getSalesData(@PathVariable Integer shopId,
			@RequestParam(defaultValue = "month") String timeRange, Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		SalesDataDTO salesData = sellerAnalyticsService.getSalesData(shopId, timeRange);
		return ResponseEntity.ok(salesData);
	}

	/**
	 * 獲取熱銷商品排行
	 */
	@GetMapping("/shops/{shopId}/analytics/top-products")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<List<TopProductDTO>> getTopProducts(@PathVariable Integer shopId,
			@RequestParam(defaultValue = "5") Integer limit, Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		List<TopProductDTO> topProducts = sellerAnalyticsService.getTopProducts(shopId, limit);
		return ResponseEntity.ok(topProducts);
	}

	/**
	 * 獲取訂單狀態分布
	 */
	@GetMapping("/shops/{shopId}/analytics/order-status")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<List<OrderStatusCountDTO>> getOrderStatusDistribution(@PathVariable Integer shopId,
			Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		List<OrderStatusCountDTO> statusCounts = sellerAnalyticsService.getOrderStatusDistribution(shopId);
		return ResponseEntity.ok(statusCounts);
	}

	/**
	 * 獲取客戶購買頻率分布
	 */
	@GetMapping("/shops/{shopId}/analytics/customer-frequency")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<List<CustomerFrequencyDTO>> getCustomerFrequency(@PathVariable Integer shopId,
			Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		List<CustomerFrequencyDTO> frequencyData = sellerAnalyticsService.getCustomerFrequency(shopId);
		return ResponseEntity.ok(frequencyData);
	}

	/**
	 * 獲取付款方式分布
	 */
	@GetMapping("/shops/{shopId}/analytics/payment-methods")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<List<PaymentMethodCountDTO>> getPaymentMethodDistribution(@PathVariable Integer shopId,
			Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		List<PaymentMethodCountDTO> methodCounts = sellerAnalyticsService.getPaymentMethodDistribution(shopId);
		return ResponseEntity.ok(methodCounts);
	}

	/**
	 * 獲取銷售預測數據
	 */
	@GetMapping("/shops/{shopId}/analytics/forecast")
	@PreAuthorize("hasRole('SELLER')")
	public ResponseEntity<SalesForecastDTO> getSalesForecast(@PathVariable Integer shopId,
			@RequestParam(defaultValue = "30") Integer days, Principal principal) {

		Integer userId = Integer.parseInt(principal.getName());
		// 驗證用戶是否擁有該商店
		sellerAnalyticsService.validateSellerOwnsShop(userId, shopId);

		SalesForecastDTO forecast = sellerAnalyticsService.getSalesForecast(shopId, days);
		return ResponseEntity.ok(forecast);
	}
}