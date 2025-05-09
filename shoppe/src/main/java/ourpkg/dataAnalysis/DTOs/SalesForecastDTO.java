package ourpkg.dataAnalysis.DTOs;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SalesForecastDTO {
	private Integer orderCount;
	private BigDecimal revenue;
	private SalesDataDTO data;

	public SalesForecastDTO() {
		this.orderCount = 0;
		this.revenue = BigDecimal.ZERO;
		this.data = new SalesDataDTO();
	}

}