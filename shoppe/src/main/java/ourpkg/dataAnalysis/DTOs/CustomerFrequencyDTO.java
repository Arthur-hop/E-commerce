package ourpkg.dataAnalysis.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerFrequencyDTO {
	private String frequency;
	private Integer count;

}
