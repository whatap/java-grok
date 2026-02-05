# Java Grok

> **WhaTap Log Monitoring Grok Parser**
>
> This is a fork of [io.krakens:java-grok](https://github.com/thekrakken/java-grok), customized for [WhaTap](https://www.whatap.io) Log Monitoring service. It provides enhanced pattern support, ECS-style field names, and reserved keyword handling for WhaTap's internal processing.

Java Grok is a powerful API that allows you to easily parse logs and other files (single line). With Java Grok, you can turn unstructured log and event data into structured data (JSON).

## ✨ Features

- **🚀 High Performance**: Built with O(1) LRU caching and memory optimization
- **🔒 Security**: ReDoS protection with configurable input length limits
- **🧵 Thread-Safe**: Concurrent pattern compilation and matching
- **📦 Enum-based Pattern Management**: 23 categorized pattern types with 450+ patterns
- **🔍 Advanced Pattern Search**: Find patterns across multiple types and categories
- **📊 Pattern Statistics**: Get comprehensive insights about available patterns
- **🏷️ Type-safe Pattern Access**: Enum-based approach for better IDE support
- **🔄 ECS Field Support**: Supports Elastic Common Schema style field names like `[log][level]`
- **🔄 Backward Compatibility**: Drop-in replacement for io.krakens:java-grok

-----------------------

## ⚠️ Breaking Changes (v0.1.1)

### Reserved Field Name Changes for WhaTap Log Monitoring

WhaTap Log Monitoring uses certain field names as reserved keywords for internal processing. To avoid conflicts with these system fields, the following field names have been renamed in all built-in patterns:

| Original Field | New Field | Reason (WhaTap Reserved) |
|---------------|-----------|--------------------------|
| `timestamp` | `log_timestamp` | `AbstractPack.time` |
| `time` | `log_time` | `AbstractPack.time` |
| `message` | `log_message` | System reserved |
| `content` | `log_content` | `LogSinkPack.content` |
| `category` | `log_category` | `LogSinkPack.category` |
| `pcode` | `log_pcode` | `AbstractPack.pcode` |
| `logContent` | `log_body` | System reserved |

**Impact**: If you are using built-in patterns like `COMBINEDAPACHELOG`, `SYSLOG5424LINE`, `CATALINA_LOG`, etc., the extracted field names have changed. Update your code to use the new field names.

**Example Migration**:
```java
// Before (v0.1.0)
Map<String, Object> result = match.capture();
String timestamp = (String) result.get("timestamp");
String message = (String) result.get("message");

// After (v0.1.1)
Map<String, Object> result = match.capture();
String timestamp = (String) result.get("log_timestamp");
String message = (String) result.get("log_message");
```

**Note**: Custom field names defined in your own patterns (e.g., `%{TIMESTAMP_ISO8601:my_timestamp}`) are not affected by this change.

-----------------------

### What can I use Grok for?
* **Log Processing**: Parse Apache, Nginx, MongoDB, PostgreSQL, Redis, Zeek, and more log formats
* **Pattern Discovery**: Search and explore 450+ built-in patterns across 23 categories
* **JSON Conversion**: Transform unstructured text into structured JSON data
* **ECS Compliance**: Extract fields using Elastic Common Schema naming conventions
* **Error Reporting**: Extract specific patterns from logs and processes
* **Regular Expression Management**: Apply 'write-once use-everywhere' to regex patterns

### Maven repository

```maven
<!-- https://mvnrepository.com/artifact/io.github.whatap/java-grok -->
<dependency>
    <groupId>io.github.whatap</groupId>
    <artifactId>java-grok</artifactId>
    <version>0.1.1</version>
</dependency>
```

Or with gradle

```gradle
// https://mvnrepository.com/artifact/io.github.whatap/java-grok
implementation 'io.github.whatap:java-grok:0.1.1'
```

### What is different from io.krakens:java-grok

**Key Improvements:**

1. **ECS-Style Field Names**: Supports `[log][level]` style nested field names
2. **Thread Safety**: ConcurrentHashMap for pattern definitions
3. **O(1) LRU Cache**: Efficient cache eviction using LinkedHashMap
4. **ReDoS Protection**: Configurable input length limits
5. **More Patterns**: 23 pattern types (vs 18), 450+ patterns (vs 400+)
6. **Better Regex**: Improved pattern and subname validation

**Pattern Regex Differences:**

| Feature | io.krakens:java-grok | io.github.whatap |
|---------|---------------------|------------------|
| pattern | `[A-z0-9]+` | `[a-zA-Z][a-zA-Z0-9_\-\.]*[a-zA-Z0-9]` |
| subname | `[A-z0-9_:;,\-\/\s\.']+` | ECS-style `[field][name]` + legacy |

@See [GrokUtils.java](src/main/java/io/whatap/grok/api/GrokUtils.java)

## 🚀 Quick Start

### Basic Usage

```java
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Grok;
import io.whatap.grok.api.Match;

// Create a new grok compiler instance
GrokCompiler grokCompiler = GrokCompiler.newInstance();
grokCompiler.registerDefaultPatterns();

// Compile a grok pattern for Apache logs
final Grok grok = grokCompiler.compile("%{COMBINEDAPACHELOG}");

// Parse a log line
String log = "112.169.19.192 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22\"";

Match match = grok.match(log);
Map<String, Object> result = match.capture();
```

### 🏷️ Enum-based Pattern Management (New!)

```java
import io.whatap.grok.api.*;

// Get pattern management service
PatternManagementService service = new PatternManagementService();

// Get all available pattern types
List<PatternManagementService.PatternTypeInfo> types = service.getAllPatternTypes();

// Get patterns by category
Map<String, List<PatternManagementService.PatternTypeInfo>> categories = 
    service.getPatternTypesByCategory();

// Load specific pattern type
PatternManagementService.PatternTypeDetails mongoPatterns = 
    service.getPatternTypeDetails(PatternType.MONGODB);

// Search for specific patterns
List<PatternManagementService.PatternInfo> patterns = 
    service.searchPatterns("MONGO_QUERY");

// Get comprehensive statistics
PatternManagementService.PatternStatistics stats = service.getPatternStatistics();
System.out.println("Total patterns: " + stats.getTotalPatterns()); // 450+
```

### 📦 Using Specific Pattern Types

```java
// Register only specific pattern types
GrokCompiler compiler = GrokCompiler.newInstance();
compiler.registerPatterns(PatternType.MONGODB, PatternType.PATTERNS);

// Or register patterns from specific categories
PatternRepository repo = PatternRepository.getInstance();
Map<String, String> awsPatterns = repo.loadPatterns(PatternType.AWS);
compiler.register(awsPatterns);
```

### 🏷️ ECS-Style Field Names

Supports Elastic Common Schema style nested field names:

```java
GrokCompiler compiler = GrokCompiler.newInstance();
compiler.registerDefaultPatterns();

// Use ECS-style field names
Grok grok = compiler.compile("%{LOGLEVEL:[log][level]} %{IP:[source][ip]}");
Match match = grok.match("ERROR 192.168.1.1");
Map<String, Object> result = match.capture();

// Access nested fields
// result = {log.level=ERROR, source.ip=192.168.1.1}
```

## 📋 Available Pattern Types

| Category | Pattern Types | Description |
|----------|---------------|-------------|
| **Core** | PATTERNS | Base Grok patterns (IP, URI, NUMBER, etc.) |
| **Cloud & Infrastructure** | AWS, HAPROXY, HTTPD, SQUID | S3, ELB, CloudFront, load balancer, proxy logs |
| **Databases** | MONGODB, POSTGRESQL, REDIS | Database query and server logs |
| **System & Network** | LINUX_SYSLOG, FIREWALLS, BIND, JUNOS, BRO, ZEEK | Syslog, iptables, DNS, network security |
| **Applications** | JAVA, RAILS, RUBY, POSTFIX, EXIM | Application and mail server logs |
| **Monitoring & Backup** | NAGIOS, BACULA, MCOLLECTIVE | Monitoring and backup system logs |
| **Build Tools** | MAVEN | Version patterns for build tools |

**Total: 23 pattern types with 450+ patterns**

## 🔒 Security Features

### ReDoS Protection

Java Grok includes protection against Regular Expression Denial of Service (ReDoS) attacks:

```java
// Default: 1MB input limit
Grok grok = grokCompiler.compile("%{COMBINEDAPACHELOG}");

// Configure custom limit
Grok.setMaxInputLength(512 * 1024); // 512KB

// Disable limit (not recommended)
Grok.setMaxInputLength(0);
```

### Thread Safety

All pattern compilation and matching operations are thread-safe:

```java
// Safe for concurrent use
GrokCompiler compiler = GrokCompiler.newInstance();
compiler.registerDefaultPatterns();
Grok grok = compiler.compile("%{IP:client}");

// Can be used from multiple threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        Match match = grok.match("192.168.1.1");
        // ...
    });
}
```

## 🔧 Build & Test

```bash
# Build the project
./gradlew assemble

# Run tests
./gradlew test

# Run specific tests
./gradlew test --tests PatternManagementServiceTest
```

## 📖 Documentation

- [Pattern Management API Examples](src/test/java/io/whatap/grok/api/PatternUsageExamplesTest.java)
- [Performance Tests](src/test/java/io/whatap/grok/api/PerformanceTest.java)
- [Available Patterns](src/main/resources/patterns/)

## 🤝 Contributing

**Any contributions are warmly welcome!**

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Grok is inspired by the [Logstash Grok filter](http://logstash.net/docs/1.4.1/filters/grok) and builds upon the foundation of io.krakens:java-grok with significant enhancements.
