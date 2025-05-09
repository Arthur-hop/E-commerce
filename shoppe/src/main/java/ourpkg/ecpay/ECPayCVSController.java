package ourpkg.ecpay;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.StringReader;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

@RestController
@RequestMapping("/api/ecpay")
public class ECPayCVSController {

    private Map<String, String> lastStoreInfo = new HashMap<>();

    private static final String MERCHANT_ID = "2000132";
    private static final String HASH_KEY = "5294y06JbISpM5x9";
    private static final String HASH_IV = "v77hoKGq4kWxNNIS";

    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @GetMapping(value = "/cvs-map", produces = MediaType.TEXT_HTML_VALUE)
    public String createCvsMap() {
        Map<String, String> params = new HashMap<>();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        params.put("MerchantID", MERCHANT_ID);
        params.put("LogisticsType", "CVS");
        params.put("LogisticsSubType", "FAMI");
        params.put("IsCollection", "N");
        params.put("ServerReplyURL", "https://myecpaytest.loca.lt/api/ecpay/cvs-reply");
        params.put("ExtraData", UUID.randomUUID().toString());
        params.put("Device", "0");
        params.put("MerchantTradeNo", "CVS" + UUID.randomUUID().toString().replace("-", ""));
        params.put("MerchantTradeDate", new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));

        String checkMacValue = EcpayCVSEncryptor.generateCheckMacValue(params, HASH_KEY, HASH_IV);
        params.put("CheckMacValue", checkMacValue);

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'></head><body>");
        html.append("<form id=\"cvsForm\" method=\"POST\" action=\"https://logistics-stage.ecpay.com.tw/Express/map\">\n");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type=\"hidden\" name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\"/>\n");
        }
        html.append("</form>");
        html.append("<script>document.getElementById('cvsForm').submit();</script>");
        html.append("</body></html>");

        return html.toString();
    }

    @PostMapping("/cvs-reply")
    public ResponseEntity<String> cvsReply(HttpServletRequest request) {
        try {
            // 🟩 設定請求的編碼為 Big5（全家的表單編碼）
            request.setCharacterEncoding("Big5");
        } catch (Exception e) {
            System.err.println("編碼設置失敗: " + e.getMessage());
        }

        System.out.println("[✅ 綠界已回傳門市資料]");

        String storeId = request.getParameter("CVSStoreID");
        String storeName = request.getParameter("CVSStoreName");
        String storeAddress = request.getParameter("CVSAddress");
        String telephone = request.getParameter("CVSTelephone");

        System.out.println("CVSStoreName = " + storeName);
        System.out.println("CVSAddress = " + storeAddress);

        // 記錄門市資訊
        lastStoreInfo.put("CVSStoreID", storeId);
        lastStoreInfo.put("CVSStoreName", storeName);
        lastStoreInfo.put("CVSAddress", storeAddress);
        lastStoreInfo.put("CVSTelephone", telephone);

        return ResponseEntity.ok("1|OK");
    }



    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @GetMapping("/selected-store")
    public ResponseEntity<Map<String, String>> getSelectedStore() {
        return ResponseEntity.ok(lastStoreInfo);
    }
    
    @PostMapping("/store-list")
    @CrossOrigin(origins = "http://localhost:5173")
    public ResponseEntity<String> getStoreList(@RequestParam String cvsType) {
        String merchantId = "2000132";
        String hashKey = "5294y06JbISpM5x9";
        String hashIV = "v77hoKGq4kWxNNIS";

        Map<String, String> params = new HashMap<>();
        params.put("MerchantID", merchantId);
        params.put("CvsType", cvsType);

        String checkMac = EcpayCVSEncryptor.generateCheckMacValue(params, hashKey, hashIV);
        params.put("CheckMacValue", checkMac);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        params.forEach(body::add);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            String result = restTemplate.postForObject(
                "https://logistics-stage.ecpay.com.tw/Helper/GetStoreList",
                request,
                String.class
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("綠界 API 呼叫失敗: " + e.getMessage());
        }
    
    
    }


}
