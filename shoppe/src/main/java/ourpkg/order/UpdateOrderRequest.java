// UpdateOrderRequest.java
package ourpkg.order;

public class UpdateOrderRequest {
    
    private String status;  // 訂單狀態
    
    // 運送資訊
    private String shipmentMethod;
    private String shipmentStatus;
    private String trackingNumber;
    
    // 付款資訊
    private String paymentMethod;
    private String paymentStatus;
    
    // 地址相關
    private String billingAddress;
    private String shippingAddress;
    
    // Getters and Setters
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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
}