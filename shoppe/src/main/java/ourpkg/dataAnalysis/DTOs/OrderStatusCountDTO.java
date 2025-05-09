package ourpkg.dataAnalysis.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusCountDTO {
	private Integer id;
	private String status;
	private Integer count;

}
