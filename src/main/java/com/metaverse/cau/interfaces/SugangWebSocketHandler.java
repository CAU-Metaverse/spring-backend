package com.metaverse.cau.interfaces;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class SugangWebSocketHandler extends TextWebSocketHandler{
	
	private static Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); // 대기실
	private static Map<String, WebSocketSession> gameSessions = new ConcurrentHashMap<>(); // 게임방 (Go버튼)
	
	private static Map<String, String> realUID = new ConcurrentHashMap<>(); // 유니티 상의 uid
	private static Map<String, String> nickname = new ConcurrentHashMap<>(); // 닉네임
	private static Map<String, String> character = new ConcurrentHashMap<>(); // 캐릭터
	private static AtomicInteger playerCount = new AtomicInteger(0); // 레이스 접속인원 수
	private static AtomicInteger maxCount = new AtomicInteger(0);
	private static AtomicInteger sugangPlayerCount = new AtomicInteger(0); //Go 버튼 누른사람수
	
	
	private static JSONObject gameResult = new JSONObject(); // 이거랑
	private static Map<String, WebSocketSession> notClickedUsers = new ConcurrentHashMap<>(); // 신청버튼 안누른 유저 걸러내기
	
	private static int secondsToPlay;
	private static Timer timer; // 30초 카운트를 위한 카운터
	private static TimerTask task;
	private static AtomicInteger seatsLeft = new AtomicInteger(0); //남은좌석
	private static int playFlag =0; //0:게임대기중   1:게임중
	private static int nextRoundSeconds;
	private static int thisRoundSeconds;
	
	static void getResult() {
    	
    	int myrank = 1000;
    	for (Map.Entry<String, WebSocketSession> entry : notClickedUsers.entrySet()) {
    		//실패한 유저들은 1000부터 +1해서
    		String uid = entry.getKey();;
        	JSONArray loserData = new JSONArray();
        	JSONObject loserDataField = new JSONObject();	
        	loserDataField.put("UID",realUID.get(uid));
        	loserDataField.put("NICKNAME",nickname.get(uid));
        	loserDataField.put("CHARACTER",character.get(uid));
        	loserData.add(loserDataField);
        	gameResult.put(myrank,loserData);
        	myrank++;
            
            
        }
    	
    	try {
    		String data = gameResult.toJSONString();
        	JSONParser parser = new JSONParser();
			JSONObject crownParse = (JSONObject)parser.parse(data);
			JSONArray crownArr = (JSONArray)crownParse.get("0");
			JSONObject crownPeel = (JSONObject)crownArr.get(0);
			String crown = (String)crownPeel.get("UID");
			MyWebSocketHandler.setCrown(crown);
			System.out.println("crown:"+crown);
		} catch (Exception e) {
			// 
			e.printStackTrace();
			
		}
    	// 현재 라운드가 다 끝나면 MyWebSocketHandler로 넘겨줘야함.
        
    	
		
		
		for(String key : sessions.keySet()) {
			
    		WebSocketSession toSendSession = sessions.get(key);
    		try {
    			synchronized(toSendSession) {
    				toSendSession.sendMessage(new TextMessage("raceresult:"+gameResult.toJSONString()));
    			}
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}

		gameResult.clear();
		sugangPlayerCount.set(0);
		notClickedUsers.clear();
		gameSessions.clear();
	}
	
	static void nextGameTimer() { 
		Timer nextTimer = new Timer();
		nextRoundSeconds = 15; //
		getResult();
		
		
		TimerTask nextTask = new TimerTask() {
			@Override
			public void run() {
				if(nextRoundSeconds > 0) {
					nextRoundSeconds--;
					String secToPlay = Integer.toString(nextRoundSeconds);
					
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			JSONObject raceRound = new JSONObject();
			    			//다음 라운드 대기중 : 결과창 볼수있는 시간.
			    			raceRound.put("RACE_NEXT_TIME_LEFT",secToPlay);			    				
			    				//toSendSession.sendMessage(new TextMessage("Battle:Next:timeLeft:"+secToPlay)); // 테스트용
			    			
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(raceRound.toJSONString()));
			    			}
			    		}catch(Exception e) {
			    			e.printStackTrace();
			    		}
			    	}
				}else {

					playFlag =0; 
					nextTimer.cancel();
					
				}
			}
		};
		nextTimer.schedule(nextTask,0,1000);
		
	}

	
	static void thisGameTimer() { //flag =1 : 현 라운드 끝. 다음 라운드 대기중 // flag =0 : 현재 라운드 진행중
		Timer nextTimer = new Timer();
		thisRoundSeconds = 15; //
		
		
		TimerTask nextTask = new TimerTask() {
			@Override
			public void run() {
				if(thisRoundSeconds > 0) { 
					thisRoundSeconds--;
					String secToPlay = Integer.toString(thisRoundSeconds);
					
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			JSONObject raceRound = new JSONObject();

			    			// 현재 라운드 남은시간 : 신청버튼 누를 수 있는 시간
			    			
			    			raceRound.put("RACE_CURRENT_TIME_LEFT",secToPlay);
			    				//toSendSession.sendMessage(new TextMessage("Battle:This:timeLeft:"+secToPlay));// 테스트용
			    			
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(raceRound.toJSONString()));
			    			}
			    		}catch(Exception e) {
			    			e.printStackTrace();
			    		}
			    	}
				}else {
					nextGameTimer();

					nextTimer.cancel();
					
				}
			}
		};
		nextTimer.schedule(nextTask,0,1000);
		
	}

	static void playGameTimer() {// n초부터 다시 시작하는 타이머			1
		if(timer != null) {// 타이머가 작동중이면 취소한다.
			timer.cancel();
			timer = null;
			
		}
		task = null;
		timer = new Timer();
		secondsToPlay = 15;
		
		
		task = new TimerTask() {
			@Override
			public void run() {
				if(secondsToPlay > 0) {
					secondsToPlay--;
					String secToPlay = Integer.toString(secondsToPlay);
					JSONObject currentRaceWait = new JSONObject();
					currentRaceWait.put("RACE_WAIT_TIME_LEFT",secToPlay);
					currentRaceWait.put("RACE_WAIT_USERS_NUM",sugangPlayerCount.toString());
					currentRaceWait.put("RACE_WAIT_SEATS_LEFT",seatsLeft.toString());
					currentRaceWait.put("RACE_WAIT_STATE_MESSAGE","RE-COUNTING_30_SECS");
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			
			    			//현재 경쟁 참여인원
//			    			toSendSession.sendMessage(new TextMessage("Battle:timeLeft:"+secToPlay)); // 시작남은시간
//			    			toSendSession.sendMessage(new TextMessage("Battle:Users:"+sugangPlayerCount.toString())); // 경쟁참여인원
//			    			toSendSession.sendMessage(new TextMessage("Battle:Seats:"+seatsLeft.toString())); // 수강가능인원
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(currentRaceWait.toJSONString()));
			    			}
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
	
	static void playSugangBattle() {//타이머 종료. 시작.							2
		JSONObject raceRound = new JSONObject();
		raceRound.put("RACE_CURRENT_STATE","START");
		String btnEnable = raceRound.toJSONString();
		for(String key : gameSessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
    		WebSocketSession toSendSession = gameSessions.get(key);
    		try {
    			toSendSession.sendMessage(new TextMessage(btnEnable));
    			//toSendSession.sendMessage(new TextMessage("Battle:enableBattleButton"));
    			playFlag =1; //게임시작

    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
		notClickedUsers.putAll(gameSessions);
		thisGameTimer(); //현재 라운드 진행중

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
        
		JSONObject newUserConnected = new JSONObject();
		newUserConnected.put("RACE_EVERY_PLAYER_COUNT",playerCount.toString()); //현재 레이스 접속한 모든인원
        newUserConnected.put("RACE_JOINED_PLAYER_COUNT",sugangPlayerCount.toString()); //레이스에 참여중인 모든인원
        newUserConnected.put("RACE_SEATS",seatsLeft.get()); // 좌석수
        newUserConnected.put("RACE_WAIT_NEW",String.valueOf(playerNumber)); //새로 접속한 인원
        
        for (WebSocketSession clientSession : sessions.values()) {
            if (clientSession.isOpen()) {
                try {
                    synchronized (session) {
                        if(currentPlayers.length()>=1){
                            clientSession.sendMessage(new TextMessage("currentSugangPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
                        }
                        // 현재 수강신청 발판 밟은인원
                        //clientSession.sendMessage(new TextMessage("currentPlayerCount,"+playerCount.get()));
                        //clientSession.sendMessage(new TextMessage(message)); // 새 유저 들어옴 알림.
                        
                        
                        
                        clientSession.sendMessage(new TextMessage(newUserConnected.toJSONString()));
                        
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
    	int playUsers =0;
 
    	
		
    	if( msg.contains("join_sugangBattle") && playFlag ==0) {// 배틀에 참여
    		//join_sugangBattle :userId :userCharacter :userNickname
    		
    		sugangPlayerCount.incrementAndGet();
    		if(sugangPlayerCount.get() != 0) {
    			playUsers = sugangPlayerCount.get()/2;
    		}
    		seatsLeft.set(playUsers);
    		
    		if(sugangPlayerCount.get()>1)
    			playGameTimer();
    		String[] segments = msg.split(":");
    		gameSessions.put(String.valueOf(playerName),session);
    		realUID.put(playerName,segments[1]);
    		character.put(playerName,segments[2]);
    		nickname.put(playerName,segments[3]);
    		
    		JSONObject joinRace = new JSONObject();
			joinRace.put("RACE_WAIT_USER_ADD",playerName);
			
			for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
	    		WebSocketSession toSendSession = sessions.get(key);
	    		try {
	    			synchronized(toSendSession) {
//	    			toSendSession.sendMessage(new TextMessage("sugangPlayerCount:"+sugangPlayerCount.get()));
//	    			toSendSession.sendMessage(new TextMessage("sugangPlayerCount/2:"+sugangPlayerCount.get()/2));
//	    			toSendSession.sendMessage(new TextMessage("playusers:"+playUsers));
//	    			toSendSession.sendMessage(new TextMessage("seatsLeft:"+seatsLeft.get()));
	    			toSendSession.sendMessage(new TextMessage(joinRace.toJSONString()));
	    			}
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
    		


    	}else if( msg.equals("disconnected_sugangBattle") && playFlag ==0) {// 배틀 나가기
    		
    		
    		if(sugangPlayerCount.decrementAndGet() <= 1) { // 플레이 할 사람이 없으면 카운트다운 취소
    			try {
    				if(sugangPlayerCount.get() != 0) {
    	    			playUsers = sugangPlayerCount.get()/2;
    	    		}
    	    		seatsLeft.set(playUsers);
    	    		
    				timer.cancel();
                	timer = null;	
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}
            	
            }
    		gameSessions.remove(playerName);
    		
    		JSONObject leftRace = new JSONObject();
    		leftRace.put("RACE_WAIT_USER_DEL",playerName);
			
			for(String key : sessions.keySet()) { 
	    		WebSocketSession toSendSession = sessions.get(key);
	    		try {
	    			
	    			toSendSession.sendMessage(new TextMessage(leftRace.toJSONString()));
	    			
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
    		
    	}
    	
    	// 근데 신청버튼 여러번눌러서 여러번 날라오는건 처리 어케하지. ★
    	if(msg.equals("register_sugangBattle")  && playFlag ==1) {// 신청버튼 누름.
    		//int result = registerSugang();
    		
    		clickSugangBtn(session);
   
    		
    	}
    	else {
    		//do nothing
//    		try {
//    			// 본인 결과 알려줌
//    			String[] helloStrArr = msg.split("");
//    			int[] resultIntArr = new int[helloStrArr.length];
//    			
//    			for (int i = 0; i < helloStrArr.length; i++) {
//    			    int helloItemNum = helloStrArr[i].charAt(0);
//    			    resultIntArr[i] = helloItemNum;
//    			}
//    			
//    			session.sendMessage(new TextMessage(Arrays.toString(resultIntArr))); // 경쟁참여인원
//    			
//    		}catch(Exception e) {
//    			e.printStackTrace();
//    		}
    	}
    	
    }
    
    

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String playerName = getPlayerName(session);
        sessions.remove(playerName);
        if(nickname.containsKey(playerName))
        	nickname.remove(playerName);
        if(character.containsKey(playerName))
        	character.remove(playerName);
        if(gameSessions.containsKey(playerName))
        	gameSessions.remove(playerName);

        
        playerCount.decrementAndGet();
        System.out.println(playerName + " disconnected.");
        
        String message = "disconnected," + playerName;
        String currentPlayers="";
        for(Map.Entry item : sessions.entrySet()){
            currentPlayers += (String) item.getKey()+",";
        }
        
        JSONObject newUserDisconnected = new JSONObject();
		newUserDisconnected.put("RACE_EVERY_PLAYER_COUNT",playerCount.toString()); //현재 레이스 접속한 모든인원
		newUserDisconnected.put("RACE_JOINED_PLAYER_COUNT",sugangPlayerCount.toString()); //레이스에 참여중인 모든인원
        newUserDisconnected.put("RACE_SEATS",seatsLeft.get()); // 좌석수
        newUserDisconnected.put("RACE_WAIT_DEL",playerName); //새로 접속한 인원
        for (WebSocketSession clientSession : sessions.values()) {
            if (clientSession.isOpen()) {
                try {
                    synchronized (session) {
                        clientSession.sendMessage(new TextMessage("currentPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
                                                
                        clientSession.sendMessage(new TextMessage(newUserDisconnected.toJSONString()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    synchronized private void clickSugangBtn(WebSocketSession session) {

    	String uid = getPlayerName(session);
    	int myrank = gameResult.size();
    	//System.out.println("seatsLeft:"+seatsLeft.get());
		//System.out.println("myrank:"+myrank);
    	if(seatsLeft.get() - myrank >= 1) {
    		//System.out.println("inseatsLeft:"+seatsLeft.get());
    		//System.out.println("inmyrank:"+myrank);
        	JSONArray winnerData = new JSONArray();
        	JSONObject winnerDataField = new JSONObject();	
        	winnerDataField.put("UID",realUID.get(uid));
        	winnerDataField.put("NICKNAME",nickname.get(uid));
        	winnerDataField.put("CHARACTER",character.get(uid));
        	winnerData.add(winnerDataField);
        	gameResult.put(myrank,winnerData);
        	
        	notClickedUsers.remove(uid);
    	}

    	//"0":[user1,닉네임1,캐릭터1], "1":[user2,닉네임2,캐릭터2]

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