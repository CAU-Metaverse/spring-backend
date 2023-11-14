package com.metaverse.cau.configuration;

import com.metaverse.cau.service.JsonUtility;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.metaverse.cau.interfaces.MyWebSocketHandler;
import com.metaverse.cau.interfaces.SugangWebSocketHandler;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final JsonUtility jsonUtility;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(jsonUtility), "/my-websocket")
                .setAllowedOrigins("*"); // WebSocket 핸들러를 "/my-websocket" 경로로 등록
        registry.addHandler(new SugangWebSocketHandler(), "/sugang-websocket")
        		.setAllowedOrigins("*");
    }
}