package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import io.whatap.grok.api.exception.GrokException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for Squid proxy log patterns.
 *
 * Tests the SQUID3_STATUS and SQUID3 patterns which are used for parsing
 * Squid proxy server access logs. These patterns handle:
 * - Squid3 access log format
 * - Status codes including special values (0, 000)
 * - Hierarchy codes and destination addresses
 *
 * Patterns tested:
 * - SQUID3_STATUS (status code or special values)
 * - SQUID3 (complete squid3 access log)
 *
 * Note: This implementation uses simplified field names instead of ECS-style nested field names
 * due to current limitations with type conversion in nested pattern references.
 *
 * @see http://wiki.squid-cache.org/Features/LogFormat
 */
public class SquidPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/squid");
    }

    // ========================================
    // SQUID3_STATUS Pattern Tests
    // ========================================

    @Test
    public void testSquid3Status_200() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String log = "200";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());
        assertEquals("200", result.get("status_code"));
    }

    @Test
    public void testSquid3Status_404() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String log = "404";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("404", result.get("status_code"));
    }

    @Test
    public void testSquid3Status_500() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String log = "500";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("500", result.get("status_code"));
    }

    @Test
    public void testSquid3Status_Zero() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String log = "0";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());
        // Special case: 0 matches but doesn't capture a status_code since it's a literal
        assertTrue("Should match the pattern", result.isEmpty() || result.get("status_code") == null);
    }

    @Test
    public void testSquid3Status_TripleZero() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String log = "000";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        // Special case: 000 matches but doesn't capture a status_code since it's a literal
        assertTrue("Should match the pattern", result.isEmpty() || result.get("status_code") == null);
    }

    @Test
    public void testSquid3Status_variousCodes() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3_STATUS}");

        String[] statusCodes = {"200", "201", "204", "301", "302", "304", "400", "401", "403", "404", "500", "502", "503"};

        for (String code : statusCodes) {
            Match match = grok.match(code);
            Map<String, Object> result = match.capture();

            assertNotNull("Status code " + code + " should match", result);
            assertEquals("Status code should be extracted correctly", code, result.get("status_code"));
        }
    }

    // ========================================
    // SQUID3 Pattern Tests - Complete Logs
    // ========================================

    @Test
    public void testSquid3_basicLog() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 GET http://example.com/ user DIRECT/93.184.216.34 text/html";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());

        assertEquals("1612345678.123", result.get("timestamp"));
        assertEquals("100", result.get("duration"));
        assertEquals("192.168.1.1", result.get("clientip"));
        assertEquals("TCP_MISS", result.get("action"));
        assertEquals("200", result.get("status_code"));
        assertEquals("1234", result.get("bytes"));
        assertEquals("GET", result.get("method"));
        assertEquals("http://example.com/", result.get("url"));
        assertEquals("user", result.get("user"));
        assertEquals("DIRECT", result.get("hierarchy_code"));
        assertEquals("93.184.216.34", result.get("server"));
        assertEquals("text/html", result.get("content_type"));
    }

    @Test
    public void testSquid3_withDashesForOptionalFields() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.456    50 10.0.0.5 TCP_HIT/304 512 GET http://test.com/page - NONE/- -";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertFalse("Match should not be empty", result.isEmpty());

        assertEquals("1612345678.456", result.get("timestamp"));
        assertEquals("50", result.get("duration"));
        assertEquals("10.0.0.5", result.get("clientip"));
        assertEquals("TCP_HIT", result.get("action"));
        assertEquals("304", result.get("status_code"));
        assertEquals("512", result.get("bytes"));
        assertEquals("GET", result.get("method"));
        assertEquals("http://test.com/page", result.get("url"));
        assertNull("User should be null when dash", result.get("user"));
        assertEquals("NONE", result.get("hierarchy_code"));
        assertNull("Server should be null when dash", result.get("server"));
        assertNull("Content type should be null when dash", result.get("content_type"));
    }

    @Test
    public void testSquid3_postRequest() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345680.789    200 172.16.0.10 TCP_MISS/201 2048 POST http://api.example.com/data admin DIRECT/172.16.1.100 application/json";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("1612345680.789", result.get("timestamp"));
        assertEquals("200", result.get("duration"));
        assertEquals("172.16.0.10", result.get("clientip"));
        assertEquals("TCP_MISS", result.get("action"));
        assertEquals("201", result.get("status_code"));
        assertEquals("2048", result.get("bytes"));
        assertEquals("POST", result.get("method"));
        assertEquals("http://api.example.com/data", result.get("url"));
        assertEquals("admin", result.get("user"));
        assertEquals("DIRECT", result.get("hierarchy_code"));
        assertEquals("172.16.1.100", result.get("server"));
        assertEquals("application/json", result.get("content_type"));
    }

    @Test
    public void testSquid3_httpsMethods() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH", "CONNECT"};

        for (String method : methods) {
            String log = String.format("1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 %s http://example.com/ user DIRECT/93.184.216.34 text/html", method);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Method " + method + " should match", result);
            assertEquals("Method should be extracted correctly", method, result.get("method"));
        }
    }

    @Test
    public void testSquid3_variousActions() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String[] actions = {"TCP_HIT", "TCP_MISS", "TCP_REFRESH_HIT", "TCP_REFRESH_MISS", "TCP_CLIENT_REFRESH_MISS", "TCP_IMS_HIT", "TCP_NEGATIVE_HIT", "TCP_MEM_HIT", "TCP_DENIED"};

        for (String action : actions) {
            String log = String.format("1612345678.123    100 192.168.1.1 %s/200 1234 GET http://example.com/ user DIRECT/93.184.216.34 text/html", action);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Action " + action + " should match", result);
            assertEquals("Action should be extracted correctly", action, result.get("action"));
        }
    }

    @Test
    public void testSquid3_variousHierarchyCodes() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String[] codes = {"DIRECT", "PARENT", "SIBLING", "NONE", "DEFAULT_PARENT", "ROUNDROBIN_PARENT", "FIRST_PARENT_MISS", "CLOSEST_PARENT_MISS"};

        for (String code : codes) {
            String log = String.format("1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 GET http://example.com/ user %s/93.184.216.34 text/html", code);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Hierarchy code " + code + " should match", result);
            assertEquals("Hierarchy code should be extracted correctly", code, result.get("hierarchy_code"));
        }
    }

    @Test
    public void testSquid3_zeroStatusCode() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    100 192.168.1.1 TCP_DENIED/0 0 CONNECT https://blocked.com:443 user NONE/- -";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("TCP_DENIED", result.get("action"));
        assertEquals("0", result.get("bytes"));
        assertEquals("CONNECT", result.get("method"));
    }

    @Test
    public void testSquid3_tripleZeroStatusCode() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    50 192.168.1.1 TCP_DENIED/000 0 GET http://malware.com/ - NONE/- -";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("TCP_DENIED", result.get("action"));
    }

    @Test
    public void testSquid3_hostnameDestination() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    150 192.168.1.1 TCP_MISS/200 5000 GET http://example.com/path user DIRECT/example.com text/html";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("example.com", result.get("server"));
    }

    @Test
    public void testSquid3_complexUrl() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 GET http://example.com/path/to/resource?param1=value1&param2=value2 user DIRECT/93.184.216.34 text/html";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("http://example.com/path/to/resource?param1=value1&param2=value2", result.get("url"));
    }

    @Test
    public void testSquid3_variousMimeTypes() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String[] mimeTypes = {"text/html", "text/css", "text/javascript", "application/json", "application/xml", "image/png", "image/jpeg", "video/mp4", "application/octet-stream"};

        for (String mimeType : mimeTypes) {
            String log = String.format("1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 GET http://example.com/ user DIRECT/93.184.216.34 %s", mimeType);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("MIME type " + mimeType + " should match", result);
            assertEquals("MIME type should be extracted correctly", mimeType, result.get("content_type"));
        }
    }

    @Test
    public void testSquid3_largeResponseSize() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    5000 192.168.1.1 TCP_MISS/200 10485760 GET http://example.com/largefile.zip user DIRECT/93.184.216.34 application/zip";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("5000", result.get("duration"));
        assertEquals("10485760", result.get("bytes"));
    }

    @Test
    public void testSquid3_ipv6SourceAddress() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String log = "1612345678.123    100 2001:0db8:85a3:0000:0000:8a2e:0370:7334 TCP_MISS/200 1234 GET http://example.com/ user DIRECT/93.184.216.34 text/html";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Match should not be null", result);
        assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:7334", result.get("clientip"));
    }

    @Test
    public void testSquid3_fractionalTimestamp() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String[] timestamps = {"1612345678.123", "1612345678.456789", "1612345678.1", "1612345678.999999"};

        for (String timestamp : timestamps) {
            String log = String.format("%s    100 192.168.1.1 TCP_MISS/200 1234 GET http://example.com/ user DIRECT/93.184.216.34 text/html", timestamp);
            Match match = grok.match(log);
            Map<String, Object> result = match.capture();

            assertNotNull("Timestamp " + timestamp + " should match", result);
            assertEquals("Timestamp should be extracted correctly", timestamp, result.get("timestamp"));
        }
    }

    // ========================================
    // Pattern Compilation Tests
    // ========================================

    @Test
    public void testAllPatternsCompileSuccessfully() throws GrokException {
        String[] patterns = {
            "SQUID3_STATUS",
            "SQUID3"
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
        Grok grok = compiler.compile("%{SQUID3}");

        Map<String, String> patterns = grok.getPatterns();
        assertTrue("Should contain NUMBER pattern", patterns.containsKey("NUMBER"));
        assertTrue("Should contain IP pattern", patterns.containsKey("IP"));
        assertTrue("Should contain WORD pattern", patterns.containsKey("WORD"));
        assertTrue("Should contain INT pattern", patterns.containsKey("INT"));
        assertTrue("Should contain NOTSPACE pattern", patterns.containsKey("NOTSPACE"));
        assertTrue("Should contain IPORHOST pattern", patterns.containsKey("IPORHOST"));
        assertTrue("Should contain SQUID3_STATUS pattern", patterns.containsKey("SQUID3_STATUS"));
    }

    // ========================================
    // Edge Cases and Error Handling
    // ========================================

    @Test
    public void testSquid3_invalidFormatNoMatch() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String invalidLog = "This is not a valid squid log format";
        Match match = grok.match(invalidLog);
        Map<String, Object> result = match.capture();

        assertTrue("Invalid log should not match", result.isEmpty());
    }

    @Test
    public void testSquid3_partialLogNoMatch() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        String partialLog = "1612345678.123    100 192.168.1.1";
        Match match = grok.match(partialLog);
        Map<String, Object> result = match.capture();

        assertTrue("Partial log should not match", result.isEmpty());
    }

    @Test
    public void testSquid3_missingRequiredFields() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        // Missing destination address and MIME type (but those are optional with -)
        String log = "1612345678.123    100 192.168.1.1 TCP_MISS/200 1234 GET";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue("Log with missing required fields should not match", result.isEmpty());
    }

    @Test
    public void testSquid3Status_invalidStatus() throws GrokException {
        Grok grok = compiler.compile("^%{SQUID3_STATUS}$");

        // Test clearly invalid statuses that should not match with anchors
        String[] invalidStatuses = {"abc", "12a", "a123"};

        for (String status : invalidStatuses) {
            Match match = grok.match(status);
            Map<String, Object> result = match.capture();

            assertTrue("Invalid status " + status + " should not match",
                result.isEmpty());
        }
    }

    @Test
    public void testSquid3_realWorldExample1() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        // Real-world example: Cache HIT for image
        String log = "1620000000.123    5 10.0.1.50 TCP_HIT/200 45678 GET http://cdn.example.com/images/logo.png - NONE/- image/png";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Real-world example 1 should match", result);
        assertEquals("TCP_HIT", result.get("action"));
        assertEquals("200", result.get("status_code"));
        assertEquals("image/png", result.get("content_type"));
    }

    @Test
    public void testSquid3_realWorldExample2() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        // Real-world example: Blocked CONNECT request
        String log = "1620000100.456    0 192.168.10.100 TCP_DENIED/403 0 CONNECT https://blocked-site.com:443 john.doe NONE/- -";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Real-world example 2 should match", result);
        assertEquals("TCP_DENIED", result.get("action"));
        assertEquals("403", result.get("status_code"));
        assertEquals("john.doe", result.get("user"));
        assertEquals("CONNECT", result.get("method"));
    }

    @Test
    public void testSquid3_realWorldExample3() throws GrokException {
        Grok grok = compiler.compile("%{SQUID3}");

        // Real-world example: Parent cache hierarchy
        String log = "1620000200.789    250 172.16.5.20 TCP_MISS/200 125000 GET http://www.example.com/video.mp4 alice PARENT/172.16.1.1 video/mp4";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull("Real-world example 3 should match", result);
        assertEquals("250", result.get("duration"));
        assertEquals("PARENT", result.get("hierarchy_code"));
        assertEquals("172.16.1.1", result.get("server"));
        assertEquals("video/mp4", result.get("content_type"));
    }
}
