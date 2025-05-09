package ourpkg.payment;

import org.springframework.stereotype.Service;

@Service
public class EcpayService {
    
    private final EcpayProperties ecpayProperties;
    
    // 使用構造函數注入，不需要@Autowired
    public EcpayService(EcpayProperties ecpayProperties) {
        this.ecpayProperties = ecpayProperties;
    }
    
    // 在方法中使用這些屬性
    public void processPayment() {
        String merchantId = ecpayProperties.getMerchantId();
        // 其他實現
    }
}