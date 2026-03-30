package io.whatap.grok.api;

/**
 * Enumeration of available Grok pattern files.
 * Each enum value represents a pattern file in the resources/patterns directory.
 *
 * @since 1.0.1
 */
public enum PatternType {

    // ========================================
    // Web Servers
    // ========================================

    HTTPD("httpd", "Apache HTTP server log patterns", Category.WEB_SERVER),
    NGINX("nginx", "Nginx web server log patterns", Category.WEB_SERVER),
    HAPROXY("haproxy", "HAProxy load balancer log patterns", Category.WEB_SERVER),
    IIS("iis", "IIS web server log patterns", Category.WEB_SERVER),
    SQUID("squid", "Squid proxy server log patterns", Category.WEB_SERVER),

    // ========================================
    // Application Servers & Frameworks
    // ========================================

    JAVA("java", "Java application log patterns", Category.APPLICATION),
    SPRINGBOOT("springboot", "Spring Boot application log patterns", Category.APPLICATION),
    LOG4J("log4j", "Log4j/Logback log patterns", Category.APPLICATION),
    RAILS("rails", "Ruby on Rails log patterns", Category.APPLICATION),
    RUBY("ruby", "Ruby logger patterns", Category.APPLICATION),

    // ========================================
    // Databases
    // ========================================

    MONGODB("mongodb", "MongoDB database log patterns", Category.DATABASE),
    MYSQL("mysql", "MySQL/MariaDB database log patterns", Category.DATABASE),
    ORACLE("oracle", "Oracle Database log patterns", Category.DATABASE),
    POSTGRESQL("postgresql", "PostgreSQL database log patterns", Category.DATABASE),
    REDIS("redis", "Redis database log patterns", Category.DATABASE),

    // ========================================
    // Messaging & Search
    // ========================================

    KAFKA("kafka", "Apache Kafka log patterns", Category.MESSAGING),
    ELASTICSEARCH("elasticsearch", "Elasticsearch log patterns", Category.MESSAGING),

    // ========================================
    // System & Infrastructure
    // ========================================

    LINUX_SYSLOG("linux-syslog", "Linux system log patterns", Category.INFRASTRUCTURE),
    KUBERNETES("kubernetes", "Kubernetes log patterns", Category.INFRASTRUCTURE),
    DOCKER("docker", "Docker log patterns", Category.INFRASTRUCTURE),

    // ========================================
    // Network & Security
    // ========================================

    FIREWALLS("firewalls", "Firewall log patterns", Category.NETWORK),
    ZEEK("zeek", "Zeek network security monitoring patterns", Category.NETWORK),
    BRO("bro", "Bro/Zeek network security monitoring patterns", Category.NETWORK),
    BIND("bind", "DNS BIND server log patterns", Category.NETWORK),
    JUNOS("junos", "Juniper JunOS log patterns", Category.NETWORK),

    // ========================================
    // Cloud
    // ========================================

    AWS("aws", "AWS services log patterns", Category.CLOUD),

    // ========================================
    // Mail
    // ========================================

    POSTFIX("postfix", "Postfix mail server log patterns", Category.MAIL),
    EXIM("exim", "Exim mail transfer agent log patterns", Category.MAIL),

    // ========================================
    // Monitoring & Backup
    // ========================================

    NAGIOS("nagios", "Nagios monitoring log patterns", Category.MONITORING),
    BACULA("bacula", "Bacula backup system log patterns", Category.MONITORING),
    MCOLLECTIVE("mcollective", "MCollective orchestration patterns", Category.MONITORING),

    // ========================================
    // Build Tools
    // ========================================

    MAVEN("maven", "Apache Maven version patterns", Category.BUILD),

    // ========================================
    // Core (registered last to ensure base patterns take precedence)
    // ========================================

    PATTERNS("patterns", "Base Grok patterns", Category.CORE);

    /**
     * Pattern category enumeration.
     */
    public enum Category {
        CORE("Core"),
        WEB_SERVER("Web Servers"),
        APPLICATION("Application Servers & Frameworks"),
        DATABASE("Databases"),
        MESSAGING("Messaging & Search"),
        INFRASTRUCTURE("System & Infrastructure"),
        NETWORK("Network & Security"),
        CLOUD("Cloud"),
        MAIL("Mail"),
        MONITORING("Monitoring & Backup"),
        BUILD("Build Tools");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final String fileName;
    private final String description;
    private final String resourcePath;
    private final Category category;

    PatternType(String fileName, String description, Category category) {
        this.fileName = fileName;
        this.description = description;
        this.resourcePath = "/patterns/" + fileName;
        this.category = category;
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
     * Get the category of this pattern type.
     *
     * @return the category
     */
    public Category getCategory() {
        return category;
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
        return String.format("PatternType{name=%s, file=%s, category=%s, description=%s}",
            name(), fileName, category.getDisplayName(), description);
    }
}
