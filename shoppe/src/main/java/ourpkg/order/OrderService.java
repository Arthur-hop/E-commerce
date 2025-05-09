package ourpkg.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ourpkg.address.AddressTypeCorrespond;
import ourpkg.address.AddressTypeRepository;
import ourpkg.address.OrderAddress;
import ourpkg.address.OrderAddressRepository;
import ourpkg.address.UserAddress;
import ourpkg.campaign.entity.UserCoupon;
import ourpkg.campaign.repository.UserCouponRepository;
import ourpkg.cart.Cart;
import ourpkg.cart.CartRepository;
import ourpkg.coupon.entity.Coupon;
import ourpkg.coupon.model.DiscountType;
import ourpkg.notification.Notification;
import ourpkg.notification.NotificationRepository;
import ourpkg.payment.Payment;
import ourpkg.payment.PaymentMethod;
import ourpkg.payment.PaymentMethodRepository;
import ourpkg.payment.PaymentRepository;
import ourpkg.payment.PaymentStatus;
import ourpkg.payment.PaymentStatusRepository;
import ourpkg.product.Product;
import ourpkg.product.ProductImageRepository;
import ourpkg.product.ProductRepository;
import ourpkg.review.ReviewRepository;
import ourpkg.shipment.Shipment;
import ourpkg.shipment.ShipmentMethod;
import ourpkg.shipment.ShipmentMethodRepository;
import ourpkg.shipment.ShipmentRepository;
import ourpkg.shipment.ShipmentStatus;
import ourpkg.shipment.ShipmentStatusRepository;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.service.UserService;

@Service
public class OrderService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderStatusCorrespondRepository statusCorrespondRepository;

	@Autowired
	private OrderItemRepository orderItemRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private SkuRepository skuRepository;

	@Autowired
	private UserService userService; // 獲取當前登入用戶

	@Autowired
	private OrderStatusRepository orderStatusRepository;
	
	@Autowired
	private OrderStatusHistoryRepository orderStatusHistoryRepository;

	@Autowired
	private OrderAddressRepository orderAddressRepository;
	
	
	@Autowired
	private ShipmentMethodRepository shipmentMethodRepository;

	@Autowired
	private ShipmentStatusRepository shipmentStatusRepository;


	@Autowired
	private ShipmentRepository shipmentRepository;

	@Autowired
	private AddressTypeRepository addressTypeRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private PaymentStatusRepository paymentStatusRepository;

	@Autowired
	private PaymentMethodRepository paymentMethodRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ReviewRepository reviewRepository;
	
	@Autowired
	private ProductRepository productRepository;
	
	@Autowired
	private UserCouponRepository userCouponRepository;

//新============================================
	@Autowired
	private ProductImageRepository productImageRepository; // 添加 ProductImageRepository 依賴
