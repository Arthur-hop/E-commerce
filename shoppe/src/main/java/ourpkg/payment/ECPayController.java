package ourpkg.payment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpServletResponse;
import ourpkg.order.Order;
import ourpkg.order.OrderRepository;
import ourpkg.order.OrderRequest;
import ourpkg.order.OrderService;
import ourpkg.order.OrderStatusCorrespond;
import ourpkg.order.OrderStatusCorrespondRepository;
import ourpkg.order.OrderStatusHistory;
import ourpkg.order.OrderStatusHistoryRepository;
import ourpkg.order.UpdateOrderRequest;

@RestController
@RequestMapping("/api/payment")
public class ECPayController {
	private static final Logger logger = LoggerFactory.getLogger(ECPayController.class);

	@Value("${ecpay.merchant-id}")
	private String merchantId;

	@Value("${ecpay.hash-key}")
	private String hashKey;

	@Value("${ecpay.hash-iv}")
	private String hashIv;

	@Value("${ecpay.return-url}")
	private String returnUrl;

	@Value("${ecpay.client-back-url}")
	private String clientBackUrl;

	@Value("${ecpay.api-url}")
	private String ecpayApiUrl;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderStatusHistoryRepository orderStatusHistoryRepository;

	@Autowired
	private EcpayProperties ecpayProperties;

	@Autowired
	private OrderStatusCorrespondRepository orderStatusCorrespondRepository;

	@Autowired
	private PaymentStatusRepository paymentStatusRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;

//    @PostMapping("/orders/actions/create")
//    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest) {
//        logger.info("接收到創建訂單請求: {}", orderRequest);
//
//        try {
//            // 獲取當前用戶
//            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//            String username = auth.getName();
//
//            // 驗證基本參數
//            if (orderRequest.getProductId() == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "商品ID不能為空"
//                ));
//            }
//
//            if (orderRequest.getQuantity() == null || orderRequest.getQuantity() <= 0) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "商品數量必須大於0"
//                ));
//            }
//
//            // 調用訂單服務創建訂單
//            Order order = orderService.createDirectOrder(username, orderRequest);
//
//            logger.info("訂單創建成功: orderId={}, totalPrice={}", order.getOrderId(), order.getTotalPrice());
//
//            // 返回包含更多細節的響應
//            return ResponseEntity.ok(Map.of(
//                "success", true,
//                "data", Map.of(
//                    "orderId", order.getOrderId(),
//                    "totalPrice", order.getTotalPrice(),
//                    "productId", orderRequest.getProductId(),
//                    "quantity", orderRequest.getQuantity()
//                ),
//                "message", "訂單創建成功"
//            ));
//
//        } catch (RuntimeException e) {
//            // 捕獲 Service 層可能拋出的特定異常
//            logger.error("訂單創建失敗", e);
//            return ResponseEntity.badRequest().body(Map.of(
//                "success", false,
//                "message", e.getMessage()
//            ));
//        } catch (Exception e) {
//            logger.error("系統異常", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                "success", false,
//                "message", "訂單創建失敗: " + e.getMessage()
//            ));
//        }
//    }
	public boolean verifyPaymentResponse(Map<String, String> responseData) {
		String receivedCheckMac = responseData.get("CheckMacValue");

		// 1️⃣ 建立一份乾淨的參數 map（不包含 CheckMacValue、自動去空值）
		Map<String, String> filteredData = new HashMap<>();
		for (Map.Entry<String, String> entry : responseData.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!"CheckMacValue".equals(key) && value != null && !value.isBlank()) {
				filteredData.put(key, value);
			}
		}

		// 2️⃣ 計算 CheckMacValue
		String calculatedCheckMac = generateCheckMacValue(filteredData);

		logger.warn("CheckMacValue 驗證結果: 收到={}, 計算={}", receivedCheckMac, calculatedCheckMac);

