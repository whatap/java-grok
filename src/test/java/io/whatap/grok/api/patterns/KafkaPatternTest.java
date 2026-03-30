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

public class KafkaPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/java").openStream());
        compiler.register(Resources.getResource("patterns/kafka").openStream());
    }

    @Test
    public void testKafkaLog_info() throws GrokException {
        Grok grok = compiler.compile("%{KAFKA_LOG}");

        String log = "[2024-01-15 08:30:00,123] INFO [KafkaServer id=0] started (kafka.server.KafkaServer)";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("INFO", result.get("log.level"));
        assertTrue(result.get("log_message").toString().contains("KafkaServer id=0"));
        assertEquals("kafka.server.KafkaServer", result.get("java.log.origin.class.name"));
    }

    @Test
    public void testKafkaLog_error() throws GrokException {
        Grok grok = compiler.compile("%{KAFKA_LOG}");

        String log = "[2024-03-20 12:00:00,456] ERROR Processor got uncaught exception (kafka.network.Processor)";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("ERROR", result.get("log.level"));
        assertEquals("kafka.network.Processor", result.get("java.log.origin.class.name"));
    }

    @Test
    public void testKafkaControllerLog() throws GrokException {
        Grok grok = compiler.compile("%{KAFKA_CONTROLLER}");

        String log = "[2024-01-15 08:30:00,789] INFO [Controller id=0]: Starting controller (kafka.controller.KafkaController)";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("INFO", result.get("log.level"));
        assertEquals("Controller id=0", result.get("kafka.log.component"));
        assertTrue(result.get("log_message").toString().contains("Starting controller"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{KAFKA_LOG}");

        String log = "not a kafka log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"KAFKA_LOG", "KAFKA_CONTROLLER"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
