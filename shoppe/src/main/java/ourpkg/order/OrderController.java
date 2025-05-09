package ourpkg.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ourpkg.address.OrderAddress;
import ourpkg.config.ApiResponse;
import ourpkg.payment.Payment;
import ourpkg.payment.PaymentRepository;
import ourpkg.product.Product;
import ourpkg.product.ProductImageRepository;
import ourpkg.shipment.Shipment;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;
import ourpkg.user_role_permission.user.service.UserService;

@RestController
@RequestMapping("/api/orders")
@ResponseBody
public class OrderController {

	@Autowired
	private OrderService orderService;

	@Autowired
	private UserService userService;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

//新============================================
	@Autowired
	private ProductImageRepository productImageRepository;
//新============================================
	@Autowired
	private UserRepository userRepository;
	    
	@PostMapping("/create")
	public ResponseEntity<?> createOrder(@RequestBody OrderRequest orderRequest) {
	    try {
	        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	        String username = auth.getName();

	        Order order = orderService.createOrderFromUser(username, orderRequest);

	        return ResponseEntity.ok().body(
	            Map.of(
	                "success", true,
	                "orderId", order.getOrderId(),
	                "totalPrice", order.getTotalPrice()
	            )
	        );
	    } catch (Exception e) {
	        return ResponseEntity.badRequest().body(
	            Map.of(
	                "success", false,
	                "message", e.getMessage()
	            )
	        );
	    }
	}

	
	
	
    private Payment getLatestPayment(Order order) {
        return order.getPayment().stream()
            .filter(p -> p.getUpdatedAt() != null)
            .max(Comparator.comparing(Payment::getUpdatedAt))
            .orElse(null);
    }


    private Shipment getLatestShipment(Order order) {
        return order.getShipment().stream() 
            .max((s1, s2) -> Integer.compare(s1.getShipmentId(), s2.getShipmentId()))
            .orElse(null);
    }

    
    @GetMapping("/{orderId}/payment")
    public ResponseEntity<?> redirectToPayment(@PathVariable Integer orderId) {
        try {
            // 檢查訂單是否存在
            Order order = orderService.getOrderById(orderId);
            
            // 檢查訂單狀態是否為未付款
            if (!"未付款".equals(order.getOrderStatusCorrespond().getName())) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(400, "error", "該訂單不是未付款狀態，無法發起支付", null));
            }
            
            // 返回支付相關信息
            Map<String, Object> paymentInfo = new HashMap<>();
            paymentInfo.put("orderId", order.getOrderId());
            paymentInfo.put("amount", order.getTotalPrice());
            paymentInfo.put("paymentUrl", "/api/payment/redirect/" + orderId);
            
