package ourpkg.init;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import ourpkg.product.version2.controller_service.ImageFileService;

/**
 * 產品與圖片初始化器 負責創建產品、SKU和產品圖片 依賴於CategoryInitializer已經創建的分類結構
 */
@Component
@Order(3)
public class ProductAndImageInitializer implements CommandLineRunner {

	private static final Logger logger = Logger.getLogger(ProductAndImageInitializer.class.getName());

	private final JdbcTemplate jdbcTemplate;
	private final ImageFileService imageFileService;
	private final Random random = new Random();

	// 初始化圖片分類名稱
	private final String[] sampleImageFolders = { "fashion", "electronics", "home", "beauty", "sports", "food", "baby",
			"pets", "books", "auto" };

	// 每個類別中範例圖片的數量
	private final int sampleImagesPerCategory = 5;

	// 上傳目錄路徑
	private final String uploadDir = "./uploads/";

	public ProductAndImageInitializer(JdbcTemplate jdbcTemplate, ImageFileService imageFileService) {
		this.jdbcTemplate = jdbcTemplate;
		this.imageFileService = imageFileService;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		try {
			// 檢查是否已存在產品資料
			Integer productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Product", Integer.class);

			if (productCount != null && productCount > 0) {
				logger.info("產品資料已存在，驗證圖片...");
				verifyProductImages();
				return;
			}

			// 檢查分類資料是否存在
			Integer category1Count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Category1", Integer.class);

			if (category1Count == null || category1Count == 0) {
				logger.warning("分類資料不存在，無法初始化產品");
				return;
			}

			// 檢查是否有商店資料
			Integer shopCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Shop", Integer.class);

			if (shopCount == null || shopCount == 0) {
				logger.warning("商店資料不存在，無法初始化產品");
				return;
			}

			logger.info("開始初始化產品與圖片資料...");

			// 確保上傳目錄存在
			ensureUploadDirectoryExists();

			// 確保有默認圖片
			String defaultImagePath = ensureDefaultImageExists();

			// 準備分類圖片路徑
			Map<Integer, List<String>> categoryImages = prepareProductImages();

			// 載入一級分類資料
			List<Map<String, Object>> category1List = jdbcTemplate.queryForList("SELECT id, name FROM Category1");

			// 為每個分類創建產品
			for (Map<String, Object> category1 : category1List) {
				Integer category1Id = (Integer) category1.get("id");
				String category1Name = (String) category1.get("name");

				// 載入該一級分類下的二級分類
				List<Map<String, Object>> category2List = jdbcTemplate
						.queryForList(
								"SELECT c2.id, c2.name FROM Category2 c2 "
										+ "JOIN Category1_Category2 cc ON c2.id = cc.c2_id " + "WHERE cc.c1_id = ?",
								category1Id);

				// 獲取該分類的圖片列表
				List<String> categoryImageList = categoryImages.getOrDefault(category1Id, new ArrayList<>());
				if (categoryImageList.isEmpty()) {
					categoryImageList.add(defaultImagePath);
				}

				// 為每個二級分類創建產品
				for (Map<String, Object> category2 : category2List) {
					Integer category2Id = (Integer) category2.get("id");
					String category2Name = (String) category2.get("name");

					createProductsForCategory(category1Id, category1Name, category2Id, category2Name,
							categoryImageList);
				}
			}

			// 完成後驗證所有產品圖片
			verifyProductImages();

			logger.info("產品與圖片資料初始化完成！");
		} catch (Exception e) {
			logger.log(Level.SEVERE, "初始化產品與圖片資料時發生錯誤", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 確保上傳目錄存在
	 */
	private void ensureUploadDirectoryExists() {
		File uploadFolder = new File(uploadDir);
		if (!uploadFolder.exists()) {
			boolean created = uploadFolder.mkdirs();
			if (!created) {
				logger.severe("無法創建上傳目錄： " + uploadDir);
				throw new RuntimeException("無法創建上傳目錄： " + uploadDir);
			}
			logger.info("成功創建上傳目錄： " + uploadDir);
		}

		// 確保每個分類目錄也存在
		for (String folder : sampleImageFolders) {
			File categoryFolder = new File(uploadDir + folder);
			if (!categoryFolder.exists()) {
				boolean created = categoryFolder.mkdirs();
				if (created) {
					logger.info("成功創建分類目錄： " + categoryFolder.getPath());
				} else {
					logger.warning("無法創建分類目錄： " + categoryFolder.getPath());
				}
			}
		}
	}

	/**
	 * 確保默認圖片存在
	 */
	private String ensureDefaultImageExists() {
		String defaultImagePath = "/uploads/default-product-image.jpg";
		File defaultImage = new File("." + defaultImagePath);

		if (!defaultImage.exists()) {
			logger.info("默認圖片不存在，嘗試創建...");

			// 如果默認圖片目錄不存在，確保創建
			defaultImage.getParentFile().mkdirs();

			try {
				// 從uploads目錄複製現有圖片
				File[] existingImages = new File(uploadDir).listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg")
						|| name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".png"));

				if (existingImages != null && existingImages.length > 0) {
					Files.copy(existingImages[0].toPath(), defaultImage.toPath());
					logger.info("從現有圖片創建默認圖片: " + existingImages[0].getPath());
					return defaultImagePath;
				}

				// 如果沒有現有圖片，檢查分類目錄
				for (String folder : sampleImageFolders) {
					File categoryDir = new File(uploadDir + folder);
					if (categoryDir.exists()) {
						File[] categoryImages = categoryDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg")
								|| name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".png"));

						if (categoryImages != null && categoryImages.length > 0) {
							Files.copy(categoryImages[0].toPath(), defaultImage.toPath());
							logger.info("從分類目錄創建默認圖片: " + categoryImages[0].getPath());
							return defaultImagePath;
						}
					}
				}

				// 如果還是找不到，嘗試創建一個簡單的預設圖片
				createPlaceholderImage(defaultImage);
				logger.info("已創建默認產品圖片: " + defaultImage.getPath());
			} catch (IOException e) {
				logger.log(Level.WARNING, "創建默認圖片失敗", e);
				// 嘗試使用備用路徑
				defaultImagePath = "/uploads/no-image.jpg";
				File backupImage = new File("." + defaultImagePath);
				if (!backupImage.exists()) {
					try {
						createPlaceholderImage(backupImage);
						logger.info("已創建備用默認圖片: " + backupImage.getPath());
					} catch (IOException ex) {
						logger.log(Level.SEVERE, "創建備用默認圖片也失敗", ex);
					}
				}
			}
		}

