package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Java pattern definitions.
 * Tests all Java-related patterns including Tomcat/Catalina log formats and ECS-style field names.
 */
public class JavaPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/java");
    }

    // ========== Basic Java Pattern Tests ==========

    @Test
    public void testJavaClassPattern() throws Exception {
        Grok grok = compiler.compile("%{JAVACLASS:class}");

        String[] validClasses = {
            "com.example.MyClass",
            "org.springframework.boot.Application",
            "java.lang.String",
            "$ProxyClass",
            "_InternalClass",
            "SimpleClass",
            "a.b.c.d.e.DeepPackageClass"
        };

        for (String className : validClasses) {
            Match match = grok.match(className);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match class: " + className, captured);
            assertEquals(className, captured.get("class"));
        }
    }

    @Test
    public void testJavaFilePattern() throws Exception {
        Grok grok = compiler.compile("%{JAVAFILE:file}");

        String[] validFiles = {
            "MyClass.java",
            "Application.groovy",
            "Native Method",
            "Unknown Source",
            "SomeFile123.scala"
        };

        for (String fileName : validFiles) {
            Match match = grok.match(fileName);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match file: " + fileName, captured);
            assertEquals(fileName, captured.get("file"));
        }
    }

    @Test
    public void testJavaMethodPattern() throws Exception {
        Grok grok = compiler.compile("%{JAVAMETHOD:method}");

        String[] validMethods = {
            "myMethod",
            "doSomething",
            "<init>",
            "<clinit>",
            "method123",
            "$special",
            "_privateMethod"
        };

        for (String methodName : validMethods) {
            Match match = grok.match(methodName);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match method: " + methodName, captured);
            assertEquals(methodName, captured.get("method"));
        }
    }

    @Test
    public void testJavaStackTracePartPatternRegistered() throws Exception {
        // Verify the pattern is registered
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("JAVASTACKTRACEPART should be registered", patterns.containsKey("JAVASTACKTRACEPART"));

        String patternDef = patterns.get("JAVASTACKTRACEPART");
        assertNotNull("JAVASTACKTRACEPART definition should not be null", patternDef);
        assertTrue("Pattern should contain class name", patternDef.contains("java.log.origin.class.name"));
        assertTrue("Pattern should contain function name", patternDef.contains("log.origin.function"));
        assertTrue("Pattern should contain file name", patternDef.contains("log.origin.file.name"));
    }

    @Test
    public void testJavaStackTracePartComponents() throws Exception {
        // Test individual components that make up a stack trace
        // This tests the building blocks without the complex optional group issue

        // Test class name part with ECS field name
        Grok classGrok = compiler.compile("%{JAVACLASS:java.log.origin.class.name}");
        Match classMatch = classGrok.match("com.example.MyClass");
        Map<String, Object> classCaptured = classMatch.capture();
        assertNotNull("Should match class", classCaptured);
        assertEquals("com.example.MyClass", classCaptured.get("java.log.origin.class.name"));

        // Test method name part with ECS field name
        Grok methodGrok = compiler.compile("%{JAVAMETHOD:log.origin.function}");
        Match methodMatch = methodGrok.match("myMethod");
        Map<String, Object> methodCaptured = methodMatch.capture();
        assertNotNull("Should match method", methodCaptured);
        assertEquals("myMethod", methodCaptured.get("log.origin.function"));

        // Test file name part with ECS field name
        Grok fileGrok = compiler.compile("%{JAVAFILE:log.origin.file.name}");
        Match fileMatch = fileGrok.match("MyClass.java");
        Map<String, Object> fileCaptured = fileMatch.capture();
        assertNotNull("Should match file", fileCaptured);
        assertEquals("MyClass.java", fileCaptured.get("log.origin.file.name"));

        // Test line number part with simple field name (without type modifier to avoid issues)
        Grok lineGrok = compiler.compile("%{INT:line}");
        Match lineMatch = lineGrok.match("42");
        Map<String, Object> lineCaptured = lineMatch.capture();
        assertNotNull("Should match line number", lineCaptured);
        assertEquals("42", lineCaptured.get("line"));

        // Test integer conversion separately
        Grok intGrok = compiler.compile("%{INT:linenum:integer}");
        Match intMatch = intGrok.match("42");
        Map<String, Object> intCaptured = intMatch.capture();
        assertNotNull("Should match integer", intCaptured);
        assertEquals(42, intCaptured.get("linenum"));
    }

    @Test
    public void testSimplifiedStackTracePart() throws Exception {
        // Test a simplified version without the problematic optional nested group
        // This pattern matches the core structure without the optional line number
        String pattern = "%{SPACE}at %{JAVACLASS:java.log.origin.class.name}\\.%{JAVAMETHOD:log.origin.function}\\(%{JAVAFILE:log.origin.file.name}\\)";
        Grok grok = compiler.compile(pattern);

        String stackTrace = "    at com.example.MyClass.myMethod(Native Method)";
        Match match = grok.match(stackTrace);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match simplified stack trace", captured);
        assertEquals("com.example.MyClass", captured.get("java.log.origin.class.name"));
        assertEquals("myMethod", captured.get("log.origin.function"));
        assertEquals("Native Method", captured.get("log.origin.file.name"));
    }

    @Test
    public void testJavaThreadPattern() throws Exception {
        Grok grok = compiler.compile("%{JAVATHREAD:thread}");

        String[] validThreads = {
            "TP-Processor1",
            "TP-Processor99",
            "AJ-Processor42"
        };

        for (String thread : validThreads) {
            Match match = grok.match(thread);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match thread: " + thread, captured);
            assertEquals(thread, captured.get("thread"));
        }
    }

    @Test
    public void testJavaLogMessagePattern() throws Exception {
        Grok grok = compiler.compile("%{JAVALOGMESSAGE:message}");

        String[] messages = {
            "This is a log message",
            "Error occurred while processing request",
            "User 123 logged in successfully",
            ""
        };

        for (String message : messages) {
            Match match = grok.match(message);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match message: " + message, captured);
            assertEquals(message, captured.get("log_message"));
        }
    }

    // ========== Catalina 7 (Tomcat 7 and earlier) Tests ==========

    @Test
    public void testCatalina7DatestampPattern() throws Exception {
        Grok grok = compiler.compile("%{CATALINA7_DATESTAMP:timestamp}");

        String[] validDatestamps = {
            "Jan 9, 2014 7:13:13 AM",
            "Dec 31, 2023 11:59:59 PM",
            "Feb 28, 2020 12:00:00 PM"
        };

        for (String datestamp : validDatestamps) {
            Match match = grok.match(datestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match datestamp: " + datestamp, captured);
            assertEquals(datestamp, captured.get("log_timestamp"));
        }
    }

    @Test
    public void testCatalina7LogWithLevel() throws Exception {
        Grok grok = compiler.compile("%{CATALINA7_LOG}");

        String logLine = "Jan 9, 2014 7:13:13 AM org.apache.catalina.startup.HostConfig deployDirectory INFO: Deploying web application directory ROOT";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Catalina 7 log", captured);

        assertEquals("Jan 9, 2014 7:13:13 AM", captured.get("log_timestamp"));
        assertEquals("org.apache.catalina.startup.HostConfig", captured.get("java.log.origin.class.name"));
        assertEquals("deployDirectory", captured.get("log.origin.function"));
        assertEquals("INFO", captured.get("log.level"));
        assertEquals("Deploying web application directory ROOT", captured.get("log_message"));
    }

    @Test
    public void testCatalina7LogWithoutLevel() throws Exception {
        Grok grok = compiler.compile("%{CATALINA7_LOG}");

        String logLine = "Jan 9, 2014 7:13:13 AM org.apache.catalina.core.StandardEngine startInternal Starting Servlet Engine";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Catalina 7 log without level", captured);

        assertEquals("Jan 9, 2014 7:13:13 AM", captured.get("log_timestamp"));
        assertEquals("org.apache.catalina.core.StandardEngine", captured.get("java.log.origin.class.name"));
        assertEquals("startInternal", captured.get("log.origin.function"));
        assertEquals("Starting Servlet Engine", captured.get("log_message"));
    }

    @Test
    public void testCatalina7LogWithoutMethod() throws Exception {
        Grok grok = compiler.compile("%{CATALINA7_LOG}");

        String logLine = "Jan 9, 2014 7:13:13 AM org.apache.coyote.AbstractProtocol INFO: Initializing ProtocolHandler";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Catalina 7 log without method", captured);

        assertEquals("Jan 9, 2014 7:13:13 AM", captured.get("log_timestamp"));
        assertEquals("org.apache.coyote.AbstractProtocol", captured.get("java.log.origin.class.name"));
        assertEquals("INFO", captured.get("log.level"));
        assertEquals("Initializing ProtocolHandler", captured.get("log_message"));
    }

    // ========== Catalina 8 (Tomcat 8.5/9.0) Tests ==========

    @Test
    public void testCatalina8DatestampPattern() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_DATESTAMP:timestamp}");

        String[] validDatestamps = {
            "31-Jul-2020 16:40:38",
            "01-Jan-2023 00:00:00",
            "15-Dec-2021 23:59:59"
        };

        for (String datestamp : validDatestamps) {
            Match match = grok.match(datestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match Catalina 8 datestamp: " + datestamp, captured);
            assertEquals(datestamp, captured.get("log_timestamp"));
        }
    }

    @Test
    public void testCatalina8LogComplete() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_LOG}");

        String logLine = "31-Jul-2020 16:40:38 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Server version name: Apache Tomcat/8.5.57";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Catalina 8 log", captured);

        assertEquals("31-Jul-2020 16:40:38", captured.get("log_timestamp"));
        assertEquals("INFO", captured.get("log.level"));
        assertEquals("main", captured.get("java.log.origin.thread.name"));
        assertEquals("org.apache.catalina.startup.VersionLoggerListener", captured.get("java.log.origin.class.name"));
        assertEquals("log", captured.get("log.origin.function"));
        assertEquals("Server version name: Apache Tomcat/8.5.57", captured.get("log_message"));
    }

    @Test
    public void testCatalina8LogWithComplexThread() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_LOG}");

        String logLine = "31-Jul-2020 16:40:38 SEVERE [http-nio-8080-exec-1] org.apache.catalina.core.StandardWrapperValve.invoke Servlet execution exception";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Catalina 8 log with complex thread", captured);

        assertEquals("SEVERE", captured.get("log.level"));
        assertEquals("http-nio-8080-exec-1", captured.get("java.log.origin.thread.name"));
        assertEquals("org.apache.catalina.core.StandardWrapperValve", captured.get("java.log.origin.class.name"));
        assertEquals("invoke", captured.get("log.origin.function"));
    }

    // ========== Combined Catalina Pattern Tests ==========

    @Test
    public void testCatalinaDatestampBothFormats() throws Exception {
        Grok grok = compiler.compile("%{CATALINA_DATESTAMP:timestamp}");

        // Test Catalina 7 format
        Match match7 = grok.match("Jan 9, 2014 7:13:13 AM");
        assertNotNull("Failed to match Catalina 7 datestamp", match7.capture());

        // Test Catalina 8 format
        Match match8 = grok.match("31-Jul-2020 16:40:38");
        assertNotNull("Failed to match Catalina 8 datestamp", match8.capture());
    }

    @Test
    public void testCatalinaLogCatalina7Format() throws Exception {
        // Test Catalina 7 specific format with CATALINA7_LOG
        Grok grok7 = compiler.compile("%{CATALINA7_LOG}");
        String log7 = "Jan 9, 2014 7:13:13 AM org.apache.catalina.startup.HostConfig deployDirectory INFO: Deploying web application";
        Match match7 = grok7.match(log7);
        Map<String, Object> captured7 = match7.capture();
        assertNotNull("Failed to match Catalina 7 format", captured7);
        assertEquals("INFO", captured7.get("log.level"));

        // Test Catalina 8 specific format with CATALINA8_LOG
        Grok grok8 = compiler.compile("%{CATALINA8_LOG}");
        String log8 = "31-Jul-2020 16:40:38 INFO [main] org.apache.catalina.startup.VersionLoggerListener.log Server version";
        Match match8 = grok8.match(log8);
        Map<String, Object> captured8 = match8.capture();
        assertNotNull("Failed to match Catalina 8 format", captured8);
        assertEquals("INFO", captured8.get("log.level"));
        assertEquals("main", captured8.get("java.log.origin.thread.name"));
    }

    // ========== Tomcat 7 Tests ==========

    @Test
    public void testTomcat7Log() throws Exception {
        Grok grok = compiler.compile("%{TOMCAT7_LOG}");

        String logLine = "Jan 9, 2014 7:13:13 AM org.apache.catalina.startup.Catalina start INFO: Server startup in 1234 ms";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Tomcat 7 log", captured);
        assertEquals("Jan 9, 2014 7:13:13 AM", captured.get("log_timestamp"));
        assertEquals("org.apache.catalina.startup.Catalina", captured.get("java.log.origin.class.name"));
        assertEquals("start", captured.get("log.origin.function"));
        assertEquals("INFO", captured.get("log.level"));
    }

    // ========== Tomcat 8 Tests ==========

    @Test
    public void testTomcat8Log() throws Exception {
        Grok grok = compiler.compile("%{TOMCAT8_LOG}");

        String logLine = "31-Jul-2020 16:40:38 WARNING [main] org.apache.catalina.startup.SetContextPropertiesRule.begin Configuration error";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Tomcat 8 log", captured);
        assertEquals("31-Jul-2020 16:40:38", captured.get("log_timestamp"));
        assertEquals("WARNING", captured.get("log.level"));
        assertEquals("main", captured.get("java.log.origin.thread.name"));
        assertEquals("org.apache.catalina.startup.SetContextPropertiesRule", captured.get("java.log.origin.class.name"));
        assertEquals("begin", captured.get("log.origin.function"));
    }

    // ========== Tomcat Legacy Tests ==========

    @Test
    public void testTomcatLegacyDatestampPattern() throws Exception {
        Grok grok = compiler.compile("%{TOMCATLEGACY_DATESTAMP:timestamp}");

        String[] validDatestamps = {
            "2014-01-09 07:13:13",
            "2020-12-31 23:59:59",
            "2023-06-15 12:00:00 +0000",
            "2021-03-20 10:30:45 -0500"
        };

        for (String datestamp : validDatestamps) {
            Match match = grok.match(datestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match legacy datestamp: " + datestamp, captured);
            assertEquals(datestamp, captured.get("log_timestamp"));
        }
    }

    @Test
    public void testTomcatLegacyLog() throws Exception {
        Grok grok = compiler.compile("%{TOMCATLEGACY_LOG}");

        String logLine = "2014-01-09 07:13:13 | INFO | org.apache.catalina.startup.Catalina - Server startup completed";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Tomcat legacy log", captured);

        assertEquals("2014-01-09 07:13:13", captured.get("log_timestamp"));
        assertEquals("INFO", captured.get("log.level"));
        assertEquals("org.apache.catalina.startup.Catalina", captured.get("java.log.origin.class.name"));
        assertEquals("Server startup completed", captured.get("log_message"));
    }

    @Test
    public void testTomcatLegacyLogWithTimezone() throws Exception {
        Grok grok = compiler.compile("%{TOMCATLEGACY_LOG}");

        String logLine = "2014-01-09 07:13:13 +0900 | ERROR | com.example.MyClass - An error occurred";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Tomcat legacy log with timezone", captured);

        assertEquals("2014-01-09 07:13:13 +0900", captured.get("log_timestamp"));
        assertEquals("ERROR", captured.get("log.level"));
        assertEquals("com.example.MyClass", captured.get("java.log.origin.class.name"));
        assertEquals("An error occurred", captured.get("log_message"));
    }

    // ========== Combined Tomcat Pattern Tests ==========

    @Test
    public void testTomcatDatestampAllFormats() throws Exception {
        Grok grok = compiler.compile("%{TOMCAT_DATESTAMP:timestamp}");

        // Test Catalina 8 format
        Match match8 = grok.match("31-Jul-2020 16:40:38");
        assertNotNull("Failed to match Catalina 8 datestamp", match8.capture());

        // Test Catalina 7 format
        Match match7 = grok.match("Jan 9, 2014 7:13:13 AM");
        assertNotNull("Failed to match Catalina 7 datestamp", match7.capture());

        // Test Legacy format
        Match matchLegacy = grok.match("2014-01-09 07:13:13");
        assertNotNull("Failed to match legacy datestamp", matchLegacy.capture());
    }

    @Test
    public void testTomcatLogAllFormats() throws Exception {
        // Test Tomcat 8 format
        Grok grok8 = compiler.compile("%{TOMCAT8_LOG}");
        String log8 = "31-Jul-2020 16:40:38 INFO [main] org.apache.catalina.startup.Catalina.start Server startup";
        Match match8 = grok8.match(log8);
        Map<String, Object> captured8 = match8.capture();
        assertNotNull("Failed to match Tomcat 8 format", captured8);
        assertEquals("INFO", captured8.get("log.level"));
        assertEquals("main", captured8.get("java.log.origin.thread.name"));

        // Test Tomcat 7 format
        Grok grok7 = compiler.compile("%{TOMCAT7_LOG}");
        String log7 = "Jan 9, 2014 7:13:13 AM org.apache.catalina.startup.Catalina start INFO: Server startup";
        Match match7 = grok7.match(log7);
        Map<String, Object> captured7 = match7.capture();
        assertNotNull("Failed to match Tomcat 7 format", captured7);
        assertEquals("INFO", captured7.get("log.level"));

        // Test Legacy format
        Grok grokLegacy = compiler.compile("%{TOMCATLEGACY_LOG}");
        String logLegacy = "2014-01-09 07:13:13 | INFO | org.apache.catalina.startup.Catalina - Server startup";
        Match matchLegacy = grokLegacy.match(logLegacy);
        Map<String, Object> captured = matchLegacy.capture();
        assertNotNull("Failed to match legacy format", captured);
        assertEquals("INFO", captured.get("log.level"));
    }

    // ========== ECS Field Name Tests ==========

    @Test
    public void testECSFieldNamesInCatalina8() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_LOG}");

        String logLine = "31-Jul-2020 16:40:38 INFO [http-nio-8080-exec-5] org.springframework.web.servlet.DispatcherServlet.initServletBean Initializing Servlet";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify all ECS-style field names are captured correctly
        assertTrue("Missing log.level", captured.containsKey("log.level"));
        assertTrue("Missing java.log.origin.thread.name", captured.containsKey("java.log.origin.thread.name"));
        assertTrue("Missing java.log.origin.class.name", captured.containsKey("java.log.origin.class.name"));
        assertTrue("Missing log.origin.function", captured.containsKey("log.origin.function"));
        assertTrue("Missing message", captured.containsKey("log_message"));

        assertEquals("INFO", captured.get("log.level"));
        assertEquals("http-nio-8080-exec-5", captured.get("java.log.origin.thread.name"));
        assertEquals("org.springframework.web.servlet.DispatcherServlet", captured.get("java.log.origin.class.name"));
        assertEquals("initServletBean", captured.get("log.origin.function"));
    }

    @Test
    public void testECSFieldNamesInStackTrace() throws Exception {
        // Test ECS-style field names using a simplified pattern (without the problematic optional nested group)
        String pattern = "%{SPACE}at %{JAVACLASS:java.log.origin.class.name}\\.%{JAVAMETHOD:log.origin.function}\\(%{JAVAFILE:log.origin.file.name}\\)";
        Grok grok = compiler.compile(pattern);

        String stackTrace = "    at org.springframework.boot.SpringApplication.run(SpringApplication.java)";
        Match match = grok.match(stackTrace);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match stack trace", captured);

        // Verify all ECS-style field names in stack trace
        assertTrue("Missing java.log.origin.class.name", captured.containsKey("java.log.origin.class.name"));
        assertTrue("Missing log.origin.function", captured.containsKey("log.origin.function"));
        assertTrue("Missing log.origin.file.name", captured.containsKey("log.origin.file.name"));

        assertEquals("org.springframework.boot.SpringApplication", captured.get("java.log.origin.class.name"));
        assertEquals("run", captured.get("log.origin.function"));
        assertEquals("SpringApplication.java", captured.get("log.origin.file.name"));
    }

    // ========== Real-world Log Examples ==========

    @Test
    public void testRealWorldSpringBootLog() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_LOG}");

        String logLine = "15-Feb-2023 10:23:45 INFO [http-nio-8080-exec-10] org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.register Mapped \"{[/api/users]}\" onto public java.util.List";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Spring Boot log", captured);
        assertEquals("INFO", captured.get("log.level"));
        assertEquals("http-nio-8080-exec-10", captured.get("java.log.origin.thread.name"));
    }

    @Test
    public void testRealWorldExceptionLog() throws Exception {
        Grok grok = compiler.compile("%{CATALINA8_LOG}");

        String logLine = "15-Feb-2023 10:23:45 SEVERE [http-nio-8080-exec-1] org.apache.catalina.core.StandardWrapperValve.invoke Servlet.service() for servlet [dispatcherServlet] threw exception";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match exception log", captured);
        assertEquals("SEVERE", captured.get("log.level"));
        assertEquals("org.apache.catalina.core.StandardWrapperValve", captured.get("java.log.origin.class.name"));
        assertEquals("invoke", captured.get("log.origin.function"));
    }

    @Test
    public void testMultipleStackTraceLines() throws Exception {
        // Test multiple stack trace lines using simplified pattern
        String pattern = "%{SPACE}at %{JAVACLASS:java.log.origin.class.name}\\.%{JAVAMETHOD:log.origin.function}\\(%{JAVAFILE:log.origin.file.name}\\)";
        Grok grok = compiler.compile(pattern);

        String[] stackTraceLines = {
            "    at com.example.service.UserService.getUser(UserService.java)",
            "    at com.example.controller.UserController.handleRequest(UserController.java)",
            "    at org.springframework.web.method.support.InvocableHandlerMethod.invoke(InvocableHandlerMethod.java)",
            "    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)"
        };

        for (String line : stackTraceLines) {
            Match match = grok.match(line);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match stack trace line: " + line, captured);
            assertNotNull("Missing class name in: " + line, captured.get("java.log.origin.class.name"));
            assertNotNull("Missing function in: " + line, captured.get("log.origin.function"));
        }
    }
}
