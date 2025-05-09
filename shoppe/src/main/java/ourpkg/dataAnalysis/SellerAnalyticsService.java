package ourpkg.dataAnalysis;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ourpkg.dataAnalysis.DTOs.CustomerFrequencyDTO;
import ourpkg.dataAnalysis.DTOs.OrderStatusCountDTO;
import ourpkg.dataAnalysis.DTOs.PaymentMethodCountDTO;
import ourpkg.dataAnalysis.DTOs.SalesDataDTO;
import ourpkg.dataAnalysis.DTOs.SalesForecastDTO;
import ourpkg.dataAnalysis.DTOs.SellerStatisticsDTO;
import ourpkg.dataAnalysis.DTOs.ShopDTO;
import ourpkg.dataAnalysis.DTOs.TopProductDTO;
import ourpkg.exception.ResourceNotFoundException;
import ourpkg.exception.UnauthorizedException;

@Service
@RequiredArgsConstructor
public class SellerAnalyticsService {

	private final DataSource dataSource;

	/**
	 * 根據用戶ID獲取商店ID
	 */
	public Integer getShopIdByUserId(Integer userId) {
		String sql = "SELECT shop_id FROM Shop WHERE user_id = ? AND is_active = 1";

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, userId);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				return rs.getInt("shop_id");
			} else {
				throw new ResourceNotFoundException("找不到該用戶的商店");
			}
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 根據用戶ID獲取商店列表 一個賣家只有一個商店的情況下，仍然返回列表格式
	 */
	public List<ShopDTO> getShopsByUserId(Integer userId) {
		String sql = "SELECT s.shop_id, s.shop_name, s.description " + "FROM Shop s "
				+ "WHERE s.user_id = ? AND s.is_active = 1";

		List<ShopDTO> shops = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, userId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				ShopDTO shop = new ShopDTO();
				shop.setId(rs.getInt("shop_id"));
				shop.setName(rs.getString("shop_name"));
				shop.setDescription(rs.getString("description"));

				shops.add(shop);
			}

			return shops;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 驗證賣家是否擁有該商店
	 */
	public void validateSellerOwnsShop(Integer userId, Integer shopId) {
		String sql = "SELECT 1 FROM Shop WHERE user_id = ? AND shop_id = ? AND is_active = 1";

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, userId);
			stmt.setInt(2, shopId);
			ResultSet rs = stmt.executeQuery();

			if (!rs.next()) {
				throw new UnauthorizedException("無權訪問該商店");
			}
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取賣家統計數據
	 */
	public SellerStatisticsDTO getSellerStatistics(Integer shopId) {
		SellerStatisticsDTO statistics = new SellerStatisticsDTO();

		// 獲取總訂單數
		String totalOrdersSql = "SELECT COUNT(DISTINCT o.order_id) AS total_orders " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "WHERE oi.shop_id = ?";

		// 獲取總銷售額
		String totalSalesSql = "SELECT SUM(oi.unit_price * oi.quantity) AS total_sales " + "FROM OrderItem oi "
				+ "WHERE oi.shop_id = ?";

		// 獲取本月訂單數
		String monthlyOrdersSql = "SELECT COUNT(DISTINCT o.order_id) AS monthly_orders " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "WHERE oi.shop_id = ? AND YEAR(o.created_at) = YEAR(GETDATE()) "
				+ "AND MONTH(o.created_at) = MONTH(GETDATE())";

		// 獲取本月銷售額
		String monthlySalesSql = "SELECT SUM(oi.unit_price * oi.quantity) AS monthly_sales " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "WHERE oi.shop_id = ? AND YEAR(o.created_at) = YEAR(GETDATE()) "
				+ "AND MONTH(o.created_at) = MONTH(GETDATE())";

		// 獲取上月總訂單數
		String lastMonthOrdersSql = "SELECT COUNT(DISTINCT o.order_id) AS last_month_orders " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "WHERE oi.shop_id = ? AND "
				+ "((YEAR(o.created_at) = YEAR(DATEADD(month, -1, GETDATE())) AND "
				+ "MONTH(o.created_at) = MONTH(DATEADD(month, -1, GETDATE()))) OR "
				+ "(YEAR(o.created_at) = YEAR(GETDATE()) AND "
				+ "MONTH(o.created_at) = MONTH(DATEADD(month, -1, GETDATE()))))";

		// 獲取上月總銷售額
		String lastMonthSalesSql = "SELECT SUM(oi.unit_price * oi.quantity) AS last_month_sales " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "WHERE oi.shop_id = ? AND "
				+ "((YEAR(o.created_at) = YEAR(DATEADD(month, -1, GETDATE())) AND "
				+ "MONTH(o.created_at) = MONTH(DATEADD(month, -1, GETDATE()))) OR "
				+ "(YEAR(o.created_at) = YEAR(GETDATE()) AND "
				+ "MONTH(o.created_at) = MONTH(DATEADD(month, -1, GETDATE()))))";

		// 獲取活躍客戶數 (3個月內有購買的客戶)
		String activeCustomersSql = "SELECT COUNT(DISTINCT o.user_id) AS active_customers " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "WHERE oi.shop_id = ? AND o.created_at >= DATEADD(month, -3, GETDATE())";

		// 獲取總商品數
		String totalProductsSql = "SELECT COUNT(DISTINCT p.product_id) AS total_products " + "FROM Product p "
				+ "WHERE p.shop_id = ?";

		// 獲取上架商品數
		String activeProductsSql = "SELECT COUNT(DISTINCT p.product_id) AS active_products " + "FROM Product p "
				+ "WHERE p.shop_id = ? AND p.active = 1";

		try (Connection conn = dataSource.getConnection();
				PreparedStatement totalOrdersStmt = conn.prepareStatement(totalOrdersSql);
				PreparedStatement totalSalesStmt = conn.prepareStatement(totalSalesSql);
				PreparedStatement monthlyOrdersStmt = conn.prepareStatement(monthlyOrdersSql);
				PreparedStatement monthlySalesStmt = conn.prepareStatement(monthlySalesSql);
				PreparedStatement lastMonthOrdersStmt = conn.prepareStatement(lastMonthOrdersSql);
				PreparedStatement lastMonthSalesStmt = conn.prepareStatement(lastMonthSalesSql);
				PreparedStatement activeCustomersStmt = conn.prepareStatement(activeCustomersSql);
				PreparedStatement totalProductsStmt = conn.prepareStatement(totalProductsSql);
				PreparedStatement activeProductsStmt = conn.prepareStatement(activeProductsSql)) {

			// 設置參數
			totalOrdersStmt.setInt(1, shopId);
			totalSalesStmt.setInt(1, shopId);
			monthlyOrdersStmt.setInt(1, shopId);
			monthlySalesStmt.setInt(1, shopId);
			lastMonthOrdersStmt.setInt(1, shopId);
			lastMonthSalesStmt.setInt(1, shopId);
			activeCustomersStmt.setInt(1, shopId);
			totalProductsStmt.setInt(1, shopId);
			activeProductsStmt.setInt(1, shopId);

			// 查詢總訂單數
			ResultSet totalOrdersRs = totalOrdersStmt.executeQuery();
			if (totalOrdersRs.next()) {
				statistics.setTotalOrders(totalOrdersRs.getInt("total_orders"));
			}

			// 查詢總銷售額
			ResultSet totalSalesRs = totalSalesStmt.executeQuery();
			if (totalSalesRs.next()) {
				BigDecimal totalSales = totalSalesRs.getBigDecimal("total_sales");
				statistics.setTotalSales(totalSales != null ? totalSales : BigDecimal.ZERO);
			}

			// 查詢本月訂單數
			ResultSet monthlyOrdersRs = monthlyOrdersStmt.executeQuery();
			if (monthlyOrdersRs.next()) {
				statistics.setCurrentMonthOrders(monthlyOrdersRs.getInt("monthly_orders"));
			}

			// 查詢本月銷售額
			ResultSet monthlySalesRs = monthlySalesStmt.executeQuery();
			if (monthlySalesRs.next()) {
				BigDecimal monthlySales = monthlySalesRs.getBigDecimal("monthly_sales");
				statistics.setCurrentMonthSales(monthlySales != null ? monthlySales : BigDecimal.ZERO);
			}

			// 查詢上月訂單數
			ResultSet lastMonthOrdersRs = lastMonthOrdersStmt.executeQuery();
			if (lastMonthOrdersRs.next()) {
				statistics.setLastMonthOrders(lastMonthOrdersRs.getInt("last_month_orders"));
			}

			// 查詢上月銷售額
			ResultSet lastMonthSalesRs = lastMonthSalesStmt.executeQuery();
			if (lastMonthSalesRs.next()) {
				BigDecimal lastMonthSales = lastMonthSalesRs.getBigDecimal("last_month_sales");
				statistics.setLastMonthSales(lastMonthSales != null ? lastMonthSales : BigDecimal.ZERO);
			}

			// 計算同比增長率
			if (statistics.getLastMonthOrders() > 0) {
				double orderGrowth = ((double) statistics.getCurrentMonthOrders() / statistics.getLastMonthOrders() - 1)
						* 100;
				statistics.setOrderGrowth(Math.round(orderGrowth * 10) / 10.0); // 取一位小數
			}

			if (statistics.getLastMonthSales().compareTo(BigDecimal.ZERO) > 0) {
				double salesGrowth = (statistics.getCurrentMonthSales().doubleValue()
						/ statistics.getLastMonthSales().doubleValue() - 1) * 100;
				statistics.setSalesGrowth(Math.round(salesGrowth * 10) / 10.0); // 取一位小數
			}

			// 查詢活躍客戶數
			ResultSet activeCustomersRs = activeCustomersStmt.executeQuery();
			if (activeCustomersRs.next()) {
				statistics.setActiveCustomers(activeCustomersRs.getInt("active_customers"));
			}

			// 查詢總商品數
			ResultSet totalProductsRs = totalProductsStmt.executeQuery();
			if (totalProductsRs.next()) {
				statistics.setTotalProducts(totalProductsRs.getInt("total_products"));
			}

			// 查詢上架商品數
			ResultSet activeProductsRs = activeProductsStmt.executeQuery();
			if (activeProductsRs.next()) {
				statistics.setActiveProducts(activeProductsRs.getInt("active_products"));
			}

			return statistics;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取銷售數據趨勢
	 */
	public SalesDataDTO getSalesData(Integer shopId, String timeRange) {
		// 根據時間範圍確定查詢時間和格式化方式
		LocalDateTime startDateTime;
		String dateFormat;

		LocalDateTime now = LocalDateTime.now();

		switch (timeRange) {
		case "week":
			startDateTime = now.minusDays(6);
			dateFormat = "yyyy-MM-dd";
			break;
		case "month":
			startDateTime = now.minusDays(29);
			dateFormat = "yyyy-MM-dd";
			break;
		case "quarter":
			startDateTime = now.minusDays(89);
			dateFormat = "yyyy-MM-dd";
			break;
		case "year":
			startDateTime = now.minusYears(1).plusDays(1);
			dateFormat = "yyyy-MM";
			break;
		default:
			startDateTime = now.minusDays(29);
			dateFormat = "yyyy-MM-dd";
		}

		// 創建日期格式化器
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);

		// 查詢銷售數據
		String sql;
		if (timeRange.equals("year")) {
			// 年視圖按月匯總
			sql = "SELECT FORMAT(o.created_at, 'yyyy-MM') AS date, "
					+ "SUM(oi.unit_price * oi.quantity) AS daily_sales " + "FROM [Order] o "
					+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "WHERE oi.shop_id = ? AND o.created_at >= ? "
					+ "GROUP BY FORMAT(o.created_at, 'yyyy-MM') " + "ORDER BY date";
		} else {
			// 其他視圖按日匯總
			sql = "SELECT CONVERT(VARCHAR, o.created_at, 23) AS date, "
					+ "SUM(oi.unit_price * oi.quantity) AS daily_sales " + "FROM [Order] o "
					+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "WHERE oi.shop_id = ? AND o.created_at >= ? "
					+ "GROUP BY CONVERT(VARCHAR, o.created_at, 23) " + "ORDER BY date";
		}

		// 準備返回結果
		SalesDataDTO salesData = new SalesDataDTO();
		List<String> labels = new ArrayList<>();
		List<BigDecimal> values = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, shopId);
			stmt.setObject(2, startDateTime);
			ResultSet rs = stmt.executeQuery();

			// 處理查詢結果
			Map<String, BigDecimal> salesByDate = new HashMap<>();
			while (rs.next()) {
				String date = rs.getString("date");
				BigDecimal sales = rs.getBigDecimal("daily_sales");
				salesByDate.put(date, sales);
			}

			// 填充日期範圍內沒有銷售的日期
			LocalDate startDate = startDateTime.toLocalDate();
			LocalDate endDate = now.toLocalDate();

			if (timeRange.equals("year")) {
				// 按月填充
				LocalDate current = startDate.withDayOfMonth(1); // 從月初開始
				while (!current.isAfter(endDate)) {
					String monthKey = current.format(formatter);

					// 格式化顯示標籤
					String displayLabel = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
					labels.add(displayLabel);

					values.add(salesByDate.getOrDefault(monthKey, BigDecimal.ZERO));
					current = current.plusMonths(1);
				}
			} else {
				// 按日填充
				LocalDate current = startDate;
				while (!current.isAfter(endDate)) {
					String dateKey = current.format(formatter);

					// 格式化顯示標籤 (僅月和日)
					String displayLabel = current.format(DateTimeFormatter.ofPattern("MM/dd"));
					labels.add(displayLabel);

					values.add(salesByDate.getOrDefault(dateKey, BigDecimal.ZERO));
					current = current.plusDays(1);
				}
			}

			salesData.setLabels(labels);
			salesData.setValues(values);

			return salesData;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取熱銷商品排行
	 */
	public List<TopProductDTO> getTopProducts(Integer shopId, Integer limit) {
		String sql = "SELECT p.product_id, p.product_name, " + "SUM(oi.quantity) AS total_quantity, "
				+ "SUM(oi.unit_price * oi.quantity) AS total_revenue " + "FROM OrderItem oi "
				+ "JOIN SKU s ON oi.sku_id = s.sku_id " + "JOIN Product p ON s.product_id = p.product_id "
				+ "WHERE oi.shop_id = ? " + "GROUP BY p.product_id, p.product_name " + "ORDER BY total_revenue DESC "
				+ "OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";

		List<TopProductDTO> topProducts = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, shopId);
			stmt.setInt(2, limit);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				TopProductDTO product = new TopProductDTO();
				product.setId(rs.getInt("product_id"));
				product.setName(rs.getString("product_name"));
				product.setQuantity(rs.getInt("total_quantity"));
				product.setRevenue(rs.getBigDecimal("total_revenue"));

				topProducts.add(product);
			}

			return topProducts;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取訂單狀態分布
	 */
	public List<OrderStatusCountDTO> getOrderStatusDistribution(Integer shopId) {
		String sql = "SELECT osc.id, osc.name AS status, COUNT(DISTINCT o.order_id) AS count " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "JOIN OrderStatusCorrespond osc ON o.order_status_id = osc.id " + "WHERE oi.shop_id = ? "
				+ "GROUP BY osc.id, osc.name " + "ORDER BY osc.id";

		List<OrderStatusCountDTO> statusCounts = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, shopId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				OrderStatusCountDTO statusCount = new OrderStatusCountDTO();
				statusCount.setId(rs.getInt("id"));
				statusCount.setStatus(rs.getString("status"));
				statusCount.setCount(rs.getInt("count"));

				statusCounts.add(statusCount);
			}

			return statusCounts;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取客戶購買頻率分布
	 */
	public List<CustomerFrequencyDTO> getCustomerFrequency(Integer shopId) {
		String sql = "WITH CustomerOrders AS ( " + "  SELECT o.user_id, COUNT(DISTINCT o.order_id) AS order_count "
				+ "  FROM [Order] o " + "  JOIN OrderItem oi ON o.order_id = oi.order_id " + "  WHERE oi.shop_id = ? "
				+ "  GROUP BY o.user_id " + ") " + "SELECT " + "  CASE " + "    WHEN order_count = 1 THEN '1次' "
				+ "    WHEN order_count BETWEEN 2 AND 3 THEN '2-3次' "
				+ "    WHEN order_count BETWEEN 4 AND 5 THEN '4-5次' "
				+ "    WHEN order_count BETWEEN 6 AND 10 THEN '6-10次' " + "    ELSE '10次以上' " + "  END AS frequency, "
				+ "  COUNT(*) AS count " + "FROM CustomerOrders " + "GROUP BY " + "  CASE "
				+ "    WHEN order_count = 1 THEN '1次' " + "    WHEN order_count BETWEEN 2 AND 3 THEN '2-3次' "
				+ "    WHEN order_count BETWEEN 4 AND 5 THEN '4-5次' "
				+ "    WHEN order_count BETWEEN 6 AND 10 THEN '6-10次' " + "    ELSE '10次以上' " + "  END " + "ORDER BY "
				+ "  CASE frequency " + "    WHEN '1次' THEN 1 " + "    WHEN '2-3次' THEN 2 " + "    WHEN '4-5次' THEN 3 "
				+ "    WHEN '6-10次' THEN 4 " + "    ELSE 5 " + "  END";

		List<CustomerFrequencyDTO> frequencyData = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, shopId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				CustomerFrequencyDTO frequency = new CustomerFrequencyDTO();
				frequency.setFrequency(rs.getString("frequency"));
				frequency.setCount(rs.getInt("count"));

				frequencyData.add(frequency);
			}

			return frequencyData;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取付款方式分布
	 */
	public List<PaymentMethodCountDTO> getPaymentMethodDistribution(Integer shopId) {
		String sql = "SELECT pmc.id, pmc.name AS method, COUNT(DISTINCT o.order_id) AS count " + "FROM [Order] o "
				+ "JOIN OrderItem oi ON o.order_id = oi.order_id " + "JOIN Payment p ON o.order_id = p.order_id "
				+ "JOIN PaymentMethodCorrespond pmc ON p.payment_method_id = pmc.id " + "WHERE oi.shop_id = ? "
				+ "GROUP BY pmc.id, pmc.name " + "ORDER BY count DESC";

		List<PaymentMethodCountDTO> methodCounts = new ArrayList<>();

		try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, shopId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				PaymentMethodCountDTO methodCount = new PaymentMethodCountDTO();
				methodCount.setId(rs.getInt("id"));
				methodCount.setMethod(rs.getString("method"));
				methodCount.setCount(rs.getInt("count"));

				methodCounts.add(methodCount);
			}

			return methodCounts;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}

	/**
	 * 獲取銷售預測數據 注意：這只是一個簡單的預測模型，實際應用中可能需要更複雜的算法
	 */
	public SalesForecastDTO getSalesForecast(Integer shopId, Integer days) {
		// 獲取過去同等天數的銷售數據做為參考
		String historySql = "SELECT CONVERT(VARCHAR, o.created_at, 23) AS date, "
				+ "COUNT(DISTINCT o.order_id) AS daily_orders, " + "SUM(oi.unit_price * oi.quantity) AS daily_sales "
				+ "FROM [Order] o " + "JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "WHERE oi.shop_id = ? AND o.created_at >= DATEADD(day, -?, GETDATE()) "
				+ "GROUP BY CONVERT(VARCHAR, o.created_at, 23) " + "ORDER BY date";

		// 獲取店鋪過去30天平均值
		String avgSql = "SELECT " + "AVG(daily_orders) AS avg_orders, " + "AVG(daily_sales) AS avg_sales " + "FROM ( "
				+ "  SELECT " + "    CONVERT(VARCHAR, o.created_at, 23) AS date, "
				+ "    COUNT(DISTINCT o.order_id) AS daily_orders, "
				+ "    SUM(oi.unit_price * oi.quantity) AS daily_sales " + "  FROM [Order] o "
				+ "  JOIN OrderItem oi ON o.order_id = oi.order_id "
				+ "  WHERE oi.shop_id = ? AND o.created_at >= DATEADD(day, -30, GETDATE()) "
				+ "  GROUP BY CONVERT(VARCHAR, o.created_at, 23) " + ") AS daily_stats";

		try (Connection conn = dataSource.getConnection();
				PreparedStatement historyStmt = conn.prepareStatement(historySql);
				PreparedStatement avgStmt = conn.prepareStatement(avgSql)) {

			// 獲取歷史銷售數據
			historyStmt.setInt(1, shopId);
			historyStmt.setInt(2, days);
			ResultSet historyRs = historyStmt.executeQuery();

			// 獲取平均值
			avgStmt.setInt(1, shopId);
			ResultSet avgRs = avgStmt.executeQuery();

			// 計算平均每日訂單和銷售額
			double avgDailyOrders = 0;
			double avgDailySales = 0;

			if (avgRs.next()) {
				avgDailyOrders = avgRs.getDouble("avg_orders");
				avgDailySales = avgRs.getDouble("avg_sales");
			}

			// 創建預測數據
			SalesForecastDTO forecast = new SalesForecastDTO();
			List<String> labels = new ArrayList<>();
			List<BigDecimal> values = new ArrayList<>();

			// 預計訂單總數和總銷售額
			int totalOrders = (int) Math.round(avgDailyOrders * days);
			BigDecimal totalRevenue = BigDecimal.valueOf(avgDailySales * days);

			forecast.setOrderCount(totalOrders);
			forecast.setRevenue(totalRevenue);

			// 生成未來日期
			LocalDate startDate = LocalDate.now();
			for (int i = 0; i < days; i++) {
				LocalDate forecastDate = startDate.plusDays(i);
				labels.add(forecastDate.format(DateTimeFormatter.ofPattern("M/d")));

				// 產生預測值，加入一些隨機波動
				double randomFactor = 0.8 + (Math.random() * 0.4); // 0.8 - 1.2 之間的隨機因子
				double forecastValue = avgDailySales * randomFactor;

				// 星期效應（週末銷售更好）
				int dayOfWeek = forecastDate.getDayOfWeek().getValue();
				if (dayOfWeek == 6 || dayOfWeek == 7) { // 週六週日
					forecastValue *= 1.3;
				}

				values.add(BigDecimal.valueOf(forecastValue));
			}

			forecast.getData().setLabels(labels);
			forecast.getData().setValues(values);

			return forecast;
		} catch (SQLException e) {
			throw new RuntimeException("資料庫錯誤", e);
		}
	}
}