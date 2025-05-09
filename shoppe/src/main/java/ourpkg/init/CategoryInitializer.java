package ourpkg.init;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 分類初始化器 負責創建第一層和第二層分類，以及它們之間的關聯
 */
@Component
@Order(2)
public class CategoryInitializer implements CommandLineRunner {

	private static final Logger logger = Logger.getLogger(CategoryInitializer.class.getName());
	private final JdbcTemplate jdbcTemplate;

	public CategoryInitializer(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		// 檢查是否已存在分類資料
		Integer category1Count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM Category1", Integer.class);

		if (category1Count != null && category1Count > 0) {
			logger.info("分類資料已存在，跳過初始化");
			return;
		}

		logger.info("開始初始化分類資料...");

		try {
			// 創建第一層分類
			List<Integer> category1Ids = createCategory1();

			// 創建第二層分類及其與第一層的關聯
			createCategory2AndRelations(category1Ids);

			logger.info("分類資料初始化完成！");
		} catch (Exception e) {
			logger.severe("初始化分類資料時發生錯誤: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 創建第一層分類
	 */
	private List<Integer> createCategory1() {
		List<String> category1Names = Arrays.asList("服裝與配件", "電子產品", "家居用品", "美妝保養", "運動戶外", "食品飲料", "母嬰用品", "寵物用品",
				"書籍文具", "汽機車用品");

		List<Integer> categoryIds = new ArrayList<>();
		for (String name : category1Names) {
			try {
				// 插入第一層分類
				jdbcTemplate.update("INSERT INTO Category1 (name) VALUES (?)", name);

				// 獲取剛插入的分類ID
				Integer categoryId = jdbcTemplate.queryForObject("SELECT id FROM Category1 WHERE name = ?",
						Integer.class, name);

				categoryIds.add(categoryId);
				logger.info("創建第一層分類: " + name + " (ID: " + categoryId + ")");
			} catch (Exception e) {
				logger.severe("創建第一層分類 '" + name + "' 時發生錯誤: " + e.getMessage());
				// 繼續處理其他分類
			}
		}
		return categoryIds;
	}

	/**
	 * 創建第二層分類及其與第一層的關聯
	 */
	private void createCategory2AndRelations(List<Integer> category1Ids) {
		if (category1Ids.isEmpty()) {
			logger.warning("沒有第一層分類，無法創建第二層分類");
			return;
		}

		// 定義每個第一層分類對應的第二層分類數據
		Map<Integer, List<String>> categoryDefinitions = new HashMap<>();

		// 服裝與配件
		categoryDefinitions.put(category1Ids.get(0), Arrays.asList("男裝", "女裝", "童裝", "內衣與睡衣", "鞋類", "包包", "配飾"));

		// 電子產品
		categoryDefinitions.put(category1Ids.get(1), Arrays.asList("手機與配件", "電腦與平板", "相機與攝影", "耳機音響", "智能設備", "家電"));

		// 家居用品
		categoryDefinitions.put(category1Ids.get(2), Arrays.asList("傢俱", "廚房用品", "居家裝飾", "收納整理", "寢具", "浴室用品"));

		// 美妝保養
		categoryDefinitions.put(category1Ids.get(3), Arrays.asList("保養品", "彩妝", "香水", "美髮護理", "美容工具", "男士保養"));

		// 運動戶外
		categoryDefinitions.put(category1Ids.get(4), Arrays.asList("運動服飾", "運動鞋", "健身器材", "戶外裝備", "自行車", "游泳與水上運動"));

		// 食品飲料
		categoryDefinitions.put(category1Ids.get(5), Arrays.asList("零食", "飲料", "茶與咖啡", "烘焙食品", "保健食品", "調味品"));

		// 母嬰用品
		categoryDefinitions.put(category1Ids.get(6), Arrays.asList("嬰兒服飾", "奶瓶與餵食", "尿布與清潔", "嬰兒推車", "玩具", "孕婦用品"));

		// 寵物用品
		categoryDefinitions.put(category1Ids.get(7), Arrays.asList("狗狗用品", "貓咪用品", "小寵用品", "寵物食品", "寵物玩具", "寵物保健"));

		// 書籍文具
		categoryDefinitions.put(category1Ids.get(8), Arrays.asList("小說", "教育與參考", "漫畫與輕小說", "文具", "辦公用品", "藝術與手作"));

		// 汽機車用品
		categoryDefinitions.put(category1Ids.get(9), Arrays.asList("汽車配件", "汽車美容", "汽車電子", "機車配件", "機車裝備", "導航與安全"));

		// 遍歷所有第一層分類，創建對應的第二層分類
		for (Integer category1Id : category1Ids) {
			List<String> subCategories = categoryDefinitions.get(category1Id);
			if (subCategories != null) {
				createAndLinkCategory2(category1Id, subCategories);
			} else {
				logger.warning("找不到分類ID " + category1Id + " 的第二層分類定義");
			}
		}
	}

	/**
	 * 創建第二層分類並與第一層分類建立關聯
	 */
	private void createAndLinkCategory2(Integer category1Id, List<String> category2Names) {
		for (String name : category2Names) {
			try {
				// 插入第二層分類
				jdbcTemplate.update("INSERT INTO Category2 (name) VALUES (?)", name);

				// 獲取剛插入的分類ID
				Integer category2Id = jdbcTemplate.queryForObject("SELECT id FROM Category2 WHERE name = ?",
						Integer.class, name);

				// 創建第一與第二層分類的關聯
				jdbcTemplate.update("INSERT INTO Category1_Category2 (c1_id, c2_id) VALUES (?, ?)", category1Id,
						category2Id);

				logger.info("創建第二層分類: " + name + " (ID: " + category2Id + "), 關聯到第一層分類ID: " + category1Id);
			} catch (Exception e) {
				logger.severe("創建第二層分類 '" + name + "' 時發生錯誤: " + e.getMessage());
				// 繼續處理其他分類
			}
		}
	}
}