            return ResponseEntity.ok(
                new ApiResponse<>(200, "success", "準備支付信息成功", paymentInfo));
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(500, "error", "準備支付信息失敗: " + e.getMessage(), null));
        }
    }

	//  ✅ 取得特定訂單 //這方法先保留 
	// @GetMapping("/{orderId}")
	// public ResponseEntity<OrderDto> getOrder(@PathVariable int orderId) {
	// 	Order order = orderService.getOrderById(orderId);
	// 	OrderDto orderDto = convertOrderToDto(order);
	// 	return ResponseEntity.ok(orderDto);
	// }

     //✅ 取得特定訂單 
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable int orderId) {
        Order order = orderService.getOrderById(orderId);
        OrderDto orderDto = convertOrderToDto(order);
        return ResponseEntity.ok(orderDto);
    }

	// ✅ Checkout 功能，建立訂單
	@PostMapping("/checkout/{userId}")
	public ResponseEntity<OrderDto> checkout(@PathVariable int userId) {
		OrderDto orderDto = orderService.checkout(userId);
		return ResponseEntity.ok(orderDto);
	}

	@PreAuthorize("hasAnyRole('SELLER', 'SUPER_ADMIN', 'USER')")
	@GetMapping("/user/orders")
	public ResponseEntity<ApiResponse<List<OrderDto>>> getUserOrders() {
		try {
			// 添加調試代碼以檢查權限問題
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			System.out.println("當前用戶: " + auth.getName());
			System.out.println("當前權限: " + auth.getAuthorities());

			// 獲取當前用戶ID
			Integer userId = userService.getCurrentUserId();
			if (userId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(401, "error", "未登入或身份驗證失敗", null));
			}

			// 獲取用戶的訂單
			List<Order> orders = orderService.getOrdersForUser(userId);

			// 如果沒有訂單，返回空列表但狀態是成功的
			if (orders.isEmpty()) {
				return ResponseEntity.ok(new ApiResponse<>(200, "success", "沒有訂單記錄", Collections.emptyList()));
			}

			// 將訂單轉換為DTO
			List<OrderDto> orderDtos = convertOrdersToDto(orders);

			// 返回成功響應
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "訂單查詢成功", orderDtos));

		} catch (AccessDeniedException e) {
			// 捕獲權限錯誤
			System.err.println("權限不足: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ApiResponse<>(403, "error", "您沒有權限訪問此資源", null));
		} catch (Exception e) {
			// 記錄異常
			System.err.println("獲取用戶訂單時發生錯誤: " + e.getMessage());
			e.printStackTrace();

			// 返回錯誤響應
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "獲取訂單失敗: " + e.getMessage(), null));
		}
	}

	@PreAuthorize("hasAnyRole('SELLER', 'SUPER_ADMIN')")
	@GetMapping("/seller/orders")
	public ResponseEntity<ApiResponse<List<OrderDto>>> getSellerOrders() {
		try {
			// 獲取當前賣家ID
			Integer sellerId = userService.getCurrentUserId();
			if (sellerId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(401, "error", "未登入或身份驗證失敗", null));
			}

			// 獲取賣家的訂單
			List<Order> orders = orderService.getOrdersForSeller(sellerId);

			// 如果沒有訂單，返回空列表但狀態是成功的
			if (orders.isEmpty()) {
				return ResponseEntity.ok(new ApiResponse<>(200, "success", "沒有訂單記錄", Collections.emptyList()));
			}

			// 將訂單轉換為DTO
			List<OrderDto> orderDtos = convertOrdersToDto(orders);

			// 返回成功響應
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "賣家訂單查詢成功", orderDtos));
		} catch (AccessDeniedException e) {
			// 捕獲權限錯誤
			System.err.println("權限不足: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, "error", "您沒有賣家權限", null));
		} catch (Exception e) {
			// 記錄異常
			System.err.println("獲取賣家訂單時發生錯誤: " + e.getMessage());
			e.printStackTrace();

			// 返回錯誤響應
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "獲取賣家訂單失敗: " + e.getMessage(), null));
		}
	}

	/**
	 * 將訂單列表轉換為DTO列表
	 */
	private List<OrderDto> convertOrdersToDto(List<Order> orders) {
		return orders.stream().map(this::convertOrderToDto).collect(Collectors.toList());
	}

	private OrderDto convertOrderToDto(Order order) {
		// 記錄日誌
		System.out.println("🔍 訂單 ID：" + order.getOrderId());
		System.out.println("📅 訂單成立時間：" + order.getCreatedAt());

		User user = order.getUser();

		// 獲取用戶資訊
		System.out.println("👤 用戶名稱：" + user.getUsername());

		// 格式化地址
		String billingAddress = formatAddress(order.getBillingAddress());
		String shippingAddress = formatAddress(order.getShippingAddress());

		// 處理訂單項目
		List<OrderItemDto> orderItems;
		if (order.getOrderItem() == null || order.getOrderItem().isEmpty()) {
			System.out.println("⚠️ 訂單商品為 NULL 或空列表");
			orderItems = Collections.emptyList();
		} else {
			System.out.println("🔍 訂單商品數量：" + order.getOrderItem().size());

			// 轉換訂單項目，並加入商品圖片
			orderItems = order.getOrderItem().stream().map(item -> {
				Product product = item.getSku().getProduct();
				System.out.println("🛒 商品：" + product.getProductName() + " 數量：" + item.getQuantity());

//原============================================
//				// 獲取商品圖片
//				String imageUrl = null;
//				if (product.getImage() != null && !product.getImage().isEmpty()) {
//					imageUrl = product.getImage();
//					System.out.println("🖼️ 商品圖片：" + imageUrl);
//				} else {
//					System.out.println("⚠️ 商品無圖片");
//				}
//原============================================
//新============================================
				// 獲取商品主圖片
				String imageUrl = productImageRepository.findPrimaryImageByProduct_ProductId(product.getProductId())
						.orElse("");

				if (!imageUrl.isEmpty()) {
					System.out.println("🖼️ 商品圖片：" + imageUrl);
				} else {
					System.out.println("⚠️ 商品無圖片");
				}
//新============================================
				// 創建帶有圖片資訊的 OrderItemDto
				return new OrderItemDto(product.getProductName(), item.getQuantity(), item.getUnitPrice(), imageUrl, // 加入圖片URL
						product.getProductId() // 可選：加入商品ID
				);
			}).collect(Collectors.toList());
		}

		// 獲取支付和運送資訊
		String paymentMethodName = "";
		String paymentStatusName = "";


        // if (order.getPayment() != null && !order.getPayment().isEmpty()) {
		// 	Payment latestPayment = order.getPayment().get(0); // 假設按最新的付款記錄
		// 	paymentMethodName = latestPayment.getPaymentMethod().getName(); // 使用 getName() 從 PaymentMethod 獲取名稱
		// 	paymentStatusName = latestPayment.getPaymentStatus().getName(); // 使用 getName() 從 PaymentStatus 獲取名稱
		// }
        if (order.getPayment() != null && !order.getPayment().isEmpty()) {
        	Payment latestPayment = getLatestPayment(order);
        	if (latestPayment != null) {
        	    paymentMethodName = latestPayment.getPaymentMethod().getName();
        	    paymentStatusName = latestPayment.getPaymentStatus().getName();
        	}
        }

		String shipmentMethodName = "";
		String shipmentStatusName = "";
		String trackingNumber = ""; // 注意：您的 Shipment 類中沒有 trackingNumber 屬性


        // if (order.getShipment() != null && !order.getShipment().isEmpty()) {
		// 	Shipment latestShipment = order.getShipment().get(0); // 假設按最新的運送記錄
		// 	shipmentMethodName = latestShipment.getShipmentMethod().getName(); // 使用 getName() 從 ShipmentMethod 獲取名稱
		// 	shipmentStatusName = latestShipment.getShipmentStatus().getName(); // 使用 getName() 從 ShipmentStatus 獲取名稱
		// 	// 注意：您的 Shipment 類中沒有 trackingNumber 屬性，所以這裡設為空字符串
		// }
        Shipment latestShipment = getLatestShipment(order);
        if (latestShipment != null) {
            shipmentMethodName = latestShipment.getShipmentMethod().getName();
            shipmentStatusName = latestShipment.getShipmentStatus().getName();
        }
        

		// 創建完整的 OrderDto - 使用含 shipmentMethod 參數的構造函數
		return new OrderDto(order.getOrderId(), order.getTotalPrice(), order.getOrderStatusCorrespond().getName(),
				orderItems, order.getCreatedAt(), order.getUpdatedAt(), user.getUserId(), user.getUsername(),
				user.getEmail(), user.getPhone(), billingAddress, shippingAddress, paymentMethodName, paymentStatusName,
				shipmentMethodName, // 添加 shipmentMethodName 參數
				shipmentStatusName, trackingNumber);
	}

	/**
	 * 格式化地址
	 */
	private String formatAddress(OrderAddress address) {
		if (address == null) {
			return "未提供";
		}

		StringBuilder formattedAddress = new StringBuilder();

		if (address.getCity() != null) {
			formattedAddress.append(address.getCity());
		}

		if (address.getDistrict() != null) {
			formattedAddress.append(" ").append(address.getDistrict());
		}

		if (address.getZipCode() != null) {
			formattedAddress.append(" ").append(address.getZipCode());
		}

		if (address.getStreetEtc() != null) {
			formattedAddress.append(" ").append(address.getStreetEtc());
		}

		if (address.getRecipientName() != null) {
			formattedAddress.append(" (收件人: ").append(address.getRecipientName());

			if (address.getRecipientPhone() != null) {
				formattedAddress.append(", 電話: ").append(address.getRecipientPhone());
			}

			formattedAddress.append(")");
		}

		return formattedAddress.toString();
	}

	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
	@GetMapping("/admin/orders")
	public ResponseEntity<ApiResponse<List<OrderDto>>> getAllOrders() {
		try {
			// 獲取所有訂單
			List<Order> orders = orderService.getAllOrders();

			// 如果沒有訂單，返回空列表但狀態是成功的
			if (orders.isEmpty()) {
				return ResponseEntity.ok(new ApiResponse<>(200, "success", "沒有訂單記錄", Collections.emptyList()));
			}

			List<OrderDto> orderDtos = orders.stream().map(order -> {
			    List<OrderItemDto> orderItemDtos = order.getOrderItem().stream().map(item -> {
			        Product product = item.getSku().getProduct();
			        String imageUrl = productImageRepository
			                .findPrimaryImageByProduct_ProductId(product.getProductId())
			                .orElse("");
			        return new OrderItemDto(
			                product.getProductName(),
			                item.getQuantity(),
			                item.getUnitPrice(),
			                imageUrl,
			                product.getProductId()
			        );
			    }).collect(Collectors.toList());

			    User user = order.getUser();
			    String username = (user != null && user.getUsername() != null && !user.getUsername().isBlank())
			        ? user.getUsername()
			        : "未知用戶";

			    return new OrderDto(
			            order.getOrderId(),
			            order.getTotalPrice(),
			            order.getOrderStatusCorrespond().getName(),
			            orderItemDtos,
			            order.getCreatedAt(),
			            order.getUpdatedAt(),
			            user != null ? user.getUserId() : null,
			            username,
			            user != null ? user.getEmail() : null,
			            user != null ? user.getPhone() : null,
			            formatAddress(order.getBillingAddress()),
			            formatAddress(order.getShippingAddress()),
			            getPaymentMethodName(order),
			            getPaymentStatusName(order),
			            getShipmentMethodName(order),
			            getShipmentStatusName(order),
			            getTrackingNumber(order)
			    );
			}).collect(Collectors.toList());
			// 返回成功響應
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "所有訂單查詢成功", orderDtos));
		} catch (AccessDeniedException e) {
			// 捕獲權限錯誤
			System.err.println("權限不足: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiResponse<>(403, "error", "您沒有管理員權限", null));
		} catch (Exception e) {
			// 記錄異常
			System.err.println("獲取所有訂單時發生錯誤: " + e.getMessage());
			e.printStackTrace();

			// 返回錯誤響應
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "獲取所有訂單失敗: " + e.getMessage(), null));
		}
	}

	// 輔助方法，獲取付款方式名稱
	private String getPaymentMethodName(Order order) {
		if (order.getPayment() != null && !order.getPayment().isEmpty()) {
			Payment latestPayment = order.getPayment().get(0);
			return latestPayment.getPaymentMethod().getName();
		}
		return "";
	}

	// 輔助方法，獲取付款狀態名稱
	private String getPaymentStatusName(Order order) {
		if (order.getPayment() != null && !order.getPayment().isEmpty()) {
			Payment latestPayment = order.getPayment().get(0);
			return latestPayment.getPaymentStatus().getName();
		}
		return "";
	}

	// 輔助方法，獲取運送方式名稱
	private String getShipmentMethodName(Order order) {
		if (order.getShipment() != null && !order.getShipment().isEmpty()) {
			Shipment latestShipment = order.getShipment().get(0);
			return latestShipment.getShipmentMethod().getName();
		}
		return "";
	}

	// 輔助方法，獲取運送狀態名稱
	private String getShipmentStatusName(Order order) {
		if (order.getShipment() != null && !order.getShipment().isEmpty()) {
			Shipment latestShipment = order.getShipment().get(0);
			return latestShipment.getShipmentStatus().getName();
		}
		return "";
	}

	// 輔助方法，獲取追蹤號碼
	private String getTrackingNumber(Order order) {
		// 如果您的 Shipment 類中有 trackingNumber 屬性，使用下面的代碼
		// if (order.getShipment() != null && !order.getShipment().isEmpty()) {
		// Shipment latestShipment = order.getShipment().get(0);
		// return latestShipment.getTrackingNumber();
		// }
		return ""; // 目前返回空字符串，因為您的 Shipment 類可能沒有 trackingNumber 屬性
	}

	/**
	 * 更新訂單資訊
	 */
	@PreAuthorize("hasAnyRole('SELLER', 'SUPER_ADMIN')")
	@PutMapping("/{orderId}")
	public ResponseEntity<ApiResponse<OrderDto>> updateOrder(@PathVariable int orderId,
			@RequestBody UpdateOrderRequest request) {
		try {
			// 獲取當前用戶ID
			Integer userId = userService.getCurrentUserId();
			if (userId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(401, "error", "未登入或身份驗證失敗", null));
			}

			// 檢查訂單是否存在
			Order order = orderService.getOrderById(orderId);
			if (order == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, "error", "訂單不存在", null));
			}

			// 更新訂單資訊
			Order updatedOrder = orderService.updateOrder(orderId, request);
			OrderDto orderDto = convertOrderToDto(updatedOrder);

			// 返回成功響應
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "訂單更新成功", orderDto));
		} catch (AccessDeniedException e) {
			// 捕獲權限錯誤
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ApiResponse<>(403, "error", "您沒有權限更新此訂單", null));
		} catch (Exception e) {
			// 記錄異常
			e.printStackTrace();

			// 返回錯誤響應
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "更新訂單失敗: " + e.getMessage(), null));
		}
	}

	/**
	 * 刪除訂單
	 */
	@PreAuthorize("hasAnyRole('SELLER', 'SUPER_ADMIN')")
	@DeleteMapping("/{orderId}")
	public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable int orderId) {
		try {
			// 獲取當前用戶ID
			Integer userId = userService.getCurrentUserId();
			if (userId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(401, "error", "未登入或身份驗證失敗", null));
			}

			// 檢查訂單是否存在
			Order order = orderService.getOrderById(orderId);
			if (order == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse<>(404, "error", "訂單不存在", null));
			}

			// 刪除訂單
			orderService.deleteOrder(orderId);

			// 返回成功響應
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "訂單刪除成功", null));
		} catch (AccessDeniedException e) {
			// 捕獲權限錯誤
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(new ApiResponse<>(403, "error", "您沒有權限刪除此訂單", null));
		} catch (Exception e) {
			// 記錄異常
			e.printStackTrace();

			// 返回錯誤響應
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "刪除訂單失敗: " + e.getMessage(), null));
		}
	}

	// 取消訂單
	@PreAuthorize("hasAnyRole('SELLER', 'SUPER_ADMIN', 'USER')")
	@PutMapping("/cancel/{orderId}")
	public ResponseEntity<ApiResponse<OrderDto>> cancelOrder(@PathVariable int orderId) {
		try {
			Integer userId = userService.getCurrentUserId();
			if (userId == null) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(new ApiResponse<>(401, "error", "未登入或身份驗證失敗", null));
			}

			// 嘗試取消訂單（更新狀態為「已取消」）
			Order updatedOrder = orderService.cancelOrder(orderId);
			OrderDto orderDto = convertOrderToDto(updatedOrder);

			return ResponseEntity.ok(new ApiResponse<>(200, "success", "訂單取消成功", orderDto));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(500, "error", "取消訂單失敗: " + e.getMessage(), null));
		}
	}

	// 出貨API
	@PutMapping("/{orderId}/ship")
	public ResponseEntity<?> shipOrder(@PathVariable int orderId) {
		orderService.shipOrder(orderId);
		return ResponseEntity.ok("出貨成功，已通知買家");
	}

	// 評價
	@GetMapping("/completed/{userId}/products-to-review")
	public ResponseEntity<List<ProductReviewInfo>> getProductsToReview(@PathVariable Integer userId) {
		List<ProductReviewInfo> products = orderService.getCompletedOrderProductsByUserId(userId);
		return ResponseEntity.ok(products);
	}

	@GetMapping("/check-payment/{orderId}")
	public ResponseEntity<ApiResponse<Void>> checkOrderPayment(@PathVariable int orderId) {
		try {
			// 取得訂單
			Order order = orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("找不到訂單"));

			// 更嚴格的狀態檢查
			String orderStatus = order.getOrderStatusCorrespond().getName();
			List<String> pendingStatuses = Arrays.asList("PENDING", "待付款", "未付款");

			if (!pendingStatuses.contains(orderStatus)) {
				return ResponseEntity.ok(new ApiResponse<>(400, "error", "訂單狀態不可付款：" + orderStatus, null));
			}

			// 檢查付款記錄
			List<Payment> payments = paymentRepository.findByOrderOrderId(orderId);

			// 更詳細的付款狀態檢查
			boolean canPay = payments.isEmpty()
					|| payments.stream().allMatch(p -> p.getPaymentStatus().getName().equals("未付款")
							|| p.getPaymentStatus().getName().equals("PENDING"));

			if (!canPay) {
				return ResponseEntity.ok(new ApiResponse<>(400, "error", "此訂單已有付款記錄", null));
			}

			// 可以付款
			return ResponseEntity.ok(new ApiResponse<>(200, "success", "訂單可以付款", null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(new ApiResponse<>(500, "error", e.getMessage(), null));
		}
	}

	// 🔔 賣家通知：待處理訂單數（待付款、已付款、備貨中）
    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/notification/pending-count/seller")
    public ResponseEntity<Integer> getPendingOrderCountForSeller() {
        Integer sellerId = userService.getCurrentUserId();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int count = orderService.countPendingOrdersForSeller(sellerId);
        return ResponseEntity.ok(count);
    }

    // 🔔 買家通知：配送中訂單數
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/notification/shipped-count/user")
    public ResponseEntity<Integer> getShippedOrderCountForUser() {
        Integer userId = userService.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int count = orderService.countShippedOrdersForUser(userId);
        return ResponseEntity.ok(count);
    }

    
}
