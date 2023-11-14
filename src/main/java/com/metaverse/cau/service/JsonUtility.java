package com.metaverse.cau.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

@Service
public class JsonUtility {

    public HashMap<String, Object> jsonToMap(String jsonString) {
        try {
            // Jackson ObjectMapper를 사용하여 JSON을 Map으로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonString, HashMap.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public <T> T mapJsonToPerson(String json, Class<T> mappedClass) {
        try {
            // Jackson ObjectMapper를 사용하여 JSON을 자바 객체로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, mappedClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
