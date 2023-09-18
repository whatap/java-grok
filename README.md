# Grok

Java Grok is simple API that allows you to easily parse logs and other files (single line). With Java Grok, you can turn unstructured log and event data into structured data (JSON).

-----------------------

### What can I use Grok for?
* reporting errors and other patterns from logs and processes
* parsing complex text output and converting it to json for external processing
* apply 'write-once use-everywhere' to regular expressions
* automatically providing patterns for unknown text inputs (logs you want patterns generated for future matching)

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

### Usage ([Grok java documentation](http://grok.nflabs.com/javadoc))
Example of how to use java-grok:

```java
/* Create a new grokCompiler instance */
GrokCompiler grokCompiler = GrokCompiler.newInstance();
grokCompiler.registerDefaultPatterns();

/* Grok pattern to compile, here httpd logs */
final Grok grok = grokCompiler.compile("%{COMBINEDAPACHELOG}");

/* Line of log to match */
String log = "112.169.19.192 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22\"";

Match gm = grok.match(log);

/* Get the map with matches */
final Map<String, Object> capture = gm.capture();
```

### Build Java Grok

Java Grok support Gradle: `./gradlew assemble`

**Any contributions are warmly welcome**

Grok is inspired by the logstash inteceptor or filter available [here](http://logstash.net/docs/1.4.1/filters/grok)
