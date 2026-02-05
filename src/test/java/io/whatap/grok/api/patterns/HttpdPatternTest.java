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
 * Comprehensive test suite for HTTPD/Apache log patterns.
 *
 * Tests the COMMONAPACHELOG and COMBINEDAPACHELOG patterns which are used for parsing
 * Apache HTTP server access logs. These patterns handle:
 * - Common Log Format (CLF)
 * - Combined Log Format (includes referrer and user agent)
 *
 * Patterns tested:
 * - HTTPDUSER (email or username)
 * - COMMONAPACHELOG (basic apache access log)
 * - COMBINEDAPACHELOG (apache access log with referrer and user agent)
 *
 * @see https://httpd.apache.org/docs/current/logs.html
 */
public class HttpdPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
    }

    // ========================================
    // COMMONAPACHELOG Pattern Tests (CLF)
    // ========================================

    @Test
    public void testCommonApacheLog_basic() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "192.168.1.1 - john [10/Oct/2023:13:55:36 -0700] \"GET /index.html HTTP/1.1\" 200 1234";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());

        assertEquals("192.168.1.1", result.get("clientip"));
        assertEquals("john", result.get("auth"));
        assertEquals("GET", result.get("verb"));
        assertEquals("/index.html", result.get("request"));
        assertEquals("1.1", result.get("httpversion"));
        assertEquals("200", result.get("response"));
        assertEquals("1234", result.get("bytes"));
    }

    @Test
    public void testCommonApacheLog_withIdentity() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "10.0.0.1 user1 john [15/Nov/2023:08:30:15 +0000] \"POST /api/data HTTP/1.0\" 201 567";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("10.0.0.1", result.get("clientip"));
        assertEquals("user1", result.get("ident"));
        assertEquals("john", result.get("auth"));
        assertEquals("POST", result.get("verb"));
        assertEquals("/api/data", result.get("request"));
        assertEquals("1.0", result.get("httpversion"));
        assertEquals("201", result.get("response"));
        assertEquals("567", result.get("bytes"));
    }

    @Test
    public void testCommonApacheLog_withDashes() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "172.16.0.10 - - [20/Dec/2023:12:00:00 +0900] \"GET /test HTTP/1.1\" 404 -";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("172.16.0.10", result.get("clientip"));
        assertEquals("-", result.get("ident"));
        assertEquals("-", result.get("auth"));
        assertEquals("GET", result.get("verb"));
        assertEquals("/test", result.get("request"));
        assertEquals("404", result.get("response"));
        // When bytes is -, it won't be captured
    }

    @Test
    public void testCommonApacheLog_withHostname() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "example.com - admin [01/Jan/2024:00:00:00 +0000] \"DELETE /resource/123 HTTP/2.0\" 204 0";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("example.com", result.get("clientip"));
        assertEquals("admin", result.get("auth"));
        assertEquals("DELETE", result.get("verb"));
        assertEquals("/resource/123", result.get("request"));
        assertEquals("2.0", result.get("httpversion"));
        assertEquals("204", result.get("response"));
        assertEquals("0", result.get("bytes"));
    }

    @Test
    public void testCommonApacheLog_noHttpVersion() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "192.168.1.100 - user [25/Dec/2023:10:30:45 -0500] \"GET /path\" 200 2048";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("192.168.1.100", result.get("clientip"));
        assertEquals("GET", result.get("verb"));
        assertEquals("/path", result.get("request"));
        // HTTP version part is optional, so verb and request are still parsed
        assertNull(result.get("httpversion"));
        assertEquals("200", result.get("response"));
        assertEquals("2048", result.get("bytes"));
    }

    @Test
    public void testCommonApacheLog_invalidRequest() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "192.168.1.1 - - [10/Oct/2023:13:55:36 -0700] \"INVALID REQUEST LINE\" 400 0";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("192.168.1.1", result.get("clientip"));
        assertEquals("INVALID REQUEST LINE", result.get("rawrequest"));
        assertEquals("400", result.get("response"));
    }

    // ========================================
    // COMBINEDAPACHELOG Pattern Tests
    // ========================================

    @Test
    public void testCombinedApacheLog_full() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        String log = "192.168.1.1 - john [10/Oct/2023:13:55:36 -0700] \"GET /index.html HTTP/1.1\" 200 1234 \"http://example.com/\" \"Mozilla/5.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());

        assertEquals("192.168.1.1", result.get("clientip"));
        assertEquals("john", result.get("auth"));
        assertEquals("GET", result.get("verb"));
        assertEquals("/index.html", result.get("request"));
        assertEquals("1.1", result.get("httpversion"));
        assertEquals("200", result.get("response"));
        assertEquals("1234", result.get("bytes"));
        assertEquals("http://example.com/", result.get("referrer"));
        assertEquals("Mozilla/5.0", result.get("agent"));
    }

    @Test
    public void testCombinedApacheLog_fullUserAgent() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        String log = "112.169.19.192 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346 \"-\" " +
                "\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) " +
                "Chrome/25.0.1364.152 Safari/537.22\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("112.169.19.192", result.get("clientip"));
        assertEquals("GET", result.get("verb"));
        assertEquals("/", result.get("request"));
        assertEquals("200", result.get("response"));
        assertEquals("44346", result.get("bytes"));
        assertEquals("-", result.get("referrer"));
        assertTrue(result.get("agent").toString().contains("Mozilla/5.0"));
        assertTrue(result.get("agent").toString().contains("Chrome/25.0.1364.152"));
    }

    @Test
    public void testCombinedApacheLog_withReferrer() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        String log = "10.0.0.5 - alice [12/Nov/2023:14:22:10 +0000] \"GET /page.html HTTP/1.1\" 200 5000 " +
                "\"https://google.com/search\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("10.0.0.5", result.get("clientip"));
        assertEquals("alice", result.get("auth"));
        assertEquals("https://google.com/search", result.get("referrer"));
        assertTrue(result.get("agent").toString().contains("Mozilla/5.0"));
    }

    @Test
    public void testCombinedApacheLog_dashesForOptionalFields() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        String log = "192.168.100.1 - - [31/Dec/2023:23:59:59 -0800] \"HEAD /status HTTP/1.1\" 200 - \"-\" \"-\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("192.168.100.1", result.get("clientip"));
        assertEquals("HEAD", result.get("verb"));
        assertEquals("200", result.get("response"));
        assertEquals("-", result.get("referrer"));
        assertEquals("-", result.get("agent"));
    }

    // ========================================
    // Pattern Compilation Tests
    // ========================================

    @Test
    public void testAllPatternsCompileSuccessfully() throws GrokException {
        // Verify the main Apache patterns compile
        String[] patterns = {
            "COMMONAPACHELOG",
            "COMBINEDAPACHELOG"
        };

        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
            assertTrue("Pattern " + pattern + " should be in patterns map",
                grok.getPatterns().containsKey(pattern));
        }
    }

    @Test
    public void testPatternChaining() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        // COMMONAPACHELOG should reference base patterns
        Map<String, String> patterns = grok.getPatterns();
        assertTrue("Should contain IPORHOST pattern", patterns.containsKey("IPORHOST"));
        assertTrue("Should contain USER pattern", patterns.containsKey("USER"));
        assertTrue("Should contain HTTPDATE pattern", patterns.containsKey("HTTPDATE"));
        assertTrue("Should contain WORD pattern", patterns.containsKey("WORD"));
    }

    // ========================================
    // Edge Cases and Error Handling
    // ========================================

    @Test
    public void testCommonApacheLog_invalidFormatNoMatch() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String invalidLog = "This is not a valid apache log format";
        Match match = grok.match(invalidLog);
        Map<String, Object> result = match.capture();

        assertTrue("Invalid log should not match", result.isEmpty());
    }

    @Test
    public void testCombinedApacheLog_partialMatch() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        // Missing referrer and user agent - should not match COMBINEDLOG
        String log = "192.168.1.1 - john [10/Oct/2023:13:55:36 -0700] \"GET /index.html HTTP/1.1\" 200 1234";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue("Log without referrer and user agent should not match COMBINEDLOG", result.isEmpty());
    }

    @Test
    public void testCommonApacheLog_multipleRequestsInSameString() throws GrokException {
        // Grok should match the first occurrence
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "192.168.1.1 - - [10/Oct/2023:13:55:36 -0700] \"GET /page1 HTTP/1.1\" 200 100 " +
                    "192.168.1.2 - - [10/Oct/2023:13:55:37 -0700] \"GET /page2 HTTP/1.1\" 200 200";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Should match first log entry", result);
        assertFalse("Should not be empty", result.isEmpty());
        assertEquals("192.168.1.1", result.get("clientip"));
        assertEquals("/page1", result.get("request"));
    }

    @Test
    public void testCommonApacheLog_variousHttpMethods() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "TRACE", "CONNECT"};

        for (String method : methods) {
            String log = String.format("192.168.1.1 - user [10/Oct/2023:13:55:36 -0700] \"%s /test HTTP/1.1\" 200 100",
                method);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Method " + method + " should match", result);
            assertEquals("Method should be extracted correctly", method, result.get("verb"));
        }
    }

    @Test
    public void testCommonApacheLog_variousStatusCodes() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String[] statusCodes = {"200", "201", "204", "301", "302", "304", "400", "401", "403", "404", "500", "502", "503"};

        for (String code : statusCodes) {
            String log = String.format("192.168.1.1 - - [10/Oct/2023:13:55:36 -0700] \"GET /test HTTP/1.1\" %s 100",
                code);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Status code " + code + " should match", result);
            assertEquals("Status code should be extracted correctly", code, result.get("response"));
        }
    }

    @Test
    public void testCommonApacheLog_ipv6Address() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "2001:0db8:85a3:0000:0000:8a2e:0370:7334 - - [10/Oct/2023:13:55:36 -0700] \"GET /test HTTP/1.1\" 200 100";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("IPv6 address should match", result);
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", result.get("clientip"));
    }

    @Test
    public void testCommonApacheLog_queryStringInUrl() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        String log = "192.168.1.1 - - [10/Oct/2023:13:55:36 -0700] \"GET /search?q=test&page=1 HTTP/1.1\" 200 1000";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("URL with query string should match", result);
        assertEquals("/search?q=test&page=1", result.get("request"));
    }

    @Test
    public void testCombinedApacheLog_complexReferrer() throws GrokException {
        Grok grok = compiler.compile("%{COMBINEDAPACHELOG}");

        String log = "192.168.1.1 - - [10/Oct/2023:13:55:36 -0700] \"GET /page HTTP/1.1\" 200 1000 " +
                "\"https://www.google.com/search?q=apache+logs&oq=apache\" \"Mozilla/5.0\"";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Complex referrer should match", result);
        assertTrue(result.get("referrer").toString().contains("https://www.google.com/search?q=apache+logs"));
    }

    @Test
    public void testCommonApacheLog_timestampVariations() throws GrokException {
        Grok grok = compiler.compile("%{COMMONAPACHELOG}");

        // Test different timezone offsets
        String[] timestamps = {
            "[10/Oct/2023:13:55:36 +0000]",
            "[10/Oct/2023:13:55:36 -0700]",
            "[01/Jan/2024:00:00:00 +0900]",
            "[31/Dec/2023:23:59:59 -1200]"
        };

        for (String ts : timestamps) {
            String log = "192.168.1.1 - - " + ts + " \"GET /test HTTP/1.1\" 200 100";
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Timestamp " + ts + " should match", result);
            assertTrue("Should contain timestamp field", result.containsKey("log_timestamp"));
        }
    }
}
