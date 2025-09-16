package io.whatap.grok.api;

/**
 * Enumeration of available Grok pattern files.
 * Each enum value represents a pattern file in the resources/patterns directory.
 * 
 * @since 1.0.1
 */
public enum PatternType {
    
    /**
     * AWS services patterns (S3, ELB, CloudFront, etc.).
     */
    AWS("aws", "AWS services log patterns"),
    
    /**
     * Bacula backup system patterns.
     */
    BACULA("bacula", "Bacula backup system log patterns"),
    
    /**
     * DNS BIND server patterns.
     */
    BIND("bind", "DNS BIND server log patterns"),
    
    /**
     * Bro/Zeek network security monitoring patterns.
     */
    BRO("bro", "Bro/Zeek network security monitoring patterns"),
    
    /**
     * Firewall log patterns.
     */
    FIREWALLS("firewalls", "Firewall log patterns"),
    
    /**
     * HAProxy load balancer patterns.
     */
    HAPROXY("haproxy", "HAProxy load balancer log patterns"),
    
    /**
     * Apache HTTP server patterns.
     */
    HTTPD("httpd", "Apache HTTP server log patterns"),
    
    /**
     * Java application patterns.
     */
    JAVA("java", "Java application log patterns"),
    
    /**
     * Juniper JunOS patterns.
     */
    JUNOS("junos", "Juniper JunOS log patterns"),
    
    /**
     * Linux system log patterns.
     */
    LINUX_SYSLOG("linux-syslog", "Linux system log patterns"),
    
    /**
     * MCollective orchestration patterns.
     */
    MCOLLECTIVE("mcollective", "MCollective orchestration patterns"),
    
    /**
     * MongoDB database patterns.
     */
    MONGODB("mongodb", "MongoDB database log patterns"),
    
    /**
     * Nagios monitoring patterns.
     */
    NAGIOS("nagios", "Nagios monitoring log patterns"),
    
    /**
     * Base patterns (core Grok patterns).
     */
    PATTERNS("patterns", "Base Grok patterns"),
    
    /**
     * Postfix mail server patterns.
     */
    POSTFIX("postfix", "Postfix mail server log patterns"),
    
    /**
     * PostgreSQL database patterns.
     */
    POSTGRESQL("postgresql", "PostgreSQL database log patterns"),
    
    /**
     * Ruby on Rails patterns.
     */
    RAILS("rails", "Ruby on Rails log patterns"),
    
    /**
     * Ruby logger patterns.
     */
    RUBY("ruby", "Ruby logger patterns");
    
    private final String fileName;
    private final String description;
    private final String resourcePath;
    
    PatternType(String fileName, String description) {
        this.fileName = fileName;
        this.description = description;
        this.resourcePath = "/patterns/" + fileName;
    }
    
    /**
     * Get the pattern file name.
     * 
     * @return the file name
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Get the pattern description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the resource path for this pattern file.
     * 
     * @return the resource path
     */
    public String getResourcePath() {
        return resourcePath;
    }
    
    /**
     * Find PatternType by file name.
     * 
     * @param fileName the file name to search for
     * @return the matching PatternType or null if not found
     */
    public static PatternType fromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        
        for (PatternType type : values()) {
            if (type.fileName.equals(fileName)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Check if this is the base patterns file.
     * 
     * @return true if this is the base patterns file
     */
    public boolean isBasePatterns() {
        return this == PATTERNS;
    }
    
    @Override
    public String toString() {
        return String.format("PatternType{name=%s, file=%s, description=%s}", 
            name(), fileName, description);
    }
}