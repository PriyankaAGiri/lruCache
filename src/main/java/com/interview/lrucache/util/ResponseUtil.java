package com.interview.lrucache.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
@Component
public class ResponseUtil {

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        return new ResponseEntity<>(body, status);
    }
}