		return defaultImagePath;
	}

	/**
	 * 創建一個簡單的占位圖片
	 */
	private void createPlaceholderImage(File imageFile) throws IOException {
		// 如果父目錄不存在，創建它
		if (!imageFile.getParentFile().exists()) {
			imageFile.getParentFile().mkdirs();
		}

		// 嘗試從resources目錄加載預設圖片
		try {
			ClassPathResource resource = new ClassPathResource("static/images/default-product-image.jpg");
			if (resource.exists()) {
				try (InputStream inputStream = resource.getInputStream()) {
					byte[] bytes = StreamUtils.copyToByteArray(inputStream);
					Files.write(imageFile.toPath(), bytes);
					return;
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "從資源目錄加載圖片失敗", e);
		}

		// 如果無法從資源加載，創建一個簡單的佔位圖片
		byte[] placeholderImageBytes = new byte[10240]; // 10KB的假圖片數據
		Random rnd = new Random();
		rnd.nextBytes(placeholderImageBytes);

		// 添加JPEG文件頭部標記，使其基本可識別為圖片
		placeholderImageBytes[0] = (byte) 0xFF;
		placeholderImageBytes[1] = (byte) 0xD8;
		placeholderImageBytes[placeholderImageBytes.length - 2] = (byte) 0xFF;
		placeholderImageBytes[placeholderImageBytes.length - 1] = (byte) 0xD9;

		Files.write(imageFile.toPath(), placeholderImageBytes);
	}

	/**
	 * 檢查產品圖片和修復問題
	 */
	@Transactional
	private void verifyProductImages() {
		logger.info("開始驗證產品圖片...");

		try {
			// 確保有默認圖片
			String defaultImagePath = ensureDefaultImageExists();

			// 檢查所有產品圖片
			List<Map<String, Object>> productImages = jdbcTemplate
					.queryForList("SELECT image_id, product_id, image_path FROM ProductImage");

			int fixedCount = 0;
			for (Map<String, Object> image : productImages) {
				Integer imageId = (Integer) image.get("image_id");
				String imagePath = (String) image.get("image_path");

				if (imagePath == null || imagePath.isEmpty()) {
					// 圖片路徑為空
					jdbcTemplate.update("UPDATE ProductImage SET image_path = ? WHERE image_id = ?", defaultImagePath,
							imageId);
					fixedCount++;
					continue;
				}

				// 檢查圖片是否存在
				if (imagePath.startsWith("/uploads/")) {
					// 移除開頭的斜杠，因為文件系統路徑不需要
					String relativePath = imagePath.substring(1); // 從 /uploads/... 變成 uploads/...
					File imageFile = new File("./" + relativePath);

					if (!imageFile.exists()) {
						logger.warning("圖片文件不存在: " + imagePath);

						// 更新為默認圖片
						jdbcTemplate.update("UPDATE ProductImage SET image_path = ? WHERE image_id = ?",
								defaultImagePath, imageId);
						fixedCount++;
					}
				}
			}

			// 修復產品主圖信息
			fixPrimaryImages();

			logger.info("圖片驗證完成，修復了 " + fixedCount + " 個圖片記錄");

		} catch (Exception e) {
			logger.log(Level.SEVERE, "驗證產品圖片時發生錯誤", e);
		}
	}

	/**
	 * 修復產品主圖
	 */
	private void fixPrimaryImages() {
		try {
			// 獲取所有產品ID
			List<Integer> productIds = jdbcTemplate.queryForList("SELECT DISTINCT product_id FROM ProductImage",
					Integer.class);

			int fixedPrimaryCount = 0;
			for (Integer productId : productIds) {
				// 檢查產品是否有主圖
				Integer primaryCount = jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM ProductImage WHERE product_id = ? AND is_primary = 1", Integer.class,
						productId);

				if (primaryCount == null || primaryCount == 0) {
					// 沒有主圖，選擇第一張圖片作為主圖
					// 注意：SQL Server 使用 SELECT TOP 1 語法
					List<Integer> imageIds = jdbcTemplate.queryForList(
							"SELECT TOP 1 image_id FROM ProductImage WHERE product_id = ? ORDER BY display_order, image_id",
							Integer.class, productId);

					if (!imageIds.isEmpty()) {
						jdbcTemplate.update("UPDATE ProductImage SET is_primary = 1 WHERE image_id = ?",
								imageIds.get(0));
						fixedPrimaryCount++;
					}
				} else if (primaryCount > 1) {
					// 有多個主圖，只保留一個
					List<Integer> imageIds = jdbcTemplate.queryForList(
							"SELECT image_id FROM ProductImage WHERE product_id = ? AND is_primary = 1 ORDER BY display_order, image_id",
							Integer.class, productId);

					// 保留第一個主圖，其他的設為非主圖
					for (int i = 1; i < imageIds.size(); i++) {
						jdbcTemplate.update("UPDATE ProductImage SET is_primary = 0 WHERE image_id = ?",
								imageIds.get(i));
						fixedPrimaryCount++;
					}
				}
			}

			logger.info("已修復 " + fixedPrimaryCount + " 個產品的主圖信息");
		} catch (Exception e) {
			logger.log(Level.WARNING, "修復產品主圖時發生錯誤", e);
		}
	}

	/**
	 * 準備產品圖片 - 直接使用uploads目錄下的圖片
	 */
	private Map<Integer, List<String>> prepareProductImages() {
		Map<Integer, List<String>> categoryImages = new HashMap<>();
		String defaultImagePath = "/uploads/default-product-image.jpg";

		// 遍歷十個分類目錄
		for (int i = 0; i < sampleImageFolders.length; i++) {
			int categoryId = i + 1;
			List<String> imagePaths = new ArrayList<>();

			// 檢查該分類目錄是否存在
			File categoryDir = new File(uploadDir + sampleImageFolders[i]);
			if (categoryDir.exists() && categoryDir.isDirectory()) {
				// 修改文件過濾器以包含 .jpeg 擴展名
				File[] imageFiles = categoryDir.listFiles((dir, name) -> {
					String lowerName = name.toLowerCase();
					return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png");
				});

				if (imageFiles != null && imageFiles.length > 0) {
					for (File imageFile : imageFiles) {
						// 構建包含子文件夾的完整路徑
						String imagePath = "/uploads/" + sampleImageFolders[i] + "/" + imageFile.getName();
						imagePaths.add(imagePath);
						logger.info("找到分類圖片: " + imagePath);
					}
				} else {
					logger.warning("分類目錄 " + categoryDir.getPath() + " 存在但沒有圖片文件");
				}
			} else {
				logger.warning("分類目錄不存在: " + categoryDir.getPath());
			}

			// 如果該分類沒有圖片，創建一個預設圖片
			if (imagePaths.isEmpty()) {
				String categoryDefaultPath = "/uploads/" + sampleImageFolders[i] + "/default.jpg";
				File categoryDefaultFile = new File("." + categoryDefaultPath);
				try {
					if (!categoryDefaultFile.exists()) {
						if (!categoryDefaultFile.getParentFile().exists()) {
							categoryDefaultFile.getParentFile().mkdirs();
						}
						createPlaceholderImage(categoryDefaultFile);
					}
					imagePaths.add(categoryDefaultPath);
					logger.info("為分類 " + sampleImageFolders[i] + " 創建默認圖片: " + categoryDefaultPath);
				} catch (IOException e) {
					logger.log(Level.WARNING, "無法創建分類預設圖片: " + categoryDefaultPath, e);
					imagePaths.add(defaultImagePath);
				}
			}

			categoryImages.put(categoryId, imagePaths);
		}

		return categoryImages;
	}

	/**
	 * 為特定分類創建產品
	 */
	private void createProductsForCategory(Integer category1Id, String category1Name, Integer category2Id,
			String category2Name, List<String> categoryImageList) {

		try {
			// 獲取所有商店
			List<Map<String, Object>> shops = jdbcTemplate.queryForList("SELECT shop_id FROM Shop");
			if (shops.isEmpty()) {
				logger.warning("沒有找到商店，無法創建商品");
				return;
			}

			// 獲取管理員用戶IDs用於審核
			List<Map<String, Object>> adminUsers = jdbcTemplate.queryForList("SELECT u.user_id FROM [User] u "
					+ "JOIN User_Role ur ON u.user_id = ur.user_id " + "JOIN Role r ON ur.role_id = r.id "
					+ "WHERE r.role_name = 'ADMIN' OR r.role_name = 'SUPER_ADMIN'");

			if (adminUsers.isEmpty()) {
				logger.warning("找不到管理員用戶，將使用1作為預設審核者ID");
				Map<String, Object> defaultAdmin = new HashMap<>();
				defaultAdmin.put("user_id", 1);
				adminUsers.add(defaultAdmin);
			}

			// 每個二級分類創建 2-4 個商品
			int productCount = 2 + random.nextInt(3);

			for (int i = 0; i < productCount; i++) {
				try {
					// 隨機選擇一個商店
					Map<String, Object> shop = shops.get(random.nextInt(shops.size()));
					Integer shopId = (Integer) shop.get("shop_id");

					// 隨機選擇一個管理員ID作為審核者
					Map<String, Object> adminUser = adminUsers.get(random.nextInt(adminUsers.size()));
					Integer reviewedBy = (Integer) adminUser.get("user_id");

					// 生成產品名稱和描述
					String productName = generateProductName(category1Name, category2Name);
					String description = generateProductDescription(category1Name, category2Name, productName);

					// 創建商品
					String insertProductSQL = "INSERT INTO Product "
							+ "(shop_id, c1_id, c2_id, product_name, description, active, review_status, reviewed_by, review_at, created_at, updated_at, is_deleted) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), GETDATE(), ?)";
					jdbcTemplate.update(insertProductSQL, shopId, category1Id, category2Id, productName, description, 1,
							1, reviewedBy, 0);

					// 獲取剛插入的商品ID
					Integer productId = jdbcTemplate.queryForObject(
							"SELECT MAX(product_id) FROM Product WHERE shop_id = ? AND product_name = ?", Integer.class,
							shopId, productName);

					logger.info("已創建產品: ID=" + productId + ", 名稱=" + productName);

					// 創建商品圖片
					createProductImages(productId, categoryImageList);

					// 創建SKU
					createDefaultSku(productId);

					logger.info("完成產品創建: " + productName + ", 分類: " + category1Name + " > " + category2Name);
				} catch (Exception e) {
					logger.log(Level.SEVERE, "創建單個產品時發生錯誤", e);
					e.printStackTrace();
					// 繼續處理其他產品
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "為分類創建產品時發生錯誤: " + category1Name + " > " + category2Name, e);
			e.printStackTrace();
		}
	}

	/**
	 * 創建商品圖片
	 */
	private void createProductImages(Integer productId, List<String> categoryImageList) {
		// 為每個產品創建1-3張圖片
		int imageCount = 1 + random.nextInt(Math.min(2, categoryImageList.size()));

		for (int i = 0; i < imageCount; i++) {
			// 從分類圖片中隨機選擇
			String imagePath = categoryImageList.get(random.nextInt(categoryImageList.size()));
			boolean isPrimary = (i == 0); // 第一張為主圖

			try {
				// 創建圖片記錄
				String insertImageSQL = "INSERT INTO ProductImage "
						+ "(product_id, image_path, is_primary, display_order, created_at, updated_at) "
						+ "VALUES (?, ?, ?, ?, GETDATE(), GETDATE())";
				jdbcTemplate.update(insertImageSQL, productId, imagePath, isPrimary ? 1 : 0, i);

				logger.info("已為產品 " + productId + " 添加圖片: " + imagePath + (isPrimary ? " (主圖)" : ""));
			} catch (Exception e) {
				logger.log(Level.SEVERE, "創建產品圖片時發生錯誤", e);
				e.printStackTrace();
				// 繼續處理其他圖片
			}
		}
	}

	/**
	 * 創建默認SKU
	 */
	private void createDefaultSku(Integer productId) {
		// 生成隨機價格 (100-5000)
		BigDecimal price = new BigDecimal(100 + random.nextInt(4901));

		// 生成隨機庫存 (10-200)
		int stock = 10 + random.nextInt(191);

		try {
			// 創建默認 SKU
			String insertSkuSQL = "INSERT INTO SKU (product_id, stock, price, spec_pairs, is_deleted) VALUES (?, ?, ?, ?, ?)";
			jdbcTemplate.update(insertSkuSQL, productId, stock, price, "{\"default\":\"default\"}", 0);

			logger.info("已為產品 " + productId + " 創建SKU: 價格=" + price + ", 庫存=" + stock);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "創建SKU時發生錯誤: " + e.getMessage(), e);
			e.printStackTrace();
			// 繼續執行
		}
	}

	/**
	 * 生成產品名稱
	 */
	private String generateProductName(String category1Name, String category2Name) {
		// 根據分類生成品牌名
		String[] brands = { "台灣良品", "東方美學", "綠野仙蹤", "純真年代", "青春無敵", "星空漫步", "自然之選", "匠心工藝", "品味生活", "優選家居", "科技尖端",
				"時尚先鋒" };
		String brand = brands[random.nextInt(brands.length)];

		// 根據分類生成形容詞
		String[] adjectives = { "經典", "時尚", "高級", "輕便", "舒適", "環保", "高效", "豪華", "實用", "精緻", "簡約", "創意", "多功能", "智能",
				"典雅", "復古", "現代", "純淨" };
		String adjective = adjectives[random.nextInt(adjectives.length)];

		// 根據分類生成特性詞
		String feature = "";
		if (category1Name.contains("服裝")) {
			String[] features = { "修身", "寬鬆", "彈性", "防水", "透氣", "保暖", "速乾", "防皺" };
			feature = features[random.nextInt(features.length)];
		} else if (category1Name.contains("電子")) {
			String[] features = { "快充", "高清", "無線", "觸控", "防水", "超薄", "長效", "智能" };
			feature = features[random.nextInt(features.length)];
		} else if (category1Name.contains("家居")) {
			String[] features = { "可折疊", "防塵", "節省空間", "抗污", "耐用", "易清潔", "環保", "多用途" };
			feature = features[random.nextInt(features.length)];
		} else if (category1Name.contains("食品")) {
			String[] features = { "有機", "無糖", "低脂", "即食", "天然", "手工", "傳統", "新鮮" };
			feature = features[random.nextInt(features.length)];
		}

		return brand + " " + adjective + feature + " " + category2Name;
	}

	/**
	 * 生成產品描述
	 */
	private String generateProductDescription(String category1Name, String category2Name, String productName) {
		StringBuilder description = new StringBuilder();

		description.append("【產品名稱】").append(productName).append("\n\n");
		description.append("【產品特點】\n");

		// 根據分類生成不同的特點
		if (category1Name.contains("服裝")) {
			description.append("• 採用高品質面料，舒適透氣\n");
			description.append("• 精細車縫工藝，耐穿耐用\n");
			description.append("• 時尚設計，凸顯個人風格\n");
			description.append("• 多場合適用，實用百搭\n");
		} else if (category1Name.contains("電子")) {
			description.append("• 採用先進技術，性能穩定\n");
			description.append("• 低耗能設計，續航持久\n");
			description.append("• 時尚外觀，輕薄便攜\n");
			description.append("• 多功能整合，使用便利\n");
		} else if (category1Name.contains("家居")) {
			description.append("• 優質材料製造，堅固耐用\n");
			description.append("• 人體工學設計，使用舒適\n");
			description.append("• 簡約美觀，融入各種家居風格\n");
			description.append("• 安裝便捷，使用方便\n");
		} else if (category1Name.contains("食品")) {
			description.append("• 精選原料，健康美味\n");
			description.append("• 嚴格把關，品質保證\n");
			description.append("• 營養均衡，適合日常食用\n");
			description.append("• 獨特配方，口感絕佳\n");
		} else {
			description.append("• 高品質選材，品質保證\n");
			description.append("• 精心設計，使用便捷\n");
			description.append("• 多功能實用，物超所值\n");
			description.append("• 售後保障，使用無憂\n");
		}

		description.append("\n【使用方法】\n");
		description.append("請參考產品說明書或包裝上的指示使用。\n\n");

		description.append("【注意事項】\n");
		description.append("1. 請按照產品說明正確使用\n");
		description.append("2. 請妥善保存產品包裝及說明書\n");
		description.append("3. 如有任何疑問，請聯繫客服\n\n");

		description.append("【產品規格】\n");
		description.append("品牌：").append(productName.split(" ")[0]).append("\n");
		description.append("型號：").append(generateRandomModelNumber()).append("\n");
		description.append("產地：台灣\n");
		description.append("保固：一年\n\n");

		description.append("感謝您選購本產品，希望能為您帶來美好的使用體驗！");

		return description.toString();
	}

	/**
	 * 生成隨機型號
	 */
	private String generateRandomModelNumber() {
		StringBuilder sb = new StringBuilder();

		// 兩個大寫字母
		for (int i = 0; i < 2; i++) {
			sb.append((char) ('A' + random.nextInt(26)));
		}

		// 四個數字
		for (int i = 0; i < 4; i++) {
			sb.append(random.nextInt(10));
		}

		return sb.toString();
	}
}