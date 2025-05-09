package ourpkg.order;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItemId implements Serializable {

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "order_item_id")
    private Integer orderItemId;

    // Constructors
    public OrderItemId() {}

    public OrderItemId(Integer itemId, Integer orderItemId) {
        this.itemId = itemId;
        this.orderItemId = orderItemId;
    }

    // Getters & Setters
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Integer orderItemId) {
        this.orderItemId = orderItemId;
    }

    // equals() & hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItemId)) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(getItemId(), that.getItemId()) &&
               Objects.equals(getOrderItemId(), that.getOrderItemId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getItemId(), getOrderItemId());
    }
}
