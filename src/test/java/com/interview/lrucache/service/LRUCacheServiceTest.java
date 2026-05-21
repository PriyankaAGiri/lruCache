package com.interview.lrucache.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LRUCacheServiceTest {

    @Autowired
    private LRUCacheService1 cacheService;

    @BeforeEach
    void setUp() {
        cacheService.clear();
    }

    @Test
    void testPutBeyondCapacityEvictsLeastRecentlyUsed() {
        assertEquals(5, cacheService.getCapacity());

        cacheService.put("A", "ValueA");
        cacheService.put("B", "ValueB");
        cacheService.put("C", "ValueC");
        cacheService.put("D", "ValueD");
        cacheService.put("E", "ValueE");

        cacheService.get("A");
        // Insert 6th element ("F"), soo it will evict older data
        cacheService.put("F", "ValueF");
        // Verify "B" is evicted
        assertNull(cacheService.get("B"), "Oldest unaccessed element 'B' should have been evicted.");
        assertNotNull(cacheService.get("A"), "Element 'A' should be present because it was accessed.");
        assertEquals(5, cacheService.getCurrentSize());
    }

    @Test
    void testGetOnMissingKeyReturnsNull() {
        String value = cacheService.get("P");
        assertNull(value, "Retrieving a key missing from cache.");
    }

    @RepeatedTest(3)
    void testConcurrentPutAndGetOperations() throws InterruptedException {
        cacheService.clear();
        int totalThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch putsConfirmLatch = new CountDownLatch(totalThreads);
        CountDownLatch getsConfirmLatch = new CountDownLatch(totalThreads);


        for (int i = 0; i < totalThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Hold until fired
                    cacheService.put("Key-" + index, "Value-" + index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    putsConfirmLatch.countDown();
                }
            });
        }


        startLatch.countDown();
        boolean putsFinished = putsConfirmLatch.await(5, TimeUnit.SECONDS);
        assertTrue(putsFinished, "Concurrent puts execution timed out.");


        for (int i = 0; i < totalThreads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    String val = cacheService.get("Key-" + index);
                    assertNotNull(val, "Value for Key-" + index + " was missing under heavy concurrency concurrent lookup.");
                    assertEquals("Value-" + index, val, "Data corruption identified under thread pressure.");
                } finally {
                    getsConfirmLatch.countDown();
                }
            });
        }

        boolean getsFinished = getsConfirmLatch.await(5, TimeUnit.SECONDS);
        assertTrue(getsFinished, "Concurrent gets verification timed out.");
        executor.shutdown();
    }

    @Test
    void testStatsCachingAndStaleScenario() {
        cacheService.clear();

        cacheService.put("K1", "V1");
        cacheService.put("K2", "V2");
        cacheService.put("K3", "V3");

        Map<String, Object> stats1 = cacheService.getStats();
        assertEquals(3, stats1.get("currentSize"));


        cacheService.delete("K1");

        Map<String, Object> stats2 = cacheService.getStats();
        assertEquals(2, stats2.get("currentSize"), "Caffeine failed to invalidate stats cache on deletion.");
    }
}