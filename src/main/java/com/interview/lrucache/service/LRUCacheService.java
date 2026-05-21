package com.interview.lrucache.service;

import com.interview.lrucache.entity.CacheEntity;
import com.interview.lrucache.repository.CacheRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LRUCacheService {

    private final CacheRepository repository;
    private final int capacity;
    private final LinkedHashMap<String, String> internalCache;

    private final ReentrantLock lock = new ReentrantLock();

    public LRUCacheService(CacheRepository repository, @Value("${cache.capacity:5}") int capacity) {
        this.repository = repository;
        this.capacity = capacity;
        this.internalCache = new LinkedHashMap<>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                if (size() > LRUCacheService.this.capacity) {
                    LRUCacheService.this.repository.deleteById(eldest.getKey());
                    return true;
                }
                return false;
            }
        };
    }

    @PostConstruct
    public void initCacheFromDatabase() {
        lock.lock();
        try {
            List<CacheEntity> entries = repository.findAll(Sort.by(Sort.Direction.ASC, "lastAccessed"));
            for (CacheEntity entity : entries) {
                internalCache.put(entity.getCacheKey(), entity.getCacheValue());
            }
        } finally {
            lock.unlock();
        }
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getCurrentSize() {
        lock.lock();
        try {
            return internalCache.size();
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @CacheEvict(value = "statsCache", allEntries = true)
    public String get(String key) {
        lock.lock();
        try {
            if (!internalCache.containsKey(key)) {
                return null;
            }
            String value = internalCache.get(key);
            repository.findById(key).ifPresent(entity -> {
                entity.setLastAccessed(Instant.now());
                repository.save(entity);
            });

            return value;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @CacheEvict(value = "statsCache", allEntries = true)
    public void put(String key, String value) {
        lock.lock();
        try {
            internalCache.put(key, value);
            CacheEntity entity = new CacheEntity(key, value, Instant.now());
            repository.save(entity);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @CacheEvict(value = "statsCache", allEntries = true)
    public boolean delete(String key) {
        lock.lock();
        try {
            if (internalCache.containsKey(key)) {
                internalCache.remove(key);
                repository.deleteById(key);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    @CacheEvict(value = "statsCache", allEntries = true)
    public void clear() {
        lock.lock();
        try {
            internalCache.clear();
            repository.deleteAllInBatch();
        } finally {
            lock.unlock();
        }
    }

    public Map<String, String> getAllElements() {
        lock.lock();
        try {
            LinkedHashMap<String, String> reversedMap = new LinkedHashMap<>();
            String[] keys = internalCache.keySet().toArray(new String[0]);
            for (int i = keys.length - 1; i >= 0; i--) {
                reversedMap.put(keys[i], internalCache.get(keys[i]));
            }
            return reversedMap;
        } finally {
            lock.unlock();
        }
    }

    @Cacheable(value = "statsCache")
    public Map<String, Object> getStats() {
        System.out.println("Calculating fresh cache statistics...");
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("currentSize", getCurrentSize());
        stats.put("capacity", getCapacity());
        return stats;
    }
}
