package com.metaverse.cau.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metaverse.cau.dto.ChatInfo;
import com.metaverse.cau.dto.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static Map<String, UserInfo> userInfoSessions = new ConcurrentHashMap<>();
    private static AtomicInteger playerCount = new AtomicInteger(0);

    private static AtomicInteger maxCount = new AtomicInteger(0);
    
    private static String crown = new String();
    
    public static void setCrown(String uid) {
    	crown = uid;
    }

    static List<Integer> findMissingElements(List<Integer> list1, List<Integer> list2) {
        List<Integer> missingElements = new ArrayList<>();
        for (Integer element : list1) {
            if (!list2.contains(element)) {
                missingElements.add(element);
            }
        }
        return missingElements;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 유저 정보 초기화
        UserInfo userInfo = new UserInfo();
        // 랜덤 색상 부여
        Random random = new Random();
        int r = random.nextInt(256); // 0에서 255 사이의 임의의 R 값 생성
        int g = random.nextInt(256); // 0에서 255 사이의 임의의 G 값 생성
        int b = random.nextInt(256); // 0에서 255 사이의 임의의 B 값 생성
        String hexColor = String.format("%02X%02X%02X", r, g, b);
        log.info("Random RGB Color: {}", hexColor);
        // 기존 로직
        if(maxCount.get()<playerCount.get()){
            maxCount=playerCount;
        }
        List<Integer> intList = new ArrayList<>();
        List<Integer> keyList = new ArrayList<>();
        int playerNumber = playerCount.incrementAndGet();
        for(int i =1;i<=maxCount.get();i++){
            intList.add(i);
        }
        String currentPlayers="";
        for(Map.Entry item : sessions.entrySet()){
            keyList.add(Integer.parseInt((String) item.getKey()));
        }
        List<Integer> missingElements = findMissingElements(intList,keyList);
        if(!missingElements.isEmpty()){
            playerNumber=missingElements.get(0);
        }
        sessions.put(String.valueOf(playerNumber), session);
        userInfo.setPlayerName(String.valueOf(playerNumber));
        userInfo.setHexColor(hexColor);
        userInfoSessions.put(String.valueOf(playerNumber), userInfo);
        String message = "connected";

        for(Map.Entry<String, UserInfo> item : userInfoSessions.entrySet()){
            currentPlayers += (String) item.getKey()+",";
            message+="," + item.getKey() + "," + item.getValue().getHexColor();
        }

        log.info(message);

        if (session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage("currentPlayerName,"+getPlayerName(session)));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (WebSocketSession clientSession : sessions.values()) {
            if (clientSession.isOpen()) {
                try {
                    synchronized (session) {
                        if(currentPlayers.length()>=1){
                            clientSession.sendMessage(new TextMessage("currentPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
                        }
                        clientSession.sendMessage(new TextMessage("currentPlayerCount,"+playerCount.get()));
                        clientSession.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    synchronized protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String playerName = getPlayerName(session);
        ByteBuffer receivedMessage = message.getPayload();
        Charset charset = Charset.forName("UTF-8"); // 사용할 문자 인코딩
        String decodedString = charset.decode(receivedMessage).toString();
        if(decodedString.startsWith("chat")){
            String[] parts = decodedString.split(",");
            String chat = parts[1];
            String nickname = parts[3];
            //Map<String, ChatInfo> chatInfoMap = new HashMap<>();
            ChatInfo chatInfo = new ChatInfo(nickname,chat);
            //chatInfoMap.put(playerName, chatInfo);
            // 모든 연결된 클라이언트에게 메시지를 브로드캐스트합니다.
            for (WebSocketSession clientSession : sessions.values()) {
                if (clientSession.isOpen()) {
                    try {
                        synchronized (session) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            String jsonResponse = objectMapper.writeValueAsString(chatInfo);
                            log.info("jsonResponse : " + jsonResponse);
                            clientSession.sendMessage(new TextMessage("chatInfo+"+jsonResponse));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        else if(decodedString.startsWith("nickname")){
            String[] parts = decodedString.split(",");
           // String responseMessage = "userInfo," + playerName+","+"nickname,"+parts[1]+",avatar,"+parts[3];
            UserInfo userInfo = userInfoSessions.get(playerName);
            userInfo.setNickname(parts[1]);
            userInfo.setAvatar(parts[3]);

            // Map을 List<Map>으로 변환
            List<Map<String, UserInfo>> jsonSessionArray = new ArrayList<>();
            for(String key : userInfoSessions.keySet()){
                Map<String, UserInfo> jsonSession = new HashMap<>();
                jsonSession.put(key, userInfoSessions.get(key));
                jsonSessionArray.add(jsonSession);
            }

            // 모든 연결된 클라이언트에게 메시지를 브로드캐스트합니다.
            for (WebSocketSession clientSession : sessions.values()) {
                if (clientSession.isOpen()) {
                    try {
                        synchronized (session) {
                            ObjectMapper objectMapper = new ObjectMapper();
                            String jsonResponse = objectMapper.writeValueAsString(jsonSessionArray);
                            log.info("jsonResponse : " + jsonResponse);
                            clientSession.sendMessage(new TextMessage("userInfo+"+jsonResponse));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }else{
            String responseMessage = "player," + playerName + "," + decodedString;
            byte[] byteArray = responseMessage.getBytes(charset);
            // 모든 연결된 클라이언트에게 메시지를 브로드캐스트합니다.
            for (WebSocketSession clientSession : sessions.values()) {
                if (clientSession.isOpen()) {
                    try {
                        synchronized (session) {
                            clientSession.sendMessage(new TextMessage(byteArray));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String playerName = getPlayerName(session);
        sessions.remove(playerName);
        userInfoSessions.remove(playerName);
        playerCount.decrementAndGet();
        log.info("{} has disconnected.", playerName);
        String message = "disconnected," + playerName;
        String currentPlayers="";
        for(Map.Entry item : sessions.entrySet()){
            currentPlayers += (String) item.getKey()+",";
        }
        for (WebSocketSession clientSession : sessions.values()) {
            if (clientSession.isOpen()) {
                try {
                    synchronized (session) {
                        clientSession.sendMessage(new TextMessage("currentPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
                        clientSession.sendMessage(new TextMessage("currentPlayerCount,"+playerCount.get()));
                        clientSession.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String getPlayerName(WebSocketSession session) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
