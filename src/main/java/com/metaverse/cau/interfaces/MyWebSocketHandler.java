package com.metaverse.cau.interfaces;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static AtomicInteger playerCount = new AtomicInteger(0);

    private static AtomicInteger maxCount = new AtomicInteger(0);

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
        String message = "connected," + playerNumber;

        for(Map.Entry item : sessions.entrySet()){
            currentPlayers += (String) item.getKey()+",";
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

        String responseMessage = "player," + playerName + "," + decodedString;
        log.info(responseMessage);
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String playerName = getPlayerName(session);
        sessions.remove(playerName);
        playerCount.decrementAndGet();
        System.out.println(playerName + " disconnected.");

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
