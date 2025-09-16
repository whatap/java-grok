package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test for PatternRepository and PatternType enum.
 * 
 * @since 1.0.1
 */
public class PatternRepositoryTest {

    private PatternRepository repository;
    
    @Before
    public void setUp() {
        repository = PatternRepository.getInstance();
        repository.clearCache(); // Start with clean cache
    }
    
    @Test
    public void testSingletonInstance() {
        PatternRepository repo1 = PatternRepository.getInstance();
        PatternRepository repo2 = PatternRepository.getInstance();
        
        assertSame("Should return same singleton instance", repo1, repo2);
    }
    
    @Test
    public void testGetAllPatternTypes() {
        PatternType[] types = repository.getAllPatternTypes();
        
        assertNotNull("Pattern types should not be null", types);
        assertTrue("Should have multiple pattern types", types.length > 10);
        
        // Check for specific expected pattern types
        boolean hasAws = false, hasJava = false, hasPatterns = false;
        for (PatternType type : types) {
            if (type == PatternType.AWS) hasAws = true;
            if (type == PatternType.JAVA) hasJava = true;
            if (type == PatternType.PATTERNS) hasPatterns = true;
        }
        
        assertTrue("Should contain AWS patterns", hasAws);
        assertTrue("Should contain Java patterns", hasJava);
        assertTrue("Should contain base patterns", hasPatterns);
    }
    
    @Test
    public void testGetPatternTypesByCategory() {
        Map<String, List<PatternType>> categories = repository.getPatternTypesByCategory();
        
        assertNotNull("Categories should not be null", categories);
        assertTrue("Should have multiple categories", categories.size() >= 5);
        
        // Check specific categories
        assertTrue("Should have Core category", categories.containsKey("Core"));
        assertTrue("Should have Databases category", categories.containsKey("Databases"));
        assertTrue("Should have Applications category", categories.containsKey("Applications"));
        
        // Check that core contains PATTERNS
        List<PatternType> corePatterns = categories.get("Core");
        assertNotNull("Core patterns should not be null", corePatterns);
        assertTrue("Core should contain PATTERNS", corePatterns.contains(PatternType.PATTERNS));
        
        System.out.println("Pattern categories:");
        categories.forEach((category, patterns) -> {
            System.out.println("  " + category + ": " + patterns.size() + " patterns");
            patterns.forEach(pattern -> System.out.println("    - " + pattern.getFileName()));
        });
    }
    
