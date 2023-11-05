package com.metaverse.cau.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.metaverse.cau.service.sugangService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class sugangController {
	
	private final sugangService sugangService;

	// 현재 모든 유저수 반환, 결과
	@GetMapping("/sugang")
	public Map<String, Object> getSugang(){
		
		return sugangService.getSugangData();
	}
	
	@PostMapping("/sugang/post/{playerName}")
	public Map<String, Object> postSugang(@PathVariable("playerName") String playerName){
		
		
		return sugangService.postSugangData(playerName);
	}
}
