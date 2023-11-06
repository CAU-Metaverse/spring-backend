package com.metaverse.cau.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class SugangWebSocketHandler extends TextWebSocketHandler{
	
	private static Map<String, WebSocketSession> gameSessions = new ConcurrentHashMap<>(); // 게임방
	private static Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); // 대기실
	private static AtomicInteger playerCount = new AtomicInteger(0);
	private static AtomicInteger maxCount = new AtomicInteger(0);
	private static AtomicInteger sugangPlayerCount = new AtomicInteger(0);
	
	
	private static int secondsToPlay;
	private static Timer timer; // 30초 카운트를 위한 카운터
	private static TimerTask task;
	private static AtomicInteger seatsLeft = new AtomicInteger(0); //남은좌석
	private static int playFlag =0; //0:게임대기중   1:게임중
	private static int nextRoundSeconds;
	
	static void nextGameTimer(int flag) { //flag =1 : 현 라운드 끝. 다음 라운드 대기중 // flag =0 : 현재 라운드 진행중
		Timer nextTimer = new Timer();
		nextRoundSeconds = 30;
		
		
		TimerTask nextTask = new TimerTask() {
			@Override
			public void run() {
				if(nextRoundSeconds > 0) {
					nextRoundSeconds--;
					String secToPlay = Integer.toString(nextRoundSeconds);
					
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			if(flag ==1)//다음 라운드 대기중
			    				toSendSession.sendMessage(new TextMessage("Battle:Next:timeLeft:"+secToPlay)); // 시작남은시간
			    			if(flag == 0)// 현재 라운드 남은시간
			    				toSendSession.sendMessage(new TextMessage("Battle:This:timeLeft:"+secToPlay)); // 시작남은시간
			    		}catch(Exception e) {
			    			e.printStackTrace();
			    		}
			    	}
				}else {
					if(flag==1) // 다음라운드 대기가 끝나면 
						playFlag =0; //라운드 진행중 flag를 0으로 만들기
					nextTimer.cancel();
					
				}
				
			}
		};
		nextTimer.schedule(nextTask,0,1000);
		
	}

	//오류발생하진않으려나
	//동기화는 어따붙여야하능교
	static void playGameTimer() {// 30초부터 다시 시작하는 타이머
		if(timer != null) {// 타이머가 작동중이면 취소한다.
			timer.cancel();
			timer = null;
			
		}
		timer = new Timer();
		secondsToPlay = 30;
		
		
		task = new TimerTask() {
			@Override
			public void run() {
				if(secondsToPlay > 0) {
					secondsToPlay--;
					String secToPlay = Integer.toString(secondsToPlay);
					
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			
			    			//현재 경쟁 참여인원
			    			toSendSession.sendMessage(new TextMessage("Battle:timeLeft:"+secToPlay)); // 시작남은시간
			    			toSendSession.sendMessage(new TextMessage("Battle:Users:"+sugangPlayerCount.toString())); // 경쟁참여인원
			    			toSendSession.sendMessage(new TextMessage("Battle:Seats:"+seatsLeft.toString())); // 수강가능인원
			    			
			    		}catch(Exception e) {
			    			e.printStackTrace();
			    		}
			    	}
				}else {
					playSugangBattle();
					timer.cancel();
					timer = null;
				}
				
			}
		};
		timer.schedule(task,0,1000);
		
		//나가는건 타이머 재설정 필요 없이, 0
	}
	
	static void playSugangBattle() {//타이머 종료. 시작.
		for(String key : gameSessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
    		WebSocketSession toSendSession = gameSessions.get(key);
    		try {

    			toSendSession.sendMessage(new TextMessage("Battle:enableBattleButton"));
    			playFlag =1; //게임시작
    			
    			nextGameTimer(0); //현재 라운드 진행중
    			//결과는?
    			nextGameTimer(1); //라운드 끝. 대기시간
    			gameSessions.clear();
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
	}
	
	static int registerSugang() {//신청버튼
		int result = seatsLeft.decrementAndGet();
		if(result >=0) { //1자리 남았을때 result는 0이니까 true. 
			
			return result;
		}
		
		return result;
		
		
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

    //발판을 밟은상태 : figma - 게임타이틀
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if(maxCount.get()<playerCount.get()){
            maxCount=playerCount;
        }
        List<Integer> intList = new ArrayList<>();
        List<Integer> keyList = new ArrayList<>();
        int playerNumber = playerCount.incrementAndGet();
        for(int i =1;i<=maxCount.get();i++){//1번부터
            intList.add(i);
        }
        String currentPlayers="";
        for(Map.Entry item : sessions.entrySet()){
            keyList.add(Integer.parseInt((String) item.getKey()));
        }
        
        ////////////
        List<Integer> missingElements = findMissingElements(intList,keyList);
        if(!missingElements.isEmpty()){
            playerNumber=missingElements.get(0); // 번호 돌려쓰기
        }      
        
        sessions.put(String.valueOf(playerNumber), session);
        String message = "joined Sugang and waiting," + playerNumber;

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
                            clientSession.sendMessage(new TextMessage("currentSugangPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
                        }
                        // 현재 수강신청 발판 밟은인원
                        clientSession.sendMessage(new TextMessage("currentPlayerCount,"+playerCount.get()));
                        clientSession.sendMessage(new TextMessage(message)); // 새 유저 들어옴 알림.
                        clientSession.sendMessage(new TextMessage("Re-counting 30 seconds."));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        
    }

    
    @Override
    synchronized protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    	String playerName = getPlayerName(session);
    	String msg = message.getPayload();
    	
    	// 메세지가 참여일경우
        //새로 접속시에 timer를 다시 30부터 count하게됨.
    	//if( msg == "join_sugangBattle" || msg =="disconnected_sugangBattle")
    	
    	int playUsers =0;
		if(sugangPlayerCount.get() != 0) {
			playUsers = sugangPlayerCount.get()/2;
		}
		seatsLeft.set(playUsers);
		
    	if( msg.equals("join_sugangBattle") && playFlag ==0) {// 배틀에 참여
    		if(sugangPlayerCount.get()>=1)
    		playGameTimer();
    		sugangPlayerCount.incrementAndGet();
    		gameSessions.put(String.valueOf(playerName),session);


    	}else if( msg.equals("disconnected_sugangBattle") && playFlag ==0) {// 배틀 나가기
    		
    		if(sugangPlayerCount.decrementAndGet() <= 1) { // 플레이 할 사람이 없으면 카운트다운 취소
            	
            	timer.cancel();
            	timer = null;
            }
    		gameSessions.remove(playerName);
    		
    	}
    	
    	// 근데 신청버튼 여러번눌러서 여러번 날라오는건 처리 어케하지. ★
    	if(msg.equals("register_sugangBattle")  && playFlag ==1) {// 신청버튼 누름.
    		int result = registerSugang();
    		
    		try {
    			// 본인 결과 알려줌
    			session.sendMessage(new TextMessage("Battle:result:Mine:"+Integer.toString(result))); // 경쟁참여인원
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    		
    		for(String key : gameSessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
	    		WebSocketSession toSendSession = gameSessions.get(key);
	    		try {
	    			//결과값이 높을수록 순위가 높음
	    			// Battle:result:0~n 수강신청 성공        Battle:result:{-n}~{-1}  수강신청 실패
	    			if(playerName != getPlayerName(toSendSession))
	    				toSendSession.sendMessage(new TextMessage("Battle:result:FromOthers:"+playerName+":"+Integer.toString(result))); // 경쟁참여인원
	    			
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
    		
    	}
    	else {
    		//do nothing
    		try {
    			// 본인 결과 알려줌
    			String[] helloStrArr = msg.split("");
    			int[] resultIntArr = new int[helloStrArr.length];
    			
    			for (int i = 0; i < helloStrArr.length; i++) {
    			    int helloItemNum = helloStrArr[i].charAt(0);
    			    resultIntArr[i] = helloItemNum;
    			}
    			
    			session.sendMessage(new TextMessage(Arrays.toString(resultIntArr))); // 경쟁참여인원
    			
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
    	
    }
    
    

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String playerName = getPlayerName(session);
        sessions.remove(playerName);
        if(gameSessions.containsKey(playerName))
        	gameSessions.remove(playerName);

        
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
//sendMessage 수정, 오류테스트 필요