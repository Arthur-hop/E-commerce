package ourpkg.auth.mail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
	
	private final JavaMailSender mailSender;
	
	public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${frontend.reset-password-url}") // 在 properties 設定前端密碼重設頁面
    private String resetPasswordUrl;
	
    
    /**
     * 發送重設密碼 Email
     */
    public void sendResetPasswordEmail(String toEmail, String token,String username) {
        String resetLink = resetPasswordUrl + "?token=" + token;
        String subject = "密碼重製請求";
        String content = "<!DOCTYPE html>"
                + "<html lang='zh-TW'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }"
                + ".email-container { max-width: 600px; background: #ffffff; padding: 20px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }"
                + "h2 { color: #333333; }"
                + "p { color: #666666; line-height: 1.6; }"
                + ".btn { display: inline-block; background-color: #007bff; color: #ffffff; padding: 12px 20px; text-decoration: none; font-size: 16px; border-radius: 5px; }"
                + ".footer { margin-top: 20px; font-size: 12px; color: #999999; text-align: center; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='email-container'>"
                + "<h2>您好，"+username+"!!</h2>"
                + "<p>要重新設定您的密碼，請點擊以下的按鈕。您將會被導向到設定新密碼的頁面。</p>"
                + "<p style='text-align: center;'>"
                + "<a href='" + resetLink + "' class='btn'>重設密碼</a>"
                + "</p>"
                + "<p>如果您未曾請求密碼重設，請忽略此郵件。在您點選連結設定密碼前，您的密碼不會改變。</p>"
                + "<p>密碼設定連結將於 <strong>24 小時內</strong> 失效，若需要重新申請，請點擊上方按鈕並立即至您的電子郵件信箱確認。</p>"
                + "<div class='footer'>"
                + "<p>此郵件為系統自動發送，請勿直接回覆。</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        sendEmail(toEmail, subject, content);
    }
    
    
    /**
     * 發送 Email
     */
    private void sendEmail(String toEmail, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            
            // 添加優先級標頭
            message.setHeader("X-Priority", "1");
            message.setHeader("Importance", "high");
            message.setHeader("X-MSMail-Priority", "High");
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("發送郵件失敗！");
        }
    }
	
    /**
     * 發送商品審核結果通知 Email - 改進版面設計
     */
    public void sendProductReviewNotificationEmail(String toEmail, String username, boolean reviewStatus, List<Map<String, Object>> products) {
        String subject = reviewStatus ? "商品審核通過通知" : "商品審核不通過通知";
        
        // 構建HTML表格以顯示商品信息
        StringBuilder productsTable = new StringBuilder();
        productsTable.append("<table style='width:100%; border-collapse: collapse; margin-top: 15px;'>");
        productsTable.append("<tr style='background-color: #f2f2f2;'>");
        productsTable.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: center; font-size: 16px;'>商品圖片</th>");
        productsTable.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: center; font-size: 16px;'>商品ID</th>");
        productsTable.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: center; font-size: 16px;'>商品名稱</th>");
        productsTable.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: center; font-size: 16px;'>價格</th>");
        if (!reviewStatus) {
            productsTable.append("<th style='padding: 12px; border: 1px solid #ddd; text-align: center; font-size: 16px;'>不通過原因</th>");
        }
        productsTable.append("</tr>");
        
        for (Map<String, Object> product : products) {
            productsTable.append("<tr>");
            
            // 商品圖片 - 使用 cid 內嵌圖片方式
            String imageUrl = (String) product.get("imageUrl");
            String imageCid = "product_" + product.get("id") + "@myshop.com";
            
            productsTable.append("<td style='padding: 15px; border: 1px solid #ddd; text-align: center;'>");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                productsTable.append("<img src='cid:").append(imageCid).append("' alt='商品圖片' style='max-width: 120px; max-height: 120px; object-fit: contain;'>");
            } else {
                productsTable.append("<div style='color: #999; font-style: italic;'>無圖片</div>");
            }
            productsTable.append("</td>");
            
            // 商品ID - 置中顯示，較大字體
            productsTable.append("<td style='padding: 15px; border: 1px solid #ddd; text-align: center; font-size: 16px; font-weight: 500;'>")
                      .append(product.get("id"))
                      .append("</td>");
            
            // 商品名稱 - 置中顯示，較大字體
            productsTable.append("<td style='padding: 15px; border: 1px solid #ddd; text-align: center; font-size: 16px; font-weight: 500;'>")
                      .append(product.get("name"))
                      .append("</td>");
            
            // 價格 - 置中顯示，較大字體
            Object price = product.get("price");
            productsTable.append("<td style='padding: 15px; border: 1px solid #ddd; text-align: center; font-size: 16px; font-weight: 500; color: #e74c3c;'>NT$ ")
                      .append(price != null ? price : "0")
                      .append("</td>");
            
            // 如果是審核不通過，顯示原因
            if (!reviewStatus) {
                productsTable.append("<td style='padding: 15px; border: 1px solid #ddd; text-align: center; font-size: 15px;'>")
                          .append(product.get("reviewComment"))
                          .append("</td>");
            }
            
            productsTable.append("</tr>");
        }
        
        productsTable.append("</table>");
        
        // 構建郵件內容
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append("<!DOCTYPE html>")
                .append("<html lang='zh-TW'>")
                .append("<head>")
                .append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<style>")
                .append("body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }")
                .append(".email-container { max-width: 650px; margin: 0 auto; background: #ffffff; padding: 25px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }")
                .append("h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }")
                .append("p { color: #666666; line-height: 1.6; font-size: 16px; }")
                .append(".success { color: #2ecc71; font-weight: bold; font-size: 18px; }")
                .append(".warning { color: #e74c3c; font-weight: bold; font-size: 18px; }")
                .append(".footer { margin-top: 30px; font-size: 14px; color: #999999; text-align: center; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<div class='email-container'>")
                .append("<h2>您好，").append(username).append("！</h2>");
        
        if (reviewStatus) {
            // 審核通過
            contentBuilder.append("<p class='success'>恭喜您！您的以下商品已通過審核，現已上架至商城：</p>")
                    .append(productsTable.toString())
                    .append("<p>您的商品現在可以被所有用戶搜索和購買了。</p>")
                    .append("<p>感謝您使用我們的平台！</p>");
        } else {
            // 審核不通過
            contentBuilder.append("<p class='warning'>很遺憾，您的以下商品未通過審核：</p>")
                    .append(productsTable.toString())
                    .append("<p class='warning'>請根據不通過原因進行調整後重新提交審核。如有任何疑問，請及時聯繫客服部門。</p>")
                    .append("<p>感謝您的理解與配合。</p>");
        }
        
        contentBuilder.append("<div class='footer'>")
                .append("<p>此郵件為系統自動發送，請勿直接回覆。</p>")
                .append("</div>")
                .append("</div>")
                .append("</body>")
                .append("</html>");
        
        try {
            // 創建一個 MimeMessage 物件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(contentBuilder.toString(), true);
            
            // 添加內嵌圖片
            for (Map<String, Object> product : products) {
                String imageUrl = (String) product.get("imageUrl");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // 使用商品ID創建唯一的CID
                    String imageCid = "product_" + product.get("id") + "@myshop.com";
                    
                    try {
                        // 從URL讀取圖片數據並保存到ByteArrayResource中
                        byte[] imageBytes = downloadImageAsBytes(imageUrl);
                        if (imageBytes != null && imageBytes.length > 0) {
                            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                                @Override
                                public String getFilename() {
                                    return "product_image.jpg"; // 提供一個檔案名
                                }
                            };
                            
                            // 添加圖片作為內嵌資源
                            helper.addInline(imageCid, imageResource, getContentTypeFromURL(imageUrl));
                        }
                    } catch (Exception e) {
                        System.out.println("無法加載圖片 " + imageUrl + ": " + e.getMessage());
                        try {
                            // 使用默認圖片
                            ClassPathResource defaultImage = new ClassPathResource("static/images/no-image.png");
                            helper.addInline(imageCid, defaultImage);
                        } catch (Exception ex) {
                            System.out.println("無法加載默認圖片: " + ex.getMessage());
                        }
                    }
                }
            }
            
            // 發送郵件
            mailSender.send(message);
            System.out.println("成功發送郵件到: " + toEmail);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("發送郵件失敗！" + e.getMessage());
        }
    }

    /**
     * 從URL下載圖片並轉換為字節數組
     */
    private byte[] downloadImageAsBytes(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            
            // 獲取圖片大小（如果可用）
            int contentLength = connection.getContentLength();
            InputStream inputStream = connection.getInputStream();
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(contentLength > 0 ? contentLength : 4096);
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            // 讀取圖片數據到緩衝區
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            // 關閉資源
            inputStream.close();
            outputStream.close();
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            System.out.println("下載圖片時出錯: " + e.getMessage());
            return null;
        }
    }

    /**
     * 從URL獲取內容類型
     */
    private String getContentTypeFromURL(String imageUrl) {
        if (imageUrl.toLowerCase().endsWith(".jpg") || imageUrl.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (imageUrl.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (imageUrl.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (imageUrl.toLowerCase().endsWith(".webp")) {
            return "image/webp";
        } else {
            return "image/jpeg"; // 默認
        }
    }
    
    /**
     * 發送商店申請通過通知郵件
     * @param user 使用者
     * @param application 商店申請
     */
    public void sendShopApprovalEmail(ourpkg.user_role_permission.user.User user, ourpkg.shop.application.ShopApplication application) {
        try {
            String toEmail = user.getEmail();
            String username = user.getUserName() != null ? user.getUserName() : user.getUsername();
            String shopName = application.getShopName();
            
            String subject = "恭喜！您的商店申請已通過審核";
            String content = "<!DOCTYPE html>"
                    + "<html lang='zh-TW'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }"
                    + ".email-container { max-width: 600px; margin: 0 auto; background: #ffffff; padding: 25px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }"
                    + "h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }"
                    + "p { color: #666666; line-height: 1.6; font-size: 16px; }"
                    + ".success-message { color: #2ecc71; font-weight: bold; font-size: 18px; margin: 20px 0; }"
                    + ".shop-info { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #2ecc71; }"
                    + ".shop-info h3 { margin-top: 0; color: #333; }"
                    + ".shop-details { margin-bottom: 8px; font-size: 15px; }"
                    + ".next-steps { background-color: #e8f5e9; padding: 15px; border-radius: 5px; margin: 20px 0; }"
                    + ".next-steps h3 { margin-top: 0; color: #2e7d32; }"
                    + ".next-steps ul { padding-left: 20px; }"
                    + ".next-steps li { margin-bottom: 8px; }"
                    + ".footer { margin-top: 30px; font-size: 14px; color: #999999; text-align: center; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='email-container'>"
                    + "<h2>您好，" + username + "！</h2>"
                    + "<p class='success-message'>恭喜！您的商店申請已通過審核</p>"
                    + "<p>我們很高興地通知您，您的商店 <strong>" + shopName + "</strong> 申請已通過審核。</p>"
                    + "<p>您現在可以開始上架商品並經營您的店鋪了。</p>"
                    + "<div class='shop-info'>"
                    + "<h3>商店資訊</h3>"
                    + "<div class='shop-details'><strong>商店名稱：</strong>" + shopName + "</div>"
                    + "<div class='shop-details'><strong>商店類別：</strong>" + application.getShopCategory() + "</div>"
                    + "<div class='shop-details'><strong>商店描述：</strong>" + (application.getDescription() != null ? application.getDescription() : "無") + "</div>"
                    + "</div>"
                    + "<div class='next-steps'>"
                    + "<h3>接下來您可以：</h3>"
                    + "<ul>"
                    + "<li>上架您的第一件商品</li>"
                    + "<li>完善您的店鋪頁面</li>"
                    + "<li>設置您的商店折扣或優惠券</li>"
                    + "</ul>"
                    + "</div>"
                    + "<p>如果您有任何疑問或需要協助，請隨時聯繫我們的客服團隊。</p>"
                    + "<p>祝您經營順利！</p>"
                    + "<div class='footer'>"
                    + "<p>此郵件為系統自動發送，請勿直接回覆。</p>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            // 記錄錯誤但不中斷流程
            e.printStackTrace();
            System.err.println("寄送商店申請通過郵件失敗：" + e.getMessage());
        }
    }

    /**
     * 發送商店申請被拒絕的郵件通知
     * @param user 使用者
     * @param application 商店申請
     */
    public void sendShopRejectionEmail(ourpkg.user_role_permission.user.User user, ourpkg.shop.application.ShopApplication application) {
        try {
            String toEmail = user.getEmail();
            String username = user.getUserName() != null ? user.getUserName() : user.getUsername();
            String shopName = application.getShopName();
            String rejectReason = application.getAdminComment();
            
            String subject = "您的商店申請審核結果通知";
            String content = "<!DOCTYPE html>"
                    + "<html lang='zh-TW'>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }"
                    + ".email-container { max-width: 600px; margin: 0 auto; background: #ffffff; padding: 25px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }"
                    + "h2 { color: #333333; font-size: 22px; margin-bottom: 20px; }"
                    + "p { color: #666666; line-height: 1.6; font-size: 16px; }"
                    + ".status-message { color: #e74c3c; font-weight: bold; font-size: 18px; margin: 20px 0; }"
                    + ".reason-box { background-color: #f9f9f9; padding: 15px; border-radius: 5px; border-left: 4px solid #e74c3c; margin: 15px 0; }"
                    + ".shop-info { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 15px 0; }"
                    + ".shop-info h3 { margin-top: 0; color: #333; }"
                    + ".shop-details { margin-bottom: 8px; font-size: 15px; }"
                    + ".footer { margin-top: 30px; font-size: 14px; color: #999999; text-align: center; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='email-container'>"
                    + "<h2>您好，" + username + "！</h2>"
                    + "<p class='status-message'>您的商店申請未通過審核</p>"
                    + "<p>我們已收到並審核您對商店 <strong>" + shopName + "</strong> 的申請。很遺憾，您的申請目前未能通過審核。</p>"
                    + "<div class='reason-box'>"
                    + "<h3>未通過原因：</h3>"
                    + "<p>" + (rejectReason != null ? rejectReason : "未提供具體原因") + "</p>"
                    + "</div>"
                    + "<div class='shop-info'>"
                    + "<h3>您的申請資訊</h3>"
                    + "<div class='shop-details'><strong>商店名稱：</strong>" + shopName + "</div>"
                    + "<div class='shop-details'><strong>商店類別：</strong>" + application.getShopCategory() + "</div>"
                    + "<div class='shop-details'><strong>申請日期：</strong>" + application.getCreatedAt() + "</div>"
                    + "</div>"
                    + "<p>您可以根據以上未通過原因進行調整，然後重新提交申請。</p>"
                    + "<p>如有任何疑問，請聯繫我們的客服團隊。</p>"
                    + "<div class='footer'>"
                    + "<p>此郵件為系統自動發送，請勿直接回覆。</p>"
                    + "</div>"
                    + "</div>"
                    + "</body>"
                    + "</html>";

            sendEmail(toEmail, subject, content);
        } catch (Exception e) {
            // 記錄錯誤但不中斷流程
            e.printStackTrace();
            System.err.println("寄送拒絕申請郵件失敗：" + e.getMessage());
        }
    }

}
