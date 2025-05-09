package ourpkg.product.version2.complete_update;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;

import lombok.RequiredArgsConstructor;
import ourpkg.jwt.JsonWebTokenAuthentication;
import ourpkg.jwt.JsonWebTokenUtility;
import ourpkg.product.Product;
import ourpkg.product.ProductImage;
import ourpkg.product.ProductImageRepository;
import ourpkg.product.ProductRepository;
import ourpkg.product.ProductShopRepository;
import ourpkg.product.version2.controller_service.ImageFileService;
import ourpkg.shop.Shop;
import ourpkg.sku.Sku;
import ourpkg.sku.SkuRepository;
import ourpkg.user_role_permission.user.User;

/**
 * 商品完整更新控制器 - 提供一次性更新商品基本資訊、圖片和SKU的功能
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductCompleteUpdateController {

private final JsonWebTokenUtility jsonWebTokenUtility;
    
    private static final Logger logger = LoggerFactory.getLogger(ProductCompleteUpdateController.class);

    @Autowired
    private final ProductRepository repo;
    @Autowired
    private final ProductImageRepository productImageRepo;
    @Autowired
    private final ProductShopRepository productShopRepository;
    @Autowired
    private final SkuRepository skuRepo;
    @Autowired
    private final ImageFileService imageFileService;
    @Autowired
    private final ObjectMapper objectMapper;

    /**
     * 一次性更新商品、圖片和SKU
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<ProductCompleteResDTO3> updateCompleteProduct(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Integer id,
            @RequestParam(value = "productName", required = false) String productName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "deleteImageIds", required = false) String deleteImageIdsJson,
            @RequestParam(value = "primaryImageId", required = false) Integer primaryImageId,
            @RequestParam(value = "updateSkusJson", required = false) String updateSkusJson,
            @RequestParam(value = "createSkusJson", required = false) String createSkusJson,
            @RequestParam(value = "deleteSkuIds", required = false) String deleteSkuIdsJson,
            @RequestPart(required = false) List<MultipartFile> newImages) {

        try {
            // 從JWT claims中獲取userId
            Integer userId = getUserIdFromAuthentication();

            // 解析JSON字符串
            List<Integer> deleteImageIds = null;
            if (deleteImageIdsJson != null && !deleteImageIdsJson.isEmpty()) {
                deleteImageIds = objectMapper.readValue(deleteImageIdsJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            }

            List<SkuUpdateDTO3> updateSkus = new ArrayList<>();
            if (updateSkusJson != null && !updateSkusJson.isEmpty()) {
                updateSkus = objectMapper.readValue(updateSkusJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SkuUpdateDTO3.class));
            }

            List<SkuCreateDTO3> createSkus = new ArrayList<>();
            if (createSkusJson != null && !createSkusJson.isEmpty()) {
                createSkus = objectMapper.readValue(createSkusJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, SkuCreateDTO3.class));
            }

            List<Integer> deleteSkuIds = null;
            if (deleteSkuIdsJson != null && !deleteSkuIdsJson.isEmpty()) {
                deleteSkuIds = objectMapper.readValue(deleteSkuIdsJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class));
            }

            // 創建DTO對象
            ProductCompleteUpdateDTO3 updateData = new ProductCompleteUpdateDTO3();
            updateData.setProductName(productName);
            updateData.setDescription(description);
            updateData.setActive(active);
            updateData.setDeleteImageIds(deleteImageIds);
            updateData.setPrimaryImageId(primaryImageId);
            updateData.setUpdateSkus(updateSkus);
            updateData.setCreateSkus(createSkus);
            updateData.setDeleteSkuIds(deleteSkuIds);

            // 調用服務方法更新產品
            ProductCompleteResDTO3 updatedProduct = updateComplete(userId, id, updateData, newImages);

            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            logger.error("處理更新請求時出錯", e);
            throw new RuntimeException("處理更新請求時出錯: " + e.getMessage(), e);
        }
    }

    /**
     * 完整更新商品服務方法
     */
    @Transactional
    public ProductCompleteResDTO3 updateComplete(Integer userId, Integer productId, ProductCompleteUpdateDTO3 updateData,
            List<MultipartFile> newImages) {

        try {
            logger.info("開始完整更新商品 ID: {}", productId);

            // 獲取現有商品，確保存在且未刪除
            Product product = repo.findByProductIdAndIsDeletedFalse(productId)
                    .orElseThrow(() -> new IllegalArgumentException("該商品 ID 不存在或已被刪除"));

            // 確認商品屬於該用戶的店鋪
            Shop shop = productShopRepository.findByUser_UserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用戶沒有對應的店鋪"));

            if (!product.getShop().getShopId().equals(shop.getShopId())) {
                throw new IllegalArgumentException("該商品不屬於你的店鋪");
            }

            // 更新商品基本資訊
            if (updateData.getProductName() != null) {
                product.setProductName(updateData.getProductName());
            }
            if (updateData.getDescription() != null) {
                product.setDescription(updateData.getDescription());
            }
            if (updateData.getActive() != null) {
                product.setActive(updateData.getActive());
            }

            // 處理圖片刪除
            if (updateData.getDeleteImageIds() != null && !updateData.getDeleteImageIds().isEmpty()) {
                processDeleteImages(product, updateData.getDeleteImageIds());
            }

            // 處理新增圖片
            if (newImages != null && !newImages.isEmpty()) {
                processNewImages(product, newImages);
            }

            // 設置主圖
            if (updateData.getPrimaryImageId() != null) {
                setPrimaryImage(product, updateData.getPrimaryImageId());
            }

            // 確保至少有一張主圖
            ensurePrimaryImage(product);

            // 更新現有SKU
            List<SkuResDTO3> updatedSkus = new ArrayList<>();
            if (updateData.getUpdateSkus() != null && !updateData.getUpdateSkus().isEmpty()) {
                updatedSkus.addAll(updateProductSkus(product, updateData.getUpdateSkus()));
            }

            // 創建新SKU
            List<SkuResDTO3> newSkus = new ArrayList<>();
            if (updateData.getCreateSkus() != null && !updateData.getCreateSkus().isEmpty()) {
                newSkus.addAll(createProductSkus(product, updateData.getCreateSkus()));
            }

            // 刪除指定SKU
            if (updateData.getDeleteSkuIds() != null && !updateData.getDeleteSkuIds().isEmpty()) {
                deleteProductSkus(product, updateData.getDeleteSkuIds());
            }

            // 保存更新後的商品
            Product savedProduct = repo.save(product);

            // 獲取所有未刪除的SKU
            List<Sku> allActiveSkus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(productId);
            List<SkuResDTO3> allSkuDTOs = allActiveSkus.stream()
                    .map(this::convertToSkuResDTO3)
                    .collect(Collectors.toList());

            // 組合結果並返回
            return buildCompleteResponse(savedProduct, allSkuDTOs);

        } catch (Exception e) {
            logger.error("更新完整商品失敗", e);
            throw new RuntimeException("更新完整商品失敗: " + e.getMessage(), e);
        }
    }

    /**
     * 處理刪除圖片
     */
    private void processDeleteImages(Product product, List<Integer> deleteImageIds) {
        boolean deletingPrimary = false;

        for (Integer imageId : deleteImageIds) {
            ProductImage image = productImageRepo.findById(imageId)
                    .orElseThrow(() -> new IllegalArgumentException("圖片ID不存在: " + imageId));

            // 確認圖片屬於該商品
            if (!image.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalArgumentException("圖片ID " + imageId + " 不屬於該商品");
            }

            // 檢查是否為主圖
            if (image.getIsPrimary()) {
                deletingPrimary = true;
            }

            // 刪除實際圖片文件
            imageFileService.deleteImageFile(image.getImagePath());

            // 刪除資料庫中的圖片記錄
            productImageRepo.delete(image);
            product.getProductImages().remove(image);
        }

        logger.info("已刪除 {} 張圖片，其中包含主圖: {}", deleteImageIds.size(), deletingPrimary);
    }

    /**
     * 處理新增圖片
     */
    private void processNewImages(Product product, List<MultipartFile> newImages) {
        logger.info("處理 {} 張新圖片", newImages.size());
        int displayOrder = product.getProductImages().size(); // 從現有圖片數量開始計算顯示順序

        for (MultipartFile image : newImages) {
            if (image != null && !image.isEmpty()) {
                try {
                    // 使用ImageFileService儲存圖片
                    String imagePath = imageFileService.saveImage(image);
                    logger.info("已保存圖片: {}", imagePath);

                    // 創建商品圖片實體並設置關聯
                    ProductImage productImage = new ProductImage();
                    productImage.setProduct(product);
                    productImage.setImagePath(imagePath);
                    productImage.setDisplayOrder(displayOrder++);
                    productImage.setIsPrimary(false); // 默認為非主圖

                    ProductImage savedImage = productImageRepo.save(productImage);
                    product.getProductImages().add(savedImage);
                } catch (Exception e) {
                    logger.error("圖片處理失敗", e);
                    throw new RuntimeException("圖片處理失敗: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 設置主圖
     */
    private void setPrimaryImage(Product product, Integer primaryImageId) {
        // 確認圖片存在且屬於該商品
        ProductImage primaryImage = productImageRepo.findById(primaryImageId)
                .orElseThrow(() -> new IllegalArgumentException("指定的主圖ID不存在"));

        if (!primaryImage.getProduct().getProductId().equals(product.getProductId())) {
            throw new IllegalArgumentException("圖片ID " + primaryImageId + " 不屬於該商品");
        }

        // 重置所有圖片為非主圖
        for (ProductImage img : product.getProductImages()) {
            img.setIsPrimary(false);
            productImageRepo.save(img);
        }

        // 設置新的主圖
        primaryImage.setIsPrimary(true);
        productImageRepo.save(primaryImage);

        logger.info("已設置圖片ID {} 為主圖", primaryImageId);
    }

    /**
     * 確保商品至少有一張主圖
     */
    private void ensurePrimaryImage(Product product) {
        List<ProductImage> allImages = productImageRepo.findByProduct_ProductId(product.getProductId());

        if (allImages.isEmpty()) {
            // 如果沒有任何圖片，添加默認圖片
            ProductImage defaultImage = new ProductImage();
            defaultImage.setProduct(product);
            defaultImage.setImagePath("/uploads/default-product-image.jpg");
            defaultImage.setIsPrimary(true);
            defaultImage.setDisplayOrder(0);
            
            ProductImage savedImage = productImageRepo.save(defaultImage);
            product.getProductImages().add(savedImage);
            
            logger.info("沒有圖片，已添加默認圖片");
        } else {
            // 檢查是否有主圖
            boolean hasPrimary = allImages.stream().anyMatch(ProductImage::getIsPrimary);

            if (!hasPrimary) {
                // 將第一張圖片設為主圖
                ProductImage firstImage = allImages.get(0);
                firstImage.setIsPrimary(true);
                productImageRepo.save(firstImage);
                
                logger.info("設置圖片ID {} 為新主圖", firstImage.getImageId());
            }
        }
    }

    /**
     * 更新現有SKU
     */
    private List<SkuResDTO3> updateProductSkus(Product product, List<SkuUpdateDTO3> skuDTOs) {
        List<SkuResDTO3> updatedSkus = new ArrayList<>();
        
        // 獲取產品所有現有規格組合以檢查唯一性
        List<Sku> existingSkus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(product.getProductId());
        Map<String, Integer> specPairsToSkuIdMap = new HashMap<>();
        for (Sku sku : existingSkus) {
            if (sku.getSpecPairs() != null && !sku.getSpecPairs().isEmpty()) {
                specPairsToSkuIdMap.put(sku.getSpecPairs(), sku.getSkuId());
            }
        }

        for (SkuUpdateDTO3 skuDTO : skuDTOs) {
            // 驗證SKU ID
            Integer skuId = skuDTO.getSkuId();
            if (skuId == null) {
                throw new IllegalArgumentException("更新SKU時必須提供SKU ID");
            }
            
            Sku sku = skuRepo.findById(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("SKU不存在: " + skuId));
            
            // 確認SKU屬於該商品
            if (!sku.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalArgumentException("SKU " + skuId + " 不屬於該商品");
            }

            // 確認規格組合唯一性
            if (skuDTO.getSpecPairs() != null && !skuDTO.getSpecPairs().isEmpty()) {
                Sku tempSku = new Sku();
                tempSku.setSpecPairsFromMap(skuDTO.getSpecPairs());
                String specPairsJson = tempSku.getSpecPairs();
                
                // 檢查規格組合是否已被其他SKU使用
                Integer existingSkuId = specPairsToSkuIdMap.get(specPairsJson);
                if (existingSkuId != null && !existingSkuId.equals(skuId)) {
                    throw new IllegalArgumentException("規格組合已被其他SKU使用: " + skuDTO.getSpecPairs());
                }
            }

            // 更新SKU資料
            if (skuDTO.getPrice() != null) {
                if (skuDTO.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException("SKU 價格不能小於 0");
                }
                sku.setPrice(skuDTO.getPrice());
            }
            
            if (skuDTO.getStock() != null) {
                if (skuDTO.getStock() < 0) {
                    throw new IllegalArgumentException("SKU 庫存不能小於 0");
                }
                sku.setStock(skuDTO.getStock());
            }
            
            if (skuDTO.getSpecPairs() != null) {
                sku.setSpecPairsFromMap(skuDTO.getSpecPairs());
            }

            // 保存更新後的SKU
            Sku savedSku = skuRepo.save(sku);
            
            // 轉換為DTO
            SkuResDTO3 skuResDTO = convertToSkuResDTO3(savedSku);
            updatedSkus.add(skuResDTO);
        }
        
        logger.info("已更新 {} 個 SKU", updatedSkus.size());
        return updatedSkus;
    }

    /**
     * 創建新SKU
     */
    private List<SkuResDTO3> createProductSkus(Product product, List<SkuCreateDTO3> skuDTOs) {
        List<SkuResDTO3> newSkus = new ArrayList<>();
        
        // 獲取產品所有現有規格組合以檢查唯一性
        List<Sku> existingSkus = skuRepo.findByProduct_ProductIdAndIsDeletedFalse(product.getProductId());
        Set<String> existingSpecPairs = new HashSet<>();
        for (Sku sku : existingSkus) {
            if (sku.getSpecPairs() != null && !sku.getSpecPairs().isEmpty()) {
                existingSpecPairs.add(sku.getSpecPairs());
            }
        }

        for (SkuCreateDTO3 skuDTO : skuDTOs) {
            // 確認價格和庫存符合要求
            if (skuDTO.getStock() == null) {
                throw new IllegalArgumentException("SKU 庫存不能為空");
            }
            if (skuDTO.getStock() < 0) {
                throw new IllegalArgumentException("SKU 庫存不能小於 0");
            }
            if (skuDTO.getPrice() == null) {
                throw new IllegalArgumentException("SKU 價格不能為空");
            }
            if (skuDTO.getPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("SKU 價格不能小於 0");
            }

            // 檢查規格組合唯一性
            if (skuDTO.getSpecPairs() != null && !skuDTO.getSpecPairs().isEmpty()) {
                Sku tempSku = new Sku();
                tempSku.setSpecPairsFromMap(skuDTO.getSpecPairs());
                String specPairsJson = tempSku.getSpecPairs();

                if (existingSpecPairs.contains(specPairsJson)) {
                    throw new IllegalArgumentException("規格組合重複: " + skuDTO.getSpecPairs());
                }

                existingSpecPairs.add(specPairsJson);
            }

            // 創建新SKU
            Sku sku = new Sku();
            sku.setProduct(product);
            sku.setStock(skuDTO.getStock());
            sku.setPrice(skuDTO.getPrice());
            sku.setSpecPairsFromMap(skuDTO.getSpecPairs());
            sku.setIsDeleted(false);

            Sku savedSku = skuRepo.save(sku);
            
            // 轉換為DTO
            SkuResDTO3 skuResDTO = convertToSkuResDTO3(savedSku);
            newSkus.add(skuResDTO);
        }
        
        logger.info("已創建 {} 個新 SKU", newSkus.size());
        return newSkus;
    }

    /**
     * 刪除SKU
     */
    private void deleteProductSkus(Product product, List<Integer> skuIds) {
        for (Integer skuId : skuIds) {
            Sku sku = skuRepo.findById(skuId)
                    .orElseThrow(() -> new IllegalArgumentException("SKU不存在: " + skuId));
            
            // 確認SKU屬於該商品
            if (!sku.getProduct().getProductId().equals(product.getProductId())) {
                throw new IllegalArgumentException("SKU " + skuId + " 不屬於該商品");
            }
            
            // 執行軟刪除
            sku.setIsDeleted(true);
            skuRepo.save(sku);
        }
        
        logger.info("已刪除 {} 個 SKU", skuIds.size());
    }

    /**
     * 將Sku實體轉換為SkuResDTO3
     */
    private SkuResDTO3 convertToSkuResDTO3(Sku sku) {
        SkuResDTO3 dto = new SkuResDTO3();
        dto.setSkuId(sku.getSkuId());
        dto.setProductId(sku.getProduct().getProductId());
        dto.setProductName(sku.getProduct().getProductName());
        dto.setStock(sku.getStock());
        dto.setPrice(sku.getPrice());
        dto.setSpecPairs(sku.getSpecPairsAsMap());
        dto.setSpecDescription(generateSpecDescription(sku.getSpecPairsAsMap()));
        dto.setIsDeleted(sku.getIsDeleted());
        return dto;
    }

    /**
     * 根據規格鍵值對生成規格描述文字
     */
    private String generateSpecDescription(Map<String, String> specPairs) {
        if (specPairs == null || specPairs.isEmpty()) {
            return "";
        }

        return specPairs.values().stream().filter(value -> value != null && !value.isEmpty())
                .collect(Collectors.joining("/"));
    }

    /**
     * 構建完整的響應結果
     */
    private ProductCompleteResDTO3 buildCompleteResponse(Product product, List<SkuResDTO3> skus) {
        // 手動構建ProductResDTO3
        ProductResDTO3 productResDTO3 = new ProductResDTO3();
        productResDTO3.setProductId(product.getProductId());
        productResDTO3.setProductName(product.getProductName());
        productResDTO3.setDescription(product.getDescription());
        productResDTO3.setActive(product.getActive());
        productResDTO3.setCategory1Id(product.getCategory1().getId());
        productResDTO3.setCategory1Name(product.getCategory1().getName());
        productResDTO3.setCategory2Id(product.getCategory2().getId());
        productResDTO3.setCategory2Name(product.getCategory2().getName());
        productResDTO3.setIsDeleted(product.getIsDeleted());
        productResDTO3.setCreatedAt(product.getCreatedAt());
        productResDTO3.setUpdatedAt(product.getUpdatedAt());

        // 設置商店信息
        if (product.getShop() != null) {
            productResDTO3.setShopId(product.getShop().getShopId());
            productResDTO3.setShopName(product.getShop().getShopName());
        }

        // 設置主圖 URL (如果有的話)
        String primaryImageUrl = null;
        List<String> imageUrls = new ArrayList<>();
        for (ProductImage img : product.getProductImages()) {
            imageUrls.add(img.getImagePath());
            if (img.getIsPrimary()) {
                primaryImageUrl = img.getImagePath();
            }
        }
        productResDTO3.setPrimaryImageUrl(primaryImageUrl);
        productResDTO3.setImageUrls(imageUrls);

        // 設置價格範圍
        if (!skus.isEmpty()) {
            double minPrice = skus.stream().map(sku -> sku.getPrice().doubleValue()).min(Double::compare).orElse(0.0);
            double maxPrice = skus.stream().map(sku -> sku.getPrice().doubleValue()).max(Double::compare).orElse(0.0);
            ProductResDTO3.PriceRangeDTO3 priceRange = new ProductResDTO3.PriceRangeDTO3(minPrice, maxPrice);
            productResDTO3.setPriceRange(priceRange);
        }

        // 設置SKU數量
        productResDTO3.setSkuCount(skus.size());

        // 組合結果並返回
        ProductCompleteResDTO3 result = new ProductCompleteResDTO3();
        result.setProduct(productResDTO3);
        result.setSkus(skus);

        // 添加規格信息
        List<ProductSpecDTO3> specs = extractProductSpecifications(skus);
        result.setSpecifications(specs);

        return result;
    }

    /**
     * 從SKU列表中提取商品規格信息
     */
    private List<ProductSpecDTO3> extractProductSpecifications(List<SkuResDTO3> skus) {
        if (skus == null || skus.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有規格名稱和對應的值
        Map<String, Set<String>> specValueMap = new HashMap<>();

        for (SkuResDTO3 sku : skus) {
            if (sku.getSpecPairs() != null) {
                for (Map.Entry<String, String> entry : sku.getSpecPairs().entrySet()) {
                    String specName = entry.getKey();
                    String specValue = entry.getValue();

                    if (specName != null && !specName.isEmpty() && specValue != null && !specValue.isEmpty()) {
                        specValueMap.computeIfAbsent(specName, k -> new HashSet<>()).add(specValue);
                    }
                }
            }
        }

        // 轉換為 ProductSpecDTO3 列表
        return specValueMap.entrySet().stream().map(entry -> {
            ProductSpecDTO3 spec = new ProductSpecDTO3();
            spec.setSpecName(entry.getKey());

            List<SpecValueDTO3> values = entry.getValue().stream().map(value -> {
                SpecValueDTO3 valueDTO3 = new SpecValueDTO3();
                valueDTO3.setValue(value);
                return valueDTO3;
            }).collect(Collectors.toList());

            spec.setValues(values);
            return spec;
        }).collect(Collectors.toList());
    }

    /**
     * 輔助方法：從當前認證上下文中獲取userId
     */
    private Integer getUserIdFromAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JsonWebTokenAuthentication) {
            JsonWebTokenAuthentication auth = (JsonWebTokenAuthentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            String token = (String) auth.getCredentials();

            JWTClaimsSet claims = jsonWebTokenUtility.validateToken(token);
            try {
                if (claims != null && claims.getClaim("userId") != null) {
                    return ((Number) claims.getClaim("userId")).intValue();
                }
            } catch (Exception e) {
                throw new IllegalStateException("無法從token中提取用戶ID", e);
            }
        }
        throw new IllegalStateException("用戶未正確認證或token中沒有userId");
    }
}
