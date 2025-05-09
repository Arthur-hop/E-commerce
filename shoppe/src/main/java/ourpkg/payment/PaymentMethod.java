package ourpkg.payment;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "[PaymentMethodCorrespond]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;
	
	@Column(name = "[name]", nullable = false, unique = true)
	private String name;
	
	@OneToMany(mappedBy = "paymentMethod",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
	private List<Payment> paymentList;
	
}
