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

public class MysqlPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/mysql").openStream());
    }

    @Test
    public void testMysqlError_80() throws GrokException {
        Grok grok = compiler.compile("%{MYSQL_ERROR}");

        String log = "2024-01-15T08:30:00.123456Z 0 [Warning] [MY-010068] [Server] CA certificate ca.pem is self signed.";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Warning", result.get("log.level"));
        assertEquals("MY-010068", result.get("mysql.error.code"));
        assertEquals("Server", result.get("mysql.error.subsystem"));
        assertTrue(result.get("log_message").toString().contains("CA certificate"));
    }

    @Test
    public void testMysqlError_system() throws GrokException {
        Grok grok = compiler.compile("%{MYSQL_ERROR}");

        String log = "2024-03-20T12:00:00.000000Z 1 [ERROR] [MY-013183] [InnoDB] Assertion failure";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("ERROR", result.get("log.level"));
        assertEquals("InnoDB", result.get("mysql.error.subsystem"));
    }

    @Test
    public void testMysqlError_note() throws GrokException {
        Grok grok = compiler.compile("%{MYSQL_ERROR}");

        String log = "2024-01-15T08:30:00.000000Z 0 [Note] [MY-010747] [Server] Plugin 'FEDERATED' is disabled.";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("Note", result.get("log.level"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{MYSQL_ERROR}");

        String log = "not a mysql log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"MYSQL_ERROR", "MYSQL_ERROR_LEGACY"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
