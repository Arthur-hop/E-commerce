package ourpkg.product.version2.product_sales;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ourpkg.order.OrderItemRepository;

@Service
public class ProductSalesService {

private final OrderItemRepository orderItemRepository;
    
    @Autowired
    public ProductSalesService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }
    
    /**
     * 獲取特定商品的已售出數量
     * @param productId 商品ID
     * @return 該商品的總銷售數量，如果沒有銷售記錄則返回0
     */
    public Integer getProductSoldCount(Integer productId) {
        Integer soldCount = orderItemRepository.countSoldQuantityByProductId(productId);
        return soldCount != null ? soldCount : 0;
    }
    
    /**
     * 獲取特定SKU的已售出數量
     * @param skuId SKU ID
     * @return 該SKU的總銷售數量，如果沒有銷售記錄則返回0
     */
    public Integer getSkuSoldCount(Integer skuId) {
        Integer soldCount = orderItemRepository.countSoldQuantityBySkuId(skuId);
        return soldCount != null ? soldCount : 0;
    }
}
