package ecpay.logistics.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ecpay.logistics")
public class ECPayProperties {

    private String merchantId;
    private String hashKey;
    private String hashIv;

    // Getter 和 Setter 方法
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
}