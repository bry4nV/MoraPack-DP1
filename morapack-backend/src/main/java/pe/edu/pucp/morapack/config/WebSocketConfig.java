package pe.edu.pucp.morapack.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP configuration.
 *
 * NOTE: This configuration is conditional on the property
 * `morapack.websocket.enabled=true`. Set that property to true to enable
 * the STOMP endpoint; keep it false (default) to disable the connection while
 * preserving the code.
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(prefix = "morapack.websocket", name = "enabled", havingValue = "true", matchIfMissing = false)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }
}
