# Java Grok

Java Grok is a powerful API that allows you to easily parse logs and other files (single line). With Java Grok, you can turn unstructured log and event data into structured data (JSON).

## ✨ Features

- **🚀 High Performance**: Built with caching and memory optimization
- **📦 Enum-based Pattern Management**: 18+ categorized pattern types with 400+ patterns
- **🔍 Advanced Pattern Search**: Find patterns across multiple types and categories  
- **📊 Pattern Statistics**: Get comprehensive insights about available patterns
- **🏷️ Type-safe Pattern Access**: Enum-based approach for better IDE support
- **🔄 Backward Compatibility**: Drop-in replacement for io.krakens:java-grok

-----------------------

### What can I use Grok for?
* **Log Processing**: Parse Apache, Nginx, MongoDB, PostgreSQL, and more log formats
* **Pattern Discovery**: Search and explore 400+ built-in patterns across 18 categories
* **JSON Conversion**: Transform unstructured text into structured JSON data
* **Error Reporting**: Extract specific patterns from logs and processes
* **Regular Expression Management**: Apply 'write-once use-everywhere' to regex patterns

### Maven repository

```maven
<!-- https://mvnrepository.com/artifact/io.github.whatap/java-grok -->
<dependency>
    <groupId>io.github.whatap</groupId>
    <artifactId>java-grok</artifactId>
    <version>0.0.2</version>
</dependency>
```

Or with gradle

```gradle
// https://mvnrepository.com/artifact/io.github.whatap/java-grok
implementation 'io.github.whatap:java-grok:0.0.2'

```

### What is different from io.krakens:java-grok

Added characters that can be used in regular expressions that make up Grok patterns.

- io.krakens:java-grok :
  - pattern : [A-z0-9]+
  - subname : [A-z0-9_:;,\-\/\s\.']+
- io.github.whatap :
  - pattern : [a-zA-Z][a-zA-Z0-9\_\-\.]*[a-zA-Z0-9]
  - subname : [a-zA-Z][a-zA-Z0-9_:;,\-\/\s\.']*[a-zA-Z0-9]
- @See [GrokUtils.java](https://github.com/thekrakken/java-grok/blob/901fda38ef6d5c902355eb25cff3f4b4fc3debde/src/main/java/io/krakens/grok/api/GrokUtils.java#L21C1-L33C20)
```
// io.krakens:java-grok
public static final Pattern GROK_PATTERN = Pattern.compile(
    "%\\{"
        + "(?<name>"
        + "(?<pattern>[A-z0-9]+)"
        + "(?::(?<subname>[A-z0-9_:;,\\-\\/\\s\\.']+))?"
        + ")"
        + "(?:=(?<definition>"
        + "(?:"
        + "(?:[^{}]+|\\.+)+"
        + ")+"
        + ")"
        + ")?"
        + "\\}");
```

```
// io.github.whatap
public static final Pattern GROK_PATTERN = Pattern.compile(
    "%\\{"
            + "(?<name>"
            + "(?<pattern>[a-zA-Z][a-zA-Z0-9\\_\\-\\.]*[a-zA-Z0-9])"
            + "(?::(?<subname>[a-zA-Z][a-zA-Z0-9_:;,\\-\\/\\s\\.']*[a-zA-Z0-9]))?"
            + ")"
            + "(?:=(?<definition>"
            + "(?:"
            + "(?:[^{}]+|\\.+)+"
            + ")+"
            + ")"
            + ")?"
            + "\\}");
```

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
System.out.println("Total patterns: " + stats.getTotalPatterns()); // 400+
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

## 📋 Available Pattern Types

| Category | Pattern Types | Pattern Count |
|----------|---------------|---------------|
| **Core** | PATTERNS | 73 |
| **Cloud & Infrastructure** | AWS, HAPROXY, HTTPD | 28 |
| **Databases** | MONGODB, POSTGRESQL | 8 |
| **System & Network** | LINUX_SYSLOG, FIREWALLS, BIND, JUNOS, BRO | 80 |
| **Applications** | JAVA, RAILS, RUBY, POSTFIX | 114 |
| **Monitoring & Backup** | NAGIOS, BACULA, MCOLLECTIVE | 111 |

**Total: 18 pattern types with 400+ patterns**

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
