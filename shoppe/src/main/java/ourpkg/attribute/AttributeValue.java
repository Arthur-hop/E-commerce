package ourpkg.attribute;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.Product;

@Entity
@Table(name = "[AttributeValue]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeValue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[name]")
	private String name;
	
	@ManyToOne
	@JoinColumn(name = "[attribute_id]",nullable = false)
	private Attribute attribute;
	
	@ManyToMany(mappedBy = "attributeValueList")
	private List<Product> productList;
	
}
