package ourpkg.init;

import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;                       // 引入 Logger
import org.slf4j.LoggerFactory;                // 引入 LoggerFactory
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ourpkg.product.ProductShopRepository; // 假設路徑正確
import ourpkg.shop.Shop;                    // 假設路徑正確
import ourpkg.user_role_permission.Role;
import ourpkg.user_role_permission.RoleRepository;
import ourpkg.user_role_permission.user.User;
import ourpkg.user_role_permission.user.UserRepository;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    // 添加 Logger
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final ProductShopRepository productShopRepository; // Repository for Shop


    public SuperAdminInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, ProductShopRepository productShopRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
        this.productShopRepository = productShopRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // 檢查 SUPER_ADMIN 角色是否存在，若不存在則建立
        Role superAdminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("SUPER_ADMIN");
                    logger.info("Creating role: SUPER_ADMIN");
                    return roleRepository.save(newRole);
                });

        // 檢查 ADMIN 角色是否存在，若不存在則建立
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("ADMIN");
                     logger.info("Creating role: ADMIN");
                    return roleRepository.save(newRole);
                });

        // 檢查 USER 角色是否存在，若不存在則建立
        Role userRole = roleRepository.findByRoleName("USER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("USER");
                     logger.info("Creating role: USER");
                    return roleRepository.save(newRole);
                });

        // 檢查 SELLER 角色是否存在，若不存在則建立
        Role sellerRole = roleRepository.findByRoleName("SELLER")
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setRoleName("SELLER");
                     logger.info("Creating role: SELLER");
                    return roleRepository.save(newRole);
                });

        // 創建 SUPER_ADMIN 用戶
        createUserIfNotExists("SuperAdmin", "superadmin@example.com", "admin123", "0123456789", superAdminRole);

        //創建 Watcher用戶
        createUserIfNotExists("Watcher", "Watcher@example.com", "admin123", "0123456788", adminRole);
        createUserIfNotExists("吳建豪", "admin1@example.com", "admin123", "0911111222", adminRole);
        createUserIfNotExists("陳俊豪", "admin2@example.com", "admin123", "0922222333", adminRole);
        createUserIfNotExists("林思妤", "admin3@example.com", "admin123", "0933333444", adminRole);
        createUserIfNotExists("張柏翰", "admin4@example.com", "admin123", "0944444555", adminRole);
        createUserIfNotExists("黃詩涵", "admin5@example.com", "admin123", "0955555666", adminRole);
        createUserIfNotExists("李承翰", "admin6@example.com", "admin123", "0966666777", adminRole);
        createUserIfNotExists("許家豪", "admin7@example.com", "admin123", "0977777888", adminRole);
        createUserIfNotExists("周雅婷", "admin8@example.com", "admin123", "0988888999", adminRole);
        createUserIfNotExists("趙志偉", "admin9@example.com", "admin123", "0999999000", adminRole);
        createUserIfNotExists("何俊霖", "admin10@example.com", "admin123", "0910101122", adminRole);
        createUserIfNotExists("沈怡君", "admin11@example.com", "admin123", "0920202233", adminRole);
        createUserIfNotExists("徐柏宇", "admin12@example.com", "admin123", "0930303344", adminRole);
        createUserIfNotExists("許怡萱", "admin13@example.com", "admin123", "0940404455", adminRole);
        createUserIfNotExists("郭承恩", "admin14@example.com", "admin123", "0950505566", adminRole);
        createUserIfNotExists("曾柏霖", "admin15@example.com", "admin123", "0960606677", adminRole);
        createUserIfNotExists("賴詠翔", "admin16@example.com", "admin123", "0970707788", adminRole);
        createUserIfNotExists("劉芳儀", "admin17@example.com", "admin123", "0980808899", adminRole);
        createUserIfNotExists("葉家豪", "admin18@example.com", "admin123", "0990909900", adminRole);
        createUserIfNotExists("蘇俊賢", "admin19@example.com", "admin123", "0912121122", adminRole);
        createUserIfNotExists("柯欣怡", "admin20@example.com", "admin123", "0923232233", adminRole);

        // 創建 Waylay 用戶
        createUserIfNotExists("Waylay", "waylay@example.com", "Test", "0912345678", userRole);
        createUserIfNotExists("王小明", "user1@example.com", "Test", "0911111111", userRole);
        createUserIfNotExists("李大華", "user2@example.com", "Test", "0922222222", userRole);
        createUserIfNotExists("張美玲", "user3@example.com", "Test", "0933333333", userRole);
        createUserIfNotExists("陳志強", "user4@example.com", "Test", "0944444444", userRole);
        createUserIfNotExists("黃麗芬", "user5@example.com", "Test", "0955555555", userRole);
        createUserIfNotExists("林建宏", "user6@example.com", "Test", "0966666666", userRole);
        createUserIfNotExists("趙信文", "user7@example.com", "Test", "0977777777", userRole);
        createUserIfNotExists("周雅惠", "user8@example.com", "Test", "0988888888", userRole);
        createUserIfNotExists("吳宗翰", "user9@example.com", "Test", "0999999999", userRole);
        createUserIfNotExists("何婉婷", "user10@example.com", "Test", "0910101010", userRole);
        createUserIfNotExists("沈柏霖", "user11@example.com", "Test", "0920202020", userRole);
        createUserIfNotExists("徐翠萍", "user12@example.com", "Test", "0930303030", userRole);
        createUserIfNotExists("許正豪", "user13@example.com", "Test", "0940404040", userRole);
        createUserIfNotExists("郭怡君", "user14@example.com", "Test", "0950505050", userRole);
        createUserIfNotExists("曾志偉", "user15@example.com", "Test", "0960606060", userRole);
        createUserIfNotExists("賴詠翔", "user16@example.com", "Test", "0970707070", userRole);
        createUserIfNotExists("劉芳儀", "user17@example.com", "Test", "0980808080", userRole);
        createUserIfNotExists("葉家豪", "user18@example.com", "Test", "0990909090", userRole);
        createUserIfNotExists("蘇俊賢", "user19@example.com", "Test", "0912121212", userRole);
        createUserIfNotExists("柯欣怡", "user20@example.com", "Test", "0923232323", userRole);

        // 創建有商店的用戶
        createUserIfNotExistsWithShop("Cypher", "cypher@example.com", "Test", "0987654321", userRole, sellerRole); // 多個 role
        createUserIfNotExistsWithShop("陳冠宇", "user21@example.com", "Test", "0912345671", userRole, sellerRole);
        createUserIfNotExistsWithShop("林詩涵", "user22@example.com", "Test", "0923456789", userRole, sellerRole);
        createUserIfNotExistsWithShop("張家豪", "user23@example.com", "Test", "0934567890", userRole, sellerRole);
        createUserIfNotExistsWithShop("劉欣妤", "user24@example.com", "Test", "0945678901", userRole, sellerRole);
        createUserIfNotExistsWithShop("黃柏翰", "user25@example.com", "Test", "0956789012", userRole, sellerRole);
        createUserIfNotExistsWithShop("王宇翔", "user26@example.com", "Test", "0967890123", userRole, sellerRole);
        createUserIfNotExistsWithShop("何怡萱", "user27@example.com", "Test", "0978901234", userRole, sellerRole);
        createUserIfNotExistsWithShop("周志偉", "user28@example.com", "Test", "0989012345", userRole, sellerRole);
        createUserIfNotExistsWithShop("許家榮", "user29@example.com", "Test", "0990123456", userRole, sellerRole);
        createUserIfNotExistsWithShop("鄭惠雯", "user30@example.com", "Test", "0910234567", userRole, sellerRole);
        createUserIfNotExistsWithShop("邱柏宇", "user31@example.com", "Test", "0921345678", userRole, sellerRole);
        createUserIfNotExistsWithShop("簡芳瑜", "user32@example.com", "Test", "0932456789", userRole, sellerRole);
        createUserIfNotExistsWithShop("游承翰", "user33@example.com", "Test", "0943567890", userRole, sellerRole);
        createUserIfNotExistsWithShop("曾靜怡", "user34@example.com", "Test", "0954678901", userRole, sellerRole);
        createUserIfNotExistsWithShop("蘇建廷", "user35@example.com", "Test", "0965789012", userRole, sellerRole);
        createUserIfNotExistsWithShop("葉詠欣", "user36@example.com", "Test", "0976890123", userRole, sellerRole);
        createUserIfNotExistsWithShop("呂冠霖", "user37@example.com", "Test", "0987901234", userRole, sellerRole);
        createUserIfNotExistsWithShop("柯佳蓉", "user38@example.com", "Test", "0998012345", userRole, sellerRole);
        createUserIfNotExistsWithShop("馮柏軒", "user39@example.com", "Test", "0919123456", userRole, sellerRole);
        createUserIfNotExistsWithShop("魏欣彤", "user40@example.com", "Test", "0920234567", userRole, sellerRole);

        // ✅ 插入宅配與超商地址類型
        createAddressTypesIfNotExists();
    }

    private void createAddressTypesIfNotExists() {
        try {
             jdbcTemplate.update("IF NOT EXISTS (SELECT 1 FROM AddressTypeCorrespond WHERE name = '宅配') INSERT INTO AddressTypeCorrespond (name) VALUES ('宅配');");
             jdbcTemplate.update("IF NOT EXISTS (SELECT 1 FROM AddressTypeCorrespond WHERE name = '超商') INSERT INTO AddressTypeCorrespond (name) VALUES ('超商');");
             logger.info("✅ AddressType 已檢查/初始化完成");
         } catch (Exception e) {
             logger.error("初始化 AddressType 時發生錯誤", e);
         }
    }

    private void createUserIfNotExists(String username, String email, String password, String phone, Role... roles) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUserName(username);
            newUser.setPassword(passwordEncoder.encode(password)); // 加密密碼
            newUser.setPhone(phone);
            
            // 禁用自動設置日期的方法，防止@PrePersist和@PreUpdate方法運行
            // 手動設置隨機日期
            Date randomDate = generateRandomDate();
            newUser.setCreatedAt(randomDate);
            newUser.setUpdatedAt(randomDate);
            
            for (Role role : roles) {
                newUser.getRole().add(role);
            }
            userRepository.save(newUser);
            logger.info("User " + username + " 已建立");
        } else {
            logger.info("User " + username + " 已存在，跳過建立");
        }
    }

    private void createUserIfNotExistsWithShop(String username, String email, String password, String phone, Role... roles) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUserName(username);
            newUser.setPassword(passwordEncoder.encode(password)); // 加密密碼
            newUser.setPhone(phone);
            
            // 手動設置隨機日期
            Date randomDate = generateRandomDate();
            newUser.setCreatedAt(randomDate);
            newUser.setUpdatedAt(randomDate);
            
            for (Role role : roles) {
                newUser.getRole().add(role);
            }
            userRepository.save(newUser);

            // --- 新增：自動創建商店 ---
            createShopForUser(newUser); // 調用創建商店的方法
            // --- 新增部分結束 ---
            logger.info("User " + username + " (with shop) 已建立");
        } else {
            logger.info("User " + username + " 已存在，跳過建立");
        }
    }

    private void createShopForUser(User user) {
        // 檢查該用戶是否已經有商店
        Optional<Shop> op = productShopRepository.findByUser_UserId(user.getUserId()); // 假設 Repository 有此方法
        
        boolean shopExists;
        
        if(op.isPresent()) {
        	shopExists = true;
        }else {
        	shopExists = false;
		}
        
        if (shopExists) {
            logger.info("Shop for user " + user.getUserName() + " already exists. Skipping creation.");
            return;
        }

        Shop shop = new Shop();

        // --- 修改：設定商店名稱為 使用者名稱 + "的商店" ---
        shop.setShopName(user.getUserName() + "的商店");
        // --- 修改結束 ---

        // 從 shopCategories 陣列中隨機選擇一個類別
        String[] shopCategories = {
                "服飾", "3C 產品", "食品", "運動用品", "家居用品", "美妝保養",
                "母嬰用品", "寵物用品", "書籍文具", "汽機車周邊", "戶外休閒", "其他"
        };
        Random random = new Random();
        int categoryIndex = random.nextInt(shopCategories.length);
        shop.setShopCategory(shopCategories[categoryIndex]);

        // 設定商店與使用者關聯
        shop.setUser(user);

        // 設定商店為啟用狀態
        shop.setIsActive(true);

        // 其他商店屬性設定 (使用預設值或更合理的假資料)
        shop.setDescription("這是 " + user.getUserName() + " 的優質商店，提供各式" + shopCategories[categoryIndex] + "商品。");
        shop.setReturnRecipientName(user.getUserName()); // 可以用用戶名稱
        shop.setReturnRecipientPhone(user.getPhone()); // 可以用用戶電話
        shop.setReturnZipCode("813"); // 範例：左營區郵遞區號
        shop.setReturnCity("高雄市");
        shop.setReturnDistrict("左營區");
        shop.setReturnStreetEtc("預設退貨路 " + (random.nextInt(100) + 1) + " 號"); // 稍微隨機

        productShopRepository.save(shop);
        logger.info("Shop '" + shop.getShopName() + "' for user " + user.getUserName() + " 已建立");
    }


    // --- 以下輔助方法可以保留，如果其他地方可能用到，或者如果未來想改回隨機名稱 ---

    // 輔助方法：產生隨機中文名稱 (目前 createShopForUser 中未使用)
    private String generateRandomChineseName() {
        Random random = new Random();
        String[] surnames = {"王", "李", "張", "陳", "林", "黃", "吳", "劉", "蔡", "楊"}; // 常見姓氏
        String[] shopTypes = {"商店", "小舖", "專賣店", "旗艦店", "工坊", "商行"}; // 商店類型

        // 隨機姓氏 + 隨機名字 + 隨機商店類型
        String surname = surnames[random.nextInt(surnames.length)];
        String givenName = generateRandomChineseCharacters(2); // 產生 2 個中文字元的隨機名字
        String shopType = shopTypes[random.nextInt(shopTypes.length)];

        return surname + givenName + shopType;
    }

    // 輔助方法：產生指定數量的隨機中文字元 (目前 createShopForUser 中未使用)
    private String generateRandomChineseCharacters(int count) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            // 常用的中文字符範圍：0x4E00 (19968) - 0x9FA5 (40869)
            char randomChar = (char) (0x4E00 + random.nextInt(0x9FA5 - 0x4E00 + 1));
            sb.append(randomChar);
        }
        return sb.toString();
    }
    /**
     * 生成2025/3/16至2025/4/14之間的隨機日期
     */
    private Date generateRandomDate() {
        try {
            Calendar startCalendar = Calendar.getInstance();
            startCalendar.set(2025, Calendar.MARCH, 16, 0, 0, 0);  // 月份從0開始，所以3月是2
            startCalendar.set(Calendar.MILLISECOND, 0);
            
            Calendar endCalendar = Calendar.getInstance();
            endCalendar.set(2025, Calendar.APRIL, 14, 23, 59, 59);  // 4月是3
            endCalendar.set(Calendar.MILLISECOND, 999);
            
            long startTime = startCalendar.getTimeInMillis();
            long endTime = endCalendar.getTimeInMillis();
            long randomTime = startTime + (long) (Math.random() * (endTime - startTime));
            
            return new Date(randomTime);
        } catch (Exception e) {
            logger.error("生成隨機日期時發生錯誤: " + e.getMessage());
            return new Date(); // 發生錯誤時返回當前時間
        }
    }
}