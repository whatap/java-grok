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

public class IisPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/iis").openStream());
    }

    @Test
    public void testIisLog_basic() throws GrokException {
        Grok grok = compiler.compile("%{IIS_LOG}");

        String log = "2024-01-15 08:30:00 W3SVC1 192.168.1.10 GET /index.html - 80 - 10.0.0.1 Mozilla/5.0 - 200 0 0 125";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("W3SVC1", result.get("iis.site.name"));
        assertEquals("GET", result.get("http.request.method"));
        assertEquals("/index.html", result.get("url.original"));
        assertEquals(80, result.get("destination.port"));
        assertEquals("10.0.0.1", result.get("source.address"));
        assertEquals(200, result.get("http.response.status_code"));
    }

    @Test
    public void testIisLog_post() throws GrokException {
        Grok grok = compiler.compile("%{IIS_LOG}");

        String log = "2024-03-20 12:00:00 W3SVC2 10.0.0.5 POST /api/users q=test 443 admin 192.168.1.50 curl/7.88 https://example.com 201 0 0 340";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("POST", result.get("http.request.method"));
        assertEquals(443, result.get("destination.port"));
        assertEquals("admin", result.get("user.name"));
        assertEquals(201, result.get("http.response.status_code"));
    }

    @Test
    public void testIisLog_error() throws GrokException {
        Grok grok = compiler.compile("%{IIS_LOG}");

        String log = "2024-01-15 08:30:00 W3SVC1 192.168.1.10 GET /missing.html - 80 - 10.0.0.1 Mozilla/5.0 - 404 0 2 50";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals(404, result.get("http.response.status_code"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{IIS_LOG}");

        String log = "not an iis log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"IIS_LOG", "IIS_LOG_SIMPLE", "IIS_HTTPERR"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
