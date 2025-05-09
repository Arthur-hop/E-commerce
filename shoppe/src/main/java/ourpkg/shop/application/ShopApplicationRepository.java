package ourpkg.shop.application;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ourpkg.shop.application.ShopApplication.ApplicationStatus;
import ourpkg.user_role_permission.user.User;

public interface ShopApplicationRepository extends JpaRepository<ShopApplication, Integer>{
	
	public Optional<ShopApplication> findByApplicationId(Integer id);

	public Optional<ShopApplication> findByUserAndStatus(User user, ApplicationStatus pending);

	public List<ShopApplication> findByStatus(ApplicationStatus pending);

	public List<ShopApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus rejected);
	
	long countByStatus(ApplicationStatus status);
	
	  // 根據狀態查詢並分頁
    Page<ShopApplication> findByStatus(ApplicationStatus status, Pageable pageable);
    
    // 搜尋（根據店名或描述）
    @Query("SELECT s FROM ShopApplication s WHERE " +
           "LOWER(s.shopName) LIKE LOWER(:keyword) OR " +
           "LOWER(s.description) LIKE LOWER(:keyword)")
    Page<ShopApplication> searchByKeyword(String keyword, Pageable pageable);
    
    // 根據關鍵字和狀態搜尋
    @Query("SELECT s FROM ShopApplication s WHERE " +
           "(LOWER(s.shopName) LIKE LOWER(:keyword) OR " +
           "LOWER(s.description) LIKE LOWER(:keyword)) AND " +
           "s.status = :status")
    Page<ShopApplication> searchByKeywordAndStatus(String keyword, ApplicationStatus status, Pageable pageable);
    
 // 根據用戶查詢並按創建時間降序排序
    List<ShopApplication> findByUserOrderByCreatedAtDesc(User user);
}