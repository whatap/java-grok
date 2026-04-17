package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for managing and accessing Grok pattern files.
 * Provides APIs to load patterns from enum-defined pattern types.
 * 
 * @since 1.0.1
 */
public class PatternRepository {
    
    private static final PatternRepository INSTANCE = new PatternRepository();
    
    /**
     * Cache for loaded patterns to avoid repeated file I/O.
     */
    private final Map<PatternType, Map<String, String>> patternCache = new HashMap<>();

    /**
     * Cache for loaded sample logs to avoid repeated file I/O.
     */
    private final Map<PatternType, Map<String, String>> sampleCache = new HashMap<>();
    
    private PatternRepository() {
        // Singleton pattern
    }
    
    /**
     * Get the singleton instance.
     * 
     * @return the PatternRepository instance
     */
    public static PatternRepository getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get all available pattern types.
     * 
     * @return array of all PatternType values
     */
    public PatternType[] getAllPatternTypes() {
        return PatternType.values();
    }
    
    /**
     * Get pattern types by category.
     * 
     * @return map of categories to pattern types
     */
    public Map<String, List<PatternType>> getPatternTypesByCategory() {
        Map<String, List<PatternType>> categories = new LinkedHashMap<>();

        for (PatternType type : PatternType.values()) {
            String categoryName = type.getCategory().getDisplayName();
            categories.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(type);
        }

        return categories;
    }
    
    /**
     * Load patterns from a specific pattern type.
     * 
     * @param patternType the pattern type to load
     * @return map of pattern names to pattern definitions
     * @throws GrokException if pattern file cannot be loaded
     */
    public Map<String, String> loadPatterns(PatternType patternType) throws GrokException {
        return loadPatterns(patternType, StandardCharsets.UTF_8);
    }
    
    /**
     * Load patterns from a specific pattern type with custom charset.
     * 
     * @param patternType the pattern type to load
     * @param charset the charset to use for reading
     * @return map of pattern names to pattern definitions
     * @throws GrokException if pattern file cannot be loaded
     */
    public Map<String, String> loadPatterns(PatternType patternType, Charset charset) throws GrokException {
        // Check cache first
        Map<String, String> cached = patternCache.get(patternType);
        if (cached != null) {
            return new HashMap<>(cached); // Return copy to prevent modification
        }
        
        try {
            Map<String, String> patterns = loadPatternsFromResource(patternType.getResourcePath(), charset);
            patternCache.put(patternType, patterns);
            return new HashMap<>(patterns);
        } catch (IOException e) {
            throw new GrokException("Failed to load patterns from " + patternType.getFileName(), e);
        }
    }
    
    /**
     * Load patterns from multiple pattern types.
     * 
     * @param patternTypes the pattern types to load
     * @return merged map of all patterns
     * @throws GrokException if any pattern file cannot be loaded
     */
    public Map<String, String> loadPatterns(PatternType... patternTypes) throws GrokException {
        Map<String, String> allPatterns = new HashMap<>();
        
        for (PatternType patternType : patternTypes) {
            try {
                // Skip if pattern file is not available
                if (!isPatternFileAvailable(patternType)) {
                    continue;
                }
                
                Map<String, String> patterns = loadPatterns(patternType);
                allPatterns.putAll(patterns);
            } catch (GrokException e) {
                // Continue with other pattern types
            }
        }
        
        return allPatterns;
    }
    
    /**
     * Load all available patterns.
     * 
     * @return map containing all patterns from all types
     * @throws GrokException if any pattern file cannot be loaded
     */
    public Map<String, String> loadAllPatterns() throws GrokException {
        return loadPatterns(PatternType.values());
    }
    
    /**
     * Get pattern names from a specific pattern type.
     * 
     * @param patternType the pattern type
     * @return set of pattern names
     * @throws GrokException if pattern file cannot be loaded
     */
    public Set<String> getPatternNames(PatternType patternType) throws GrokException {
        return loadPatterns(patternType).keySet();
    }
    
    /**
     * Get a specific pattern definition.
     * 
     * @param patternType the pattern type
     * @param patternName the pattern name
     * @return the pattern definition or null if not found
     * @throws GrokException if pattern file cannot be loaded
     */
    public String getPattern(PatternType patternType, String patternName) throws GrokException {
        return loadPatterns(patternType).get(patternName);
    }
    
    /**
     * Search for patterns by name across all pattern types.
     * 
     * @param patternName the pattern name to search for
     * @return map of pattern types to pattern definitions
     * @throws GrokException if pattern files cannot be loaded
     */
    public Map<PatternType, String> findPattern(String patternName) throws GrokException {
        Map<PatternType, String> results = new HashMap<>();
        
        for (PatternType patternType : PatternType.values()) {
            try {
                // Skip if pattern file is not available
                if (!isPatternFileAvailable(patternType)) {
                    continue;
                }
                
                String pattern = getPattern(patternType, patternName);
                if (pattern != null) {
                    results.put(patternType, pattern);
                }
            } catch (GrokException e) {
                // Continue searching other pattern types
            }
        }
        
        return results;
    }
    
