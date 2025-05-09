package ourpkg.user_role_permission.user;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmail(String string);

	Optional<User> findByUserName(String userName);

	Optional<User> findByPhone(String phone);

	Optional<User> findByUserId(Integer userId);

	@Query("SELECT u FROM User u WHERE u.userId = :userId")
	User findByUserId2(@Param("userId") Integer userId);

	Optional<User> deleteByUserId(Integer userId);

	@EntityGraph(attributePaths = { "shop", "role" })
	Page<User> findByRoleRoleName(String roleName, Pageable pageable);

	@EntityGraph(attributePaths = { "shop", "role" })
	Page<User> findByRoleRoleNameAndUserNameContaining(String roleName, String userName, Pageable pageable);

	boolean existsByUserId(Integer userId);

	User findByGoogleId(String googleId);

	// 更新查詢，同時載入Role和對應的Permission集合
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.role r LEFT JOIN FETCH r.permission WHERE u.userName = :userName")
    Optional<User> findByUserNameWithRoles(@Param("userName") String userName);
    
    boolean existsByEmail(String email);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.profilePhotoUrl = :photoUrl WHERE u.userId = :userId")
    void updateUserProfilePhoto(@Param("userId") Integer userId, @Param("photoUrl") String photoUrl);

	// ==========================================by chien
	/**
	 * 找出所有管理員用戶的ID
	 */
	@Query(value = "SELECT u.user_id FROM [User] u " + "JOIN User_Role ur ON u.user_id = ur.user_id "
			+ "JOIN Role r ON ur.role_id = r.id "
			+ "WHERE r.role_name = 'ADMIN' OR r.role_name = 'SUPER_ADMIN'", nativeQuery = true)
	List<Integer> findAdminUserIds();
	// ==========================================by chien

	// 按日期範圍統計新用戶數量 - 這個可以保持不變
	@Query(value = "SELECT CONVERT(DATE, u.created_at) as date, COUNT(u.user_id) as count FROM [User] u " +
	              "WHERE u.created_at >= :startDate AND u.created_at <= :endDate " +
	              "GROUP BY CONVERT(DATE, u.created_at) ORDER BY date", nativeQuery = true)
	List<Object[]> countNewUsersByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);

	// 獲取特定時間段內的新用戶總數 - 修改返回類型為 Integer 而不是 Long
	@Query(value = "SELECT COUNT(u.user_id) FROM [User] u WHERE u.created_at >= :startDate AND u.created_at <= :endDate", 
	       nativeQuery = true)
	Integer countTotalNewUsersByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
	}