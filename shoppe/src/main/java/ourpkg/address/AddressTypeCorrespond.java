package ourpkg.address;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "[AddressTypeCorrespond]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressTypeCorrespond {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[id]")
	private Integer id;

	@Column(name = "[name]", nullable = false, unique = true)
	private String name;

	@OneToMany(mappedBy = "addressTypeCorrespond", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference(value = "address-type-user")
	private List<UserAddress> userAddress;

	@OneToMany(mappedBy = "addressTypeCorrespond", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference(value = "address-type-order")
	private List<OrderAddress> orderAddress;
}
