package ourpkg.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ecpay")
public class EcpayProperties {
    private String merchantId;
    private String hashKey;
    private String hashIv;
    private String returnUrl;
    private String clientBackUrl;
    private String apiUrl;
    
    // Getters and Setters
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public String getHashKey() {
        return hashKey;
    }
    
    public void setHashKey(String hashKey) {
        this.hashKey = hashKey;
    }
    
    public String getHashIv() {
        return hashIv;
    }
    
    public void setHashIv(String hashIv) {
        this.hashIv = hashIv;
    }
    
    public String getReturnUrl() {
        return returnUrl;
    }
    
    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
    
    public String getClientBackUrl() {
        return clientBackUrl;
    }
    
    public void setClientBackUrl(String clientBackUrl) {
        this.clientBackUrl = clientBackUrl;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}