package io.whatap.grok.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Cache for compiled Grok patterns with O(1) LRU eviction.
 * Thread-safe implementation using synchronized LinkedHashMap.
 *
 * @since 1.0.1
 */
public class GrokCache {

    private static final int DEFAULT_MAX_SIZE = 500;
    private static final long CLEANUP_INTERVAL_SECONDS = 300;
    private static final long ENTRY_EXPIRE_MILLIS = TimeUnit.MINUTES.toMillis(30);

    private final int maxSize;
    private final Map<String, CacheEntry<Grok>> compiledPatterns;
    private final Map<String, CacheEntry<Pattern>> regexPatterns;
    private final ScheduledExecutorService cleanupExecutor;

    public GrokCache() {
        this(DEFAULT_MAX_SIZE);
    }

    public GrokCache(int maxSize) {
        this.maxSize = Math.min(maxSize, 2000);
        this.compiledPatterns = createLRUCache(this.maxSize);
        this.regexPatterns = createLRUCache(this.maxSize);
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "GrokCache-Cleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupExecutor.scheduleAtFixedRate(this::performCleanup,
            CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private <T> Map<String, CacheEntry<T>> createLRUCache(int maxSize) {
        return Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry<T>>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry<T>> eldest) {
                return size() > maxSize;
            }
        });
    }

    public Grok getGrok(String pattern) {
        CacheEntry<Grok> entry = compiledPatterns.get(pattern);
        if (entry != null) {
            entry.updateAccessTime();
            return entry.getValue();
        }
        return null;
    }

    public void putGrok(String pattern, Grok grok) {
        compiledPatterns.put(pattern, new CacheEntry<>(grok));
    }

    public Pattern getPattern(String regex) {
        CacheEntry<Pattern> entry = regexPatterns.get(regex);
        if (entry != null) {
            entry.updateAccessTime();
            return entry.getValue();
        }
        return null;
    }

    public void putPattern(String regex, Pattern pattern) {
        regexPatterns.put(regex, new CacheEntry<>(pattern));
    }

    public void clear() {
        compiledPatterns.clear();
        regexPatterns.clear();
    }

    public CacheStats getStats() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        return new CacheStats(compiledPatterns.size(), regexPatterns.size(), maxSize,
            usedMemory, maxMemory, (double) usedMemory / maxMemory * 100);
    }

    private void performCleanup() {
        long expireTime = System.currentTimeMillis() - ENTRY_EXPIRE_MILLIS;
        synchronized (compiledPatterns) {
            compiledPatterns.entrySet().removeIf(e -> e.getValue().getLastAccessTime() < expireTime);
        }
        synchronized (regexPatterns) {
            regexPatterns.entrySet().removeIf(e -> e.getValue().getLastAccessTime() < expireTime);
        }
    }

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

    private static class CacheEntry<T> {
        private final T value;
        private volatile long lastAccessTime;

        public CacheEntry(T value) {
            this.value = value;
            this.lastAccessTime = System.currentTimeMillis();
        }

        public T getValue() {
            return value;
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
            return String.format("CacheStats{grok=%d, regex=%d, max=%d, memory=%.1f%%}",
                grokCacheSize, regexCacheSize, maxSize, memoryUsagePercent);
        }
    }
}
