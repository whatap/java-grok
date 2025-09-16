package io.whatap.grok.api;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Cache for compiled Grok patterns to improve performance.
 * Thread-safe implementation using ConcurrentHashMap.
 * 
 * @since 1.0.1
 */
public class GrokCache {
    
    private static final int DEFAULT_MAX_SIZE = 500; // Reduced for safety
    private static final long CLEANUP_INTERVAL_SECONDS = 300; // 5 minutes
    private static final long MAX_MEMORY_THRESHOLD = Runtime.getRuntime().maxMemory() / 10; // 10% of max heap
    
    private final ConcurrentMap<String, CacheEntry<Grok>> compiledPatterns;
    private final ConcurrentMap<String, CacheEntry<Pattern>> regexPatterns;
    private final int maxSize;
    private final ScheduledExecutorService cleanupExecutor;
    private volatile long lastCleanupTime = System.currentTimeMillis();
    
    public GrokCache() {
        this(DEFAULT_MAX_SIZE);
    }
    
    public GrokCache(int maxSize) {
        this.maxSize = Math.min(maxSize, 2000); // Hard limit to prevent OOM
        this.compiledPatterns = new ConcurrentHashMap<>();
        this.regexPatterns = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GrokCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        // Schedule periodic cleanup
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 
            CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    
    /**
     * Get cached Grok pattern or null if not found.
     */
    public Grok getGrok(String pattern) {
        CacheEntry<Grok> entry = compiledPatterns.get(pattern);
        if (entry != null) {
            Grok grok = entry.getValue();
            if (grok != null) {
                entry.updateAccessTime();
                return grok;
            } else {
                // Weak reference was cleared, remove the entry
                compiledPatterns.remove(pattern);
            }
        }
        return null;
    }
    
    /**
     * Cache compiled Grok pattern.
     */
    public void putGrok(String pattern, Grok grok) {
        // Check memory pressure before adding
        if (isMemoryPressureHigh()) {
            performEmergencyCleanup();
            return; // Skip caching under memory pressure
        }
        
        if (compiledPatterns.size() >= maxSize) {
            evictLeastRecentlyUsed(compiledPatterns);
        }
        compiledPatterns.put(pattern, new CacheEntry<>(grok));
    }
    
    /**
     * Get cached compiled regex Pattern or null if not found.
     */
    public Pattern getPattern(String regex) {
        CacheEntry<Pattern> entry = regexPatterns.get(regex);
        if (entry != null) {
            Pattern pattern = entry.getValue();
            if (pattern != null) {
                entry.updateAccessTime();
                return pattern;
            } else {
                // Weak reference was cleared, remove the entry
                regexPatterns.remove(regex);
            }
        }
        return null;
    }
    
    /**
     * Cache compiled regex Pattern.
     */
    public void putPattern(String regex, Pattern pattern) {
        // Check memory pressure before adding
        if (isMemoryPressureHigh()) {
            performEmergencyCleanup();
            return; // Skip caching under memory pressure
        }
        
        if (regexPatterns.size() >= maxSize) {
            evictLeastRecentlyUsed(regexPatterns);
        }
        regexPatterns.put(regex, new CacheEntry<>(pattern));
    }
    
    /**
     * Clear all cached patterns.
     */
    public void clear() {
        compiledPatterns.clear();
        regexPatterns.clear();
    }
    
    /**
     * Get cache statistics.
     */
    public CacheStats getStats() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        return new CacheStats(compiledPatterns.size(), regexPatterns.size(), maxSize, 
            usedMemory, maxMemory, memoryUsagePercent);
    }
    
    /**
     * Check if memory pressure is high.
     */
    private boolean isMemoryPressureHigh() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return usedMemory > MAX_MEMORY_THRESHOLD;
    }
    
    /**
     * Perform emergency cleanup when memory pressure is high.
     */
    private void performEmergencyCleanup() {
        compiledPatterns.clear();
        regexPatterns.clear();
        System.gc(); // Suggest garbage collection
    }
    
    /**
     * Periodic cleanup of expired entries.
     */
    private void performCleanup() {
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - TimeUnit.MINUTES.toMillis(30); // 30 minutes
        
        compiledPatterns.entrySet().removeIf(entry -> 
            entry.getValue().getLastAccessTime() < expireTime || entry.getValue().getValue() == null);
        regexPatterns.entrySet().removeIf(entry -> 
            entry.getValue().getLastAccessTime() < expireTime || entry.getValue().getValue() == null);
            
        lastCleanupTime = currentTime;
    }
    
    /**
     * Evict least recently used entry from cache.
     */
    private <T> void evictLeastRecentlyUsed(ConcurrentMap<String, CacheEntry<T>> cache) {
        String oldestKey = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (java.util.Map.Entry<String, CacheEntry<T>> entry : cache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestTime) {
                oldestTime = accessTime;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
    
    /**
     * Shutdown the cache and cleanup resources.
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
    }
    
    /**
     * Cache entry with weak reference and access time tracking.
     */
    private static class CacheEntry<T> {
        private final WeakReference<T> valueRef;
        private volatile long lastAccessTime;
        
        public CacheEntry(T value) {
            this.valueRef = new WeakReference<>(value);
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public T getValue() {
            return valueRef.get();
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
    
    public static class CacheStats {
        public final int grokCacheSize;
        public final int regexCacheSize;
        public final int maxSize;
        public final long usedMemory;
        public final long maxMemory;
        public final double memoryUsagePercent;
        
        public CacheStats(int grokCacheSize, int regexCacheSize, int maxSize, 
                         long usedMemory, long maxMemory, double memoryUsagePercent) {
            this.grokCacheSize = grokCacheSize;
            this.regexCacheSize = regexCacheSize;
            this.maxSize = maxSize;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
            this.memoryUsagePercent = memoryUsagePercent;
        }
        
        @Override
        public String toString() {
            return String.format("CacheStats{grok=%d, regex=%d, max=%d, memory=%.1f%% (%dMB/%dMB)}", 
                grokCacheSize, regexCacheSize, maxSize, memoryUsagePercent,
                usedMemory / (1024 * 1024), maxMemory / (1024 * 1024));
        }
    }
}