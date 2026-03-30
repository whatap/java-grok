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

public class OraclePatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/oracle").openStream());
    }

    @Test
    public void testOracleListener() throws GrokException {
        Grok grok = compiler.compile("%{ORACLE_LISTENER}");

        String log = "2024-01-15T08:30:00.123456+09:00 * (CONNECT_DATA=(SERVICE_NAME=ORCL)) * (ADDRESS=(PROTOCOL=tcp)(HOST=192.168.1.10)(PORT=1521)) * establish * ORCL * 0";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("ORCL", result.get("oracle.listener.service_name"));
        assertEquals(0, result.get("oracle.listener.status"));
    }

    @Test
    public void testOracleListener_error() throws GrokException {
        Grok grok = compiler.compile("%{ORACLE_LISTENER}");

        String log = "2024-03-20T12:00:00.000000+00:00 * (CONNECT_DATA=(SID=PROD)) * (ADDRESS=(PROTOCOL=tcp)(HOST=10.0.0.1)(PORT=1521)) * establish * PROD * 12514";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("PROD", result.get("oracle.listener.service_name"));
        assertEquals(12514, result.get("oracle.listener.status"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{ORACLE_LISTENER}");

        String log = "not an oracle log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"ORACLE_LISTENER", "ORACLE_ALERT_LEGACY"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