		return receivedCheckMac != null && receivedCheckMac.trim().equalsIgnoreCase(calculatedCheckMac.trim());
	}

	@GetMapping("/order/{orderId}")
	public String createPaymentFromOrder(@PathVariable Integer orderId) {
		logger.info("從訂單創建支付: orderId={}", orderId);

		Optional<Order> orderOpt = orderRepository.findOrderWithItems(orderId);

		if (orderOpt.isPresent()) {
			Order order = orderOpt.get();

			// 記錄訂單的詳細資訊
			logger.info("訂單詳細: Order ID: {}, Amount: {}, Description: {}", order.getOrderId(), order.getTotalPrice(),
					order.getDescription());

			OrderRequest orderRequest = new OrderRequest();
			orderRequest.setAmount(order.getTotalPrice().intValue());
			orderRequest.setDescription("訂單 #" + order.getOrderId());
			orderRequest.setItemName(getItemNameFromOrder(order));

			// 記錄訂單的支付請求
			logger.info("創建支付請求: {}", orderRequest);

			String orderIdStr = String.format("%06d", order.getOrderId()); // 補0到6位
			String timestampPart = String.valueOf(System.currentTimeMillis()).substring(9); // 取最後4位時間戳
			String merchantTradeNo = "ORD" + orderIdStr + timestampPart; // e.g., ORD0001238800
			orderRequest.setMerchantTradeNo(merchantTradeNo);

			return createECPayOrder(orderRequest);
		} else {
			logger.warn("找不到訂單: orderId={}", orderId);
			return "訂單不存在";
		}
	}

	/**
	 * 重定向到綠界付款頁面的方法（與原有方法整合）
	 */
	@GetMapping("/redirect/{orderId}")
	public ResponseEntity<?> redirectToECPay(@PathVariable Integer orderId, HttpServletResponse response)
			throws IOException {
		logger.info("收到綠界付款重定向請求，orderId: {}", orderId);

		try {
			Optional<Order> orderOpt = orderRepository.findOrderWithItems(orderId);
			if (orderOpt.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "找不到訂單"));
			}

			Order order = orderOpt.get();

			// ✅ 確保使用最新的折扣後金額
			logger.info("✅ 準備綠界付款，訂單總金額（折扣後）：{}", order.getTotalPrice());

			// ✅ 建立 payment 紀錄（如尚未建立）
			createPaymentIfNotExist(order);

			// ✅ 準備綠界訂單資料
			OrderRequest orderRequest = new OrderRequest();
			orderRequest.setAmount(order.getTotalPrice().setScale(0, RoundingMode.HALF_UP).intValue()); // 四捨五入為整數
			orderRequest.setDescription("訂單 #" + order.getOrderId());
			orderRequest.setItemName(getItemNameFromOrder(order));

			// ✅ 統一格式處理訂單號
			String orderIdStr = String.format("%06d", order.getOrderId()); // 補0到6位
			String timestampPart = String.valueOf(System.currentTimeMillis()).substring(9); // 取最後幾位
			String merchantTradeNo = "ORD" + orderIdStr + timestampPart; // 確保長度 < 20
			orderRequest.setMerchantTradeNo(merchantTradeNo);

			// ✅ 設定 ReturnURL 與 ClientBackURL
			String fullReturnUrl = ensureUrlPrefix(ecpayProperties.getReturnUrl());
			orderRequest.setReturnUrl(fullReturnUrl);

			String clientBackUrl = "http://localhost:5173/payment/result/" + orderId;
			orderRequest.setClientBackUrl(clientBackUrl);

			// ✅ 產生綠界付款表單 HTML
			String ecpayFormHtml = createECPayOrder(orderRequest);

			return ResponseEntity.ok(
					Map.of("redirectUrl", ecpayApiUrl, "formHtml", ecpayFormHtml, "merchantTradeNo", merchantTradeNo));

		} catch (Exception e) {
			logger.error("重定向到ECPay失敗", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "處理付款時發生錯誤: " + e.getMessage()));
		}
	}

	// 建立 payment 紀錄
	private void createPaymentIfNotExist(Order order) {
		List<Payment> existing = paymentRepository.findByOrderOrderId(order.getOrderId());

		// 如果已經有 payment，就略過
		if (!existing.isEmpty()) {
			logger.info("✅ 訂單已有 Payment 記錄，略過建立: orderId={}", order.getOrderId());
			return;
		}

		logger.info("🆕 為訂單建立 Payment 記錄: orderId={}", order.getOrderId());

		Payment payment = new Payment();
		payment.setOrder(order);

		// ✅ 應該使用 PaymentMethodRepository 來查找付款方式
		PaymentMethod method = paymentMethodRepository.findByNameIgnoreCase("CREDIT")
				.orElseThrow(() -> new RuntimeException("找不到付款方式 CREDIT"));

		// ✅ 使用 PaymentStatusRepository 來查找付款狀態
		PaymentStatus status = paymentStatusRepository.findByName("未付款")
				.orElseThrow(() -> new RuntimeException("找不到付款狀態 未付款"));

		// 正確設值
		payment.setPaymentMethod(method);
		payment.setPaymentStatus(status);

		paymentRepository.save(payment);

		logger.info("✅ 已建立 Payment 記錄並設為未付款");
	}

	/**
	 * 創建綠界付款訂單的方法
	 */
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
		params.put("MerchantTradeNo",
				orderRequest.getMerchantTradeNo() != null ? orderRequest.getMerchantTradeNo() : generateOrderNumber());

		// 修正日期格式為綠界要求的格式 (yyyy/MM/dd HH:mm:ss)
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		params.put("MerchantTradeDate", sdf.format(new Date()));

		params.put("PaymentType", "aio");
		params.put("TotalAmount", String.valueOf(orderRequest.getAmount()));

		// 處理可能包含特殊字符的欄位
		params.put("TradeDesc", escapeHtml(orderRequest.getDescription()));
		params.put("ItemName", escapeHtml(orderRequest.getItemName()));

		params.put("ReturnURL",
				ensureUrlPrefix(orderRequest.getReturnUrl() != null ? orderRequest.getReturnUrl() : returnUrl));

		params.put("ClientBackURL", ensureUrlPrefix(orderRequest.getClientBackUrl()));

		params.put("ChoosePayment", "ALL");

		// 記錄原始參數字串，用於調試
		String originalParamsString = buildCheckMacValueString(params, hashKey, hashIv);
		logger.info("原始字串: {}", originalParamsString);

		// URL 編碼
		String encodedParams;
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
		String checkMacValue = generateCheckMacValue(params);
		params.put("CheckMacValue", checkMacValue);

		return generateAutoPostForm(params, ecpayApiUrl);
	}

	// 生成訂單號
	private String generateOrderNumber() {
		return "ORDER" + System.currentTimeMillis();
	}

	// HTML 轉義
	private String escapeHtml(String input) {
		return HtmlUtils.htmlEscape(input != null ? input : "");
	}

	// 確保 URL 有協議前綴的方法
	private String ensureUrlPrefix(String url) {
		if (url == null || url.isEmpty())
			return "";
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			return "https://" + url;
		}
		return url;
	}

	// 構建用於生成 CheckMacValue 的字串
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

	private String generateCheckMacValue(Map<String, String> params) {
		// 1️⃣ 排除空值與 CheckMacValue 自身
		Map<String, String> filtered = new HashMap<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (!"CheckMacValue".equals(key) && value != null && !value.isBlank()) {
				filtered.put(key, value);
			}
		}

		// 2️⃣ 將參數依 key 排序（A-Z）
		List<String> sortedKeys = new ArrayList<>(filtered.keySet());
		Collections.sort(sortedKeys);

		// 3️⃣ 組成字串：HashKey=...&key1=val1&key2=val2...&HashIV=...
		StringBuilder raw = new StringBuilder();
		raw.append("HashKey=").append(hashKey);
		for (String key : sortedKeys) {
			raw.append("&").append(key).append("=").append(filtered.get(key));
		}
		raw.append("&HashIV=").append(hashIv);

		String rawString = raw.toString();
		logger.debug("CheckMac 原始字串: {}", rawString);

		// 4️⃣ URL 編碼並轉小寫
		try {
			String encoded = URLEncoder.encode(rawString, "UTF-8").toLowerCase()

					// 5️⃣ 符號轉換（符合 ECPay RFC3986 規範）
					.replaceAll("%21", "!").replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%2a", "*")
					.replaceAll("%2d", "-").replaceAll("%2e", ".").replaceAll("%5f", "_");

			logger.debug("CheckMac 編碼後字串: {}", encoded);

			// 6️⃣ MD5 加密
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] hash = md.digest(encoded.getBytes("UTF-8"));

			// 7️⃣ 轉成大寫的十六進位字串
			StringBuilder hex = new StringBuilder();
			for (byte b : hash) {
				String hexStr = Integer.toHexString(0xff & b);
				if (hexStr.length() == 1)
					hex.append('0');
				hex.append(hexStr);
			}
			return hex.toString().toUpperCase();

		} catch (Exception e) {
			throw new RuntimeException("CheckMacValue 產生失敗", e);
		}
	}

	// 生成自動提交表單
	private String generateAutoPostForm(Map<String, String> params, String url) {
		// ✅ 直接指定返回商店的頁面為 /shop
		String clientBackUrl = "http://localhost:5173/shop"; // 若上線請改成正式域名

		StringBuilder form = new StringBuilder();
		form.append("<!DOCTYPE html>");
		form.append("<html>");
		form.append("<head>");
		form.append("<meta charset=\"utf-8\"/>");
		form.append("<title>正在連接到綠界支付</title>");
		form.append("<style>");
		form.append("body{font-family:Arial,sans-serif;text-align:center;padding:50px;}");
		form.append(".loading{margin:30px 0;}");
		form.append(
				"a.button{padding:10px 20px;background-color:#28a745;color:white;text-decoration:none;border-radius:5px;margin-top:20px;display:inline-block;}");
		form.append("</style>");
		form.append("</head>");
		form.append("<body>");
		form.append("<h2>正在連接到綠界支付系統，請稍候...</h2>");
		form.append("<div class=\"loading\">如果頁面沒有自動跳轉，請點擊下方按鈕</div>");

		form.append("<form id='ecpayForm' action='").append(HtmlUtils.htmlEscape(url)).append("' method='post'>");

		for (Map.Entry<String, String> entry : params.entrySet()) {
			form.append("<input type='hidden' name='").append(HtmlUtils.htmlEscape(entry.getKey())).append("' value='")
					.append(HtmlUtils.htmlEscape(entry.getValue())).append("'>");
		}

		form.append("<button type='submit' style='padding:10px 20px;'>前往付款</button>");
		form.append("</form>");

		// ✅ 修改為導回 /shop
		if (clientBackUrl != null && !clientBackUrl.isBlank()) {
			form.append("<p style='margin-top:30px;'>或點此模擬付款完成後返回商店：</p>");
			form.append("<a class='button' href='").append(clientBackUrl).append("'>返回商店</a>");
		}

		form.append("<script>setTimeout(function(){document.getElementById('ecpayForm').submit();}, 1000);</script>");
		form.append("</body></html>");

		return form.toString();
	}

