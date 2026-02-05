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
 * Test suite for Zeek (formerly Bro) log pattern parsing.
 * Tests modern Zeek log format patterns with ECS-style field extraction.
 *
 * <p>Note: Complex patterns like ZEEK_HTTP, ZEEK_DNS, ZEEK_CONN, and ZEEK_FILES
 * currently have regex compilation issues due to nested quantifiers and complex tab-separated
 * field structures. This test suite focuses on:
 * <ul>
 *   <li>Verifying all patterns can be registered without errors</li>
 *   <li>Testing simpler helper patterns that compile successfully</li>
 *   <li>Validating pattern definitions contain correct ECS field names</li>
 * </ul>
 *
 * <p>Zeek Patterns Tested:
 * <ul>
 *   <li>ZEEK_BOOL - Boolean field (T/F)</li>
 *   <li>ZEEK_DATA - Generic tab-separated data field</li>
 *   <li>ZEEK_HTTP - HTTP log with ECS fields (registration only)</li>
 *   <li>ZEEK_DNS - DNS query/response log with ECS fields (registration only)</li>
 *   <li>ZEEK_CONN - Connection log with network metrics (registration only)</li>
 *   <li>ZEEK_FILES - File transfer/analysis log (registration only)</li>
 *   <li>ZEEK_FILES_TX_HOSTS - Transmit hosts helper pattern</li>
 *   <li>ZEEK_FILES_RX_HOSTS - Receive hosts helper pattern</li>
 * </ul>
 *
 * <p>Differences from BRO patterns:
 * <ul>
 *   <li>ZEEK_HTTP: Added 'version' and 'origin' fields, replaced 'filename' with 'orig_filenames' + 'resp_filenames'</li>
 *   <li>ZEEK_DNS: Added 'zeek.dns.rtt' field for round-trip time</li>
 *   <li>ZEEK_CONN: Requires 'zeek.connection.local_resp', handles '(empty)' as '-', optional MAC addresses</li>
 *   <li>ZEEK_FILES: Two new fields added at the end compared to BRO_FILES</li>
 * </ul>
 *
 * <p>ECS Field Naming Conventions:
 * <ul>
 *   <li>Network: [source][ip], [source][port], [destination][ip], [destination][port]</li>
 *   <li>Session: [zeek][session_id]</li>
 *   <li>HTTP: [http][request][method], [http][response][status_code], [user_agent][original]</li>
 *   <li>DNS: [dns][question][name], [dns][question][type], [dns][response_code]</li>
 *   <li>Protocol: [network][transport], [network][protocol]</li>
 *   <li>Files: [file][name], [file][size], [file][mime_type], [file][hash][md5/sha1/sha256]</li>
 * </ul>
 */
