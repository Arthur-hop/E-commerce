package ourpkg.product.review;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import ourpkg.category.Category1;
import ourpkg.category.Category2;
import ourpkg.category.dto.Category1ResDTO;
import ourpkg.category.dto.Category2ResDTO;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.sku.Sku;

@Getter
@Setter
public class ReviewProductDTO {
    private Integer productId;
    private String productName;
    private String description;
    private String sellerName;
    private Integer shopId;  
    private List<String> imageUrls;
    private BigDecimal lowestPrice;
    private Category1ResDTO category1;
    private Category2ResDTO category2;
    
    // 審核相關欄位
    private Boolean reviewStatus;  // true 代表審核通過，false 代表被拒絕
    private String reviewComment;
    private String reviewerUsername;  // 審核管理員的用戶名
    private Integer reviewerId;      // 審核管理員ID
    private Date reviewAt;  // 審核時間
    
    // 額外的商品狀態
    private Boolean active;  // 賣家控制的上下架狀態
    private Boolean isDeleted;
    private Date createdAt;
    private Date updatedAt;
    
    public ReviewProductDTO(Product product) {
        this.productId = product.getProductId();
        this.productName = product.getProductName();
        this.description = product.getDescription();
        
        // 商店和賣家信息
        if (product.getShop() != null) {
            this.shopId = product.getShop().getShopId(); // 注意：這裡可能需要改為getShopId()
            if (product.getShop().getUser() != null) {
                this.sellerName = product.getShop().getUser().getUsername();
            }
        }
        
        // 圖片URL
        if (product.getProductImages() != null) {
            this.imageUrls = product.getProductImages().stream()
                .map(ProductImage::getImagePath)
                .collect(Collectors.toList());
        } else {
            this.imageUrls = new ArrayList<>();
        }
        
        // 價格信息
        if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
            this.lowestPrice = product.getSkuList().stream()
                .map(Sku::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
        }
        
        // 分類信息
        if (product.getCategory1() != null) {
            this.category1 = mapToCategory1ResDTO(product.getCategory1());
        }
        
        if (product.getCategory2() != null) {
            this.category2 = mapToCategory2ResDTO(product.getCategory2());
        }
        
        // 審核信息
        this.reviewStatus = product.getReviewStatus();
        this.reviewComment = product.getReviewComment();
        if (product.getReviewer() != null) {
            this.reviewerId = product.getReviewer().getUserId(); // 注意：這裡可能需要根據你的 User 類調整
            this.reviewerUsername = product.getReviewer().getUsername();
        }
        this.reviewAt = product.getReviewAt();
        
        // 狀態欄位
        this.active = product.getActive();
        this.isDeleted = product.getIsDeleted();
        this.createdAt = product.getCreatedAt();
        this.updatedAt = product.getUpdatedAt();
    }
    
    // --- 映射分類的輔助方法 ---
    private Category1ResDTO mapToCategory1ResDTO(Category1 category1Entity) {
        if (category1Entity == null) {
            return null;
        }
        // 直接使用 Category1ResDTO 的構造函數或 Setter
        return new Category1ResDTO(
            category1Entity.getId(), // 假設 Category1 實體有 getId() 方法返回 Integer
            category1Entity.getName()  // 假設 Category1 實體有 getName() 方法返回 String
        );
    }
    
    // 映射 Category2
    private Category2ResDTO mapToCategory2ResDTO(Category2 category2Entity) {
        if (category2Entity == null) {
            return null;
        }
        List<Integer> cat1Ids = new ArrayList<>();
        // 假設 Category2 實體有關聯 Category1 列表的方法，例如 getCategory1List()
        if (category2Entity.getCategory1List() != null) {
            cat1Ids = category2Entity.getCategory1List().stream()
                        .map(Category1::getId) // 假設 Category1::getId 返回 Integer
                        .collect(Collectors.toList());
        }
        return new Category2ResDTO(
            category2Entity.getId(), // 假設 Category2 實體有 getId() 返回 Integer
            category2Entity.getName(), // 假設 Category2 實體有 getName() 返回 String
            cat1Ids
        );
    }
}