//    /**
//     * 接收綠界支付通知
//     */
//    @PostMapping("/notify")
//    @ResponseBody
//    public String handlePaymentNotification(@RequestParam Map<String, String> responseData) {
//        logger.info("📩 接收到綠界支付通知: {}", responseData);
//
//        try {
//            // 1️⃣ 驗證 CheckMacValue
//            if (!paymentService.verifyPaymentResponse(responseData)) {
//                logger.error("❌ 支付通知驗證失敗");
//                return "0|Error: Verification failed";
//            }
//
//            String merchantTradeNo = responseData.get("MerchantTradeNo");
//            String rtnCode = responseData.get("RtnCode");
//            String tradeAmt = responseData.get("TradeAmt");
//
//            logger.info("✅ 支付通知驗證成功: merchantTradeNo={}, rtnCode={}, tradeAmt={}", 
//                         merchantTradeNo, rtnCode, tradeAmt);
//
//            // 2️⃣ 從merchantTradeNo中解析訂單ID
//            // 格式為: ORDER{orderId}_{timestamp}
//            String orderIdStr = null;
//            if (merchantTradeNo.contains("_")) {
//                orderIdStr = merchantTradeNo.substring(5, merchantTradeNo.indexOf("_"));
//            } else {
//                orderIdStr = merchantTradeNo.replaceAll("ORDER(\\d+).*", "$1");
//            }
//
//            try {
//                int orderId = Integer.parseInt(orderIdStr);
//                logger.info("🔍 解析出訂單ID: {}", orderId);
//                
//                // 判斷支付結果
//                if ("1".equals(rtnCode)) {
//                    logger.info("💰 支付成功，更新訂單狀態...");
//                    updateOrderStatusToPaid(orderId);
//                    return "1|OK";
//                } else {
//                    logger.warn("⚠️ 支付失敗 (rtnCode={})，更新訂單狀態為付款失敗", rtnCode);
//                    updateOrderStatusToPaymentFailed(orderId);
//                    return "1|OK";
//                }
//            } catch (NumberFormatException e) {
//                // 如果無法從merchantTradeNo解析訂單ID，
//                // 使用交易金額查詢可能的訂單
//                logger.warn("⚠️ 無法從merchantTradeNo解析訂單ID: {}", merchantTradeNo);
//                int amount = Integer.parseInt(tradeAmt);
//                
//                List<Order> orders = orderRepository.findByTotalPriceAndOrderStatusCorrespond_Id((double) amount, 1); // 1 = 未付款
//                logger.info("🔎 根據金額找到訂單 ID: {}", orders.get(0).getOrderId());
//
//                if (!orders.isEmpty()) {
//                    Order order = orders.get(0); // 假設只處理第一筆符合的訂單
//                    updateOrderStatusToPaid(order.getOrderId());
//                    return "1|OK";
//                }
//                logger.error("❌ 無法找到對應的訂單");
//                return "0|Error: Order not found";
//            }
//        } catch (Exception e) {
//            logger.error("❌ 處理支付通知時發生例外", e);
//            return "0|Error: " + e.getMessage();
//        }
//    }
//    
	@PostMapping("/notify")
	@ResponseBody
	public String handlePaymentNotification(@RequestParam Map<String, String> responseData) {
		logger.info("📩 接收到綠界支付通知: {}", responseData);
//        boolean isDevMode = true;
		try {
			// 1️⃣ 驗證 CheckMacValue
			if (!paymentService.verifyPaymentResponse(responseData)) {
				logger.error("❌ 支付通知驗證失敗");
				return "0|Error: Verification failed";
//        	if (!isDevMode && !paymentService.verifyPaymentResponse(responseData)) {
//        	    logger.error("❌ 支付通知驗證失敗");
//        	    return "0|Error: Verification failed";
			}

			String merchantTradeNo = responseData.get("MerchantTradeNo");
			String rtnCode = responseData.get("RtnCode");
			String tradeAmt = responseData.get("TradeAmt");

			logger.info("✅ 驗證成功 merchantTradeNo={}, rtnCode={}, tradeAmt={}", merchantTradeNo, rtnCode, tradeAmt);

			// 2️⃣ 解析訂單 ID：ORDER{orderId}_{timestamp}
			String orderIdStr = merchantTradeNo.replaceAll("^ORDER(\\d+)_.*$", "$1");
			int orderId = Integer.parseInt(orderIdStr);

			logger.info("🔍 成功解析出訂單 ID: {}", orderId);

			// 3️⃣ 根據付款狀態更新
			if ("1".equals(rtnCode)) {
				logger.info("💰 綠界回傳成功，準備更新訂單與付款狀態");
				updateOrderStatusToPaid(orderId);
			} else {
				logger.warn("⚠️ 綠界回傳付款失敗 (rtnCode={})，可選擇記錄付款失敗狀態", rtnCode);
			}

			return "1|OK";

		} catch (NumberFormatException e) {
			logger.warn("⚠️ 訂單 ID 解析失敗，嘗試用金額查找：{}", e.getMessage());

			try {
				BigDecimal amount = new BigDecimal(responseData.get("TradeAmt"));
				List<Order> orders = orderRepository.findByTotalPriceAndOrderStatusCorrespond_Id(amount, 1); // 1 = 未付款

				if (!orders.isEmpty()) {
					Order fallbackOrder = orders.get(0);
					updateOrderStatusToPaid(fallbackOrder.getOrderId());
					return "1|OK";
				} else {
					logger.error("❌ 根據金額找不到未付款訂單");
					return "0|Error: Order not found";
				}

			} catch (Exception ex) {
				logger.error("❌ 用金額查訂單時出錯: {}", ex.getMessage(), ex);
				return "0|Error: " + ex.getMessage();
			}

		} catch (Exception e) {
			logger.error("❌ 處理綠界通知時發生錯誤: {}", e.getMessage(), e);
			return "0|Error: " + e.getMessage();
		}
	}

	/**
	 * 處理綠界的 GET 請求（非正式通知，只是為了避免報錯）
	 */
	@GetMapping("/notify")
	public ResponseEntity<String> handleNotifyGetFallback() {
		logger.warn("⚠️ 收到 GET 請求 /api/payment/notify，但綠界正式通知應為 POST");
		return ResponseEntity.ok("請使用 POST 方法通知付款結果");
	}

	/**
	 * 檢查訂單是否可付款
	 */
	@GetMapping("/orders/check-payment/{orderId}")
	public ResponseEntity<?> checkPayment(@PathVariable Integer orderId) {
		try {
			// 獲取訂單
			Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單"));

			// 檢查訂單狀態
			String orderStatus = order.getOrderStatusCorrespond().getName();

			// 列出所有可付款的狀態
			List<String> pendingStatuses = Arrays.asList("PENDING", "待付款", "未付款");

			if (!pendingStatuses.contains(orderStatus)) {
				return ResponseEntity.ok(Map.of("status", "error", "message", "訂單狀態不可付款：" + orderStatus));
			}

			// 檢查付款狀態
			if (order.getPayment() != null && !order.getPayment().isEmpty()) {
				Payment payment = order.getPayment().get(0);
				String paymentStatus = payment.getPaymentStatus() != null ? payment.getPaymentStatus().getName() : "";

				List<String> paidStatuses = Arrays.asList("已付款", "付款完成", "PAID");
				if (paidStatuses.contains(paymentStatus)) {
					return ResponseEntity.ok(Map.of("status", "error", "message", "此訂單已經完成付款"));
				}
			}

			// 訂單可以付款
			return ResponseEntity.ok(Map.of("status", "success", "message", "訂單可以付款"));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	/**
	 * 處理手機轉帳和貨到付款訂單
	 */
	@PostMapping("/orders/{orderId}/payment")
	public ResponseEntity<?> processAlternativePayment(@PathVariable Integer orderId,
			@RequestBody Map<String, String> paymentData) {

		try {
			// 獲取訂單
			Optional<Order> orderOpt = orderRepository.findOrderWithItems(orderId);

			if (!orderOpt.isPresent()) {
				return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "找不到訂單"));
			}

			Order order = orderOpt.get();
			String paymentMethod = paymentData.get("method");

			logger.info("處理替代付款方式: orderId={}, method={}", orderId, paymentMethod);

			// 查詢當前訂單狀態
			String currentStatus = order.getOrderStatusCorrespond().getName();
			logger.info("當前訂單狀態: {}", currentStatus);

			if ("MOBILE_TRANSFER".equals(paymentMethod)) {
				// 處理手機跨行轉帳
				UpdateOrderRequest request = new UpdateOrderRequest();
				request.setStatus("未付款"); // 使用較通用的狀態名稱
				request.setPaymentMethod("MOBILE_TRANSFER"); // 設置付款方式
				orderService.updateOrder(orderId, request);

				// 生成轉帳信息
				Map<String, Object> transferInfo = generateTransferInfo(order);

				return ResponseEntity
						.ok(Map.of("status", "success", "method", "MOBILE_TRANSFER", "transferInfo", transferInfo));
			} else if ("CASH_ON_DELIVERY".equals(paymentMethod)) {
				// 處理貨到付款
				UpdateOrderRequest request = new UpdateOrderRequest();
				request.setStatus("未付款"); // 使用較通用的狀態名稱
				request.setPaymentMethod("CASH_ON_DELIVERY"); // 設置付款方式
				request.setPaymentStatus("待付款"); // 貨到付款的付款狀態
				orderService.updateOrder(orderId, request);

				return ResponseEntity
						.ok(Map.of("status", "success", "method", "CASH_ON_DELIVERY", "message", "訂單已確認，將於送達時付款"));
			} else {
				return ResponseEntity.badRequest()
						.body(Map.of("status", "error", "message", "不支持的支付方式: " + paymentMethod));
			}
		} catch (Exception e) {
			logger.error("處理替代付款方式失敗: orderId={}", orderId, e);
			return ResponseEntity.status(500).body(Map.of("status", "error", "message", e.getMessage()));
		}
	}

	/**
	 * 生成轉帳信息
	 */
	private Map<String, Object> generateTransferInfo(Order order) {
		Map<String, Object> transferInfo = new HashMap<>();

		// 銀行轉帳信息
		transferInfo.put("bankName", "台灣第一銀行");
		transferInfo.put("bankCode", "007");
		transferInfo.put("accountNumber", "123-456-789-000");
		transferInfo.put("accountName", "MyShop 購物網");

		// 設置轉帳期限（當前時間後3天）
		Date deadline = new Date();
		deadline.setTime(deadline.getTime() + 3 * 24 * 60 * 60 * 1000);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		transferInfo.put("transferDeadline", sdf.format(deadline));

		// 轉帳金額
		transferInfo.put("amount", order.getTotalPrice());

		// 轉帳備註（建議客戶填寫訂單編號）
		transferInfo.put("reference", "請備註訂單號: " + order.getOrderId());

		return transferInfo;
	}

	/**
	 * 從訂單獲取商品名稱列表
	 */
	private String getItemNameFromOrder(Order order) {
		StringBuilder itemNames = new StringBuilder();

		// 假設訂單中有orderItem列表
		if (order.getOrderItem() != null && !order.getOrderItem().isEmpty()) {
			order.getOrderItem().forEach(item -> {
				if (itemNames.length() > 0) {
					itemNames.append("#"); // 使用#分隔不同商品
				}
				try {
					// 嘗試獲取產品名稱，優先使用 getName，沒有則使用 getProductName
					String productName = null;
					if (item.getSku() != null && item.getSku().getProduct() != null) {
						try {
							productName = item.getSku().getProduct().getName();
						} catch (Exception e) {
							productName = item.getSku().getProduct().getProductName();
						}
					}
					itemNames.append(productName != null ? productName : "商品");
				} catch (Exception e) {
					itemNames.append("商品"); // 發生異常時使用默認值
				}
			});
		} else {
			itemNames.append("訂單商品");
		}

		return itemNames.toString();
	}

	@Transactional
	private void updateOrderStatusToPaid(Integer orderId) {
		try {
			Optional<Order> orderOpt = orderRepository.findOrderWithItems(orderId);
			if (orderOpt.isEmpty()) {
				logger.error("❌ 找不到訂單：orderId={}", orderId);
				return;
			}

			Order order = orderOpt.get();

			// 1️⃣ 檢查狀態是否為未付款
			if (order.getOrderStatusCorrespond().getId() != 1) {
				logger.warn("⚠️ 訂單狀態非未付款：orderId={}, currentStatus={}", orderId,
						order.getOrderStatusCorrespond().getId());
				return;
			}

			// 2️⃣ 更新訂單狀態為已付款
			OrderStatusCorrespond paidStatus = orderStatusCorrespondRepository.findById(2)
					.orElseThrow(() -> new RuntimeException("❌ 找不到『已付款』狀態記錄"));
			order.setOrderStatusCorrespond(paidStatus);
			orderRepository.save(order);
			logger.info("✅ 訂單狀態已更新為已付款");

			// 3️⃣ 寫入狀態歷史紀錄
			OrderStatusHistory history = new OrderStatusHistory();
			history.setOrder(order);
			history.setOrderStatusCorrespond(paidStatus);
			orderStatusHistoryRepository.save(history);
			logger.info("📝 記錄訂單狀態歷史完成");

			// 4️⃣ 更新付款狀態與付款方式
			List<Payment> payments = paymentRepository.findByOrderOrderId(orderId);
			if (!payments.isEmpty()) {
				Payment payment = payments.get(0);

				// 狀態：已付款
				PaymentStatus paymentStatus = paymentStatusRepository.findByName("已付款").orElseGet(() -> {
					PaymentStatus newStatus = new PaymentStatus();
					newStatus.setName("已付款");
					return paymentStatusRepository.save(newStatus);
				});

				// 方式：信用卡
				PaymentMethod method = paymentMethodRepository.findByNameIgnoreCase("CREDIT").orElseGet(() -> {
					PaymentMethod m = new PaymentMethod();
					m.setName("CREDIT");
					return paymentMethodRepository.save(m);
				});

				payment.setPaymentStatus(paymentStatus);
				payment.setPaymentMethod(method);
				paymentRepository.save(payment);

				logger.info("✅ 付款狀態與付款方式更新完成：已付款 / 信用卡");
			} else {
				logger.warn("⚠️ 找不到付款紀錄，無法更新付款狀態與方式");
			}

		} catch (Exception e) {
			logger.error("❌ 更新訂單為已付款時發生錯誤: {}", e.getMessage(), e);
		}
	}

	/**
	 * 更新訂單狀態為支付失敗
	 */
	private void updateOrderStatusToPaymentFailed(Integer orderId) {
		try {
			Optional<Order> orderOpt = orderRepository.findOrderWithItems(orderId);
			if (orderOpt.isEmpty()) {
				logger.error("❌ 找不到訂單：orderId={}", orderId);
				return;
			}

			Order order = orderOpt.get();

			// 若訂單已非「未付款」，就不處理
			if (order.getOrderStatusCorrespond().getId() != 1) {
				logger.warn("⚠️ 訂單狀態非未付款，不更新：orderId={}, status={}", orderId, order.getOrderStatusCorrespond().getId());
				return;
			}

			// 1️⃣ 查詢「付款失敗」狀態
			OrderStatusCorrespond failedStatus = orderStatusCorrespondRepository.findById(3).orElseGet(() -> {
				OrderStatusCorrespond status = new OrderStatusCorrespond();
				status.setId(3);
				status.setName("付款失敗");
				return orderStatusCorrespondRepository.save(status);
			});

			// 2️⃣ 更新訂單狀態
			order.setOrderStatusCorrespond(failedStatus);
			orderRepository.save(order);
			logger.info("⚠️ 訂單狀態已更新為『付款失敗』");

			// 3️⃣ 紀錄訂單狀態歷史
			OrderStatusHistory history = new OrderStatusHistory();
			history.setOrder(order);
			history.setOrderStatusCorrespond(failedStatus);
			orderStatusHistoryRepository.save(history);

			// 4️⃣ 更新付款狀態為「付款失敗」
			List<Payment> payments = paymentRepository.findByOrderOrderId(orderId);
			if (!payments.isEmpty()) {
				Payment payment = payments.get(0);
				PaymentStatus failedPayStatus = paymentStatusRepository.findByName("付款失敗").orElseGet(() -> {
					PaymentStatus status = new PaymentStatus();
					status.setName("付款失敗");
					return paymentStatusRepository.save(status);
				});

				payment.setPaymentStatus(failedPayStatus);
				paymentRepository.save(payment);
				logger.info("⚠️ 付款狀態已更新為『付款失敗』");
			} else {
				logger.warn("⚠️ 沒有找到付款紀錄，付款狀態無法更新");
			}

		} catch (Exception e) {
			logger.error("❌ 更新訂單狀態為付款失敗時發生錯誤: {}", e.getMessage(), e);
		}

	}

}