//新============================================
	

	@Transactional
	public Order createOrderFromUser(String username, OrderRequest request) {
	    // ✅ 查詢使用者
	    User user = userRepository.findByUserName(username)
	        .orElseThrow(() -> new RuntimeException("找不到使用者"));

	    // ✅ 建立收件地址
	    OrderAddress addr = new OrderAddress();
	    addr.setRecipientName(request.getReceiverName());
	    addr.setRecipientPhone(request.getReceiverPhone());
	    addr.setCity(request.getReceiverCity());
	    addr.setDistrict(request.getReceiverDistrict());
	    addr.setZipCode(request.getReceiverZipCode());
	    addr.setStreetEtc(request.getReceiverAddress());

	    AddressTypeCorrespond shippingType = addressTypeRepository.findById(2)
	        .orElseThrow(() -> new RuntimeException("找不到 AddressType 2"));
	    addr.setAddressTypeCorrespond(shippingType);
	    orderAddressRepository.save(addr);

	    // ✅ 訂單狀態（預設：未付款）
	    OrderStatusCorrespond status = statusCorrespondRepository.findById(1)
	        .orElseThrow(() -> new RuntimeException("找不到訂單狀態 ID 1"));

	    // ✅ 建立訂單主檔
	    Order order = new Order();
	    order.setUser(user);
	    order.setOrderAddressBilling(addr);
	    order.setOrderAddressShipping(addr);
	    order.setOrderStatusCorrespond(status);

	    // ✅ 處理訂單商品
	    List<OrderItem> itemList = new ArrayList<>();
	    BigDecimal originalTotal = BigDecimal.ZERO;

	    for (OrderItemRequest itemReq : request.getItems()) {
	        Integer skuId = itemReq.getSkuId();
	        Integer qty = itemReq.getQuantity();

	        Sku sku = skuRepository.findById(skuId)
	            .orElseThrow(() -> new RuntimeException("找不到 SKU：" + skuId));
	        BigDecimal unitPrice = sku.getPrice();
	        BigDecimal subTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
	        originalTotal = originalTotal.add(subTotal);

	        Shop shop = sku.getProduct().getShop();
	        if (shop == null) {
	            throw new RuntimeException("商品沒有對應的商店");
	        }

	        OrderItem item = new OrderItem();
	        item.setOrder(order);
	        item.setSku(sku);
	        item.setQuantity(qty);
	        item.setUnitPrice(unitPrice);
	        item.setShop(shop);

	        itemList.add(item);
	    }

	    BigDecimal finalTotal = originalTotal;

	 // ✅ 處理優惠券邏輯（新增部分）
	    if (request.getUserCouponId() != null) {
	        Integer userCouponId = request.getUserCouponId();
	        UserCoupon userCoupon = userCouponRepository.findById(userCouponId)
	            .orElseThrow(() -> new RuntimeException("找不到優惠券"));

	        if (!userCoupon.getUserId().equals(user.getUserId())) {
	            throw new RuntimeException("這張優惠券不屬於你");
	        }

	        if ("USED".equalsIgnoreCase(userCoupon.getStatus())) {
	            throw new RuntimeException("這張優惠券已使用過，不能再次使用");
	        }

	        if (!"ACTIVE".equalsIgnoreCase(userCoupon.getStatus())) {
	            throw new RuntimeException("優惠券已使用或失效");
	        }

	        Coupon coupon = userCoupon.getCoupon();
	        Integer shopIdInOrder = itemList.get(0).getShop().getShopId();

	        if (!coupon.getShop().getShopId().equals(shopIdInOrder)) {
	            throw new RuntimeException("這張優惠券不適用於該商店");
	        }

	        Date now = new Date();
	        if (now.before(coupon.getStartDate()) || now.after(coupon.getEndDate())) {
	            throw new RuntimeException("優惠券不在有效期內");
	        }

	        DiscountType discountType;
	        try {
	            discountType = DiscountType.valueOf(coupon.getDiscountType().toUpperCase());
	        } catch (IllegalArgumentException e) {
	            throw new RuntimeException("❌ 不支援的折扣類型：" + coupon.getDiscountType());
	        }

	        // ✅ 根據折扣類型計算
	        switch (discountType) {
	            case PERCENTAGE -> {
	                finalTotal = originalTotal.multiply(BigDecimal.ONE.subtract(
	                    coupon.getDiscountValue().divide(BigDecimal.valueOf(100))
	                ));
	            }
	            case FIXED_AMOUNT -> {
	                finalTotal = originalTotal.subtract(coupon.getDiscountValue());
	            }
	            default -> throw new RuntimeException("❌ 未知折扣類型");
	        }

	        // ✅ 最少金額為 1 元，避免串接綠界錯誤
	        finalTotal = finalTotal.max(BigDecimal.ONE);

	        LocalDateTime currentDateTime = LocalDateTime.now();
	        userCouponRepository.useCoupon(userCouponId, currentDateTime, order.getOrderId());
	    }


	    // ✅ 設定訂單總金額（折扣後）
	    order.setTotalPrice(finalTotal);
	    order.setOrderItem(itemList);
	    order = orderRepository.save(order);
	    orderItemRepository.saveAll(itemList);

	    System.out.println("✅ 訂單建立成功，總金額：" + finalTotal);

	    // ✅ 處理付款方式（支援英文代碼）
	    String methodInput = request.getPaymentMethod().trim();
	    Optional<PaymentMethod> optionalMethod = paymentMethodRepository.findByNameIgnoreCase(methodInput);

	    if (optionalMethod.isEmpty()) {
	        String mappedName = switch (methodInput.toUpperCase()) {
	            case "CREDIT" -> "信用卡";
	            case "CASH_ON_DELIVERY" -> "貨到付款";
	            case "ATM" -> "ATM";
	            case "APPLE_PAY" -> "Apple Pay";
	            case "LINE_PAY" -> "LINE Pay";
	            default -> throw new RuntimeException("❌ 不支援的付款方式：" + methodInput);
	        };
	        optionalMethod = paymentMethodRepository.findByNameIgnoreCase(mappedName);
	    }

	    PaymentMethod method = optionalMethod
	        .orElseThrow(() -> new RuntimeException("找不到付款方式：" + methodInput));

	    PaymentStatus payStatus = paymentStatusRepository.findByName("未付款")
	        .orElseThrow(() -> new RuntimeException("找不到付款狀態"));

	    // ✅ 建立付款資訊（不需寫入金額欄位，若沒有對應欄位就省略）
	    Payment payment = new Payment();
	    payment.setOrder(order);
	    payment.setPaymentMethod(method);
	    payment.setPaymentStatus(payStatus);
	    paymentRepository.save(payment);

	    return order;
	}



	
	
	
	
	
	public Order getOrderById(int orderId) {
		return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單 ID: " + orderId));
	}

	@Transactional
	public OrderDto checkout(int userId) {
		// 取得使用者
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用戶不存在"));

		// 取得購物車內的商品
		List<Cart> cartItems = cartRepository.findByUser_UserId(userId);
		if (cartItems.isEmpty()) {
			throw new RuntimeException("購物車內沒有商品");
		}

		// 計算總價
		BigDecimal totalPrice = BigDecimal.ZERO;
		List<OrderItem> orderItems = new ArrayList<>();

		for (Cart cart : cartItems) {
			Sku sku = cart.getSku();
			if (sku.getStock() < cart.getQuantity()) {
				throw new RuntimeException("庫存不足，無法結帳");
			}
			totalPrice = totalPrice.add(sku.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
		}

		Optional<OrderAddress> byIdopt = orderAddressRepository.findById(1);
		Optional<OrderStatusCorrespond> statusopt = statusCorrespondRepository.findById(1);

		if (byIdopt.isEmpty() || statusopt.isEmpty()) {
			throw new RuntimeException("地址或訂單狀態對應資料缺失");
		}

		OrderAddress orderAddress = byIdopt.get();
		OrderStatusCorrespond orderStatusCorrespond = statusopt.get();

		Order order = new Order();
		order.setUser(user);
		order.setTotalPrice(totalPrice);
		order.setOrderItem(orderItems);
		order.setOrderAddressBilling(orderAddress);
		order.setOrderAddressShipping(orderAddress);
		order.setOrderStatusCorrespond(orderStatusCorrespond);

		orderRepository.save(order);

		for (Cart cart : cartItems) {
			Sku sku = cart.getSku();
			if (sku.getStock() < cart.getQuantity()) {
				throw new RuntimeException("庫存不足，無法結帳");
			}

			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setSku(sku);
			orderItem.setQuantity(cart.getQuantity());
			orderItem.setUnitPrice(sku.getPrice());

			Shop shop = sku.getProduct() != null ? sku.getProduct().getShop() : null;
			if (shop == null) {
				throw new RuntimeException("商品無對應商店，無法結帳");
			}
			orderItem.setShop(shop);
			orderItems.add(orderItem);

			sku.setStock(sku.getStock() - cart.getQuantity());
			skuRepository.save(sku);
		}

		orderItemRepository.saveAll(orderItems);

		// ✅ 建立付款紀錄
		Payment payment = new Payment();
		payment.setOrder(order);
		payment.setPaymentStatusById(1, paymentStatusRepository);
		payment.setPaymentMethodById(1, paymentMethodRepository);
		paymentRepository.save(payment);

		// ✅ 清空購物車
		cartRepository.deleteAll(cartItems);

		return new OrderDto(order.getOrderId(), order.getTotalPrice(), "訂單建立成功");
	}

	// 🔥 買家查詢自己的訂單
	public List<Order> getOrdersForUser(Integer userId) {
		return orderRepository.findOrdersByUserId(userId);
	}

	// 🔥 賣家查詢自己的訂單
	public List<Order> getOrdersForSeller(Integer sellerId) {
		List<Order> orders = orderRepository.findOrdersByShopOwner(sellerId);

		if (orders.isEmpty()) {
			System.out.println("⚠️ 賣家 (ID: " + sellerId + ") 沒有任何訂單");
		} else {
			System.out.println("✅ 賣家 (ID: " + sellerId + ") 的訂單數量：" + orders.size());
		}

		return orders;
	}

	public List<Order> getAllOrders() {
		List<Order> orders = orderRepository.findAllOrdersWithItems();

		if (orders.isEmpty()) {
			System.out.println("⚠️ 沒有任何訂單！");
		} else {
			System.out.println("✅ 總共找到 " + orders.size() + " 筆訂單");
		}

		return orders;
	}
	// 英文狀態代碼對應中文名稱
	private static final Map<String, String> ORDER_STATUS_MAP = Map.of(
	    "PENDING", "未付款",
	    "PAID", "已付款",
	    "PREPARING", "備貨中",
	    "PROCESSING", "處理中",
	    "SHIPPED", "已出貨",
	    "DELIVERED", "已送達",
	    "CANCELLED", "已取消",
	    "COMPLETED", "已完成"
	);
	/**
	 * 更新訂單資訊
	 * 
	 * @param orderId 訂單ID
	 * @param request 更新請求
	 * @return 更新後的訂單
	 */
	@Transactional
	public Order updateOrder(int orderId, UpdateOrderRequest request) {
		try {
			System.out.println("開始更新訂單: " + orderId);
			System.out.println("更新請求內容: " + request);

			Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("訂單不存在"));

			// ✅ 更新訂單狀態（支援中英文轉換）
	        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
	            try {
	                String inputStatus = request.getStatus().trim();
	                String mappedStatus = ORDER_STATUS_MAP.getOrDefault(inputStatus.toUpperCase(), inputStatus);

	                System.out.println("嘗試更新訂單狀態: " + mappedStatus);

	                OrderStatusCorrespond orderStatus = orderStatusRepository.findByName(mappedStatus)
	                    .orElseThrow(() -> new RuntimeException("無效的訂單狀態: " + mappedStatus));

	                System.out.println("找到訂單狀態: " + orderStatus.getName() + ", ID: " + orderStatus.getId());
	                order.setOrderStatusCorrespond(orderStatus);
	            } catch (Exception e) {
	                System.err.println("更新訂單狀態時出錯: " + e.getMessage());
	                throw new RuntimeException("更新訂單狀態失敗: " + e.getMessage(), e);
	            }
	        }

			// 更新付款資訊
			if ((request.getPaymentMethod() != null && !request.getPaymentMethod().isEmpty())
					|| (request.getPaymentStatus() != null && !request.getPaymentStatus().isEmpty())) {
				updatePaymentInfo(order, request);
			}

			// 更新運送資訊
			if ((request.getShipmentMethod() != null && !request.getShipmentMethod().isEmpty())
					|| (request.getShipmentStatus() != null && !request.getShipmentStatus().isEmpty())) {
				updateShippingInfo(order, request);
			}

			// 更新帳單地址
			if (request.getBillingAddress() != null && !request.getBillingAddress().isEmpty()) {
				try {
					System.out.println("嘗試更新帳單地址: " + request.getBillingAddress());
					OrderAddress billingAddress = order.getOrderAddressBilling();
					if (billingAddress == null) {
						System.out.println("創建新的帳單地址");
						billingAddress = new OrderAddress();

						// 設置必要的關聯屬性
						AddressTypeCorrespond billingType = null;
						try {
							billingType = addressTypeRepository.findByName("Billing").orElse(null);

							if (billingType == null) {
								System.out.println("Billing地址類型不存在，正在創建");
								// 創建新的地址類型
								billingType = new AddressTypeCorrespond();
								billingType.setName("Billing");
								billingType = addressTypeRepository.save(billingType);
							}
						} catch (Exception e) {
							// 如果創建過程中發生異常，嘗試再次查詢
							System.out.println("創建Billing地址類型時發生異常，再次嘗試查詢: " + e.getMessage());
							billingType = addressTypeRepository.findByName("Billing").orElse(null);

							if (billingType == null) {
								System.err.println("最終無法找到或創建Billing地址類型");
								throw new RuntimeException("設置帳單地址失敗: 無法找到或創建地址類型");
							}
						}

						billingAddress.setAddressTypeCorrespond(billingType);

						// 儲存地址以獲取ID
						billingAddress = orderAddressRepository.save(billingAddress);

						// 設定訂單的帳單地址
						order.setOrderAddressBilling(billingAddress);
					}

					// 嘗試解析帳單地址格式
					updateAddressFields(billingAddress, request.getBillingAddress());

					// 保存更新的地址
					System.out.println("保存帳單地址");
					orderAddressRepository.save(billingAddress);
				} catch (Exception e) {
					System.err.println("更新帳單地址時出錯: " + e.getMessage());
					throw new RuntimeException("更新帳單地址失敗: " + e.getMessage(), e);
				}
			}

			// 更新收貨地址
			if (request.getShippingAddress() != null && !request.getShippingAddress().isEmpty()) {
				try {
					System.out.println("嘗試更新收貨地址: " + request.getShippingAddress());
					OrderAddress shippingAddress = order.getOrderAddressShipping();
					if (shippingAddress == null) {
						System.out.println("創建新的收貨地址");
						shippingAddress = new OrderAddress();

						// 設置必要的關聯屬性
						AddressTypeCorrespond shippingType = null;
						try {
							shippingType = addressTypeRepository.findByName("Shipping").orElse(null);

							if (shippingType == null) {
								System.out.println("Shipping地址類型不存在，正在創建");
								// 創建新的地址類型
								shippingType = new AddressTypeCorrespond();
								shippingType.setName("Shipping");
								shippingType = addressTypeRepository.save(shippingType);
							}
						} catch (Exception e) {
							// 如果創建過程中發生異常，嘗試再次查詢
							System.out.println("創建Shipping地址類型時發生異常，再次嘗試查詢: " + e.getMessage());
							shippingType = addressTypeRepository.findByName("Shipping").orElse(null);

							if (shippingType == null) {
								System.err.println("最終無法找到或創建Shipping地址類型");
								throw new RuntimeException("設置收貨地址失敗: 無法找到或創建地址類型");
							}
						}

						shippingAddress.setAddressTypeCorrespond(shippingType);

						// 儲存地址以獲取ID
						shippingAddress = orderAddressRepository.save(shippingAddress);

						// 設定訂單的收貨地址
						order.setOrderAddressShipping(shippingAddress);
					}

					// 嘗試解析收貨地址格式
					updateAddressFields(shippingAddress, request.getShippingAddress());

					// 保存更新的地址
					System.out.println("保存收貨地址");
					orderAddressRepository.save(shippingAddress);
				} catch (Exception e) {
					System.err.println("更新收貨地址時出錯: " + e.getMessage());
					throw new RuntimeException("更新收貨地址失敗: " + e.getMessage(), e);
				}
			}

			// 保存更新的訂單
			System.out.println("保存更新的訂單");
			Order updatedOrder = orderRepository.save(order);
			System.out.println("✅ 訂單建立成功，總金額：" + order.getTotalPrice());
			System.out.println("訂單更新成功: " + updatedOrder.getOrderId());
			return updatedOrder;
		} catch (Exception e) {
			System.err.println("更新訂單過程中發生錯誤: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("更新訂單失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 更新付款資訊 這個方法處理付款資訊的更新，確保在數據庫操作前正確初始化所有必要的關聯
	 */
	private void updatePaymentInfo(Order order, UpdateOrderRequest request) {
		try {
			System.out.println("嘗試更新付款信息");

			// 1. 首先查詢付款方式（如果指定了）
			PaymentMethod paymentMethod = null;
			if (request.getPaymentMethod() != null && !request.getPaymentMethod().isEmpty()) {
				try {
					System.out.println("嘗試查找付款方式: " + request.getPaymentMethod());
					paymentMethod = paymentMethodRepository.findByNameIgnoreCase(request.getPaymentMethod()).orElse(null);

					if (paymentMethod == null) {
						System.out.println("付款方式不存在，正在創建: " + request.getPaymentMethod());
						// 創建並保存新的付款方式
						paymentMethod = new PaymentMethod();
						paymentMethod.setName(request.getPaymentMethod());
						paymentMethod = paymentMethodRepository.save(paymentMethod);
						System.out.println("成功創建付款方式: " + paymentMethod.getName() + ", ID: " + paymentMethod.getId());
					} else {
						System.out.println("找到現有付款方式: " + paymentMethod.getName() + ", ID: " + paymentMethod.getId());
					}
				} catch (Exception e) {
					System.out.println("查詢或創建付款方式時出錯: " + e.getMessage());
					// 再次嘗試查詢，如果失敗則忽略付款方式更新
					paymentMethod = paymentMethodRepository.findByNameIgnoreCase(request.getPaymentMethod()).orElse(null);
				}
			}

			// 2. 查詢付款狀態（如果指定了）
			PaymentStatus paymentStatus = null;
			if (request.getPaymentStatus() != null && !request.getPaymentStatus().isEmpty()) {
				try {
					System.out.println("嘗試查找付款狀態: " + request.getPaymentStatus());
					paymentStatus = paymentStatusRepository.findByName(request.getPaymentStatus()).orElse(null);

					if (paymentStatus == null) {
						System.out.println("付款狀態不存在，正在創建: " + request.getPaymentStatus());
						// 創建並保存新的付款狀態
						paymentStatus = new PaymentStatus();
						paymentStatus.setName(request.getPaymentStatus());
						paymentStatus = paymentStatusRepository.save(paymentStatus);
						System.out.println("成功創建付款狀態: " + paymentStatus.getName() + ", ID: " + paymentStatus.getId());
					} else {
						System.out.println("找到現有付款狀態: " + paymentStatus.getName() + ", ID: " + paymentStatus.getId());
					}
				} catch (Exception e) {
					System.out.println("查詢或創建付款狀態時出錯: " + e.getMessage());
					// 再次嘗試查詢，如果失敗則忽略付款狀態更新
					paymentStatus = paymentStatusRepository.findByName(request.getPaymentStatus()).orElse(null);
				}
			}

			// 3. 如果沒有付款方式，則不需要繼續處理
			// 因為payment_method_id是必填欄位，所以必須有付款方式才能繼續
			if (paymentMethod == null) {
				System.out.println("沒有提供有效的付款方式，無法更新付款信息");
				return;
			}

			// 4. 現在處理付款信息 - 直接使用Order的現有Payment列表
			Payment payment = null;

			if (order.getPayment() != null && !order.getPayment().isEmpty()) {
				// 使用現有付款記錄
				payment = order.getPayment().get(0); // 獲取最新的付款記錄
				System.out.println("找到現有付款記錄, ID: " + payment.getPaymentId());

				// 更新付款方式
				payment.setPaymentMethod(paymentMethod);
				System.out.println("更新現有付款記錄的付款方式: " + paymentMethod.getName());

				// 更新付款狀態（如果有提供）
				if (paymentStatus != null) {
					payment.setPaymentStatus(paymentStatus);
					System.out.println("更新現有付款記錄的付款狀態: " + paymentStatus.getName());
				}

				// 保存更新後的付款記錄
				payment = paymentRepository.save(payment);
				System.out.println("付款記錄已更新，ID: " + payment.getPaymentId());
			} else {
				// 如果沒有付款記錄，創建一個新的
				System.out.println("創建新的付款記錄");
				payment = new Payment();
				payment.setOrder(order);

				// 重要：在保存前設置付款方式（必填欄位）
				payment.setPaymentMethod(paymentMethod);
				System.out.println("設置新付款記錄的付款方式: " + paymentMethod.getName());

				// 設置付款狀態（如果有提供）
				if (paymentStatus != null) {
					payment.setPaymentStatus(paymentStatus);
					System.out.println("設置新付款記錄的付款狀態: " + paymentStatus.getName());
				}

				// 建立關聯並保存
				if (order.getPayment() == null) {
					order.setPayment(new ArrayList<>());
				}

				// 先保存訂單以確保訂單ID存在
				order = orderRepository.save(order);

				// 保存付款記錄以獲取ID
				payment = paymentRepository.save(payment);
				System.out.println("新付款記錄已保存，ID: " + payment.getPaymentId());

				// 將付款添加到訂單的付款列表中
				order.getPayment().add(payment);
				order = orderRepository.save(order);
			}

		} catch (Exception e) {
			System.err.println("更新付款信息時出錯: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("更新付款信息失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 更新物流資訊 與付款資訊更新類似，確保在數據庫操作前正確初始化所有必要的關聯
	 */
	private void updateShippingInfo(Order order, UpdateOrderRequest request) {
		try {
			System.out.println("嘗試更新運送信息");

			// 1. 首先查詢運送方式（如果指定了）
			ShipmentMethod shipmentMethod = null;
			if (request.getShipmentMethod() != null && !request.getShipmentMethod().isEmpty()) {
				try {
					System.out.println("嘗試查找運送方式: " + request.getShipmentMethod());
					shipmentMethod = shipmentMethodRepository.findByName(request.getShipmentMethod()).orElse(null);

					if (shipmentMethod == null) {
						System.out.println("運送方式不存在，正在創建: " + request.getShipmentMethod());
						// 創建並保存新的運送方式
						shipmentMethod = new ShipmentMethod();
						shipmentMethod.setName(request.getShipmentMethod());
						shipmentMethod = shipmentMethodRepository.save(shipmentMethod);
						System.out.println("成功創建運送方式: " + shipmentMethod.getName() + ", ID: " + shipmentMethod.getId());
					} else {
						System.out.println("找到現有運送方式: " + shipmentMethod.getName() + ", ID: " + shipmentMethod.getId());
					}
				} catch (Exception e) {
					System.out.println("查詢或創建運送方式時出錯: " + e.getMessage());
					// 再次嘗試查詢，如果失敗則忽略運送方式更新
					shipmentMethod = shipmentMethodRepository.findByName(request.getShipmentMethod()).orElse(null);
				}
			}

			// 2. 查詢運送狀態（如果指定了）
			ShipmentStatus shipmentStatus = null;
			if (request.getShipmentStatus() != null && !request.getShipmentStatus().isEmpty()) {
				try {
					System.out.println("嘗試查找運送狀態: " + request.getShipmentStatus());
					shipmentStatus = shipmentStatusRepository.findByName(request.getShipmentStatus()).orElse(null);

					if (shipmentStatus == null) {
						System.out.println("運送狀態不存在，正在創建: " + request.getShipmentStatus());
						// 創建並保存新的運送狀態
						shipmentStatus = new ShipmentStatus();
						shipmentStatus.setName(request.getShipmentStatus());
						shipmentStatus = shipmentStatusRepository.save(shipmentStatus);
						System.out.println("成功創建運送狀態: " + shipmentStatus.getName() + ", ID: " + shipmentStatus.getId());
					} else {
						System.out.println("找到現有運送狀態: " + shipmentStatus.getName() + ", ID: " + shipmentStatus.getId());
					}
				} catch (Exception e) {
					System.out.println("查詢或創建運送狀態時出錯: " + e.getMessage());
					// 再次嘗試查詢，如果失敗則忽略運送狀態更新
					shipmentStatus = shipmentStatusRepository.findByName(request.getShipmentStatus()).orElse(null);
				}
			}

			// 3. 確認是否已存在 shipment_method_id 必填項
			// 同樣的，如果 shipment_method_id 是必填項，則需要 shipmentMethod 不為空
			if (shipmentMethod == null) {
				System.out.println("沒有提供有效的運送方式，無法更新運送信息");
				return;
			}

			// 4. 現在處理運送信息 - 直接使用Order的現有Shipment列表
			Shipment shipment = null;

			if (order.getShipment() != null && !order.getShipment().isEmpty()) {
				// 使用現有運送記錄
				shipment = order.getShipment().get(0); // 獲取最新的運送記錄
				System.out.println("找到現有運送記錄, ID: " + shipment.getShipmentId());

				// 更新運送方式
				shipment.setShipmentMethod(shipmentMethod);
				System.out.println("更新現有運送記錄的運送方式: " + shipmentMethod.getName());

				// 更新運送狀態（如果有提供）
				if (shipmentStatus != null) {
					shipment.setShipmentStatus(shipmentStatus);
					System.out.println("更新現有運送記錄的運送狀態: " + shipmentStatus.getName());
				}

				// 保存更新後的運送記錄
				shipment = shipmentRepository.save(shipment);
				System.out.println("運送記錄已更新，ID: " + shipment.getShipmentId());
			} else {
				// 如果沒有運送記錄，創建一個新的
				System.out.println("創建新的運送記錄");
				shipment = new Shipment();
				shipment.setOrder(order);

				// 重要：在保存前設置運送方式（可能是必填欄位）
				shipment.setShipmentMethod(shipmentMethod);
				System.out.println("設置新運送記錄的運送方式: " + shipmentMethod.getName());

				// 設置運送狀態（如果有提供）
				if (shipmentStatus != null) {
					shipment.setShipmentStatus(shipmentStatus);
					System.out.println("設置新運送記錄的運送狀態: " + shipmentStatus.getName());
				}

				// 建立關聯並保存
				if (order.getShipment() == null) {
					order.setShipment(new ArrayList<>());
				}

				// 先保存訂單以確保訂單ID存在
				order = orderRepository.save(order);

				// 保存運送記錄以獲取ID
				shipment = shipmentRepository.save(shipment);
				System.out.println("新運送記錄已保存，ID: " + shipment.getShipmentId());

				// 將運送記錄添加到訂單的運送列表中
				order.getShipment().add(shipment);
				order = orderRepository.save(order);
			}

		} catch (Exception e) {
			System.err.println("更新運送信息時出錯: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("更新運送信息失敗: " + e.getMessage(), e);
		}
	}

	/**
	 * 嘗試解析地址字符串並更新地址字段
	 */
	private void updateAddressFields(OrderAddress address, String addressStr) {
		// 嘗試解析格式: "地址 (收件人: 名稱, 電話: 號碼)"
		String recipientName = null;
		String recipientPhone = null;
		String addressText = addressStr;

		int bracketIndex = addressStr.indexOf(" (");
		if (bracketIndex > 0) {
			addressText = addressStr.substring(0, bracketIndex).trim();
			String infoText = addressStr.substring(bracketIndex + 2, addressStr.length() - 1);

			// 提取收件人和電話
			int recipientIndex = infoText.indexOf("收件人: ");
			int phoneIndex = infoText.indexOf("電話: ");

			if (recipientIndex >= 0) {
				int endIndex = phoneIndex > 0 ? infoText.indexOf(", ", recipientIndex) : infoText.length();
				recipientName = infoText.substring(recipientIndex + 5, endIndex).trim();
			}

			if (phoneIndex >= 0) {
				recipientPhone = infoText.substring(phoneIndex + 4).trim();
			}
		}

		// 解析地址部分 (城市、區域、郵編等)
		String[] parts = addressText.split(" ");
		if (parts.length >= 3) {
			address.setCity(parts[0]);
			address.setDistrict(parts[1]);
			address.setZipCode(parts[2]);

			// 其餘部分視為街道地址
			StringBuilder streetEtc = new StringBuilder();
			for (int i = 3; i < parts.length; i++) {
				if (i > 3)
					streetEtc.append(" ");
				streetEtc.append(parts[i]);
			}
			address.setStreetEtc(streetEtc.toString());
		} else {
			// 如果無法解析，則將整個地址存儲在 streetEtc 字段中
			address.setStreetEtc(addressText);
		}

		// 設置收件人和電話
		if (recipientName != null) {
			address.setRecipientName(recipientName);
		}

		if (recipientPhone != null) {
			address.setRecipientPhone(recipientPhone);
		}
	}

	/**
	 * 刪除訂單
	 * 
	 * @param orderId 訂單ID
	 */
	@Transactional
	public void deleteOrder(int orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("訂單不存在"));

		// 刪除訂單前的任何必要清理
		orderRepository.delete(order);
	}

	@Transactional
	public OrderDto createAdvancedOrder(int userId) {
		// 1. 驗證用戶
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("用戶不存在"));

		// 2. 檢查購物車
		List<Cart> cartItems = cartRepository.findByUser_UserId(userId);
		if (cartItems.isEmpty()) {
			throw new RuntimeException("購物車內沒有商品");
		}

		// 3. 準備訂單資訊
		BigDecimal totalPrice = calculateTotalPrice(cartItems);
		OrderAddress defaultAddress = getDefaultAddress();
		OrderStatusCorrespond pendingStatus = getPendingOrderStatus();

		// 4. 創建訂單
		Order order = createOrderEntity(user, totalPrice, defaultAddress, pendingStatus);

		// 5. 處理訂單項目
		List<OrderItem> orderItems = processOrderItems(order, cartItems);

		// 6. 建立付款記錄
		createPaymentRecord(order);

		// 7. 清空購物車
		cartRepository.deleteAll(cartItems);

		Payment payment = createPaymentRecord(order);

		// 8. 準備 OrderDto
		return createOrderDto(order, user, defaultAddress, orderItems);
	}

	// 計算總價
	private BigDecimal calculateTotalPrice(List<Cart> cartItems) {
		return cartItems.stream().map(cart -> {
			Sku sku = cart.getSku();
			if (sku.getStock() < cart.getQuantity()) {
				throw new RuntimeException("庫存不足，無法結帳");
			}
			return sku.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity()));
		}).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// 獲取預設地址
	private OrderAddress getDefaultAddress() {
		return orderAddressRepository.findById(1).orElseThrow(() -> new RuntimeException("預設地址不存在"));
	}

	// 獲取訂單待付款狀態
	private OrderStatusCorrespond getPendingOrderStatus() {
	    return statusCorrespondRepository.findByName("未付款").orElseGet(() -> {
	        OrderStatusCorrespond newStatus = new OrderStatusCorrespond();
	        newStatus.setName("未付款");
	        return statusCorrespondRepository.save(newStatus);
	    });
	}

	// 創建訂單實體
	private Order createOrderEntity(User user, BigDecimal totalPrice, OrderAddress defaultAddress,
			OrderStatusCorrespond pendingStatus) {
		Order order = new Order();
		order.setUser(user);
		order.setTotalPrice(totalPrice);
		order.setOrderAddressBilling(defaultAddress);
		order.setOrderAddressShipping(defaultAddress);
		order.setOrderStatusCorrespond(pendingStatus);

		return orderRepository.save(order);
	}

	// 處理訂單項目
	private List<OrderItem> processOrderItems(Order order, List<Cart> cartItems) {
		List<OrderItem> orderItems = new ArrayList<>();

		for (Cart cart : cartItems) {
			Sku sku = cart.getSku();

			// 檢查庫存
			if (sku.getStock() < cart.getQuantity()) {
				throw new RuntimeException("庫存不足，無法結帳");
			}

			// 創建訂單項目
			OrderItem orderItem = new OrderItem();
			orderItem.setOrder(order);
			orderItem.setSku(sku);
			orderItem.setQuantity(cart.getQuantity());
			orderItem.setUnitPrice(sku.getPrice());

			// 檢查商店
			Shop shop = Optional.ofNullable(sku.getProduct()).map(Product::getShop)
					.orElseThrow(() -> new RuntimeException("商品無對應商店，無法結帳"));

			orderItem.setShop(shop);
			orderItems.add(orderItem);

			// 更新庫存
			sku.setStock(sku.getStock() - cart.getQuantity());
			skuRepository.save(sku);
		}

		return orderItemRepository.saveAll(orderItems);
	}

	private OrderDto createOrderDto(Order order, User user, OrderAddress defaultAddress, List<OrderItem> orderItems) {
		List<OrderItemDto> orderItemDtos = orderItems.stream()
//原============================================
//				.map(item -> new OrderItemDto(item.getSku().getProduct().getProductId(),
//						item.getSku().getProduct().getProductName(), item.getQuantity(), item.getUnitPrice(),
//						item.getSku().getProduct().getImage()))
//原============================================
//新============================================
				.map(item -> {
					Product product = item.getSku().getProduct();
					// 獲取產品主圖片路徑
					String imagePath = productImageRepository.findPrimaryImageByProduct_ProductId(product.getProductId())
							.orElse("");
					
					return new OrderItemDto(
							product.getProductId(),
							product.getProductName(),
							item.getQuantity(),
							item.getUnitPrice(),
							imagePath);
				})
//新============================================
				.collect(Collectors.toList());

		// 支付狀態處理
		String paymentMethod = null;
		String paymentStatus = "未付款"; // 默認狀態

		if (order.getPayment() != null && !order.getPayment().isEmpty()) {
			Payment payment = order.getPayment().get(0);
			if (payment.getPaymentStatus() != null) {
				paymentStatus = payment.getPaymentStatus().getName();
			}
			// 不需要else，因為已經有默認值

			if (payment.getPaymentMethod() != null) {
				paymentMethod = payment.getPaymentMethod().getName();
			}
		}

		// 運送狀態處理
		String shipmentMethod = null;
		String shipmentStatus = null;
		String trackingNumber = null;

		if (order.getShipment() != null && !order.getShipment().isEmpty()) {
			Shipment shipment = order.getShipment().get(0);
			if (shipment.getShipmentMethod() != null) {
				shipmentMethod = shipment.getShipmentMethod().getName();
			}
			if (shipment.getShipmentStatus() != null) {
				shipmentStatus = shipment.getShipmentStatus().getName();
			}

			// 由於沒有直接取得追蹤碼的方法，我們暫時不設置追蹤碼
			// 如果將來在 Shipment 類中添加了相關方法，可以在這裡使用
			// trackingNumber = 某個獲取方法;
		}

		return new OrderDto(order.getOrderId(), order.getTotalPrice(), order.getOrderStatusCorrespond().getName(),
				orderItemDtos, order.getCreatedAt(), order.getUpdatedAt(), user.getUserId(), user.getUsername(),
				user.getEmail(), user.getPhone(), formatAddress(defaultAddress), // 帳單地址
				formatAddress(defaultAddress), // 送貨地址
				paymentMethod, paymentStatus, shipmentMethod, shipmentStatus, trackingNumber);
	}

	/**
	 * 為訂單創建支付記錄
	 * 
	 * @param order 需要創建支付記錄的訂單
	 * @return 創建的支付記錄
	 */
	private Payment createPaymentRecord(Order order) {
		// 創建新的支付對象
		Payment payment = new Payment();

		// 設置此支付關聯的訂單
		payment.setOrder(order);

		// 設置初始支付狀態為「未付款」
		PaymentStatus initialStatus = paymentStatusRepository.findByName("未付款").orElseGet(() -> {
			PaymentStatus newStatus = new PaymentStatus();
			newStatus.setName("未付款");
			return paymentStatusRepository.save(newStatus);
		});
		payment.setPaymentStatus(initialStatus);

		// 設置默認支付方式（如果有）
		PaymentMethod defaultMethod = paymentMethodRepository.findById(1).orElseGet(() -> {
			PaymentMethod newMethod = new PaymentMethod();
			newMethod.setName("信用卡");
			return paymentMethodRepository.save(newMethod);
		});
		payment.setPaymentMethod(defaultMethod);

		// 保存支付記錄
		payment = paymentRepository.save(payment);

		// 確保訂單有支付列表
		if (order.getPayment() == null) {
			order.setPayment(new ArrayList<>());
		}

		// 添加支付記錄到訂單
		order.getPayment().add(payment);

		return payment;
	}

	// 地址格式化方法
	private String formatAddress(OrderAddress address) {
		if (address == null)
			return "未提供地址";

		return String.format("%s %s %s %s (收件人: %s, 電話: %s)", address.getCity(), address.getDistrict(),
				address.getZipCode(), address.getStreetEtc(), address.getRecipientName(), address.getRecipientPhone());
	}

	@Transactional
	public Order cancelOrder(int orderId) {
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("訂單不存在"));

		OrderStatusCorrespond canceledStatus = statusCorrespondRepository.findByName("已取消")
				.orElseThrow(() -> new RuntimeException("訂單狀態 '已取消' 未定義"));

		order.setOrderStatusCorrespond(canceledStatus);
		return orderRepository.save(order);
	}

	// 賣家出貨通知買家
	public void shipOrder(int orderId) {
		// 1. 查訂單
		Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("訂單不存在"));

		// 2. 查「配送中」狀態對應物件
		OrderStatusCorrespond shippedStatus = statusCorrespondRepository.findById(4)
				.orElseThrow(() -> new RuntimeException("訂單狀態 '配送中' 未定義"));

		// 3. 更新訂單狀態為「配送中」
		order.setOrderStatusCorrespond(shippedStatus);
		orderRepository.save(order);

		// 4. 發送通知給買家
		Notification notify = new Notification();
		notify.setRecipientUserId(order.getUser().getUserId());
		notify.setMessage("您的訂單 #" + order.getOrderId() + " 已出貨，請留意收貨！");
		notificationRepository.save(notify);
	}

	// 評價
	public List<ProductReviewInfo> getCompletedOrderProductsByUserId(Integer userId) {
		List<Order> orders = orderRepository.findByUserUserIdAndStatus(userId, "已完成");

		List<ProductReviewInfo> result = new ArrayList<>();

        for (Order order : orders) {
            // 這裡要改成 getOrderItem()
            for (OrderItem item : order.getOrderItem()) {
                Product product = item.getSku().getProduct();

                // 篩選：避免重複評價
                boolean alreadyReviewed = reviewRepository.existsByUserUserIdAndOrderItemItemId(userId, item.getItemId());

                if (!alreadyReviewed) {
                	result.add(new ProductReviewInfo(
                		    product.getProductId(),
                		    product.getProductName(),
                		    product.getDescription(),
                		    item.getItemId().toString(),
                		    order.getUpdatedAt()
                		));
                }
            }
        }

        return result;
    }

    
 // 🔔 賣家通知：待處理訂單數（狀態為 "待付款" 或 "已付款"）
    public int countPendingOrdersForSeller(Integer sellerId) {
        List<Order> orders = orderRepository.findOrdersByShopOwner(sellerId);
        int count = 0;
        for (Order order : orders) {
            String status = order.getOrderStatusCorrespond().getName();
            if ("待付款".equals(status) || "已付款".equals(status)) {
                count++;
            }
        }
        return count;
    }

    // 🔔 買家通知：配送中訂單數
    public int countShippedOrdersForUser(Integer userId) {
        List<Order> orders = orderRepository.findOrdersByUserId(userId);
        int count = 0;
        for (Order order : orders) {
            String status = order.getOrderStatusCorrespond().getName();
            if ("配送中".equals(status)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * 直接創建訂單（適用於單個商品直接購買）
     * @param username 用戶名
     * @param orderRequest 訂單請求
     * @return 創建的訂單
     */
    @Transactional
    public Order createDirectOrder(String username, OrderRequest orderRequest) {
        // 1. 獲取用戶信息
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("用戶未找到"));
        
     // 2. 獲取商品信息
        Product product = productRepository.findById(orderRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        
        Sku primarySku = product.getSkuList().isEmpty() ? null : product.getSkuList().get(0);
        if (primarySku == null) {
            throw new RuntimeException("商品無有效SKU");
        }
     // 3. 計算單價和總價
        BigDecimal unitPrice = (orderRequest.getPrice() != null && orderRequest.getPrice() > 0) 
                ? BigDecimal.valueOf(orderRequest.getPrice()) 
                : primarySku.getPrice();
        // 檢查庫存
        if (primarySku.getStock() < orderRequest.getQuantity()) {
            throw new RuntimeException("庫存不足，無法下單");
        }
        
        if (orderRequest.getAmount() <= 0) {
            // 在這裡使用 unitPrice 計算總金額
            orderRequest.setAmount(unitPrice.intValue() * orderRequest.getQuantity());
        }
        
        if (orderRequest.getItemName() == null || orderRequest.getItemName().isEmpty()) {
            orderRequest.setItemName(product.getProductName());
        }
        
        if (orderRequest.getDescription() == null || orderRequest.getDescription().isEmpty()) {
            orderRequest.setDescription("訂單 #" + System.currentTimeMillis() + " - " + product.getProductName());
        }
        
        // 3. 計算訂單總價
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(orderRequest.getQuantity()));
        
        // 4. 獲取用戶默認地址
        List<UserAddress> addresses = user.getUserAddress();
        if (addresses.isEmpty()) {
            throw new RuntimeException("用戶沒有設置地址信息");
        }
        
        // 尋找默認地址
        UserAddress billingAddress = findDefaultAddressOrFirst(addresses, 1);
        UserAddress shippingAddress = findDefaultAddressOrFirst(addresses, 2);
        
        // 5. 創建訂單地址
        OrderAddress orderAddressBilling = createOrderAddress(billingAddress);
        OrderAddress orderAddressShipping = createOrderAddress(shippingAddress);
        
        // 6. 創建訂單
        Order order = new Order();
        order.setUser(user);
        order.setTotalPrice(totalPrice);
        order.setOrderAddressBilling(orderAddressBilling);
        order.setOrderAddressShipping(orderAddressShipping);
        
        // 7. 設置訂單狀態（未付款）
        OrderStatusCorrespond statusCorrespond = statusCorrespondRepository.findByName("未付款")
                .orElseGet(() -> {
                    OrderStatusCorrespond newStatus = new OrderStatusCorrespond();
                    newStatus.setName("未付款");
                    return statusCorrespondRepository.save(newStatus);
                });
        order.setOrderStatusCorrespond(statusCorrespond);
        
        // 保存訂單
        order = orderRepository.save(order);
        
        // 8. 創建訂單項
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setSku(primarySku);
        orderItem.setShop(product.getShop());
        orderItem.setUnitPrice(unitPrice);
        orderItem.setQuantity(orderRequest.getQuantity());
        orderItemRepository.save(orderItem);
        
        // 更新訂單的orderItem列表
        if (order.getOrderItem() == null) {
            order.setOrderItem(new ArrayList<>());
        }
        order.getOrderItem().add(orderItem);
        
        // 9. 創建訂單狀態歷史
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOrderStatusCorrespond(statusCorrespond);
        orderStatusHistoryRepository.save(history);
        
        // 10. 創建支付記錄
        Payment payment = new Payment();
        payment.setOrder(order);
        
        // 設置支付方式 - 綠界支付
        PaymentMethod paymentMethod = paymentMethodRepository.findByNameIgnoreCase("綠界支付")
                .orElseGet(() -> {
                    PaymentMethod newMethod = new PaymentMethod();
                    newMethod.setName("綠界支付");
                    return paymentMethodRepository.save(newMethod);
                });
        payment.setPaymentMethod(paymentMethod);
     // 改為「保持原付款方式不變」或是「找不到就新增」
        PaymentMethod method = payment.getPaymentMethod();
        if (method == null || !"CREDIT".equalsIgnoreCase(method.getName())) {
            method = paymentMethodRepository.findByNameIgnoreCase("CREDIT")
                .orElseGet(() -> {
                    PaymentMethod m = new PaymentMethod();
                    m.setName("CREDIT");
                    return paymentMethodRepository.save(m);
                });
            payment.setPaymentMethod(method);
        }

        
        // 設置支付狀態 - 待付款
        PaymentStatus paymentStatus = paymentStatusRepository.findByName("待付款")
                .orElseGet(() -> {
                    PaymentStatus newStatus = new PaymentStatus();
                    newStatus.setName("待付款");
                    return paymentStatusRepository.save(newStatus);
                });
        payment.setPaymentStatus(paymentStatus);
        
        paymentRepository.save(payment);
        
        // 更新訂單的payment列表
        if (order.getPayment() == null) {
            order.setPayment(new ArrayList<>());
        }
        order.getPayment().add(payment);
        
        // 更新庫存
        primarySku.setStock(primarySku.getStock() - orderRequest.getQuantity());
        skuRepository.save(primarySku);
        
        return orderRepository.save(order);
    }

    /**
     * 尋找默認地址或第一個地址
     */
    private UserAddress findDefaultAddressOrFirst(List<UserAddress> addresses, int addressTypeId) {
        return addresses.stream()
                .filter(addr -> addr.getAddressTypeCorrespond().getId() == addressTypeId && Boolean.TRUE.equals(addr.getIsDefault()))
                .findFirst()
                .orElse(addresses.stream()
                        .filter(addr -> addr.getAddressTypeCorrespond().getId() == addressTypeId)
                        .findFirst()
                        .orElse(addresses.get(0)));
    }

    /**
     * 創建訂單地址
     */
    private OrderAddress createOrderAddress(UserAddress userAddress) {
        OrderAddress orderAddress = new OrderAddress();
        orderAddress.setAddressTypeCorrespond(userAddress.getAddressTypeCorrespond());
        orderAddress.setCity(userAddress.getCity());
        orderAddress.setDistrict(userAddress.getDistrict());
        orderAddress.setZipCode(userAddress.getZipCode());
        orderAddress.setStreetEtc(userAddress.getStreetEtc());
        orderAddress.setRecipientName(userAddress.getRecipientName());
        orderAddress.setRecipientPhone(userAddress.getRecipientPhone());
        return orderAddressRepository.save(orderAddress);
    }

}
