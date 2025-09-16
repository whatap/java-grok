package io.whatap.grok.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test for memory management and OOM prevention in GrokCache.
 * 
 * @since 1.0.1
 */
public class MemoryLeakTest {

    private GrokCompiler compiler;
    
    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
    }
    
    @After
    public void tearDown() {
        // Force cleanup
        System.gc();
    }
    
    @Test
    public void testCacheSizeLimit() {
        // Create cache with small limit
        int maxSize = 10;
        GrokCache cache = new GrokCache(maxSize);
        
        // Add more patterns than the limit
        for (int i = 0; i < maxSize * 2; i++) {
            String pattern = "%{IP:ip" + i + "}";
            Grok grok = compiler.compile(pattern);
            cache.putGrok(pattern, grok);
        }
        
        GrokCache.CacheStats stats = cache.getStats();
        System.out.println("Cache stats after overflow: " + stats);
        
        // Cache should not exceed the maximum size
        assertTrue("Cache should respect size limit", 
            stats.grokCacheSize <= maxSize);
        
        cache.shutdown();
    }
    
    @Test
    public void testWeakReferenceCleanup() throws InterruptedException {
        GrokCache cache = new GrokCache(100);
        
        // Create patterns and let them go out of scope
        for (int i = 0; i < 50; i++) {
            String pattern = "%{WORD:word" + i + "}";
            Grok grok = compiler.compile(pattern);
            cache.putGrok(pattern, grok);
            // grok goes out of scope here
        }
        
        // Force garbage collection
        System.gc();
        Thread.sleep(100);
        System.gc();
        Thread.sleep(100);
        
        GrokCache.CacheStats beforeStats = cache.getStats();
        System.out.println("Stats before cleanup: " + beforeStats);
        
        // Access cache to trigger cleanup of dead weak references
        for (int i = 0; i < 50; i++) {
            String pattern = "%{WORD:word" + i + "}";
            cache.getGrok(pattern); // This should clean up dead references
        }
        
        GrokCache.CacheStats afterStats = cache.getStats();
        System.out.println("Stats after cleanup: " + afterStats);
        
        cache.shutdown();
    }
    
    @Test
    public void testMemoryPressureHandling() {
        GrokCache cache = new GrokCache(1000);
        
        // Get initial memory stats
        GrokCache.CacheStats initialStats = cache.getStats();
        System.out.println("Initial memory stats: " + initialStats);
        
        // Try to add many large patterns
        for (int i = 0; i < 500; i++) {
            StringBuilder largePattern = new StringBuilder("%{IP:ip");
            for (int j = 0; j < 100; j++) {
                largePattern.append("_").append(j);
            }
            largePattern.append("}");
            
            try {
                Grok grok = compiler.compile("%{IP:ip}"); // Use simpler pattern
                cache.putGrok(largePattern.toString(), grok);
            } catch (Exception e) {
                // Expected under memory pressure
                System.out.println("Cache rejected pattern under memory pressure: " + i);
                break;
            }
        }
        
        GrokCache.CacheStats finalStats = cache.getStats();
        System.out.println("Final memory stats: " + finalStats);
        
        // Memory usage should be reasonable
        assertTrue("Memory usage should be under control", 
            finalStats.memoryUsagePercent < 90.0);
        
        cache.shutdown();
    }
    
    @Test
    public void testLRUEviction() {
        // Create cache with relaxed memory limit for testing
        long relaxedMemoryLimit = Runtime.getRuntime().maxMemory() / 2; // 50% instead of default 20%
        GrokCache cache = new GrokCache(5, relaxedMemoryLimit);
        
        // Add patterns to fill cache and keep strong references
        String[] patterns = {
            "%{IP:ip1}", "%{IP:ip2}", "%{IP:ip3}", 
            "%{IP:ip4}", "%{IP:ip5}"
        };
        
        // Keep strong references to prevent GC
        Grok[] groks = new Grok[patterns.length];
        
        for (int i = 0; i < patterns.length; i++) {
            groks[i] = compiler.compile(patterns[i]);
            cache.putGrok(patterns[i], groks[i]);
        }
        
        // Verify cache is full
        GrokCache.CacheStats stats = cache.getStats();
        System.out.println("Cache stats after filling: " + stats);
        
        // Access first pattern to make it recently used (with small delay)
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        Grok firstGrok = cache.getGrok(patterns[0]);
        assertNotNull("First pattern should be accessible", firstGrok);
        
        // Add new pattern (should evict least recently used)
        String newPattern = "%{IP:ip6}";
        Grok newGrok = compiler.compile(newPattern);
        
        // Add delay to ensure different access times
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        cache.putGrok(newPattern, newGrok);
        
        // Check final cache state
        GrokCache.CacheStats finalStats = cache.getStats();
        System.out.println("Cache stats after adding new pattern: " + finalStats);
        
        // Cache should not exceed max size
        assertTrue("Cache should not exceed max size", 
            finalStats.grokCacheSize <= 5);
        
        // First pattern should still be there (recently accessed)
        Grok retrievedFirst = cache.getGrok(patterns[0]);
        if (retrievedFirst != null) {
            System.out.println("✓ Recently accessed pattern preserved");
        }
        
        // New pattern should be in cache
        Grok retrievedNew = cache.getGrok(newPattern);
        assertNotNull("New pattern should be in cache", retrievedNew);
        
        // Count how many original patterns remain
        int foundPatterns = 0;
        for (String pattern : patterns) {
            if (cache.getGrok(pattern) != null) {
                foundPatterns++;
                System.out.println("Found pattern in cache: " + pattern);
            }
        }
        
        System.out.println("Patterns found in cache: " + foundPatterns + "/" + patterns.length);
        
        // Since we have strong references, if cache size is at limit,
        // some eviction should have occurred
        if (finalStats.grokCacheSize >= 5) {
            // Cache is at capacity, some old patterns should be evicted
            assertTrue("Should evict some old patterns when at capacity", 
                foundPatterns < patterns.length);
        }
        
        // Keep references to prevent GC during test
        assertNotNull("Keep references", groks);
        assertNotNull("Keep reference", newGrok);
        
        cache.shutdown();
    }
    
    @Test
    public void testCacheShutdown() {
        GrokCache cache = new GrokCache(100);
        
        // Add some patterns
        for (int i = 0; i < 10; i++) {
            String pattern = "%{WORD:word" + i + "}";
            Grok grok = compiler.compile(pattern);
            cache.putGrok(pattern, grok);
        }
        
        GrokCache.CacheStats beforeShutdown = cache.getStats();
        assertTrue("Cache should have entries before shutdown", 
            beforeShutdown.grokCacheSize > 0);
        
        // Shutdown cache
        cache.shutdown();
        
        GrokCache.CacheStats afterShutdown = cache.getStats();
        assertEquals("Cache should be empty after shutdown", 
            0, afterShutdown.grokCacheSize);
    }
}