    /**
     * Get pattern statistics.
     * 
     * @return map of pattern types to pattern counts
     */
    public Map<PatternType, Integer> getPatternStatistics() {
        Map<PatternType, Integer> stats = new LinkedHashMap<>();
        
        for (PatternType patternType : PatternType.values()) {
            try {
                // Skip if pattern file is not available
                if (!isPatternFileAvailable(patternType)) {
                    stats.put(patternType, 0);
                    continue;
                }
                
                int count = loadPatterns(patternType).size();
                stats.put(patternType, count);
            } catch (GrokException e) {
                stats.put(patternType, 0);
            }
        }
        
        return stats;
    }
    
    /**
     * Clear pattern cache.
     */
    public void clearCache() {
        patternCache.clear();
        sampleCache.clear();
    }

    /**
     * Load sample logs for a specific pattern type.
     *
     * @param patternType the pattern type
     * @return map of pattern names to sample log strings (empty map if no samples found)
     */
    public Map<String, String> getSampleLogs(PatternType patternType) {
        Map<String, String> cached = sampleCache.get(patternType);
        if (cached != null) {
            return new HashMap<>(cached);
        }

        Map<String, String> samples = new HashMap<>();
        String resourcePath = "/samples/" + patternType.getFileName() + ".json";
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            sampleCache.put(patternType, samples);
            return samples;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            samples = parseSimpleJson(sb.toString());
        } catch (IOException e) {
            // Return empty map on error
        }

        sampleCache.put(patternType, samples);
        return new HashMap<>(samples);
    }

    /**
     * Parse a simple flat JSON object {"key":"value", ...} into a Map.
     * Handles escaped characters in values.
     */
    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return result;
        }

        // Remove outer braces
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) {
            return result;
        }

        int i = 0;
        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            // Expect opening quote for key
            if (json.charAt(i) != '"') break;
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // skip escaped char
                i++;
            }
            String key = unescapeJson(json.substring(keyStart, i));
            i++; // closing quote

            // Skip whitespace and colon
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ':')) i++;

            // Expect opening quote for value
            if (i >= json.length() || json.charAt(i) != '"') break;
            i++;
            int valueStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++; // skip escaped char
                i++;
            }
            String value = unescapeJson(json.substring(valueStart, i));
            i++; // closing quote

            result.put(key, value);

            // Skip whitespace and comma
            while (i < json.length() && (Character.isWhitespace(json.charAt(i)) || json.charAt(i) == ',')) i++;
        }

        return result;
    }

    /**
     * Unescape JSON string escape sequences.
     */
    private String unescapeJson(String s) {
        if (s == null || !s.contains("\\")) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i++; break;
                    case '\\': sb.append('\\'); i++; break;
                    case '/': sb.append('/'); i++; break;
                    case 'n': sb.append('\n'); i++; break;
                    case 't': sb.append('\t'); i++; break;
                    case 'r': sb.append('\r'); i++; break;
                    default: sb.append(c); break;
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Check if a pattern file exists and is accessible.
     * 
     * @param patternType the pattern type to check
     * @return true if the pattern file exists and is accessible
     */
    public boolean isPatternFileAvailable(PatternType patternType) {
        try (InputStream inputStream = getClass().getResourceAsStream(patternType.getResourcePath())) {
            return inputStream != null;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Get pattern content as raw text.
     * 
     * @param patternType the pattern type
     * @return raw content of the pattern file
     * @throws GrokException if pattern file cannot be loaded
     */
    public String getPatternFileContent(PatternType patternType) throws GrokException {
        return getPatternFileContent(patternType, StandardCharsets.UTF_8);
    }
    
    /**
     * Get pattern content as raw text with custom charset.
     * 
     * @param patternType the pattern type
     * @param charset the charset to use
     * @return raw content of the pattern file
     * @throws GrokException if pattern file cannot be loaded
     */
    public String getPatternFileContent(PatternType patternType, Charset charset) throws GrokException {
        InputStream inputStream = getClass().getResourceAsStream(patternType.getResourcePath());
        if (inputStream == null) {
            throw new GrokException("Pattern file not found: " + patternType.getResourcePath());
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new GrokException("Failed to read pattern file: " + patternType.getFileName(), e);
        }
    }
    
    /**
     * Load patterns from resource path.
     */
    private Map<String, String> loadPatternsFromResource(String resourcePath, Charset charset) throws IOException {
        Map<String, String> patterns = new HashMap<>();
        
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Pattern file not found: " + resourcePath);
        }
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse pattern definition: NAME regex
                String[] parts = line.split("\\s+", 2);
                if (parts.length == 2) {
                    patterns.put(parts[0], parts[1]);
                }
            }
        }
        
        return patterns;
    }
}