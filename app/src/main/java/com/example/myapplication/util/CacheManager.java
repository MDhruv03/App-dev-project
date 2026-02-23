package com.example.myapplication.util;

import android.content.Context;
import android.util.LruCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Memory cache for frequently accessed data
 */
public class CacheManager {
    
    private static CacheManager instance;
    
    // LRU Cache for objects
    private final LruCache<String, Object> memoryCache;
    
    // Simple time-based cache
    private final Map<String, CacheEntry> timedCache;
    
    private static class CacheEntry {
        Object data;
        long timestamp;
        long ttl; // Time to live in milliseconds
        
        CacheEntry(Object data, long ttl) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.ttl = ttl;
        }
        
        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > ttl;
        }
    }
    
    private CacheManager() {
        // Get max available memory
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        
        // Use 1/8th of the available memory for this cache
        final int cacheSize = maxMemory / 8;
        
        memoryCache = new LruCache<String, Object>(cacheSize) {
            @Override
            protected int sizeOf(String key, Object value) {
                // Approximate size calculation
                return 1; // Simple implementation
            }
        };
        
        timedCache = new HashMap<>();
    }
    
    public static synchronized CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }
    
    /**
     * Store value in cache
     */
    public void put(String key, Object value) {
        memoryCache.put(key, value);
    }
    
    /**
     * Store value in cache with expiry time
     */
    public void put(String key, Object value, long ttlMillis) {
        timedCache.put(key, new CacheEntry(value, ttlMillis));
    }
    
    /**
     * Get value from cache
     */
    public Object get(String key) {
        // Try memory cache first
        Object value = memoryCache.get(key);
        if (value != null) {
            return value;
        }
        
        // Try timed cache
        CacheEntry entry = timedCache.get(key);
        if (entry != null) {
            if (!entry.isExpired()) {
                return entry.data;
            } else {
                // Remove expired entry
                timedCache.remove(key);
            }
        }
        
        return null;
    }
    
    /**
     * Get value from cache with type casting
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Remove value from cache
     */
    public void remove(String key) {
        memoryCache.remove(key);
        timedCache.remove(key);
    }
    
    /**
     * Check if key exists in cache
     */
    public boolean contains(String key) {
        if (memoryCache.get(key) != null) {
            return true;
        }
        
        CacheEntry entry = timedCache.get(key);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * Clear all cache
     */
    public void clear() {
        memoryCache.evictAll();
        timedCache.clear();
    }
    
    /**
     * Clear expired entries from timed cache
     */
    public void clearExpired() {
        timedCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Get cache size
     */
    public int size() {
        return memoryCache.size() + timedCache.size();
    }
    
    /**
     * Generate cache key from multiple parts
     */
    public static String generateKey(String... parts) {
        return String.join("_", parts);
    }
}
