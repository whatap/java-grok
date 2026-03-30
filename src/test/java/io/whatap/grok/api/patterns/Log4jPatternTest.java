package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import io.whatap.grok.api.ResourceManager;
import io.whatap.grok.api.exception.GrokException;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class Log4jPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/java").openStream());
        compiler.register(Resources.getResource("patterns/log4j").openStream());
    }

    @Test
    public void testLog4j_info() throws GrokException {
        Grok grok = compiler.compile("%{LOG4J}");

        String log = "2024-01-15 08:30:00,123 [main] INFO  com.example.Application - Application started successfully";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("INFO", result.get("log.level"));
        assertEquals("main", result.get("process.thread.name"));
        assertEquals("com.example.Application", result.get("java.log.origin.class.name"));
        assertTrue(result.get("log_message").toString().contains("Application started"));
    }

    @Test
    public void testLog4j_error() throws GrokException {
        Grok grok = compiler.compile("%{LOG4J}");

        String log = "2024-03-20 12:00:00,456 [http-nio-8080-exec-1] ERROR com.example.controller.ApiController - Request failed";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("ERROR", result.get("log.level"));
        assertEquals("http-nio-8080-exec-1", result.get("process.thread.name"));
    }

    @Test
    public void testLogback() throws GrokException {
        Grok grok = compiler.compile("%{LOGBACK}");

        String log = "2024-01-15 08:30:00,123 INFO  [main] com.example.Application - Server started on port 8080";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("INFO", result.get("log.level"));
        assertEquals("main", result.get("process.thread.name"));
        assertTrue(result.get("log_message").toString().contains("Server started"));
    }

    @Test
    public void testLogback_debug() throws GrokException {
        Grok grok = compiler.compile("%{LOGBACK}");

        String log = "2024-01-15 08:30:00,789 DEBUG [scheduler-1] com.example.job.CleanupJob - Running cleanup task";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("DEBUG", result.get("log.level"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{LOG4J}");

        String log = "not a log4j log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"LOG4J", "LOG4J_MDC", "LOGBACK", "LOG4J2"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
