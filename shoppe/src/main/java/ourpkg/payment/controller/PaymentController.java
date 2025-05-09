//package ourpkg.payment.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import ourpkg.order.Order;
//import ourpkg.order.OrderRepository;
//
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/payment/orders")
//public class PaymentController {
//
//    private final OrderRepository orderRepository;
//
//    public PaymentController(OrderRepository orderRepository) {
//        this.orderRepository = orderRepository;
//    }
//
//    @GetMapping("/check-payment/{orderId}")
//    public ResponseEntity<?> checkPaymentStatus(@PathVariable Integer orderId) {
//        Optional<Order> orderOpt = orderRepository.findById(orderId);
//
//        if (orderOpt.isEmpty()) {
//            return ResponseEntity
//                .status(404)
//                .body(Map.of("status", "fail", "message", "訂單不存在"));
//        }
//
//        Order order = orderOpt.get();
//
//        if (order.getOrderStatusCorrespond() == null ||
//        	    !"未付款".equals(order.getOrderStatusCorrespond().getName())) {
//        	    return ResponseEntity.ok(Map.of(
//        	        "status", "fail",
//        	        "message", "此訂單不需要付款或已完成付款"
//        	    ));
//        	}
//
//        return ResponseEntity.ok(Map.of(
//            "status", "success",
//            "message", "訂單尚未付款，可進行付款"
//        ));
//    }
//}
