package ourpkg.user_role_permission.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class UserDetailsServiceImpl implements UserDetailsService{

	private final UserRepository userRepository; // 注入您的 UserRepository

    @Autowired
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
	
	
    @Override
    @Transactional(readOnly = true) 
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用 @Transactional 確保整個方法在一個事務中執行，
        // 這樣當方法返回時，Hibernate 會話仍然保持開啟狀態
        
        // 使用 fetchJoin 查詢來解決 N+1 問題，並預先加載角色集合
        User user = userRepository.findByUserNameWithRoles(username)
                .orElseThrow(() -> 
                        new UsernameNotFoundException("找不到用戶: " + username));
        
        return user;
    }
}
