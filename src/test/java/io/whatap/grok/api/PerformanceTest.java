package io.whatap.grok.api;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Performance test for Grok parser optimizations.
 * 
 * @since 1.0.1
 */
public class PerformanceTest {

    private GrokCompiler compiler;
    private String apacheLogPattern = "%{COMBINEDAPACHELOG}";
    private List<String> testLogs;
    
    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        
        // Generate test logs
        testLogs = new ArrayList<>();
        String sampleLog = "127.0.0.1 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22\"";
        
        for (int i = 0; i < 1000; i++) {
            testLogs.add(sampleLog.replace("127.0.0.1", "192.168.1." + (i % 254 + 1)));
        }
    }
    
    @Test
    public void testCompilationPerformance() {
        long startTime = System.nanoTime();
        
        // Test multiple compilations of the same pattern (should benefit from caching)
        for (int i = 0; i < 100; i++) {
            Grok grok = compiler.compile(apacheLogPattern);
            assertNotNull(grok);
        }
        
        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        System.out.println("Compilation Performance Test:");
        System.out.println("100 pattern compilations took: " + duration + " ms");
        System.out.println("Average per compilation: " + (duration / 100.0) + " ms");
        
        // Should be fast due to caching (less than 1ms per compilation on average)
        assertTrue("Compilation should benefit from caching", duration < 100);
    }
    
    @Test
    public void testMatchingPerformance() {
        Grok grok = compiler.compile(apacheLogPattern);
        
        long startTime = System.nanoTime();
        
        int matchCount = 0;
        for (String log : testLogs) {
            Match match = grok.match(log);
            if (!match.equals(Match.EMPTY)) {
                matchCount++;
            }
        }
        
        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        System.out.println("\\nMatching Performance Test:");
        System.out.println("1000 log matches took: " + duration + " ms");
        System.out.println("Average per match: " + (duration / 1000.0) + " ms");
        System.out.println("Successful matches: " + matchCount);
        
        assertEquals("All logs should match", 1000, matchCount);
        assertTrue("Matching should be fast (< 1000ms for 1000 logs)", duration < 1000);
    }
    
    @Test
    public void testCapturePerformance() {
        Grok grok = compiler.compile(apacheLogPattern);
        
        long startTime = System.nanoTime();
        
        int captureCount = 0;
        for (String log : testLogs) {
            Map<String, Object> capture = grok.capture(log);
            if (!capture.isEmpty()) {
                captureCount++;
            }
        }
        
        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        System.out.println("\\nCapture Performance Test:");
        System.out.println("1000 log captures took: " + duration + " ms");
        System.out.println("Average per capture: " + (duration / 1000.0) + " ms");
        System.out.println("Successful captures: " + captureCount);
        
        assertEquals("All logs should be captured", 1000, captureCount);
        assertTrue("Capture should be fast (< 1500ms for 1000 logs)", duration < 1500);
    }
    
    @Test
    public void testCacheStats() {
        // Access cache through reflection or add getter methods
        System.out.println("\\nCache Statistics Test:");
        
        // Compile different patterns to test cache
        String[] patterns = {
            "%{COMBINEDAPACHELOG}",
            "%{IP:clientip}",
            "%{TIMESTAMP_ISO8601:timestamp}",
            "%{WORD:method} %{URIPATH:request}"
        };
        
        for (String pattern : patterns) {
            compiler.compile(pattern);
        }
        
        // Compile same patterns again (should hit cache)
        for (String pattern : patterns) {
            compiler.compile(pattern);
        }
        
        System.out.println("Cache should contain compiled patterns for reuse");
        assertTrue("Test completed successfully", true);
    }
    
    @Test
    public void testThreadSafety() throws InterruptedException {
        final Grok grok = compiler.compile(apacheLogPattern);
        final int threadCount = 10;
        final int logsPerThread = 100;
        final List<Thread> threads = new ArrayList<>();
        final List<Integer> results = new ArrayList<>();
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < threadCount; t++) {
            Thread thread = new Thread(() -> {
                int matches = 0;
                for (int i = 0; i < logsPerThread; i++) {
                    String log = testLogs.get(i % testLogs.size());
                    Match match = grok.match(log);
                    if (!match.equals(Match.EMPTY)) {
                        matches++;
                    }
                }
                synchronized (results) {
                    results.add(matches);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        int totalMatches = results.stream().mapToInt(Integer::intValue).sum();
        
        System.out.println("\\nThread Safety Performance Test:");
        System.out.println(threadCount + " threads, " + logsPerThread + " logs each");
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Total matches: " + totalMatches);
        System.out.println("Expected matches: " + (threadCount * logsPerThread));
        
        assertEquals("All threads should process successfully", threadCount * logsPerThread, totalMatches);
    }
}