package ourpkg.category.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category2ResDTO {

	private Integer id;
	private String name;
	private List<Integer> category1Ids; // 存放關聯的一級分類IDs
}
