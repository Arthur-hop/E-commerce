package ourpkg.product.version2.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.sku.Sku;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPublicDTO {
    private Integer productId;
    private String productName;
    private String description;
    private Boolean active;
    private BigDecimal lowestPrice; // 直接提供最低價格而非價格範圍
    private String sellerName; // 直接提供賣家名稱，而非整個Shop對象
    private List<String> imageUrls; // 簡化為圖片URL列表
    
    // 分類信息
    private Integer category1Id;
    private String category1Name;
    private Integer category2Id;
    private String category2Name;
    private Integer skuId; // ✅ 加入這行

    // 從現有的Product實體轉換為DTO的構造函數
    public ProductPublicDTO(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.description = product.getDescription();
        this.active = product.getActive();
        
        // 計算最低價格
        this.lowestPrice = product.getSkuList() != null && !product.getSkuList().isEmpty()
                ? product.getSkuList().stream().map(Sku::getPrice).min(BigDecimal::compareTo).orElse(null)
                : null;
        
        // 獲取賣家名稱（根據實際情況，可能是商店名或用戶名）
        if (product.getShop() != null) {
            if (product.getShop().getShopName() != null) {
                this.sellerName = product.getShop().getShopName();
            } else if (product.getShop().getUser() != null) {
                this.sellerName = product.getShop().getUser().getUsername();
            } else {
                this.sellerName = "未知賣家";
            }
        } else {
            this.sellerName = "未知賣家";
        }
        
        // 獲取所有圖片URL，並按主圖優先排序
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            // 首先將主圖放在第一位
            this.imageUrls = product.getProductImages().stream()
                    .sorted((img1, img2) -> Boolean.compare(img2.getIsPrimary(), img1.getIsPrimary())) // 主圖優先
                    .map(ProductImage::getImagePath)
                    .collect(Collectors.toList());
        } else {
            this.imageUrls = List.of("/uploads/default-product-image.jpg"); // 返回默認圖片而非空列表
        }
        
        // 獲取分類信息
        if (product.getCategory1() != null) {
            this.category1Id = product.getCategory1().getId();
            this.category1Name = product.getCategory1().getName();
        }
        if (product.getCategory2() != null) {
            this.category2Id = product.getCategory2().getId();
            this.category2Name = product.getCategory2().getName();
        }
        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
            this.skuId = product.getSkuList().get(0).getSkuId();
        }
    }
    
    // 用於將商品價格格式化為字符串的輔助方法
    public String getFormattedPrice() {
        if (this.lowestPrice == null) {
            return "未定價";
        }
        return this.lowestPrice.toString();
    }
    
    // 獲取主圖URL的輔助方法
    public String getPrimaryImageUrl() {
        if (this.imageUrls == null || this.imageUrls.isEmpty()) {
            return "/uploads/default-product-image.jpg";
        }
        return this.imageUrls.get(0);
    }
}