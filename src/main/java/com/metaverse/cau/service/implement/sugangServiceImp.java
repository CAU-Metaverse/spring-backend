package com.metaverse.cau.service.implement;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import com.metaverse.cau.service.sugangService;

@Service
public class sugangServiceImp implements sugangService{
	private static AtomicInteger sugangPlayerCount = new AtomicInteger(0);
	
	@Override
	public Map<String, Object> getSugangData() {
		Map<String, Object> sugangData = new HashMap<>();
		
		sugangData.put("label1", "check1");
		sugangData.put("label2", "check2");
		sugangData.put("label3", "check3");
		return sugangData;
	}

	@Override
	public Map<String, Object> postSugangData(String playerName) {
		Map<String, Object> sugangPostData = new HashMap<>();
		
		
		sugangPostData.put(playerName,"test");
		return sugangPostData;
	}
	

}
