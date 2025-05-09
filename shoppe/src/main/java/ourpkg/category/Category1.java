package ourpkg.category;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.Product;

@Entity
@Table(name = "[Category1]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category1 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[name]", nullable = false, unique = true, length = 255)
	private String name;

	@ManyToMany(mappedBy = "category1List")
	private List<Category2> category2List;

	@OneToMany(mappedBy = "category1", fetch = FetchType.LAZY) // 刪除Category1表的一筆資料 所屬的商品也會被刪掉!!
	private List<Product> productList;

}