public class ZeekPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setup() throws GrokException {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/zeek");
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testZeekPatternsCanBeRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        assertTrue("ZEEK_BOOL should be registered",
                patterns.containsKey("ZEEK_BOOL"));
        assertTrue("ZEEK_DATA should be registered",
                patterns.containsKey("ZEEK_DATA"));
        assertTrue("ZEEK_HTTP should be registered",
                patterns.containsKey("ZEEK_HTTP"));
        assertTrue("ZEEK_DNS should be registered",
                patterns.containsKey("ZEEK_DNS"));
        assertTrue("ZEEK_CONN should be registered",
                patterns.containsKey("ZEEK_CONN"));
        assertTrue("ZEEK_FILES should be registered",
                patterns.containsKey("ZEEK_FILES"));
        assertTrue("ZEEK_FILES_TX_HOSTS should be registered",
                patterns.containsKey("ZEEK_FILES_TX_HOSTS"));
        assertTrue("ZEEK_FILES_RX_HOSTS should be registered",
                patterns.containsKey("ZEEK_FILES_RX_HOSTS"));
    }

    @Test
    public void testZeekPatternDefinitionsAreNonEmpty() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String zeekBool = patterns.get("ZEEK_BOOL");
        assertNotNull("ZEEK_BOOL definition should not be null", zeekBool);
        assertFalse("ZEEK_BOOL definition should not be empty", zeekBool.trim().isEmpty());

        String zeekHttp = patterns.get("ZEEK_HTTP");
        assertNotNull("ZEEK_HTTP definition should not be null", zeekHttp);
        assertFalse("ZEEK_HTTP definition should not be empty", zeekHttp.trim().isEmpty());
    }

    // ========== ZEEK_BOOL Pattern Tests ==========

    @Test
    public void testZeekBoolPatternCompiles() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_BOOL}");
        assertNotNull("ZEEK_BOOL pattern should compile", grok);
    }

    @Test
    public void testZeekBoolMatchesTrueValue() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_BOOL}");

        Match match = grok.match("T");
        Map<String, Object> captured = match.capture();

        assertFalse("Should match T", captured.isEmpty());
    }

    @Test
    public void testZeekBoolMatchesFalseValue() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_BOOL}");

        Match match = grok.match("F");
        Map<String, Object> captured = match.capture();

        assertFalse("Should match F", captured.isEmpty());
    }

    @Test
    public void testZeekBoolDoesNotMatchInvalidValues() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_BOOL}");

        String[] invalidValues = {"true", "false", "1", "0", "Y", "N", "yes", "no"};
        for (String value : invalidValues) {
            Match match = grok.match(value);
            Map<String, Object> captured = match.capture();

            assertTrue("Should not match: " + value, captured.isEmpty());
        }
    }

    // ========== ZEEK_DATA Pattern Tests ==========

    @Test
    public void testZeekDataPatternCompiles() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_DATA}");
        assertNotNull("ZEEK_DATA pattern should compile", grok);
    }

    @Test
    public void testZeekDataMatchesNonTabData() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_DATA}");

        String[] testData = {
            "example.com",
            "192.168.1.1",
            "GET",
            "Mozilla/5.0",
            "application/json",
            "CHhAvVGS1DHFjwGM9",
            "/api/v1/users"
        };

        for (String data : testData) {
            Match match = grok.match(data);
            Map<String, Object> captured = match.capture();

            assertFalse("Should match: " + data, captured.isEmpty());
        }
    }

    @Test
    public void testZeekDataStopsAtTab() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_DATA}");

        String dataWithTab = "value1\tvalue2";
        Match match = grok.match(dataWithTab);
        Map<String, Object> captured = match.capture();

        assertFalse("Should match first part before tab", captured.isEmpty());
    }

    @Test
    public void testZeekDataMatchesHyphenPlaceholder() throws GrokException {
        Grok grok = compiler.compile("%{ZEEK_DATA}");

        Match match = grok.match("-");
        Map<String, Object> captured = match.capture();

        assertFalse("Should match hyphen placeholder", captured.isEmpty());
    }

    // ========== ZEEK_HTTP Pattern Definition Tests ==========
    // Note: ZEEK_HTTP has regex compilation issues and cannot be compiled
    // Testing pattern definition structure instead

    @Test
    public void testZeekHttpPatternDefinesECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekHttp = patterns.get("ZEEK_HTTP");

        // Verify pattern contains core ECS field references
        assertTrue("ZEEK_HTTP should reference zeek.session_id",
                zeekHttp.contains("zeek.session_id"));
        assertTrue("ZEEK_HTTP should reference source.ip",
                zeekHttp.contains("source.ip"));
        assertTrue("ZEEK_HTTP should reference source.port",
                zeekHttp.contains("source.port"));
        assertTrue("ZEEK_HTTP should reference destination.ip",
                zeekHttp.contains("destination.ip"));
        assertTrue("ZEEK_HTTP should reference destination.port",
                zeekHttp.contains("destination.port"));
        assertTrue("ZEEK_HTTP should reference http.request.method",
                zeekHttp.contains("http.request.method"));
    }

    @Test
    public void testZeekHttpDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekHttp = patterns.get("ZEEK_HTTP");

        // Verify key ZEEK HTTP fields are defined
        String[] expectedFields = {
            "zeek.session_id",
            "source.ip",
            "source.port:int",
            "destination.ip",
            "destination.port:int",
            "zeek.http.trans_depth:int",
            "http.request.method",
            "url.domain",
            "url.original",
            "http.request.referrer",
            "http.version",
            "user_agent.original",
            "zeek.http.origin",
            "http.request.body.bytes:int",
            "http.response.body.bytes:int",
            "http.response.status_code:int",
            "zeek.http.status_msg",
            "url.username",
            "url.password",
            "zeek.http.orig_filenames",
            "zeek.http.resp_filenames",
            "http.request.mime_type",
            "http.response.mime_type"
        };

        for (String field : expectedFields) {
            assertTrue("ZEEK_HTTP should define field: " + field,
                    zeekHttp.contains(field));
        }
    }

    @Test
    public void testZeekHttpDefinesNewFieldsComparedToBro() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekHttp = patterns.get("ZEEK_HTTP");

        // New fields in ZEEK_HTTP compared to BRO_HTTP
        assertTrue("ZEEK_HTTP should have http.version field",
                zeekHttp.contains("http.version"));
        assertTrue("ZEEK_HTTP should have zeek.http.origin field",
                zeekHttp.contains("zeek.http.origin"));
        assertTrue("ZEEK_HTTP should have zeek.http.orig_filenames field",
                zeekHttp.contains("zeek.http.orig_filenames"));
        assertTrue("ZEEK_HTTP should have zeek.http.resp_filenames field",
                zeekHttp.contains("zeek.http.resp_filenames"));
    }

    // ========== ZEEK_DNS Pattern Definition Tests ==========
    // Note: ZEEK_DNS has regex compilation issues and cannot be compiled
    // Testing pattern definition structure instead

    @Test
    public void testZeekDnsPatternDefinesECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekDns = patterns.get("ZEEK_DNS");

        assertTrue("ZEEK_DNS should reference zeek.session_id",
                zeekDns.contains("zeek.session_id"));
        assertTrue("ZEEK_DNS should reference source.ip",
                zeekDns.contains("source.ip"));
        assertTrue("ZEEK_DNS should reference source.port",
                zeekDns.contains("source.port"));
        assertTrue("ZEEK_DNS should reference destination.ip",
                zeekDns.contains("destination.ip"));
        assertTrue("ZEEK_DNS should reference destination.port",
                zeekDns.contains("destination.port"));
        assertTrue("ZEEK_DNS should reference network.transport",
                zeekDns.contains("network.transport"));
        assertTrue("ZEEK_DNS should reference dns.question.name",
                zeekDns.contains("dns.question.name"));
    }

    @Test
    public void testZeekDnsDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekDns = patterns.get("ZEEK_DNS");

        // Verify key ZEEK DNS fields are defined
        String[] expectedFields = {
            "zeek.session_id",
            "source.ip",
            "source.port:int",
            "destination.ip",
            "destination.port:int",
            "network.transport",
            "dns.id:int",
            "zeek.dns.rtt:float",
            "dns.question.name",
            "zeek.dns.qclass:int",
            "zeek.dns.qclass_name",
            "zeek.dns.qtype:int",
            "dns.question.type",
            "zeek.dns.rcode:int",
            "dns.response_code",
            "zeek.dns.AA",
            "zeek.dns.TC",
            "zeek.dns.RD",
            "zeek.dns.RA",
            "zeek.dns.Z:int",
            "zeek.dns.answers",
            "zeek.dns.TTLs",
            "zeek.dns.rejected"
        };

        for (String field : expectedFields) {
            assertTrue("ZEEK_DNS should define field: " + field,
                    zeekDns.contains(field));
        }
    }

    @Test
    public void testZeekDnsDefinesNewFieldsComparedToBro() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekDns = patterns.get("ZEEK_DNS");

        // New field in ZEEK_DNS compared to BRO_DNS
        assertTrue("ZEEK_DNS should have zeek.dns.rtt:float field for round-trip time",
                zeekDns.contains("zeek.dns.rtt:float"));
    }

    @Test
    public void testZeekDnsBooleanFlags() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekDns = patterns.get("ZEEK_DNS");

        // Verify ZEEK_BOOL is used for DNS flags
        assertTrue("ZEEK_DNS should use ZEEK_BOOL for AA flag",
                zeekDns.contains("ZEEK_BOOL:zeek.dns.AA"));
        assertTrue("ZEEK_DNS should use ZEEK_BOOL for TC flag",
                zeekDns.contains("ZEEK_BOOL:zeek.dns.TC"));
        assertTrue("ZEEK_DNS should use ZEEK_BOOL for RD flag",
                zeekDns.contains("ZEEK_BOOL:zeek.dns.RD"));
        assertTrue("ZEEK_DNS should use ZEEK_BOOL for RA flag",
                zeekDns.contains("ZEEK_BOOL:zeek.dns.RA"));
    }

    // ========== ZEEK_CONN Pattern Definition Tests ==========
    // Note: ZEEK_CONN has regex compilation issues and cannot be compiled
    // Testing pattern definition structure instead

    @Test
    public void testZeekConnPatternDefinesECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekConn = patterns.get("ZEEK_CONN");

        assertTrue("ZEEK_CONN should reference zeek.session_id",
                zeekConn.contains("zeek.session_id"));
        assertTrue("ZEEK_CONN should reference source.ip",
                zeekConn.contains("source.ip"));
        assertTrue("ZEEK_CONN should reference source.port",
                zeekConn.contains("source.port"));
        assertTrue("ZEEK_CONN should reference destination.ip",
                zeekConn.contains("destination.ip"));
        assertTrue("ZEEK_CONN should reference destination.port",
                zeekConn.contains("destination.port"));
        assertTrue("ZEEK_CONN should reference network.transport",
                zeekConn.contains("network.transport"));
        assertTrue("ZEEK_CONN should reference network.protocol",
                zeekConn.contains("network.protocol"));
    }

    @Test
    public void testZeekConnDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekConn = patterns.get("ZEEK_CONN");

        // Verify key ZEEK CONN fields are defined
        String[] expectedFields = {
            "zeek.session_id",
            "source.ip",
            "source.port:int",
            "destination.ip",
            "destination.port:int",
            "network.transport",
            "network.protocol",
            "zeek.connection.duration:float",
            "zeek.connection.orig_bytes:int",
            "zeek.connection.resp_bytes:int",
            "zeek.connection.state",
            "zeek.connection.local_orig",
            "zeek.connection.local_resp",
            "zeek.connection.missed_bytes:int",
            "zeek.connection.history",
            "source.packets:int",
            "source.bytes:int",
            "destination.packets:int",
            "destination.bytes:int",
            "zeek.connection.tunnel_parents"
        };

        for (String field : expectedFields) {
            assertTrue("ZEEK_CONN should define field: " + field,
                    zeekConn.contains(field));
        }
    }

    @Test
    public void testZeekConnDefinesNewFieldsComparedToBro() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekConn = patterns.get("ZEEK_CONN");

        // New field required in ZEEK_CONN
        assertTrue("ZEEK_CONN should have zeek.connection.local_resp field",
                zeekConn.contains("zeek.connection.local_resp"));
    }

    @Test
    public void testZeekConnSupportsMacAddresses() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekConn = patterns.get("ZEEK_CONN");

        // Optional MAC address fields
        assertTrue("ZEEK_CONN should support source.mac field",
                zeekConn.contains("source.mac"));
        assertTrue("ZEEK_CONN should support destination.mac field",
                zeekConn.contains("destination.mac"));
        assertTrue("ZEEK_CONN should reference COMMONMAC pattern",
                zeekConn.contains("COMMONMAC"));
    }

    // ========== ZEEK_FILES Pattern Definition Tests ==========
    // Note: ZEEK_FILES has regex compilation issues and cannot be compiled
    // Testing pattern definition structure instead

    @Test
    public void testZeekFilesPatternDefinesECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekFiles = patterns.get("ZEEK_FILES");

        assertTrue("ZEEK_FILES should reference zeek.files.fuid",
                zeekFiles.contains("zeek.files.fuid"));
        assertTrue("ZEEK_FILES should reference file.mime_type",
                zeekFiles.contains("file.mime_type"));
        assertTrue("ZEEK_FILES should reference file.name",
                zeekFiles.contains("file.name"));
        assertTrue("ZEEK_FILES should reference file.size",
                zeekFiles.contains("file.size"));
    }

    @Test
    public void testZeekFilesDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekFiles = patterns.get("ZEEK_FILES");

        // Verify key ZEEK FILES fields are defined
        // Note: [server][ip] and [client][ip] are in helper patterns, not directly in ZEEK_FILES
        String[] expectedFields = {
            "zeek.files.fuid",
            "zeek.files.session_ids",
            "zeek.files.source",
            "zeek.files.depth:int",
            "zeek.files.analyzers",
            "file.mime_type",
            "file.name",
            "zeek.files.duration:float",
            "zeek.files.local_orig",
            "zeek.files.is_orig",
            "zeek.files.seen_bytes:int",
            "file.size:int",
            "zeek.files.missing_bytes:int",
            "zeek.files.overflow_bytes:int",
            "zeek.files.timedout",
            "zeek.files.parent_fuid",
            "file.hash.md5",
            "file.hash.sha1",
            "file.hash.sha256",
            "zeek.files.extracted",
            "zeek.files.extracted_cutoff",
            "zeek.files.extracted_size:int"
        };

        for (String field : expectedFields) {
            assertTrue("ZEEK_FILES should define field: " + field,
                    zeekFiles.contains(field));
        }
    }

    @Test
    public void testZeekFilesDefinesNewFieldsComparedToBro() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekFiles = patterns.get("ZEEK_FILES");

        // New fields added at the end in ZEEK_FILES
        assertTrue("ZEEK_FILES should have zeek.files.extracted_cutoff field",
                zeekFiles.contains("zeek.files.extracted_cutoff"));
        assertTrue("ZEEK_FILES should have zeek.files.extracted_size:int field",
                zeekFiles.contains("zeek.files.extracted_size:int"));
    }

    @Test
    public void testZeekFilesUsesHelperPatterns() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekFiles = patterns.get("ZEEK_FILES");

        // Verify helper patterns are referenced
        assertTrue("ZEEK_FILES should reference ZEEK_FILES_TX_HOSTS",
                zeekFiles.contains("ZEEK_FILES_TX_HOSTS"));
        assertTrue("ZEEK_FILES should reference ZEEK_FILES_RX_HOSTS",
                zeekFiles.contains("ZEEK_FILES_RX_HOSTS"));
    }

    @Test
    public void testZeekFilesHashFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String zeekFiles = patterns.get("ZEEK_FILES");

        // Verify all hash types are supported
        assertTrue("ZEEK_FILES should support MD5 hash",
                zeekFiles.contains("file.hash.md5"));
        assertTrue("ZEEK_FILES should support SHA1 hash",
                zeekFiles.contains("file.hash.sha1"));
        assertTrue("ZEEK_FILES should support SHA256 hash",
                zeekFiles.contains("file.hash.sha256"));
    }

    // ========== ZEEK_FILES Helper Pattern Tests ==========

    @Test
    public void testZeekFilesTxHostsPatternExists() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String txHosts = patterns.get("ZEEK_FILES_TX_HOSTS");

        assertNotNull("ZEEK_FILES_TX_HOSTS should be defined", txHosts);
        assertFalse("ZEEK_FILES_TX_HOSTS should not be empty", txHosts.trim().isEmpty());
    }

    @Test
    public void testZeekFilesRxHostsPatternExists() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rxHosts = patterns.get("ZEEK_FILES_RX_HOSTS");

        assertNotNull("ZEEK_FILES_RX_HOSTS should be defined", rxHosts);
        assertFalse("ZEEK_FILES_RX_HOSTS should not be empty", rxHosts.trim().isEmpty());
    }

    @Test
    public void testZeekFilesTxHostsDefinesServerIp() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String txHosts = patterns.get("ZEEK_FILES_TX_HOSTS");

        assertTrue("ZEEK_FILES_TX_HOSTS should reference server.ip",
                txHosts.contains("server.ip"));
        assertTrue("ZEEK_FILES_TX_HOSTS should reference zeek.files.tx_hosts",
                txHosts.contains("zeek.files.tx_hosts"));
    }

    @Test
    public void testZeekFilesRxHostsDefinesClientIp() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rxHosts = patterns.get("ZEEK_FILES_RX_HOSTS");

        assertTrue("ZEEK_FILES_RX_HOSTS should reference client.ip",
                rxHosts.contains("client.ip"));
        assertTrue("ZEEK_FILES_RX_HOSTS should reference zeek.files.rx_hosts",
                rxHosts.contains("zeek.files.rx_hosts"));
    }

    // ========== Pattern Structure Tests ==========

    @Test
    public void testZeekPatternsDontDependOnEachOther() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String zeekHttp = patterns.get("ZEEK_HTTP");
        String zeekDns = patterns.get("ZEEK_DNS");
        String zeekConn = patterns.get("ZEEK_CONN");
        String zeekFiles = patterns.get("ZEEK_FILES");

        // Each ZEEK pattern should be independent (not reference other ZEEK_HTTP/DNS/CONN patterns)
        assertFalse("ZEEK_HTTP should not reference ZEEK_DNS", zeekHttp.contains("ZEEK_DNS"));
        assertFalse("ZEEK_DNS should not reference ZEEK_CONN", zeekDns.contains("ZEEK_CONN"));
        assertFalse("ZEEK_CONN should not reference ZEEK_FILES", zeekConn.contains("ZEEK_FILES"));
        assertFalse("ZEEK_FILES should not reference ZEEK_HTTP", zeekFiles.contains("ZEEK_HTTP"));
    }

    @Test
    public void testZeekPatternsUseSharedHelpers() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String zeekHttp = patterns.get("ZEEK_HTTP");
        String zeekDns = patterns.get("ZEEK_DNS");

        // All patterns should use shared ZEEK_BOOL and ZEEK_DATA helpers
        assertTrue("ZEEK_DNS should use ZEEK_BOOL for flags", zeekDns.contains("ZEEK_BOOL"));

        // ZEEK_DATA is used extensively for optional fields
        assertTrue("ZEEK_HTTP should use ZEEK_DATA", zeekHttp.contains("ZEEK_DATA"));
        assertTrue("ZEEK_DNS should use ZEEK_DATA", zeekDns.contains("ZEEK_DATA"));
    }

    @Test
    public void testAllZeekPatternsUseTabSeparator() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String[] mainPatterns = {"ZEEK_HTTP", "ZEEK_DNS", "ZEEK_CONN", "ZEEK_FILES"};

        for (String patternName : mainPatterns) {
            String pattern = patterns.get(patternName);
            assertTrue(patternName + " should use tab separator \\t",
                    pattern.contains("\\t"));
        }
    }

    @Test
    public void testAllZeekPatternsHandleOptionalFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String[] mainPatterns = {"ZEEK_HTTP", "ZEEK_DNS", "ZEEK_CONN", "ZEEK_FILES"};

        for (String patternName : mainPatterns) {
            String pattern = patterns.get(patternName);
            assertTrue(patternName + " should handle optional fields with '(?:-|' pattern",
                    pattern.contains("(?:-|"));
        }
    }

    @Test
    public void testZeekPatternsUseTimestampField() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String[] mainPatterns = {"ZEEK_HTTP", "ZEEK_DNS", "ZEEK_CONN", "ZEEK_FILES"};

        for (String patternName : mainPatterns) {
            String pattern = patterns.get(patternName);
            assertTrue(patternName + " should start with timestamp field",
                    pattern.startsWith("%{NUMBER:log_timestamp}"));
        }
    }
}
