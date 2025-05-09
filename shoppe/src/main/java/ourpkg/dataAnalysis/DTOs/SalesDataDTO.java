package ourpkg.dataAnalysis.DTOs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SalesDataDTO {
	private List<String> labels;
	private List<BigDecimal> values;

	public SalesDataDTO() {
		this.labels = new ArrayList<>();
		this.values = new ArrayList<>();
	}

}
