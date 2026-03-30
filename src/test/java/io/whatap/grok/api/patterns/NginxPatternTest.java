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

/**
 * Test suite for Nginx log patterns.
 *
 * Tests NGINX_ACCESS (combined format) and NGINX_ERROR patterns.
 *
 * @see https://nginx.org/en/docs/http/ngx_http_log_module.html
 */
public class NginxPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/nginx").openStream());
    }

    // ========================================
    // NGINX_ACCESS Pattern Tests
    // ========================================

    @Test
    public void testNginxAccess_basic() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "192.168.1.1 - - [10/Oct/2023:13:55:36 +0900] \"GET /index.html HTTP/1.1\" 200 1234 \"-\" \"Mozilla/5.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("192.168.1.1", result.get("source.address"));
        assertEquals("GET", result.get("http.request.method"));
        assertEquals("/index.html", result.get("url.original"));
        assertEquals("1.1", result.get("http.version"));
        assertEquals(200, result.get("http.response.status_code"));
        assertEquals(1234, result.get("http.response.body.bytes"));
        assertNull("Referrer '-' should not be captured", result.get("http.request.referrer"));
        assertEquals("Mozilla/5.0", result.get("user_agent.original"));
    }

    @Test
    public void testNginxAccess_withUser() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "10.0.0.5 - alice [12/Nov/2023:14:22:10 +0000] \"POST /api/data HTTP/1.1\" 201 567 \"https://example.com\" \"curl/7.88.1\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("10.0.0.5", result.get("source.address"));
        assertEquals("alice", result.get("user.name"));
        assertEquals("POST", result.get("http.request.method"));
        assertEquals("/api/data", result.get("url.original"));
        assertEquals(201, result.get("http.response.status_code"));
        assertEquals(567, result.get("http.response.body.bytes"));
        assertEquals("https://example.com", result.get("http.request.referrer"));
        assertEquals("curl/7.88.1", result.get("user_agent.original"));
    }

    @Test
    public void testNginxAccess_withQueryString() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "172.16.0.10 - - [20/Dec/2023:12:00:00 +0900] \"GET /search?q=test&page=1 HTTP/1.1\" 200 5000 \"https://google.com\" \"Mozilla/5.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("/search?q=test&page=1", result.get("url.original"));
    }

    @Test
    public void testNginxAccess_404() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "192.168.1.100 - - [25/Dec/2023:10:30:45 -0500] \"GET /not-found HTTP/1.1\" 404 - \"-\" \"Mozilla/5.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals(404, result.get("http.response.status_code"));
    }

    @Test
    public void testNginxAccess_fullUserAgent() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "112.169.19.192 - - [06/Mar/2024:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" " +
                "\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("112.169.19.192", result.get("source.address"));
        assertTrue(result.get("user_agent.original").toString().contains("Chrome/120.0.0.0"));
    }

    @Test
    public void testNginxAccess_ipv6() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "2001:db8::1 - - [10/Oct/2023:13:55:36 +0000] \"GET /test HTTP/2.0\" 200 100 \"-\" \"curl/8.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("2001:db8::1", result.get("source.address"));
        assertEquals("2.0", result.get("http.version"));
    }

    @Test
    public void testNginxAccess_variousHttpMethods() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};

        for (String method : methods) {
            String log = String.format("192.168.1.1 - - [10/Oct/2023:13:55:36 +0900] \"%s /test HTTP/1.1\" 200 100 \"-\" \"test\"", method);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Method " + method + " should match", result);
            assertEquals(method, result.get("http.request.method"));
        }
    }

    @Test
    public void testNginxAccess_invalidLog() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ACCESS}");

        String log = "This is not a valid nginx access log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    // ========================================
    // NGINX_ERROR Pattern Tests
    // ========================================

    @Test
    public void testNginxError_basic() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ERROR}");

        String log = "2023/10/10 13:55:36 [error] 1234#0: *5678 open() \"/var/www/html/favicon.ico\" failed (2: No such file or directory), client: 192.168.1.1, server: example.com, request: \"GET /favicon.ico HTTP/1.1\", host: \"example.com\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("error", result.get("log.level"));
        assertEquals(1234, result.get("process.pid"));
        assertEquals(5678, result.get("nginx.error.connection_id"));
        assertTrue(result.get("log_message").toString().contains("client: 192.168.1.1"));
        assertTrue(result.get("log_message").toString().contains("server: example.com"));
    }

    @Test
    public void testNginxError_upstreamTimeout() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ERROR}");

        String log = "2024/01/15 08:30:00 [error] 5678#0: *12345 upstream timed out (110: Connection timed out) while reading response header from upstream, client: 10.0.0.1, server: api.example.com, request: \"POST /api/v1/data HTTP/1.1\", upstream: \"http://127.0.0.1:8080/api/v1/data\", host: \"api.example.com\", referrer: \"https://example.com/dashboard\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("error", result.get("log.level"));
        assertEquals(5678, result.get("process.pid"));
        assertEquals(12345, result.get("nginx.error.connection_id"));
        assertTrue(result.get("log_message").toString().contains("upstream timed out"));
        assertTrue(result.get("log_message").toString().contains("client: 10.0.0.1"));
    }

    @Test
    public void testNginxError_warn() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ERROR}");

        String log = "2023/12/25 00:00:00 [warn] 100#0: conflicting server name \"example.com\" on 0.0.0.0:80, ignored";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("warn", result.get("log.level"));
        assertEquals(100, result.get("process.pid"));
        assertTrue(result.get("log_message").toString().contains("conflicting server name"));
    }

    @Test
    public void testNginxError_crit() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ERROR}");

        String log = "2024/03/01 12:00:00 [crit] 999#0: *1 SSL_do_handshake() failed (SSL: error:1408F10B), client: 192.168.0.50, server: secure.example.com, request: \"GET / HTTP/1.1\", host: \"secure.example.com\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("crit", result.get("log.level"));
        assertEquals(999, result.get("process.pid"));
        assertTrue(result.get("log_message").toString().contains("SSL_do_handshake"));
    }

    @Test
    public void testNginxError_invalidLog() throws GrokException {
        Grok grok = compiler.compile("%{NGINX_ERROR}");

        String log = "This is not a valid nginx error log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    // ========================================
    // Pattern Compilation Tests
    // ========================================

    @Test
    public void testAllPatternsCompileSuccessfully() throws GrokException {
        String[] patterns = {"NGINX_ACCESS", "NGINX_ERROR"};

        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
