package ourpkg.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.order.Order;

@Entity
@Table(name = "[Shipment]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[shipment_id]")
	private Integer shipmentId;

	@ManyToOne
	@JoinColumn(name = "[order_id]", nullable = false)
	private Order order;

	@ManyToOne
	@JoinColumn(name = "[shipping_method_id]", nullable = false)
	private ShipmentMethod shipmentMethod;

	@ManyToOne
	@JoinColumn(name = "[shipping_status_id]", nullable = false)
	private ShipmentStatus shipmentStatus;

}
