package ourpkg.customerService.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import ourpkg.user_role_permission.user.User;

@Configuration
@EnableWebSocketMessageBroker // 啟用基於 STOMP 的 WebSocket 消息代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	// 聲明需要注入的 AuthChannelInterceptor Bean
    private final AuthChannelInterceptor authChannelInterceptor;

    // 使用建構函數注入 AuthChannelInterceptor Bean
    // Spring 會自動找到標記為 @Component 的 AuthChannelInterceptor 實例
    @Autowired
    public WebSocketConfig(AuthChannelInterceptor authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
    }

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 設定 WebSocket 端點，前端需連接此路徑
		registry.addEndpoint("/ws") 
				.setAllowedOrigins("http://localhost:5173") // 允許前端域名
				.withSockJS(); // 啟用 SockJS 支持
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// 設定消息代理（可選，但建議用於聊天室）
		registry.enableSimpleBroker("/topic", "/queue"); // 訂閱前綴（例如：/topic/messages）
		registry.setApplicationDestinationPrefixes("/app"); // 服務端接收前綴（例如：/app/send）
		registry.setUserDestinationPrefix("/user");
	}

	@Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 註冊您通過建構函數注入的 AuthChannelInterceptor Bean
        registration.interceptors(authChannelInterceptor);
    }
}