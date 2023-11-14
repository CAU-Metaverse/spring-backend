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

import com.metaverse.cau.dto.SubjectInfo;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class SugangWebSocketHandler extends TextWebSocketHandler{
	// 과목명, 학점, 여석, 채워진 자릿수
	static SubjectInfo musical = new SubjectInfo("연극과 뮤지컬",3,2,0);
	static SubjectInfo globalHanja = new SubjectInfo("글로벌 한자",3,1,0);
	static SubjectInfo ACT = new SubjectInfo("ACT",2,1,0);
	static SubjectInfo history = new SubjectInfo("한국사",2,3,0);
	static SubjectInfo medicine = new SubjectInfo("의약의 역사",3,2,0);
	static SubjectInfo accounting = new SubjectInfo("앙트레프레너십시대의 회계",2,1,0);
	
	
	private static Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>(); // 대기실
	private static Map<String, WebSocketSession> gameSessions = new ConcurrentHashMap<>(); // 게임방 (Go버튼)
	
	private static Map<String, String> realUID = new ConcurrentHashMap<>(); // 유니티 상의 uid
	private static Map<String, String> nickname = new ConcurrentHashMap<>(); // 닉네임
	private static Map<String, String> character = new ConcurrentHashMap<>(); // 캐릭터
	private static Map<String, AtomicInteger> gameCredit = new ConcurrentHashMap<>();
	private static AtomicInteger playerCount = new AtomicInteger(0); // 레이스 접속인원 수
	private static AtomicInteger maxCount = new AtomicInteger(0);
	private static AtomicInteger sugangPlayerCount = new AtomicInteger(0); //Go 버튼 누른사람수
	
	
	
	
	private static int secondsToPlay;
	private static Timer timer; // 30초 카운트를 위한 카운터
	private static TimerTask task;
	private static int playFlag =0; //0:게임대기중   1:게임중
	private static int nextRoundSeconds;
	private static int thisRoundSeconds;
	

    private static String findMaxKey(Map<String, AtomicInteger> map) {
        String maxKey = null;
        int maxValue = Integer.MIN_VALUE;

        for (Map.Entry<String, AtomicInteger> entry : map.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue().get();

            if (value > maxValue) {
                maxValue = value;
                maxKey = key;
            }
        }

        return maxKey;
    }
	static void gameset() {
		realUID.clear();
		nickname.clear();
		character.clear();
		gameCredit.clear();
		sugangPlayerCount.set(0);
		
		gameSessions.clear();
		
		
		musical = new SubjectInfo("연극과 뮤지컬",3,2,0);
		globalHanja = new SubjectInfo("글로벌 한자",3,1,0);
		ACT = new SubjectInfo("ACT",2,1,0);
		history = new SubjectInfo("한국사",2,3,0);
		medicine = new SubjectInfo("의약의 역사",3,2,0);
		accounting = new SubjectInfo("앙트레프레너십시대의 회계",2,1,0);
	}
	static void getResult() {
		// CREDIT 높은순으로 정렬해야함.
		JSONObject action_RACERESULT = new JSONObject();
		action_RACERESULT.put("action","RACE_RESULT");
		JSONArray dataArr = new JSONArray();
		
		
		while(true) {
			String key = findMaxKey(gameCredit);
			System.out.println("key"+key);
			if(key == null)
				break;
			
			try {
				JSONObject dataField = new JSONObject();
				dataField.put("USER",realUID.get(key));
				dataField.put("CHARACTER",character.get(key));
				dataField.put("CREDIT",gameCredit.get(key).get());
    			dataArr.add(dataField);
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
			gameCredit.remove(key);
			
		}
		action_RACERESULT.put("data",dataArr);
		
		
		
		for(String key : sessions.keySet()) {
			
    		WebSocketSession toSendSession = sessions.get(key);
    		try {
    			synchronized(toSendSession) {
    				toSendSession.sendMessage(new TextMessage(action_RACERESULT.toJSONString()));
    			}
    		}catch(Exception e) {
    			e.printStackTrace();
    		}
    	}

		gameset();
	}
	
	static void nextGameTimer() { 
		Timer nextTimer = new Timer();
		nextRoundSeconds = 15; //
		System.out.println("nextgametime");
		getResult();
		
		
		TimerTask nextTask = new TimerTask() {
			@Override
			public void run() {
				if(nextRoundSeconds > 0) {
					nextRoundSeconds--;
					String secToPlay = Integer.toString(nextRoundSeconds);
					
					JSONObject action_nextRoundTimer = new JSONObject();
					action_nextRoundTimer.put("action","TIMER_COUNT");
					
					secondsToPlay--;
					JSONObject data_nextRoundTimer = new JSONObject();
					data_nextRoundTimer.put("RACE_NEXT_TIME_LEFT",secToPlay);
					
					action_nextRoundTimer.put("data",data_nextRoundTimer);
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			
			    			//다음 라운드 대기중 : 결과창 볼수있는 시간.
			    			
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(action_nextRoundTimer.toJSONString()));
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
		thisRoundSeconds = 10; //
		
		
		TimerTask nextTask = new TimerTask() {
			@Override
			public void run() {
				if(thisRoundSeconds > 0) { 
					thisRoundSeconds--;
					String secToPlay = Integer.toString(thisRoundSeconds);
					
					JSONObject action_thisRoundTimer = new JSONObject();
					action_thisRoundTimer.put("action","TIMER_COUNT");
					
					secondsToPlay--;
					JSONObject data_thisRoundTimer = new JSONObject();
					data_thisRoundTimer.put("RACE_CURRENT_TIME_LEFT",secToPlay);
					
					action_thisRoundTimer.put("data",data_thisRoundTimer);
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			// 현재 라운드 남은시간 : 신청버튼 누를 수 있는 시간
			    			
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(action_thisRoundTimer.toJSONString()));
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
		secondsToPlay = 5;
		
		
		task = new TimerTask() {
			@Override
			public void run() {
				if(secondsToPlay > 0) {
					
					JSONObject action_timerCountTillStart = new JSONObject();
					action_timerCountTillStart.put("action","TIMER_COUNT");
					
					secondsToPlay--;
					String secToPlay = Integer.toString(secondsToPlay);
					JSONObject data_timerCountTillstart = new JSONObject();
					data_timerCountTillstart.put("RACE_WAIT_TIME_LEFT",secToPlay);
					
					action_timerCountTillStart.put("data",data_timerCountTillstart);
					
					//currentRaceWait.put("RACE_WAIT_USERS_NUM",sugangPlayerCount.toString());
					//currentRaceWait.put("RACE_WAIT_SEATS_LEFT",seatsLeft.toString());
					//currentRaceWait.put("RACE_WAIT_STATE_MESSAGE","RE-COUNTING_30_SECS");
					
					for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
			    		WebSocketSession toSendSession = sessions.get(key);
			    		try {
			    			
			    			//현재 경쟁 참여인원
//			    			toSendSession.sendMessage(new TextMessage("Battle:timeLeft:"+secToPlay)); // 시작남은시간
//			    			toSendSession.sendMessage(new TextMessage("Battle:Users:"+sugangPlayerCount.toString())); // 경쟁참여인원
//			    			toSendSession.sendMessage(new TextMessage("Battle:Seats:"+seatsLeft.toString())); // 수강가능인원
			    			synchronized(toSendSession) {
			    				toSendSession.sendMessage(new TextMessage(action_timerCountTillStart.toJSONString()));
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
		JSONObject action_raceStateStart = new JSONObject();
		action_raceStateStart.put("action","RACE_STATE");
		
		secondsToPlay--;
		JSONObject data_raceStateStart = new JSONObject();
		data_raceStateStart.put("RACE_CURRENT_STATE","START");
		
		action_raceStateStart.put("data",data_raceStateStart);
		
		
		String btnEnable = action_raceStateStart.toJSONString();
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
		
		thisGameTimer(); //현재 라운드 진행중

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
		newUserConnected.put("action","PLAYER_COUNT");
		JSONObject newUserData = new JSONObject();
		newUserData.put("RACE_EVERY_PLAYER_COUNT",playerCount.toString()); //현재 레이스 접속한 모든인원
		newUserConnected.put("data",newUserData);
		
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
    	
    	JSONParser parser = new JSONParser();
    	String action = new String();
    	JSONObject parseObj = new JSONObject();
    	try {
			parseObj = (JSONObject)parser.parse(msg);
			action = (String)parseObj.get("action");
		} catch (ParseException e) {
			
			e.printStackTrace();
		}
    	
		
    	//if( msg.contains("join_sugangBattle") && playFlag ==0) {// 배틀에 참여
    		//join_sugangBattle :userId :userCharacter :userNickname
    	if(action.equals("USER") && playFlag ==0) {
    		sugangPlayerCount.incrementAndGet();
    		if(sugangPlayerCount.get() != 0) {
    			playUsers = sugangPlayerCount.get()/2;
    		}
    		
    		
    		if(sugangPlayerCount.get()>1)
    			playGameTimer();
    		
    		
    		JSONObject data = (JSONObject)parseObj.get("data");
    		
    		String UID = data.get("ID").toString();
    		String CHARACTER = (String)data.get("CHARACTER");
    		String NICKNAME = (String)data.get("NICKNAME");
    		 
    		
    		gameSessions.put(String.valueOf(playerName),session);
    		realUID.put(playerName,UID);
    		character.put(playerName,CHARACTER);
    		nickname.put(playerName,NICKNAME);
    		gameCredit.put(playerName,new AtomicInteger(0));
    		
    		JSONObject joinRace = new JSONObject();
			joinRace.put("RACE_WAIT_USER_ADD",playerName);
			
			for(String key : sessions.keySet()) { //참여중인 인원과 비참여 인원 모두에게 인원을 알림
	    		WebSocketSession toSendSession = sessions.get(key);
	    		try {
	    			synchronized(toSendSession) {
	    			toSendSession.sendMessage(new TextMessage("test: "+joinRace.toJSONString()));
	    			}
	    		}catch(Exception e) {
	    			e.printStackTrace();
	    		}
	    	}
    		

    	}	// Go! 취소버튼 :  안쓸듯..
//    	else if( msg.equals("disconnected_sugangBattle") && playFlag ==0) {// 배틀 나가기
//    		
//    		
//    		if(sugangPlayerCount.decrementAndGet() <= 1) { // 플레이 할 사람이 없으면 카운트다운 취소
//    			try {
//    				if(sugangPlayerCount.get() != 0) {
//    	    			playUsers = sugangPlayerCount.get()/2;
//    	    		}
//    	    		seatsLeft.set(playUsers);
//    	    		
//    				timer.cancel();
//                	timer = null;	
//	    		}catch(Exception e) {
//	    			e.printStackTrace();
//	    		}
//            	
//            }
//    		gameSessions.remove(playerName);
//    		
//    		JSONObject leftRace = new JSONObject();
//    		leftRace.put("RACE_WAIT_USER_DEL",playerName);
//			
//			for(String key : sessions.keySet()) { 
//	    		WebSocketSession toSendSession = sessions.get(key);
//	    		try {
//	    			
//	    			toSendSession.sendMessage(new TextMessage(leftRace.toJSONString()));
//	    			
//	    		}catch(Exception e) {
//	    			e.printStackTrace();
//	    		}
//	    	}
//    		
//    	}
    	
    	// 신청버튼
    	if(action.equals("APPLY")  && playFlag ==1) {// 신청버튼 누름.
    		
    		JSONObject data = (JSONObject)parseObj.get("data");
    		//String UID = data.get("USER_ID").toString();
    		String SUBJECT = (String)data.get("SUBJECT");
    		
    		
    		//clickSugangBtn(session,UID, SUBJECT);
    		clickSugangBtn(session, SUBJECT);
   
    		
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
        if(realUID.containsKey(playerName))
        	realUID.remove(playerName);
        if(gameCredit.containsKey(playerName))
        	gameCredit.remove(playerName);
        
        playerCount.decrementAndGet();
        System.out.println(playerName + " disconnected.");
        
        String message = "disconnected," + playerName;
        String currentPlayers="";
        for(Map.Entry item : sessions.entrySet()){
            currentPlayers += (String) item.getKey()+",";
        }
        
//        JSONObject newUserDisconnected = new JSONObject();
//		newUserDisconnected.put("RACE_EVERY_PLAYER_COUNT",playerCount.toString()); //현재 레이스 접속한 모든인원
//		newUserDisconnected.put("RACE_JOINED_PLAYER_COUNT",sugangPlayerCount.toString()); //레이스에 참여중인 모든인원
//        newUserDisconnected.put("RACE_SEATS",seatsLeft.get()); // 좌석수
//        newUserDisconnected.put("RACE_WAIT_DEL",playerName); //새로 접속한 인원
//        for (WebSocketSession clientSession : sessions.values()) {
//            if (clientSession.isOpen()) {
//                try {
//                    synchronized (session) {
//                        clientSession.sendMessage(new TextMessage("currentPlayers,"+currentPlayers.substring(0, currentPlayers.length() - 1)));
//                                                
//                        clientSession.sendMessage(new TextMessage(newUserDisconnected.toJSONString()));
//                    }
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
    }

    private void applyResult(WebSocketSession session, SubjectInfo subject, boolean result) {
    	String uid = getPlayerName(session);
    	int leftSeats = subject.getLeftSeats();
    	
    	JSONObject action_APPLYBACK = new JSONObject();
    	action_APPLYBACK.put("action","APPLY");
		JSONObject dataField = new JSONObject();
		dataField.put("USER_ID",realUID.get(uid));
		dataField.put("SUBJECT",subject.getSubjectName());
		dataField.put("USER",nickname.get(uid));
		if(result) {
			dataField.put("RESULT","SUCCESS");
			AtomicInteger score = gameCredit.get(uid);
			System.out.println(score.incrementAndGet());
			
		}
			
		else {
			AtomicInteger score = gameCredit.get(uid);
			System.out.println(score.get());
			dataField.put("RESULT","FAIL");
			
		}
			
		dataField.put("SEATS_LEFT",String.valueOf(leftSeats));
		action_APPLYBACK.put("data",dataField);
		try {
          synchronized (session) {
              session.sendMessage(new TextMessage(action_APPLYBACK.toJSONString()));
          }
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
		
		/*
		 * { "action": "APPLY",
		 *   "data": { 
		 *  	"USER_ID": 3,
		 *  	"SUBJECT": "디비설",
		 * 		"USER":"흑석동 물주먹",
		 * 		"RESULT":"SUCCESS",
		 * 		"SEATS_LEFT":1 } 
		 * }
		 * 
		 */
		//data_APPLYBACK.put("RACE_NEXT_TIME_LEFT",secToPlay);
		
		//action_nextRoundTimer.put("data",data_nextRoundTimer);
		
    }
    
    synchronized private void clickSugangBtn(WebSocketSession session, String subject) {

    	switch(subject) {
    	case "연극과 뮤지컬":
    		applyResult(session, musical, musical.enroll());
    		break;
    	case "글로벌 한자":
    		applyResult(session, globalHanja, globalHanja.enroll());
    		break;
    	case "ACT" :
    		applyResult(session, ACT, ACT.enroll());
    		break;
    	case "한국사" :
    		applyResult(session, history, history.enroll());
    		break;
    	case "의약의 역사" :
    		applyResult(session, medicine, medicine.enroll());
    		break;
    	case "앙트레프레너십시대의 회계" :
    		applyResult(session, accounting, accounting.enroll());
    		break;
    		default:
    			System.out.println("subject error");
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