package ourpkg.category;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.attribute.Attribute;
import ourpkg.product.Product;

@Entity
@Table(name = "[Category2]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category2 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[name]", nullable = false, unique = true, length = 255)
	private String name;

	@ManyToMany
	@JoinTable(name = "[Category1_Category2]", joinColumns = @JoinColumn(name = "[c2_id]"), inverseJoinColumns = @JoinColumn(name = "[c1_id]"))
	private List<Category1> category1List;

	@OneToMany(mappedBy = "category2", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // 刪除Category2表的一筆資料
																							// 所屬的商品也會被刪掉!!
	private List<Product> productList;

	@ManyToMany
	@JoinTable(name = "[Category2_Attribute]", joinColumns = @JoinColumn(name = "[c2_id]"), inverseJoinColumns = @JoinColumn(name = "[attribute_id]"))
	private List<Attribute> attributeList;

}
