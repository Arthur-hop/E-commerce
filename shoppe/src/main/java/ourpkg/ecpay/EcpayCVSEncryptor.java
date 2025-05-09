package ourpkg.ecpay;

import java.net.URLEncoder;
import java.util.*;


public class EcpayCVSEncryptor {
	 public static String generateCheckMacValue(Map<String, String> params, String hashKey, String hashIV) {
	        SortedMap<String, String> sortedParams = new TreeMap<>(params);
	        StringBuilder sb = new StringBuilder("HashKey=" + hashKey);
	        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
	            sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
	        }
	        sb.append("&HashIV=").append(hashIV);

	        try {
	            String raw = sb.toString();
//	            System.out.println("🔹 原始字串: " + raw);

	            String urlEncoded = URLEncoder.encode(raw, "UTF-8").toLowerCase()
	                    .replace("%21", "!")
	                    .replace("%28", "(")
	                    .replace("%29", ")")
	                    .replace("%2a", "*")
	                    .replace("%2d", "-")
	                    .replace("%2e", ".")
	                    .replace("%5f", "_");

//	            System.out.println("🔹 URL Encoded: " + urlEncoded);

	            String finalMac = org.apache.commons.codec.digest.DigestUtils.md5Hex(urlEncoded).toUpperCase();
//	            System.out.println("✅ CheckMacValue: " + finalMac);

	            return finalMac;
	        } catch (Exception e) {
	            throw new RuntimeException("CheckMacValue 產生失敗", e);
	        }
	    }
}
