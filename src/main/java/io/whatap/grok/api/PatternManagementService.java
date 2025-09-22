package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing and accessing Grok patterns through enum-based pattern types.
 * Provides comprehensive APIs to retrieve pattern information, statistics, and content.
 * 
 * @since 1.0.1
 */
public class PatternManagementService {
    
    private final PatternRepository patternRepository;
    
    public PatternManagementService() {
        this.patternRepository = PatternRepository.getInstance();
    }
    
    /**
     * Get all available pattern types with their metadata.
     * 
     * @return list of pattern type information
     */
    public List<PatternTypeInfo> getAllPatternTypes() {
        return Arrays.stream(PatternType.values())
                .map(this::createPatternTypeInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * Get pattern types organized by categories.
     * 
     * @return map of categories to pattern type information
     */
    public Map<String, List<PatternTypeInfo>> getPatternTypesByCategory() {
        Map<String, List<PatternType>> categories = patternRepository.getPatternTypesByCategory();
        Map<String, List<PatternTypeInfo>> result = new LinkedHashMap<>();
        
        categories.forEach((category, types) -> {
            List<PatternTypeInfo> typeInfos = types.stream()
                    .map(this::createPatternTypeInfo)
                    .collect(Collectors.toList());
            result.put(category, typeInfos);
        });
        
        return result;
    }
    
    /**
     * Get all patterns from a specific pattern type.
     * 
     * @param patternType the pattern type
     * @return pattern information including names and definitions
     * @throws GrokException if pattern file cannot be loaded
     */
    public PatternTypeDetails getPatternTypeDetails(PatternType patternType) throws GrokException {
        Map<String, String> patterns = patternRepository.loadPatterns(patternType);
        return new PatternTypeDetails(
                createPatternTypeInfo(patternType),
                patterns
        );
    }
    
    /**
     * Get all patterns from a specific pattern type by name.
     * 
     * @param patternTypeName the pattern type name (enum name)
     * @return pattern information including names and definitions
     * @throws GrokException if pattern type not found or pattern file cannot be loaded
     */
    public PatternTypeDetails getPatternTypeDetails(String patternTypeName) throws GrokException {
        PatternType patternType = findPatternTypeByName(patternTypeName);
        return getPatternTypeDetails(patternType);
    }
    
    /**
     * Get specific pattern from a pattern type.
     * 
     * @param patternType the pattern type
     * @param patternName the pattern name
     * @return pattern information or null if not found
     * @throws GrokException if pattern file cannot be loaded
     */
    public PatternInfo getPattern(PatternType patternType, String patternName) throws GrokException {
        String pattern = patternRepository.getPattern(patternType, patternName);
        if (pattern == null) {
            return null;
        }
        
        return new PatternInfo(patternName, pattern, patternType);
    }
    
    /**
     * Search for patterns by name across all pattern types.
     * 
     * @param patternName the pattern name to search for
     * @return list of matching patterns from different types
     * @throws GrokException if pattern files cannot be loaded
     */
    public List<PatternInfo> searchPatterns(String patternName) throws GrokException {
        Map<PatternType, String> results = patternRepository.findPattern(patternName);
        
        return results.entrySet().stream()
                .map(entry -> new PatternInfo(patternName, entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get pattern statistics for all pattern types.
     * 
     * @return statistics showing pattern counts per type
     */
    public PatternStatistics getPatternStatistics() {
        Map<PatternType, Integer> stats = patternRepository.getPatternStatistics();
        
        int totalPatterns = stats.values().stream().mapToInt(Integer::intValue).sum();
        int availableTypes = (int) stats.values().stream().filter(count -> count > 0).count();
        
        Map<String, PatternTypeStats> typeStats = new LinkedHashMap<>();
        stats.forEach((type, count) -> {
            PatternTypeInfo typeInfo = createPatternTypeInfo(type);
            typeStats.put(type.name(), new PatternTypeStats(typeInfo, count));
        });
        
        return new PatternStatistics(totalPatterns, availableTypes, stats.size(), typeStats);
    }
    
    /**
     * Get raw content of a pattern file.
     * 
     * @param patternType the pattern type
     * @return raw file content
     * @throws GrokException if pattern file cannot be loaded
     */
    public String getPatternFileContent(PatternType patternType) throws GrokException {
        return patternRepository.getPatternFileContent(patternType);
    }
    
    /**
     * Check if a pattern file is available.
     * 
     * @param patternType the pattern type to check
     * @return true if available, false otherwise
     */
    public boolean isPatternFileAvailable(PatternType patternType) {
        return patternRepository.isPatternFileAvailable(patternType);
    }
    
    /**
     * Get all patterns from multiple pattern types merged together.
     * 
     * @param patternTypeNames array of pattern type names
     * @return merged patterns from all specified types
     * @throws GrokException if any pattern file cannot be loaded
     */
    public Map<String, String> getMergedPatterns(String... patternTypeNames) throws GrokException {
        PatternType[] types = Arrays.stream(patternTypeNames)
                .map(this::findPatternTypeByName)
                .toArray(PatternType[]::new);
        
        return patternRepository.loadPatterns(types);
    }
    
    /**
     * Get all available patterns from all types.
     * 
     * @return all patterns merged together
     * @throws GrokException if any pattern file cannot be loaded
     */
    public Map<String, String> getAllPatterns() throws GrokException {
        return patternRepository.loadAllPatterns();
    }
    
    private PatternType findPatternTypeByName(String name) throws GrokException {
        try {
            return PatternType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GrokException("Unknown pattern type: " + name);
        }
    }
    
    private PatternTypeInfo createPatternTypeInfo(PatternType type) {
        boolean available = patternRepository.isPatternFileAvailable(type);
        return new PatternTypeInfo(
                type.name(),
                type.getFileName(),
                type.getDescription(),
                type.getResourcePath(),
                available
        );
    }
    
    /**
     * Information about a pattern type.
     */
    public static class PatternTypeInfo {
        private final String name;
        private final String fileName;
        private final String description;
        private final String resourcePath;
        private final boolean available;
        
        public PatternTypeInfo(String name, String fileName, String description, String resourcePath, boolean available) {
            this.name = name;
            this.fileName = fileName;
            this.description = description;
            this.resourcePath = resourcePath;
            this.available = available;
        }
        
        public String getName() { return name; }
        public String getFileName() { return fileName; }
        public String getDescription() { return description; }
        public String getResourcePath() { return resourcePath; }
        public boolean isAvailable() { return available; }

        @Override
        public String toString() {
            return "PatternTypeInfo{" +
                    "name='" + name + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", description='" + description + '\'' +
                    ", resourcePath='" + resourcePath + '\'' +
                    ", available=" + available +
                    '}';
        }
    }
    
    /**
     * Detailed information about a pattern type including all its patterns.
     */
    public static class PatternTypeDetails {
        private final PatternTypeInfo typeInfo;
        private final Map<String, String> patterns;
        
        public PatternTypeDetails(PatternTypeInfo typeInfo, Map<String, String> patterns) {
            this.typeInfo = typeInfo;
            this.patterns = patterns;
        }
        
        public PatternTypeInfo getTypeInfo() { return typeInfo; }
        public Map<String, String> getPatterns() { return patterns; }
        public int getPatternCount() { return patterns.size(); }

        @Override
        public String toString() {
            return "PatternTypeDetails{" +
                    "typeInfo=" + typeInfo +
                    ", patterns=" + patterns +
                    '}';
        }
    }
    
    /**
     * Information about a specific pattern.
     */
    public static class PatternInfo {
        private final String name;
        private final String definition;
        private final PatternType type;
        
        public PatternInfo(String name, String definition, PatternType type) {
            this.name = name;
            this.definition = definition;
            this.type = type;
        }
        
        public String getName() { return name; }
        public String getDefinition() { return definition; }
        public PatternType getType() { return type; }
        public String getTypeName() { return type.name(); }

        @Override
        public String toString() {
            return "PatternInfo{" +
                    "name='" + name + '\'' +
                    ", definition='" + definition + '\'' +
                    ", type=" + type +
                    '}';
        }
    }
    
    /**
     * Statistics about patterns across all types.
     */
    public static class PatternStatistics {
        private final int totalPatterns;
        private final int availableTypes;
        private final int totalTypes;
        private final Map<String, PatternTypeStats> typeStatistics;
        
        public PatternStatistics(int totalPatterns, int availableTypes, int totalTypes, 
                               Map<String, PatternTypeStats> typeStatistics) {
            this.totalPatterns = totalPatterns;
            this.availableTypes = availableTypes;
            this.totalTypes = totalTypes;
            this.typeStatistics = typeStatistics;
        }
        
        public int getTotalPatterns() { return totalPatterns; }
        public int getAvailableTypes() { return availableTypes; }
        public int getTotalTypes() { return totalTypes; }
        public Map<String, PatternTypeStats> getTypeStatistics() { return typeStatistics; }

        @Override
        public String toString() {
            return "PatternStatistics{" +
                    "totalPatterns=" + totalPatterns +
                    ", availableTypes=" + availableTypes +
                    ", totalTypes=" + totalTypes +
                    ", typeStatistics=" + typeStatistics +
                    '}';
        }
    }
    
    /**
     * Statistics for a specific pattern type.
     */
    public static class PatternTypeStats {
        private final PatternTypeInfo typeInfo;
        private final int patternCount;
        
        public PatternTypeStats(PatternTypeInfo typeInfo, int patternCount) {
            this.typeInfo = typeInfo;
            this.patternCount = patternCount;
        }
        
        public PatternTypeInfo getTypeInfo() { return typeInfo; }
        public int getPatternCount() { return patternCount; }

        @Override
        public String toString() {
            return "PatternTypeStats{" +
                    "typeInfo=" + typeInfo +
                    ", patternCount=" + patternCount +
                    '}';
        }
    }
}