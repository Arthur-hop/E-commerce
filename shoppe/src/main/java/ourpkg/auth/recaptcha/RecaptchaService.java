package ourpkg.auth.recaptcha;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class RecaptchaService {
    
    @Value("${google.recaptcha.secret}") // 從配置文件獲取
    private String recaptchaSecret;
    
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    
    public boolean verifyRecaptcha(String recaptchaResponse) {
        // 如果回應為空，直接返回驗證失敗
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }
        
        try {
            // 建立 HTTP 請求參數
            String parameters = "secret=" + recaptchaSecret + "&response=" + recaptchaResponse;
            
            // 建立連接
            URL url = new URL(RECAPTCHA_VERIFY_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            
            // 發送請求
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(parameters);
            outputStream.flush();
            outputStream.close();
            
            // 獲取回應
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                return false;
            }
            
            // 讀取回應內容
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();
            
            // 解析 JSON 回應
            String jsonResponse = responseBuilder.toString();
            JSONObject jsonObject = new JSONObject(jsonResponse);
            
            // 檢查驗證結果
            return jsonObject.getBoolean("success");
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}