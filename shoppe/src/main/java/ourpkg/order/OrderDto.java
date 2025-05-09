package ourpkg.order;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class OrderDto {
    private int orderId;
    private BigDecimal totalPrice;
    private String status;
    private List<OrderItemDto> items;
    private Date createdAt; // 訂單成立時間 ✅
    private Date updatedAt; // 添加 updatedAt 屬性
    
    // 新增用戶資訊
    private Integer userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    
    // 新增地址資訊
    private String billingAddress;
    private String shippingAddress;
    
    // 新增支付和運送資訊
    private String paymentMethod; // 添加 paymentMethod 屬性
    private String paymentStatus; // 添加 paymentStatus 屬性
    private String shipmentMethod; // 添加 shipmentMethod 屬性
    private String shipmentStatus; // 添加 shipmentStatus 屬性
    private String trackingNumber; // 添加 trackingNumber 屬性
    private BigDecimal originalPrice;  // 未打折的原始金額
    private BigDecimal discountedPrice; // 折扣後的金額（也可用 totalPrice 當作）

    
    // ✅ 無參數建構子 (Spring Boot 需要)
    public OrderDto() {}
    
    public OrderDto(int orderId, BigDecimal originalPrice, BigDecimal discountedPrice, String status) {
        this.orderId = orderId;
        this.originalPrice = originalPrice;
        this.discountedPrice = discountedPrice;
        this.totalPrice = discountedPrice; // 與 totalPrice 綁定
        this.status = status;
    }

    
    public OrderDto(int orderId, BigDecimal totalPrice, String status, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
    }
    
    public OrderDto(int orderId, BigDecimal totalPrice, String status) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
    }
    
    // ✅ 新增建構子 (含 `createdAt`)
    public OrderDto(int orderId, BigDecimal totalPrice, String status, List<OrderItemDto> items, Date createdAt) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
    }
    
    // 包含用戶和地址資訊的建構函數
    public OrderDto(int orderId, BigDecimal totalPrice, String status, 
                   List<OrderItemDto> items, Date createdAt,
                   Integer userId, String userName, String userEmail, String userPhone,
                   String billingAddress, String shippingAddress) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
    }
    
    // 完整的建構函數，包含所有資訊
    public OrderDto(int orderId, BigDecimal totalPrice, String status, 
            List<OrderItemDto> items, Date createdAt, Date updatedAt,
            Integer userId, String userName, String userEmail, String userPhone,
            String billingAddress, String shippingAddress,
            String paymentMethod, String paymentStatus, 
            String shipmentMethod, String shipmentStatus, String trackingNumber) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.shipmentMethod = shipmentMethod;
        this.shipmentStatus = shipmentStatus;
        this.trackingNumber = trackingNumber;
    }
    
    // 相容舊版建構函數
    public OrderDto(int orderId, BigDecimal totalPrice, String status, 
            List<OrderItemDto> items, Date createdAt, Date updatedAt,
            Integer userId, String userName, String userEmail, String userPhone,
            String billingAddress, String shippingAddress,
            String paymentMethodName, String paymentStatusName, 
            String shipmentStatusName, String trackingNumber) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.status = status;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.billingAddress = billingAddress;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethodName;
        this.paymentStatus = paymentStatusName;
        this.shipmentMethod = ""; // 新增空值
        this.shipmentStatus = shipmentStatusName;
        this.trackingNumber = trackingNumber;
    }
    
    
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(BigDecimal discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    
    
    public boolean isPaid() {
        return "已付款".equals(paymentStatus) || "PAID".equals(paymentStatus);
    }

    public boolean isShipped() {
        return "已出貨".equals(shipmentStatus) || "SHIPPED".equals(shipmentStatus);
    }

    @Override
    public String toString() {
        return "OrderDto{" +
            "orderId=" + orderId +
            ", totalPrice=" + totalPrice +
            ", status='" + status + '\'' +
            ", createdAt=" + createdAt +
            '}';
    }
    
    // ✅ Getters & Setters for existing fields
    public int getOrderId() {
        return orderId;
    }
    
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }
    
    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
    
    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<OrderItemDto> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Getters & Setters for new fields
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentStatus() {
        return paymentStatus;
    }
    
    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
    
    public String getShipmentMethod() {
        return shipmentMethod;
    }
    
    public void setShipmentMethod(String shipmentMethod) {
        this.shipmentMethod = shipmentMethod;
    }
    
    public String getShipmentStatus() {
        return shipmentStatus;
    }
    
    public void setShipmentStatus(String shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
}