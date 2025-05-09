package ourpkg.product.version2.product_sales;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ourpkg.product.Product;
import ourpkg.product.ProductRepository;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;

@RestController
@RequestMapping("/api/sales")
public class ProductSalesController {

	private final ProductSalesService productSalesService;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    
    @Autowired
    public ProductSalesController(
            ProductSalesService productSalesService,
            ProductRepository productRepository,
            SkuRepository skuRepository) {
        this.productSalesService = productSalesService;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
    }
    
    /**
     * 查詢商品的銷售數量
     * @param productId 商品ID
     * @return 包含商品信息及銷售數量的DTO
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getProductSales(@PathVariable Integer productId) {
        // 檢查商品是否存在
        Product product = productRepository.findById(productId)
                .orElse(null);
        
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        
        Integer soldCount = productSalesService.getProductSoldCount(productId);
        
        ProductSalesDTO dto = new ProductSalesDTO(
                product.getProductId(),
                product.getProductName(),
                soldCount
        );
        
        return ResponseEntity.ok(dto);
    }
    
    /**
     * 查詢特定SKU的銷售數量
     * @param skuId SKU ID
     * @return 包含SKU信息及銷售數量的DTO
     */
    @GetMapping("/sku/{skuId}")
    public ResponseEntity<?> getSkuSales(@PathVariable Integer skuId) {
        // 檢查SKU是否存在
        Sku sku = skuRepository.findById(skuId)
                .orElse(null);
        
        if (sku == null) {
            return ResponseEntity.notFound().build();
        }
        
        Integer soldCount = productSalesService.getSkuSoldCount(skuId);
        
        SkuSalesDTO dto = new SkuSalesDTO(
                sku.getSkuId(),
                sku.getProduct().getProductId(),
                sku.getProduct().getProductName(),
                sku.getSpecPairs(),
                soldCount
        );
        
        return ResponseEntity.ok(dto);
    }
}
