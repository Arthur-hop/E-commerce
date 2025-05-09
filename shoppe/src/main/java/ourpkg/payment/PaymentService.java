package ourpkg.payment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.HtmlUtils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import ourpkg.order.Order;
import ourpkg.order.OrderRequest;

import ourpkg.payment.mapper.PaymentMethodCreateMapper;
@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${ecpay.merchant-id}")
    private String merchantId;
    
    @Value("${ecpay.hash-key}")
    private String hashKey;
    
    @Value("${ecpay.hash-iv}")
    private String hashIv;
    
    @Value("${ecpay.return-url}")
    private String returnUrl;
    
    @Value("${ecpay.client-back-url:}")
    private String clientBackUrl;
    
    @Value("${ecpay.api-url:https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5}")
    private String ecpayApiUrl;
    
    private String sanitizeItemName(String itemName) {
        if (itemName == null) return "商品";
        return itemName.replaceAll("[^\\w\\u4e00-\\u9fa5\\s#]", "").trim();
    }
    
    @PostConstruct
    public void init() {
        // 檢查必要的配置
        if (merchantId == null || merchantId.isEmpty()) {
            logger.error("綠界商店ID未配置");
            throw new IllegalStateException("綠界商店ID未配置");
        }
        
        if (hashKey == null || hashKey.isEmpty()) {
            logger.error("綠界HashKey未配置");
            throw new IllegalStateException("綠界HashKey未配置");
        }
        
        if (hashIv == null || hashIv.isEmpty()) {
            logger.error("綠界HashIV未配置");
            throw new IllegalStateException("綠界HashIV未配置");
        }
        
        if (returnUrl == null || returnUrl.isEmpty()) {
            logger.error("綠界ReturnURL未配置");
            throw new IllegalStateException("綠界ReturnURL未配置");
        }
        
        logger.info("綠界支付服務初始化完成");
    }
    
    
    public String createECPayOrder(OrderRequest orderRequest) {
        // 基本參數驗證
        if (orderRequest == null) {
            throw new IllegalArgumentException("訂單資料不能為空");
        }
        if (orderRequest.getAmount() <= 0) {
            throw new IllegalArgumentException("訂單金額必須大於零");
        }
        
        // 建立參數映射
        Map<String, String> params = new HashMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", orderRequest.getMerchantTradeNo() != null
                ? orderRequest.getMerchantTradeNo() : generateOrderNumber());
                
        // 修正日期格式為綠界要求的格式 (yyyy/MM/dd HH:mm:ss)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        params.put("MerchantTradeDate", sdf.format(new Date()));
        
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(orderRequest.getAmount()));
        
        // 處理可能包含特殊字符的欄位 - 不需要過度清理，只需確保不含 XML 特殊字符
        params.put("TradeDesc", escapeHtml(orderRequest.getDescription()));
        params.put("ItemName", escapeHtml(orderRequest.getItemName()));
        
        // 確保 URL 格式正確，包含協議前綴
        params.put("ReturnURL", ensureUrlPrefix(orderRequest.getReturnUrl() != null
                ? orderRequest.getReturnUrl() : returnUrl));
        params.put("ChoosePayment", "ALL");
        
        if (clientBackUrl != null && !clientBackUrl.isEmpty()) {
            params.put("ClientBackURL", ensureUrlPrefix(clientBackUrl));
        }
        
        // 記錄原始參數字串，用於調試
        String originalParamsString = buildCheckMacValueString(params, hashKey, hashIv);
        logger.info("原始字串: {}", originalParamsString);
        
        // URL 編碼
        String encodedParams = null;
        try {
            encodedParams = URLEncoder.encode(originalParamsString, "UTF-8");
            logger.info("URL編碼結果: {}", encodedParams);
        } catch (UnsupportedEncodingException e) {
            logger.error("URL編碼失敗", e);
            throw new RuntimeException("URL編碼失敗", e);
        }
        
        // 轉為小寫
        String lowerCaseParams = encodedParams.toLowerCase();
        logger.info("編碼後字串: {}", lowerCaseParams);
        
        // 產生檢查碼並加入到參數中
        String checkMacValue = generateCheckMacValue(params, hashKey, hashIv);
        params.put("CheckMacValue", checkMacValue);
        
        return generateAutoPostForm(params, ecpayApiUrl);
    }

    /**
     * 轉義 HTML 特殊字符
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    
    /**
     * 確保 URL 包含協議前綴
     */
    private String ensureUrlPrefix(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        
        return url;
    }

    /**
     * 獲取綠界要求的日期格式 (yyyy/MM/dd HH:mm:ss)
     */
    private String getECPayFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(new Date());
    }

    /**
     * 清理單一參數的方法
     */
    private String sanitizeParam(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", "");
    }

    /**
     * 清理所有參數的方法
     */
    private Map<String, String> sanitizeParams(Map<String, String> originalParams) {
        Map<String, String> sanitizedParams = new HashMap<>();
        
        for (Map.Entry<String, String> entry : originalParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // 移除空值，清理特殊字符
            if (value != null && !value.trim().isEmpty()) {
                // 特別處理URL類型的參數，保留必要的符號
                if (key.toLowerCase().contains("url")) {
                    // 確保URL格式正確
                    sanitizedParams.put(key, ensureUrlPrefix(value));
                } else if (key.equals("MerchantTradeDate")) {
                    // 日期參數特殊處理，保留斜線和冒號
                    sanitizedParams.put(key, value);
                } else {
                    // 其他參數的清理
                    sanitizedParams.put(key, value.trim().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s]", ""));
                }
            }
        }
        
        return sanitizedParams;
    }

    
    /**
     * 驗證綠界的回調通知
     * @param responseData 回調數據
     * @return 是否驗證通過
     */
    public boolean verifyPaymentResponse(Map<String, String> responseData) {
        if (responseData == null || !responseData.containsKey("CheckMacValue")) {
            logger.warn("回調數據缺少CheckMacValue");
            return false;
        }
        
        // 保存原始的檢查碼用於比較
        String receivedCheckMacValue = responseData.get("CheckMacValue");
        
        // 創建新的Map，因為需要移除CheckMacValue才能重新計算
        Map<String, String> verificationMap = new HashMap<>(responseData);
        verificationMap.remove("CheckMacValue");
        
        // 重新計算CheckMacValue
        String calculatedCheckMacValue = generateCheckMacValue(verificationMap, hashKey, hashIv);
        
        // 比較原始和計算的檢查碼
        boolean isValid = receivedCheckMacValue.equalsIgnoreCase(calculatedCheckMacValue);
        
        if (!isValid) {
            logger.warn("CheckMacValue驗證失敗: 收到={}, 計算={}", receivedCheckMacValue, calculatedCheckMacValue);
        }
        
        return isValid;
    }
    
    /**
     * 生成訂單編號
     */
    private String generateOrderNumber() {
        return "ORDER" + System.currentTimeMillis();
    }
    
    /**
     * 獲取當前日期時間，格式化為綠界需要的格式 (yyyy/MM/dd HH:mm:ss)
     */
    private String getCurrentFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return sdf.format(new Date());
    }
    
    /**
     * 構建用於生成 CheckMacValue 的字串
     */
    private String buildCheckMacValueString(Map<String, String> params, String hashKey, String hashIv) {
        // 1. 參數按照字母順序排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        // 2. 按照順序組成字符串，格式為 key1=value1&key2=value2...
        StringBuilder sb = new StringBuilder();
        sb.append("HashKey=").append(hashKey);
        
        for (String key : keys) {
            sb.append("&").append(key).append("=").append(params.get(key));
        }
        
        sb.append("&HashIV=").append(hashIv);
        
        return sb.toString();
    }
    
    /**
     * 生成檢查碼
     */
    private String generateCheckMacValue(Map<String, String> params, String hashKey, String hashIv) {
        // 1. 構建原始字串
        String originalString = buildCheckMacValueString(params, hashKey, hashIv);
        
        // 2. URL 編碼
        String urlEncodedString;
        try {
            urlEncodedString = URLEncoder.encode(originalString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("URL 編碼失敗", e);
        }
        
        // 3. 轉為小寫
        urlEncodedString = urlEncodedString.toLowerCase();
        
        // 4. MD5 加密
        String checkMacValue;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(urlEncodedString.getBytes("UTF-8"));
            
            // 轉為 16 進制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            checkMacValue = hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("MD5 加密失敗", e);
        }
        
        return checkMacValue;
    }
    
    private String getMD5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes("UTF-8")); // ✅ 指定 UTF-8
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (Exception e) {
            throw new RuntimeException("MD5加密失敗", e);
        }
    }

    
    /**
     * 生成自動提交表單
     */
    private String generateAutoPostForm(Map<String, String> params, String url) {
        StringBuilder form = new StringBuilder();
        form.append("<!DOCTYPE html>");
        form.append("<html>");
        form.append("<head>");
        form.append("<meta charset=\"utf-8\"/>");
        form.append("<title>正在連接到綠界支付</title>");
        form.append("<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;}.loading{margin:30px 0;}</style>");
        form.append("</head>");
        form.append("<body>");
        form.append("<h2>正在連接到綠界支付系統，請稍候...</h2>");
        form.append("<div class=\"loading\">如果頁面沒有自動跳轉，請點擊下方按鈕</div>");
        
        form.append("<form id='ecpayForm' action='").append(HtmlUtils.htmlEscape(url)).append("' method='post'>");
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            form.append("<input type='hidden' name='").append(HtmlUtils.htmlEscape(entry.getKey()))
                .append("' value='").append(HtmlUtils.htmlEscape(entry.getValue())).append("'>");
        }
        
        form.append("<button type='submit' style='padding:10px 20px;'>前往付款</button>");
        form.append("</form>");
        
        // 使用指定特定nonce的腳本標籤
        String nonce = "ecpay" + getMD5(String.valueOf(System.currentTimeMillis())).substring(0, 32);
        form.append("<script nonce=\"").append(nonce).append("\">setTimeout(function(){document.getElementById('ecpayForm').submit();}, 1000);</script>");
        
        form.append("</body></html>");
        
        return form.toString();
    }
    
    
}