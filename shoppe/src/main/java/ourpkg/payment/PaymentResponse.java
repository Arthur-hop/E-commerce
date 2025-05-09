package ourpkg.payment;


public class PaymentResponse {
    private String merchantID;
    private String merchantTradeNo;
    private String storeID;
    private String rtnCode;
    private String rtnMsg;
    private String tradeNo;
    private Integer tradeAmt;
    private String paymentDate;
    private String paymentType;
    private String paymentTypeChargeFee;
    private String tradeDate;
    private String simulatePaid;
    private String checkMacValue;
    
    // 構造函數
    public PaymentResponse() {}
    
    // Getters and setters
    public String getMerchantID() {
        return merchantID;
    }

    public void setMerchantID(String merchantID) {
        this.merchantID = merchantID;
    }

    public String getMerchantTradeNo() {
        return merchantTradeNo;
    }

    public void setMerchantTradeNo(String merchantTradeNo) {
        this.merchantTradeNo = merchantTradeNo;
    }

    public String getStoreID() {
        return storeID;
    }

    public void setStoreID(String storeID) {
        this.storeID = storeID;
    }

    public String getRtnCode() {
        return rtnCode;
    }

    public void setRtnCode(String rtnCode) {
        this.rtnCode = rtnCode;
    }

    public String getRtnMsg() {
        return rtnMsg;
    }

    public void setRtnMsg(String rtnMsg) {
        this.rtnMsg = rtnMsg;
    }

    public String getTradeNo() {
        return tradeNo;
    }

    public void setTradeNo(String tradeNo) {
        this.tradeNo = tradeNo;
    }

    public Integer getTradeAmt() {
        return tradeAmt;
    }

    public void setTradeAmt(Integer tradeAmt) {
        this.tradeAmt = tradeAmt;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentTypeChargeFee() {
        return paymentTypeChargeFee;
    }

    public void setPaymentTypeChargeFee(String paymentTypeChargeFee) {
        this.paymentTypeChargeFee = paymentTypeChargeFee;
    }

    public String getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(String tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getSimulatePaid() {
        return simulatePaid;
    }

    public void setSimulatePaid(String simulatePaid) {
        this.simulatePaid = simulatePaid;
    }

    public String getCheckMacValue() {
        return checkMacValue;
    }

    public void setCheckMacValue(String checkMacValue) {
        this.checkMacValue = checkMacValue;
    }
    
    // 判斷交易是否成功的方法
    public boolean isPaymentSuccessful() {
        return "1".equals(this.rtnCode);
    }
}