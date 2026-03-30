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

public class SpringBootPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/java").openStream());
        compiler.register(Resources.getResource("patterns/springboot").openStream());
    }

    @Test
    public void testSpringBootLog_info() throws GrokException {
        Grok grok = compiler.compile("%{SPRINGBOOT_LOG}");

        String log = "2024-01-15 08:30:00.123  INFO 12345 --- [           main] o.s.b.w.e.t.TomcatWebServer             : Tomcat started on port(s): 8080 (http)";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("INFO", result.get("log.level"));
        assertEquals(12345, result.get("process.pid"));
        assertTrue(result.get("log_message").toString().contains("Tomcat started"));
    }

    @Test
    public void testSpringBootLog_error() throws GrokException {
        Grok grok = compiler.compile("%{SPRINGBOOT_LOG}");

        String log = "2024-03-20 12:00:00.456 ERROR 9876 --- [nio-8080-exec-1] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() threw exception";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("ERROR", result.get("log.level"));
        assertEquals(9876, result.get("process.pid"));
    }

    @Test
    public void testSpringBootLog_warn() throws GrokException {
        Grok grok = compiler.compile("%{SPRINGBOOT_LOG}");

        String log = "2024-01-15 08:30:00.789  WARN 5555 --- [  scheduling-1] c.e.d.service.CacheService               : Cache miss for key: user-123";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("WARN", result.get("log.level"));
        assertTrue(result.get("log_message").toString().contains("Cache miss"));
    }

    @Test
    public void testSpringBootLog_debug() throws GrokException {
        Grok grok = compiler.compile("%{SPRINGBOOT_LOG}");

        String log = "2024-01-15 08:30:00.000 DEBUG 1234 --- [           main] o.s.c.a.AnnotationConfigApplicationContext : Refreshing context";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("DEBUG", result.get("log.level"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{SPRINGBOOT_LOG}");

        String log = "not a spring boot log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"SPRINGBOOT_LOG", "SPRINGBOOT_STARTUP", "SPRINGBOOT_ACTUATOR"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
