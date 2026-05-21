package com.interview.lrucache.controller;

import com.interview.lrucache.service.LRUCacheService;
import com.interview.lrucache.service.LRUCacheService1;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/cache")
@Slf4j
public class CacheController {

    private final LRUCacheService cacheService;


    public CacheController(LRUCacheService cacheService) {
        this.cacheService = cacheService;
    }


    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getCacheValue(@PathVariable String key) {
        log.info("Inside getCacheValue for key:{} ",key);
        String value = cacheService.get(key);
        if (value == null) {
            return buildResponse(HttpStatus.NOT_FOUND, "Key not found: " + key, null);
        }
        return buildResponse(HttpStatus.OK, "Key retrieved successfully", value);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> putCacheValue(@RequestBody Map<String, String> payload) {
        log.info("Inside putCacheValue for key");
        String key = payload.get("key");
        String value = payload.get("value");

        if (key == null || value == null) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Missing required payload parameters: 'key' or 'value'", null);
        }

        cacheService.put(key, value);
        return buildResponse(HttpStatus.CREATED, "Key-Value pair stored/updated successfully", null);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> deleteCacheValue(@PathVariable String key) {
        log.info("Inside deleteCacheValue for key:{}",key);
        boolean deleted = cacheService.delete(key);
        if (!deleted) {
            return buildResponse(HttpStatus.NOT_FOUND, "Key not found for deletion: " + key, null);
        }
        return buildResponse(HttpStatus.OK, "Key deleted successfully", null);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clearCache() {
        cacheService.clear();
        return buildResponse(HttpStatus.OK, "Cache and database cleared successfully", null);
    }

    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCacheValues() {
        return buildResponse(HttpStatus.OK, "All cache items fetched (MRU first)", cacheService.getAllElements());
    }

    // Existing mappings remain unchanged...

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = cacheService.getStats();
        return buildResponse(HttpStatus.OK, "Cache statistics retrieved successfully", stats);
    }
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        if (data != null) {
            body.put("data", data);
        }
        return new ResponseEntity<>(body, status);
    }

}