package ourpkg.shop;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerShopRepository extends JpaRepository<Shop, Integer> {
	Optional<Shop> findByUserUserId(Integer userId);

	@Query("SELECT s FROM Shop s WHERE s.shopId = :shopId")
	Shop findByShopId2(@Param("shopId") Integer shopId);

	Optional<Shop> findByShopId(Integer shopId);

	@Query("SELECT s FROM Shop s LEFT JOIN FETCH s.user")
	List<Shop> findAllWithUser();

	// 新增這個方法來獲取特定 userId 的所有商店列表
		List<Shop> findByUser_UserId(Integer userId);
		
		 // *** 新增：用於分頁和搜尋的 JPQL 查詢 ***
	    @Query(value = "SELECT s FROM Shop s JOIN FETCH s.user u " + // JOIN FETCH 同時載入 User
	                   "WHERE (:searchText IS NULL OR :searchText = '' " + // 如果 searchText 為空或 null
	                   "       OR s.shopName LIKE %:searchText% " +         // 則比對 shopName
	                   "       OR u.userName LIKE %:searchText%)", // 或比對 userName
	           countQuery = "SELECT COUNT(s) FROM Shop s JOIN s.user u " + // 對應的計數查詢
	                        "WHERE (:searchText IS NULL OR :searchText = '' " +
	                        "       OR s.shopName LIKE %:searchText% " +
	                        "       OR u.userName LIKE %:searchText%)")
	    Page<Shop> findBySearchText(@Param("searchText") String searchText, Pageable pageable);

//	    // 如果只需要 DTO，可以直接查詢 DTO (更高效)
//	     @Query(value = "SELECT new ourpkg.shop.ShopInfoDTO(s.shopId, s.shopName, u.userId, s.shopCategory, u.userName) " + // 直接建立 DTO
//	                   "FROM Shop s JOIN s.user u " +
//	                   "WHERE (:searchText IS NULL OR :searchText = '' " +
//	                   "       OR s.shopName LIKE %:searchText% " +
//	                   "       OR u.userName LIKE %:searchText%)",
//	           countQuery = "SELECT COUNT(s) FROM Shop s JOIN s.user u " +
//	                        "WHERE (:searchText IS NULL OR :searchText = '' " +
//	                        "       OR s.shopName LIKE %:searchText% " +
//	                        "       OR u.userName LIKE %:searchText%)")
//	    Page<ShopInfoDTO> findDTOBySearchText(@Param("searchText") String searchText, Pageable pageable); // 回傳 Page<ShopInfoDTO>
	    @Query(value = "SELECT new ourpkg.shop.ShopInfoDTO(s.shopId, s.shopName, u.userId, s.shopCategory, u.userName) " + 
	    		"FROM Shop s JOIN s.user u " +
	    		"WHERE s.isActive = true AND (:searchText IS NULL OR :searchText = '' " +
	    		" OR s.shopName LIKE %:searchText% " +
	    		" OR u.userName LIKE %:searchText%)",
	    		countQuery = "SELECT COUNT(s) FROM Shop s JOIN s.user u " +
	    		"WHERE s.isActive = true AND (:searchText IS NULL OR :searchText = '' " +
	    		" OR s.shopName LIKE %:searchText% " +
	    		" OR u.userName LIKE %:searchText%)")
	    		Page<ShopInfoDTO> findDTOBySearchText(@Param("searchText") String searchText, Pageable pageable);
	    
}
