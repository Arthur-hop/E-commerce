package ourpkg.user_role_permission.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ourpkg.address.UserAddress;
import ourpkg.cart.Cart;
import ourpkg.customerService.entity.ChatMessageEntity;
import ourpkg.customerService.entity.ChatRoomEntity;
import ourpkg.order.Order;
import ourpkg.shop.Shop;
import ourpkg.user_role_permission.Permission;
import ourpkg.user_role_permission.Role;

@Entity
@Table(name = "[User]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements UserDetails {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	// 修改User類中的getAuthorities方法
	@Override
	@JsonIgnore
	public Collection<? extends GrantedAuthority> getAuthorities() {
	    // 從Role集合中創建標準的Spring Security權限
	    List<GrantedAuthority> authorities = new ArrayList<>();
	    
	    // 添加基於角色的權限
	    for (Role role : this.role) {
	        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleName()));
	    }
	    
	    // 添加單獨的權限
	    List<String> permissions = getPermissions();
	    for (String permission : permissions) {
	        authorities.add(new SimpleGrantedAuthority(permission));
	    }
	    
	    return authorities;
	}

	@Override
	public String getUsername() {
		return this.userName; // **確保返回 `userName` 而不是 `null`**
	}

	public String getUserName() {
		return this.userName;
	}

	public List<String> getPermissions() {
		return this.role.stream().flatMap(role -> role.getPermission().stream()) // 取得該角色的所有權限
				.map(Permission::getPermissionName) // 只取權限名稱
				.distinct() // 避免重複
				.collect(Collectors.toList());
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "[user_id]")
	private Integer userId;

	@Column(name = "[user_name]", nullable = false)
//	@JsonBackReference
	private String userName;

	@Column(name = "[email]")
	private String email;

	@Column(name = "[password]")
//	@JsonIgnore
	private String password;

	@Column(name = "[phone]")
	private String phone;

//	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC+8") // 前後端分離
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") // MVC
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[created_at]", updatable = false) // 只在新增時設定，之後不允許更新
	private Date createdAt;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "[updated_at]")
	private Date updatedAt;



	// @JsonBackReference
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonManagedReference("user-shop")
	private Shop shop;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<Cart> cart;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonBackReference("user-address")
	// @JsonBackReference
	private List<UserAddress> userAddress;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//	@JsonIgnore
	@JsonBackReference("user-order")
	private List<Order> order;

	@ManyToMany()
	@JoinTable(name = "[User_Role]", joinColumns = @JoinColumn(name = "[user_id]"), inverseJoinColumns = @JoinColumn(name = "[role_id]"))
	@JsonIgnore
	private Set<Role> role = new HashSet<>();// set避免重複

	@PrePersist // 新增一筆 Entity 到資料庫 之前調用
	protected void onCreate() {
	    // 只在尚未設置日期時才設置預設值
	    if (this.createdAt == null) {
	        this.createdAt = new Date();
	    }
	    if (this.updatedAt == null) {
	        this.updatedAt = new Date();
	    }
	}

	@PreUpdate // 更新一筆 Entity 到資料庫 之前調用
	protected void onUpdate() {
	    if (this.updatedAt == null) this.updatedAt = new Date();
	}

	// 一個用戶可以有多個聊天室（作為買家或賣家）
	@JsonManagedReference("buyerReference")
	@OneToMany(mappedBy = "buyer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<ChatRoomEntity> buyerRooms = new ArrayList<>();

	
	
	
	@Column(name="[google_id]")
	private String googleId;

	// 一個用戶可以有多條訊息（由 sender 發送）
	@OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonBackReference("user-messages") // 建議也加上對應的 JSON 管理
	private List<ChatMessageEntity> messages = new ArrayList<>();

	
    public enum UserStatus {
        ACTIVE,  // 可以登入與使用功能
        BANNED   // 被封鎖，無法登入
    }

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Override
    public boolean isEnabled() {
        return this.status == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true; // 帳號永遠不過期
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 密碼永遠不過期
    }
    
    @Column(name = "[profile_photo_url]")
    private String profilePhotoUrl;
	
}
