package com.metaverse.cau.configuration;

import com.metaverse.cau.interfaces.MyWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(), "/my-websocket")
                .setAllowedOrigins("*"); // WebSocket 핸들러를 "/my-websocket" 경로로 등록
    }
}