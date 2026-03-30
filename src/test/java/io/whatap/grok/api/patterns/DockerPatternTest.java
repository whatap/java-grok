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

public class DockerPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/docker").openStream());
    }

    @Test
    public void testDaemonLog() throws GrokException {
        Grok grok = compiler.compile("%{DOCKER_DAEMON}");

        String log = "time=\"2024-01-15T08:30:00.123456789Z\" level=info msg=\"Starting up\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("info", result.get("log.level"));
        assertEquals("Starting up", result.get("log_message"));
    }

    @Test
    public void testDaemonLog_withAttributes() throws GrokException {
        Grok grok = compiler.compile("%{DOCKER_DAEMON}");

        String log = "time=\"2024-01-15T08:30:00Z\" level=warning msg=\"Container failed\" container=abc123";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("warning", result.get("log.level"));
        assertEquals("Container failed", result.get("log_message"));
    }

    @Test
    public void testComposeLOg() throws GrokException {
        Grok grok = compiler.compile("%{DOCKER_COMPOSE}");

        String log = "web-1  | 2024-01-15 08:30:00 INFO Server started on port 8080";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("web-1", result.get("docker.container.name"));
        assertTrue(result.get("log_message").toString().contains("Server started"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{DOCKER_DAEMON}");

        String log = "not a docker log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"DOCKER_DAEMON", "DOCKER_COMPOSE"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
