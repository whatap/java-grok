package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for PatternManagementService.
 */
public class PatternManagementServiceTest {
    
    private final PatternManagementService service = new PatternManagementService();
    
    @Test
    public void testGetAllPatternTypes() {
        List<PatternManagementService.PatternTypeInfo> types = service.getAllPatternTypes();
        
        assertNotNull(types);
        assertFalse(types.isEmpty());
        assertEquals(PatternType.values().length, types.size());

        for (PatternManagementService.PatternTypeInfo t: types) {
            System.out.println(t);
        }

        // Check that all pattern types are included
        for (PatternType patternType : PatternType.values()) {
            assertTrue(types.stream().anyMatch(info -> info.getName().equals(patternType.name())));
        }
    }
    
    @Test
    public void testGetPatternTypesByCategory() {
        Map<String, List<PatternManagementService.PatternTypeInfo>> categories = service.getPatternTypesByCategory();
        
        assertNotNull(categories);
        assertFalse(categories.isEmpty());
        
        System.out.println("=== Pattern Types by Category ===");
        categories.forEach((category, typeList) -> {
            System.out.println("Category: " + category);
            typeList.forEach(type -> System.out.println("  " + type));
            System.out.println();
        });
        
        // Check that core category exists
        assertTrue(categories.containsKey("Core"));
        assertTrue(categories.get("Core").stream()
                .anyMatch(info -> info.getName().equals("PATTERNS")));
    }
    
    @Test
    public void testGetPatternTypeDetails() throws GrokException {
        PatternManagementService.PatternTypeDetails details = service.getPatternTypeDetails(PatternType.MONGODB);
        
        assertNotNull(details);
        assertNotNull(details.getTypeInfo());
        assertNotNull(details.getPatterns());
        assertEquals("MONGODB", details.getTypeInfo().getName());
        assertTrue(details.getPatternCount() > 0);
        
        System.out.println("=== MongoDB Pattern Details ===");
        System.out.println("Type Info: " + details.getTypeInfo());
        System.out.println("Pattern Count: " + details.getPatternCount());
        System.out.println("Patterns:");
        details.getPatterns().forEach((name, pattern) -> 
            System.out.println("  " + name + " = " + pattern));
        System.out.println();
    }
    
    @Test
    public void testGetPatternTypeDetailsByName() throws GrokException {
        PatternManagementService.PatternTypeDetails details = service.getPatternTypeDetails("MONGODB");
        
        assertNotNull(details);
        assertEquals("MONGODB", details.getTypeInfo().getName());
    }
    
    @Test(expected = GrokException.class)
    public void testGetPatternTypeDetailsWithInvalidName() throws GrokException {
        service.getPatternTypeDetails("INVALID_TYPE");
    }
    
    @Test
    public void testGetPattern() throws GrokException {
        PatternManagementService.PatternInfo pattern = service.getPattern(PatternType.MONGODB, "MONGO_QUERY");
        
        assertNotNull(pattern);
        assertEquals("MONGO_QUERY", pattern.getName());
        assertEquals(PatternType.MONGODB, pattern.getType());
        assertNotNull(pattern.getDefinition());
        
        System.out.println("=== Single Pattern Info ===");
        System.out.println(pattern);
        System.out.println();
    }
    
    @Test
    public void testGetPatternNotFound() throws GrokException {
        PatternManagementService.PatternInfo pattern = service.getPattern(PatternType.MONGODB, "NON_EXISTENT_PATTERN");
        
        assertNull(pattern);
    }
    
    @Test
    public void testSearchPatterns() throws GrokException {
        List<PatternManagementService.PatternInfo> results = service.searchPatterns("MONGO_QUERY");
        
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(p -> p.getName().equals("MONGO_QUERY")));
        
        System.out.println("=== Search Results for 'MONGO_QUERY' ===");
        results.forEach(System.out::println);
        System.out.println();
    }
    
    @Test
    public void testGetPatternStatistics() {
        PatternManagementService.PatternStatistics stats = service.getPatternStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.getTotalPatterns() > 0);
        assertTrue(stats.getAvailableTypes() > 0);
        assertEquals(PatternType.values().length, stats.getTotalTypes());
        assertNotNull(stats.getTypeStatistics());
        
        System.out.println("=== Pattern Statistics ===");
        System.out.println(stats);
        System.out.println("Type Statistics:");
        stats.getTypeStatistics().forEach((typeName, typeStats) -> 
            System.out.println("  " + typeName + ": " + typeStats));
        System.out.println();
    }
    
    @Test
    public void testGetPatternFileContent() throws GrokException {
        String content = service.getPatternFileContent(PatternType.MONGODB);
        
        assertNotNull(content);
        assertFalse(content.trim().isEmpty());
        assertTrue(content.contains("MONGO_QUERY"));
        
        System.out.println("=== MongoDB Pattern File Content ===");
        System.out.println(content);
        System.out.println();
    }
    
    @Test
    public void testIsPatternFileAvailable() {
        // MongoDB pattern should be available
        assertTrue(service.isPatternFileAvailable(PatternType.MONGODB));
        
        // All enum types should have their availability checked
        for (PatternType type : PatternType.values()) {
            // Just ensure no exception is thrown
            service.isPatternFileAvailable(type);
        }
    }
    
    @Test
    public void testGetMergedPatterns() throws GrokException {
        Map<String, String> patterns = service.getMergedPatterns("MONGODB", "PATTERNS");
        
        assertNotNull(patterns);
        assertFalse(patterns.isEmpty());
        
        System.out.println("=== Merged Patterns (MONGODB + PATTERNS) ===");
        System.out.println("Total patterns: " + patterns.size());
        patterns.forEach((name, pattern) -> 
            System.out.println("  " + name + " = " + pattern));
        System.out.println();
    }
    
    @Test
    public void testGetAllPatterns() throws GrokException {
        Map<String, String> allPatterns = service.getAllPatterns();
        
        assertNotNull(allPatterns);
        assertFalse(allPatterns.isEmpty());
        
        System.out.println("=== All Patterns Summary ===");
        System.out.println("Total patterns across all types: " + allPatterns.size());
        System.out.println("First 10 patterns:");
        allPatterns.entrySet().stream()
                .limit(10)
                .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
        System.out.println();
    }
    
    @Test
    public void testPatternTypeInfoToString() {
        List<PatternManagementService.PatternTypeInfo> types = service.getAllPatternTypes();
        PatternManagementService.PatternTypeInfo typeInfo = types.get(0);
        
        String toString = typeInfo.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PatternTypeInfo"));
        assertTrue(toString.contains(typeInfo.getName()));
    }
    
    @Test
    public void testPatternInfoToString() throws GrokException {
        PatternManagementService.PatternInfo pattern = service.getPattern(PatternType.MONGODB, "MONGO_QUERY");
        
        if (pattern != null) {
            String toString = pattern.toString();
            assertNotNull(toString);
            assertTrue(toString.contains("PatternInfo"));
            assertTrue(toString.contains("MONGO_QUERY"));
        }
    }
    
    @Test
    public void testPatternStatisticsToString() {
        PatternManagementService.PatternStatistics stats = service.getPatternStatistics();
        
        String toString = stats.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PatternStatistics"));
        assertTrue(toString.contains(String.valueOf(stats.getTotalPatterns())));
    }
}