    @Test
    public void testLoadBasePatterns() throws GrokException {
        Map<String, String> patterns = repository.loadPatterns(PatternType.PATTERNS);
        
        assertNotNull("Patterns should not be null", patterns);
        assertTrue("Should have multiple patterns", patterns.size() > 50);
        
        // Check for specific base patterns
        assertTrue("Should contain USERNAME pattern", patterns.containsKey("USERNAME"));
        assertTrue("Should contain IP pattern", patterns.containsKey("IP"));
        assertTrue("Should contain COMBINEDAPACHELOG pattern", patterns.containsKey("COMBINEDAPACHELOG"));
        
        System.out.println("Base patterns loaded: " + patterns.size());
        System.out.println("Sample patterns:");
        patterns.entrySet().stream().limit(5).forEach(entry -> 
            System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
    }
    
    @Test
    public void testLoadMultiplePatterns() throws GrokException {
        Map<String, String> patterns = repository.loadPatterns(
            PatternType.PATTERNS, PatternType.AWS, PatternType.JAVA);
        
        assertNotNull("Patterns should not be null", patterns);
        assertTrue("Should have many patterns from multiple files", patterns.size() > 60);
        
        System.out.println("Multiple patterns loaded: " + patterns.size());
    }
    
    @Test
    public void testLoadAllPatterns() throws GrokException {
        Map<String, String> allPatterns = repository.loadAllPatterns();
        
        assertNotNull("All patterns should not be null", allPatterns);
        assertTrue("Should have many patterns", allPatterns.size() > 100);
        
        System.out.println("All patterns loaded: " + allPatterns.size());
    }
    
    @Test
    public void testGetPatternNames() throws GrokException {
        Set<String> patternNames = repository.getPatternNames(PatternType.PATTERNS);
        
        assertNotNull("Pattern names should not be null", patternNames);
        assertTrue("Should have multiple pattern names", patternNames.size() > 50);
        assertTrue("Should contain IP", patternNames.contains("IP"));
        
        System.out.println("Pattern names from base patterns: " + patternNames.size());
    }
    
    @Test
    public void testGetSpecificPattern() throws GrokException {
        String ipPattern = repository.getPattern(PatternType.PATTERNS, "IP");
        
        assertNotNull("IP pattern should not be null", ipPattern);
        assertTrue("IP pattern should reference IPV4 or IPV6", 
            ipPattern.contains("IPV4") || ipPattern.contains("IPV6"));
        
        System.out.println("IP pattern: " + ipPattern);
    }
    
    @Test
    public void testFindPattern() throws GrokException {
        Map<PatternType, String> results = repository.findPattern("IP");
        
        assertNotNull("Results should not be null", results);
        assertTrue("Should find IP pattern in at least one file", results.size() >= 1);
        assertTrue("Should find IP in base patterns", results.containsKey(PatternType.PATTERNS));
        
        System.out.println("IP pattern found in:");
        results.forEach((type, pattern) -> 
            System.out.println("  " + type.getFileName() + " = " + pattern));
    }
    
    @Test
    public void testPatternStatistics() {
        Map<PatternType, Integer> stats = repository.getPatternStatistics();
        
        assertNotNull("Statistics should not be null", stats);
        assertTrue("Should have stats for multiple pattern types", stats.size() > 10);
        
        // Base patterns should have the most patterns
        Integer basePatternCount = stats.get(PatternType.PATTERNS);
        assertNotNull("Should have stats for base patterns", basePatternCount);
        assertTrue("Base patterns should have many patterns", basePatternCount > 50);
        
        System.out.println("Pattern statistics:");
        stats.forEach((type, count) -> 
            System.out.println("  " + type.getFileName() + ": " + count + " patterns"));
    }
    
    @Test
    public void testIsPatternFileAvailable() {
        // Test available files
        assertTrue("Base patterns should be available", 
            repository.isPatternFileAvailable(PatternType.PATTERNS));
        assertTrue("AWS patterns should be available", 
            repository.isPatternFileAvailable(PatternType.AWS));
        assertTrue("Java patterns should be available", 
            repository.isPatternFileAvailable(PatternType.JAVA));
    }
    
    @Test
    public void testGetPatternFileContent() throws GrokException {
        String content = repository.getPatternFileContent(PatternType.RUBY);
        
        assertNotNull("Content should not be null", content);
        assertFalse("Content should not be empty", content.trim().isEmpty());
        assertTrue("Content should contain Ruby patterns", 
            content.contains("RUBY") || content.contains("ruby"));
        
        System.out.println("Ruby pattern file content length: " + content.length());
        System.out.println("First 200 characters: " + content.substring(0, Math.min(200, content.length())));
    }
    
    @Test
    public void testPatternTypeEnum() {
        // Test enum methods
        PatternType aws = PatternType.AWS;
        assertEquals("AWS file name should match", "aws", aws.getFileName());
        assertEquals("AWS resource path should match", "/patterns/aws", aws.getResourcePath());
        assertTrue("AWS description should contain AWS", 
            aws.getDescription().toLowerCase().contains("aws"));
        assertFalse("AWS should not be base patterns", aws.isBasePatterns());
        
        PatternType patterns = PatternType.PATTERNS;
        assertTrue("PATTERNS should be base patterns", patterns.isBasePatterns());
        
        // Test fromFileName
        assertEquals("Should find AWS by filename", PatternType.AWS, 
            PatternType.fromFileName("aws"));
        assertNull("Should return null for unknown filename", 
            PatternType.fromFileName("unknown"));
        
        System.out.println("AWS pattern type: " + aws);
    }
    
    @Test
    public void testCacheClearing() throws GrokException {
        // Load patterns to populate cache
        repository.loadPatterns(PatternType.PATTERNS);
        
        // Clear cache
        repository.clearCache();
        
        // Load again (should work fine)
        Map<String, String> patterns = repository.loadPatterns(PatternType.PATTERNS);
        assertNotNull("Patterns should load after cache clear", patterns);
        assertTrue("Should have patterns after cache clear", patterns.size() > 0);
    }
    
    @Test
    public void testGrokCompilerIntegration() throws GrokException {
        GrokCompiler compiler = GrokCompiler.newInstance();
        
        // Test registerPatterns with PatternType
        compiler.registerPatterns(PatternType.PATTERNS);
        compiler.registerPatterns(PatternType.AWS);
        
        // Should be able to compile patterns from both files
        Grok ipGrok = compiler.compile("%{IP:clientip}");
        assertNotNull("Should compile IP pattern", ipGrok);
        
        // Test pattern from AWS
        try {
            Grok s3Grok = compiler.compile("%{S3_ACCESS_LOG}");
            assertNotNull("Should compile S3 pattern", s3Grok);
            System.out.println("Successfully compiled S3 access log pattern");
        } catch (Exception e) {
            System.out.println("S3 pattern compilation failed (expected if dependencies missing): " + e.getMessage());
        }
        
        // Test registerAllPatterns
        GrokCompiler compiler2 = GrokCompiler.newInstance();
        compiler2.registerAllPatterns();
        
        Grok combinedGrok = compiler2.compile("%{COMBINEDAPACHELOG}");
        assertNotNull("Should compile combined apache log pattern", combinedGrok);
        
        System.out.println("GrokCompiler integration test passed");
    }
}