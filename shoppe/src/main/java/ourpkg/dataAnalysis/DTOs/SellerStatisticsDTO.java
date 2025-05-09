package ourpkg.dataAnalysis.DTOs;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SellerStatisticsDTO {
	private Integer totalOrders;
	private BigDecimal totalSales;
	private Integer currentMonthOrders;
	private BigDecimal currentMonthSales;
	private Integer lastMonthOrders;
	private BigDecimal lastMonthSales;
	private Double orderGrowth;
	private Double salesGrowth;
	private Integer activeCustomers;
	private Integer totalProducts;
	private Integer activeProducts;

	public SellerStatisticsDTO() {
		this.totalOrders = 0;
		this.totalSales = BigDecimal.ZERO;
		this.currentMonthOrders = 0;
		this.currentMonthSales = BigDecimal.ZERO;
		this.lastMonthOrders = 0;
		this.lastMonthSales = BigDecimal.ZERO;
		this.orderGrowth = 0.0;
		this.salesGrowth = 0.0;
		this.activeCustomers = 0;
		this.totalProducts = 0;
		this.activeProducts = 0;
	}

}
