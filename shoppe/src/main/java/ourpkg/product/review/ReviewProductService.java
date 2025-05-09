package ourpkg.product.review;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.product.Product;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Service
public class ReviewProductService {
	
	@Autowired
	private ReviewProductRepository productRepository;
	
	@Autowired
	private UserRepository userRepository;


	
    /**
     * 審核商品（變更 review_status）
     */
    @Transactional
    public Product updateReviewStatus(Integer productId, Boolean newStatus, String reviewComment, Integer reviewerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 取得審核人員
        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new RuntimeException("審核人員不存在"));
        
        product.setReviewStatus(newStatus);
        product.setReviewComment(reviewComment);
        product.setReviewer(reviewer);
        product.setReviewAt(new Date());

        // 儲存更新的商品
        productRepository.save(product);
        
        return product;
    }
    
    /**
     * 分頁查詢所有未刪除的商品
     */
    public Page<Product> findAllProductsWithPagination(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable);
    }

    /**
     * 根據店鋪ID分頁查詢商品
     */
    public Page<Product> findProductsByShopIdWithPagination(Integer shopId, Pageable pageable) {
        return productRepository.findByShop_ShopIdAndIsDeletedFalse(shopId, pageable);
    }

    /**
     * 根據一級分類分頁查詢商品
     */
    public Page<Product> findProductsByCategory1IdWithPagination(Integer category1Id, Pageable pageable) {
        return productRepository.findByCategory1_IdAndIsDeletedFalse(category1Id, pageable);
    }

    /**
     * 根據二級分類分頁查詢商品
     */
    public Page<Product> findProductsByCategory2IdWithPagination(Integer category2Id, Pageable pageable) {
        return productRepository.findByCategory2_IdAndIsDeletedFalse(category2Id, pageable);
    }

    /**
     * 根據店鋪ID和一級分類分頁查詢商品
     * 注意：由於原Repository沒有提供分頁方法，需要自行新增到Repository中
     * 或在這裡手動實現分頁邏輯
     */
    public Page<Product> findProductsByShopIdAndCategory1IdWithPagination(Integer shopId, Integer category1Id, Pageable pageable) {
        // 這裡需要Repository提供對應的分頁方法
        // 假設Repository已經新增了相應的方法：
        // return productRepository.findByShop_ShopIdAndCategory1_IdAndIsDeletedFalse(shopId, category1Id, pageable);
        
        // 如果Repository沒有該方法，可以使用非分頁方法，然後手動進行分頁處理
        // 但這樣效率較低，不建議在生產環境中使用
        throw new UnsupportedOperationException("此方法需要Repository提供對應的分頁查詢支持");
    }

    /**
     * 根據店鋪ID和二級分類分頁查詢商品
     * 注意：由於原Repository沒有提供分頁方法，需要自行新增到Repository中
     * 或在這裡手動實現分頁邏輯
     */
    public Page<Product> findProductsByShopIdAndCategory2IdWithPagination(Integer shopId, Integer category2Id, Pageable pageable) {
        // 這裡需要Repository提供對應的分頁方法
        // 假設Repository已經新增了相應的方法：
        // return productRepository.findByShop_ShopIdAndCategory2_IdAndIsDeletedFalse(shopId, category2Id, pageable);
        
        throw new UnsupportedOperationException("此方法需要Repository提供對應的分頁查詢支持");
    }

    /**
     * 模糊查詢商品名稱
     */
    public Page<Product> findProductsByNameContaining(String keyword, Pageable pageable) {
        return productRepository.findByProductNameContainingForAdmin(keyword, pageable);
    }

    /**
     * 根據商品上架狀態查詢商品
     */
    public Page<Product> findProductsByActiveStatus(Boolean active, Pageable pageable) {
        return productRepository.findByActiveStatusForAdmin(active, pageable);
    }

    /**
     * 根據商品審核狀態查詢商品
     */
    public Page<Product> findProductsByReviewStatus(Boolean reviewStatus, Pageable pageable) {
        return productRepository.findByReviewStatusForAdmin(reviewStatus, pageable);
    }

    /**
     * 根據商品上架狀態和審核狀態查詢商品
     */
    public Page<Product> findProductsByActiveAndReviewStatus(Boolean active, Boolean reviewStatus, Pageable pageable) {
        return productRepository.findByActiveAndReviewStatusForAdmin(active, reviewStatus, pageable);
    }
    
    /**
     * 根據價格範圍搜尋商品
     */
    public Page<Product> findProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        // 若最低價格為空，則設為0
        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }
        
        // 若最高價格為空，則設為一個極大值
        if (maxPrice == null) {
            maxPrice = new BigDecimal("9999999999.99");
        }
        
        return productRepository.findByPriceRangeForAdmin(minPrice, maxPrice, pageable);
    }

    /**
     * 根據創建日期範圍搜尋商品
     */
    public Page<Product> findProductsByCreatedDateRange(Date startDate, Date endDate, Pageable pageable) {
        // 若開始日期為空，則設為一個很早的日期
        if (startDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(2000, 0, 1, 0, 0, 0); // 2000年1月1日
            startDate = cal.getTime();
        }
        
        // 若結束日期為空，則設為當前日期加一天（包含今天）
        if (endDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, 1); // 明天
            endDate = cal.getTime();
        } else {
            // 如果有提供結束日期，將時分秒設為23:59:59，確保包含當天
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            endDate = cal.getTime();
        }
        
        return productRepository.findByCreatedDateRangeForAdmin(startDate, endDate, pageable);
    }

    /**
     * 進階組合條件搜尋
     */
    public Page<Product> findProductsByAdvancedCriteria(
            String keyword, 
            BigDecimal minPrice, 
            BigDecimal maxPrice, 
            Date startDate, 
            Date endDate, 
            Integer shopId, 
            Integer category1Id, 
            Integer category2Id, 
            Boolean active, 
            Boolean reviewStatus, 
            Pageable pageable) {
        
        // 處理日期範圍
        if (startDate != null && endDate != null) {
            // 確保結束日期包含當天（設為23:59:59）
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            endDate = cal.getTime();
        }
        
        // 若最高價格為空，且最低價格不為空，則設定最高價格為一個較大值
        if (minPrice != null && maxPrice == null) {
            maxPrice = new BigDecimal("9999999999.99");
        }
        
        return productRepository.findByAdvancedCriteriaForAdmin(
                keyword, minPrice, maxPrice, startDate, endDate, 
                shopId, category1Id, category2Id, active, reviewStatus, 
                pageable);
    }
    
    
}
