package ourpkg.category.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category2CreateDTO {

	@NotBlank @Size(max = 255) // 必填、最多255字
	private String name;
	
	@NotEmpty @Size(min = 1) // 必填、至少要有一個關聯的一級分類ID
	private List<Integer> category1Ids; // 存放關聯的一級分類IDs
}
