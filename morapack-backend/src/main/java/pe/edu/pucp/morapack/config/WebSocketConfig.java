package pe.edu.pucp.morapack.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket/STOMP configuration for real-time simulation updates.
 * 
 * TWO TYPES OF COMMUNICATION:
 * 
 * 1. SIMULATIONS (WEEKLY/COLLAPSE):
 *    - Per-user sessions: /user/queue/simulation
 *    - Each user controls their own simulation
 *    - Controls: Start, Pause, Resume, Stop, Reset, Speed
 *    - Data from CSV files
 *    - Results NOT saved to database
 * 
 * 2. DAILY OPERATIONS (Live):
 *    - Broadcast to all: /topic/daily-operations
 *    - Single shared instance
 *    - No user controls (automatic)
 *    - Data from database
 *    - Results saved to database
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        // /topic for broadcast (DAILY operations - everyone sees the same)
        // /user for individual messages (SIMULATIONS - per-user)
        config.enableSimpleBroker("/topic", "/user");
        
        // Prefix for messages FROM client TO server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register "/ws" endpoint for WebSocket handshake
        // Clients connect to: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS - adjust for production
                .withSockJS();  // Fallback to SockJS if WebSocket not available
    }
}
