package ourpkg.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import ourpkg.sku.Sku;

@Getter
@Setter
public class ProductDto {
    private Integer productId;
    private String productName;
    private String description;
    private String sellerName;
    private List<String> imageUrls;
    private BigDecimal lowestPrice;
    
    private Integer skuId;

    
    

//    public ProductDto(Product product) {
//        this.productId = product.getProductId();
//        this.productName = product.getProductName();
//        this.description = product.getDescription();
//
//        // 取得賣家名稱
//        if (product.getShop() != null && product.getShop().getUser() != null) {
//            this.sellerName = product.getShop().getUser().getUsername(); // 是 getUsername()
//        }
//
//        // 取得圖片列表
//        if (product.getProductImages() != null) {
//            this.imageUrls = product.getProductImages().stream()
//                .map(ProductImage::getImagePath) // ✅ 使用正確欄位
//                .collect(Collectors.toList());
//        } else {
//            this.imageUrls = new ArrayList<>();
//        }
//
//        // 取得最低價格
//        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
//            this.lowestPrice = product.getSkuList().stream()
//                .map(Sku::getPrice)
//                .min(BigDecimal::compareTo)
//                .orElse(null);
//        }
//    }
    public ProductDto(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.description = product.getDescription();

        if (product.getShop() != null && product.getShop().getUser() != null) {
            this.sellerName = product.getShop().getUser().getUsername();
        }

        if (product.getProductImages() != null) {
            this.imageUrls = product.getProductImages().stream()
                .map(ProductImage::getImagePath)
                .collect(Collectors.toList());
        } else {
            this.imageUrls = new ArrayList<>();
        }

        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
            this.lowestPrice = product.getSkuList().stream()
                .map(Sku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);

            // ✅ 選第一個 SKU 當作預設
            this.skuId = product.getSkuList().get(0).getSkuId();
        }
    }
}

