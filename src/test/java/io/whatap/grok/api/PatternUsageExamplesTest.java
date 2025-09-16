package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Usage examples and demo for Pattern API.
 * Shows how to use the new pattern management features.
 * 
 * @since 1.0.1
 */
public class PatternUsageExamplesTest {
    
    @Test
    public void example01_ListAllAvailablePatterns() {
        System.out.println("=== Example 1: List All Available Pattern Types ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        PatternType[] allTypes = repo.getAllPatternTypes();
        
        System.out.println("Available pattern types (" + allTypes.length + "):");
        for (PatternType type : allTypes) {
            System.out.printf("  %-15s - %s%n", type.getFileName(), type.getDescription());
        }
        
        assertTrue("Should have multiple pattern types", allTypes.length > 15);
    }
    
    @Test
    public void example02_ExplorePatternCategories() {
        System.out.println("\\n=== Example 2: Explore Pattern Categories ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        Map<String, List<PatternType>> categories = repo.getPatternTypesByCategory();
        
        categories.forEach((category, patterns) -> {
            System.out.printf("%s (%d patterns):%n", category, patterns.size());
            patterns.forEach(pattern -> 
                System.out.printf("  - %-15s (%s)%n", 
                    pattern.getFileName(), pattern.getDescription()));
            System.out.println();
        });
        
        assertTrue("Should have multiple categories", categories.size() >= 5);
    }
    
    @Test
    public void example03_LoadSpecificPatternType() throws GrokException {
        System.out.println("\\n=== Example 3: Load AWS Patterns ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        Map<String, String> awsPatterns = repo.loadPatterns(PatternType.AWS);
        
        System.out.println("AWS patterns loaded: " + awsPatterns.size());
        awsPatterns.forEach((name, pattern) -> {
            System.out.printf("  %-25s = %s%n", name, 
                pattern.length() > 80 ? pattern.substring(0, 80) + "..." : pattern);
        });
        
        assertTrue("Should have AWS patterns", awsPatterns.size() > 0);
    }
    
    @Test
    public void example04_SearchForSpecificPattern() throws GrokException {
        System.out.println("\\n=== Example 4: Search for 'IP' Pattern ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        Map<PatternType, String> ipPatterns = repo.findPattern("IP");
        
        System.out.println("IP pattern found in " + ipPatterns.size() + " files:");
        ipPatterns.forEach((type, pattern) -> {
            System.out.printf("  %-15s: %s%n", type.getFileName(), pattern);
        });
        
        assertTrue("Should find IP pattern", ipPatterns.size() >= 1);
    }
    
    @Test
    public void example05_CreateGrokWithSpecificPatterns() throws GrokException {
        System.out.println("\\n=== Example 5: Create Grok with Specific Patterns ===");
        
        // Create compiler and register only needed patterns
        GrokCompiler compiler = GrokCompiler.newInstance();
        compiler.registerPatterns(PatternType.PATTERNS); // Base patterns
        compiler.registerPatterns(PatternType.AWS);      // AWS patterns
        
        // Try to parse Apache access log
        Grok apacheGrok = compiler.compile("%{COMBINEDAPACHELOG}");
        String apacheLog = "127.0.0.1 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22\"";
        
        Match match = apacheGrok.match(apacheLog);
        Map<String, Object> result = match.capture();
        
        System.out.println("Apache log parsing result:");
        result.forEach((key, value) -> 
            System.out.printf("  %-15s = %s%n", key, value));
        
        assertFalse("Should parse apache log successfully", result.isEmpty());
        assertEquals("Should extract client IP", "127.0.0.1", result.get("clientip"));
    }
    
    @Test
    public void example06_RegisterAllPatternsAndCompileComplex() throws GrokException {
        System.out.println("\\n=== Example 6: Register All Patterns ===");
        
        GrokCompiler compiler = GrokCompiler.newInstance();
        compiler.registerAllPatterns(); // Register everything
        
        // Now we can use patterns from any file
        System.out.println("Available pattern definitions: " + compiler.getPatternDefinitions().size());
        
        // Test with various patterns
        String[] testPatterns = {
            "%{IP:ip}",
            "%{HTTPDATE:timestamp}",
            "%{WORD:method} %{URIPATH:path}",
            "%{RUBY_LOGGER}"
        };
        
        for (String pattern : testPatterns) {
            try {
                Grok grok = compiler.compile(pattern);
                System.out.printf("✓ Successfully compiled: %s%n", pattern);
            } catch (Exception e) {
                System.out.printf("✗ Failed to compile: %s (%s)%n", pattern, e.getMessage());
            }
        }
    }
    
    @Test
    public void example07_GetPatternStatistics() {
        System.out.println("\\n=== Example 7: Pattern Statistics ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        Map<PatternType, Integer> stats = repo.getPatternStatistics();
        
        int totalPatterns = stats.values().stream().mapToInt(Integer::intValue).sum();
        
        System.out.println("Pattern Statistics (Total: " + totalPatterns + " patterns):");
        stats.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue())) // Sort by count desc
            .forEach(entry -> {
                PatternType type = entry.getKey();
                int count = entry.getValue();
                double percentage = (count * 100.0) / totalPatterns;
                System.out.printf("  %-15s: %3d patterns (%5.1f%%)%n", 
                    type.getFileName(), count, percentage);
            });
        
        assertTrue("Should have significant number of patterns", totalPatterns > 100);
    }
    
    @Test
    public void example08_ExaminePatternFileContent() throws GrokException {
        System.out.println("\\n=== Example 8: Examine Pattern File Content ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        
        // Look at Ruby patterns file
        String rubyContent = repo.getPatternFileContent(PatternType.RUBY);
        String[] lines = rubyContent.split("\\n");
        
        System.out.println("Ruby pattern file content:");
        System.out.println("  Total lines: " + lines.length);
        System.out.println("  Content preview:");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String line = lines[i].trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                System.out.println("    " + line);
            }
        }
        
        assertFalse("Ruby file should not be empty", rubyContent.trim().isEmpty());
    }
    
    @Test
    public void example09_CheckPatternAvailability() {
        System.out.println("\\n=== Example 9: Check Pattern File Availability ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        
        System.out.println("Pattern file availability check:");
        for (PatternType type : PatternType.values()) {
            boolean available = repo.isPatternFileAvailable(type);
            System.out.printf("  %-15s: %s%n", type.getFileName(), 
                available ? "✓ Available" : "✗ Not Available");
        }
        
        // Core patterns should always be available
        assertTrue("Base patterns should be available", 
            repo.isPatternFileAvailable(PatternType.PATTERNS));
    }
    
    @Test
    public void example10_PerformanceWithCaching() throws GrokException {
        System.out.println("\\n=== Example 10: Performance with Caching ===");
        
        PatternRepository repo = PatternRepository.getInstance();
        repo.clearCache(); // Start fresh
        
        // First load - should read from file
        long start1 = System.currentTimeMillis();
        Map<String, String> patterns1 = repo.loadPatterns(PatternType.PATTERNS);
        long time1 = System.currentTimeMillis() - start1;
        
        // Second load - should use cache
        long start2 = System.currentTimeMillis();
        Map<String, String> patterns2 = repo.loadPatterns(PatternType.PATTERNS);
        long time2 = System.currentTimeMillis() - start2;
        
        System.out.printf("First load (from file): %d ms%n", time1);
        System.out.printf("Second load (from cache): %d ms%n", time2);
        System.out.printf("Speed improvement: %.1fx%n", (double) time1 / Math.max(time2, 1));
        
        assertEquals("Should load same patterns", patterns1.size(), patterns2.size());
        assertTrue("Cache should be faster", time2 <= time1);
    }
}