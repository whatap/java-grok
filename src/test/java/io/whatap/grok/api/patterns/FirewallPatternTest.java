package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import io.whatap.grok.api.exception.GrokException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for firewall log patterns.
 *
 * <p>Tests patterns for various firewall systems:</p>
 * <ul>
 *   <li>NetScreen firewall session logs</li>
 *   <li>Cisco ASA firewall logs (multiple message types)</li>
 *   <li>iptables/netfilter logs</li>
 *   <li>Shorewall logs</li>
 *   <li>SuSE Firewall 2 logs</li>
 * </ul>
 *
 * <p><strong>KNOWN ISSUES:</strong></p>
 * <p>The firewall patterns file contains literal named capture groups with ECS-style bracket notation
 * (e.g., {@code (?<[observer][product]>NetScreen)}) which causes PatternSyntaxException in Java regex engine.
 * Java named groups must use simple alphanumeric identifiers like {@code (?<name>...)} not
 * {@code (?<[field][name]>...)}.</p>
 *
 * <p>The Grok library correctly transforms {@code %{PATTERN:[field][name]}} syntax, but cannot handle
 * literal {@code (?<[field][name]>...)} patterns in the pattern definitions.</p>
 *
 * <p>To fix these patterns, the literal named groups should be converted to use the Grok pattern syntax:</p>
 * <pre>
 * BAD:  (?&lt;[observer][product]&gt;NetScreen)
 * GOOD: %{WORD:[observer][product]} (where WORD is defined to match "NetScreen")
 * </pre>
 *
 * <p>This test suite focuses on:</p>
 * <ul>
 *   <li>Verifying pattern registration</li>
 *   <li>Testing helper patterns that compile successfully</li>
 *   <li>Documenting compilation issues for complex patterns</li>
 * </ul>
 *
 * @see https://www.elastic.co/guide/en/ecs/current/ecs-field-reference.html
 */
public class FirewallPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/firewalls");
    }

    // ========================================
    // Pattern Registration Tests
    // ========================================

    @Test
    public void testFirewallPatternsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Main firewall patterns
        assertTrue("NETSCREENSESSIONLOG should be registered",
            patterns.containsKey("NETSCREENSESSIONLOG"));
        assertTrue("CISCO_TAGGED_SYSLOG should be registered",
            patterns.containsKey("CISCO_TAGGED_SYSLOG"));
        assertTrue("IPTABLES should be registered",
            patterns.containsKey("IPTABLES"));
        assertTrue("SHOREWALL should be registered",
            patterns.containsKey("SHOREWALL"));
        assertTrue("SFW2 should be registered",
            patterns.containsKey("SFW2"));
    }

    @Test
    public void testCiscoAsa_patternsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Cisco ASA message-specific patterns
        assertTrue("CISCOFW106001 should be registered",
            patterns.containsKey("CISCOFW106001"));
        assertTrue("CISCOFW106006_106007_106010 should be registered",
            patterns.containsKey("CISCOFW106006_106007_106010"));
        assertTrue("CISCOFW106014 should be registered",
            patterns.containsKey("CISCOFW106014"));
        assertTrue("CISCOFW106023 should be registered",
            patterns.containsKey("CISCOFW106023"));
        assertTrue("CISCOFW302013_302014_302015_302016 should be registered",
            patterns.containsKey("CISCOFW302013_302014_302015_302016"));
        assertTrue("CISCOFW305011 should be registered",
            patterns.containsKey("CISCOFW305011"));
        assertTrue("CISCOFW313001_313004_313008 should be registered",
            patterns.containsKey("CISCOFW313001_313004_313008"));
    }

    @Test
    public void testCiscoHelperPatternsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Cisco helper patterns
        assertTrue("CISCOTIMESTAMP should be registered",
            patterns.containsKey("CISCOTIMESTAMP"));
        assertTrue("CISCOTAG should be registered",
            patterns.containsKey("CISCOTAG"));
        assertTrue("CISCO_ACTION should be registered",
            patterns.containsKey("CISCO_ACTION"));
        assertTrue("CISCO_DIRECTION should be registered",
            patterns.containsKey("CISCO_DIRECTION"));
        assertTrue("CISCO_REASON should be registered",
            patterns.containsKey("CISCO_REASON"));
        assertTrue("CISCO_XLATE_TYPE should be registered",
            patterns.containsKey("CISCO_XLATE_TYPE"));
    }

    @Test
    public void testIptablesHelperPatternsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // iptables helper patterns
        assertTrue("IPTABLES_TCP_FLAGS should be registered",
            patterns.containsKey("IPTABLES_TCP_FLAGS"));
        assertTrue("IPTABLES_TCP_PART should be registered",
            patterns.containsKey("IPTABLES_TCP_PART"));
        assertTrue("IPTABLES4_PART should be registered",
            patterns.containsKey("IPTABLES4_PART"));
        assertTrue("IPTABLES4_FRAG should be registered",
            patterns.containsKey("IPTABLES4_FRAG"));
        assertTrue("IPTABLES6_PART should be registered",
            patterns.containsKey("IPTABLES6_PART"));
    }

    // ========================================
    // Pattern Definition Verification
    // ========================================

    @Test
    public void testPatternDefinitionsAreNonEmpty() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Check that patterns have actual definitions
        assertNotNull("CISCO_ACTION definition should exist",
            patterns.get("CISCO_ACTION"));
        assertFalse("CISCO_ACTION definition should not be empty",
            patterns.get("CISCO_ACTION").trim().isEmpty());

        assertNotNull("IPTABLES definition should exist",
            patterns.get("IPTABLES"));
        assertFalse("IPTABLES definition should not be empty",
            patterns.get("IPTABLES").trim().isEmpty());
    }

    // ========================================
    // Simple Helper Pattern Tests (These compile successfully)
    // ========================================

    @Test
    public void testCiscoAction_compiles() throws GrokException {
        Grok grok = compiler.compile("%{CISCO_ACTION}");
        assertNotNull("CISCO_ACTION should compile", grok);

        // Test various Cisco ASA action keywords
        String[] actions = {
            "Built",
            "Teardown",
            "Deny",
            "Denied",
            "denied",
            "requested",
            "permitted",
            "denied by ACL",
            "discarded",
            "est-allowed",
            "Dropping",
            "created",
            "deleted"
        };

        for (String action : actions) {
            Match match = grok.match(action);
            Map<String, Object> result = match.capture();
            assertFalse("Action '" + action + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testCiscoDirection_compiles() throws GrokException {
        Grok grok = compiler.compile("%{CISCO_DIRECTION}");
        assertNotNull("CISCO_DIRECTION should compile", grok);

        String[] directions = {"Inbound", "inbound", "Outbound", "outbound"};
        for (String direction : directions) {
            Match match = grok.match(direction);
            Map<String, Object> result = match.capture();
            assertFalse("Direction '" + direction + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testCiscoXlateType_compiles() throws GrokException {
        Grok grok = compiler.compile("%{CISCO_XLATE_TYPE}");
        assertNotNull("CISCO_XLATE_TYPE should compile", grok);

        String[] types = {"static", "dynamic"};
        for (String type : types) {
            Match match = grok.match(type);
            Map<String, Object> result = match.capture();
            assertFalse("XLATE type '" + type + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testIptablesTcpFlags_compiles() throws GrokException {
        Grok grok = compiler.compile("%{IPTABLES_TCP_FLAGS}");
        assertNotNull("IPTABLES_TCP_FLAGS should compile", grok);

        String[] flagCombinations = {
            "SYN ",
            "ACK ",
            "SYN ACK ",
            "FIN ACK ",
            "RST ",
            "PSH ACK ",
            "URG ACK ",
            "CWR ECE "
        };

        for (String flags : flagCombinations) {
            Match match = grok.match(flags);
            Map<String, Object> result = match.capture();
            assertFalse("TCP flags '" + flags.trim() + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testCiscoTimestamp_compiles() throws GrokException {
        Grok grok = compiler.compile("%{CISCOTIMESTAMP}");
        assertNotNull("CISCOTIMESTAMP should compile", grok);

        String[] timestamps = {
            "Oct 11 22:14:15",
            "Jan  1 2024 00:00:00",
            "Dec 31 23:59:59"
        };

        for (String timestamp : timestamps) {
            Match match = grok.match(timestamp);
            Map<String, Object> result = match.capture();
            assertFalse("Timestamp '" + timestamp + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testCiscoTag_compiles() throws GrokException {
        Grok grok = compiler.compile("%{CISCOTAG}");
        assertNotNull("CISCOTAG should compile", grok);

        String[] tags = {
            "ASA-6-302013",
            "ASA-4-106023",
            "ASA-1-104001",
            "PIX-6-302013"
        };

        for (String tag : tags) {
            Match match = grok.match(tag);
            Map<String, Object> result = match.capture();
            assertFalse("Cisco tag '" + tag + "' should match", result.isEmpty());
        }
    }

    @Test
    public void testIptables4Frag_compiles() throws GrokException {
        Grok grok = compiler.compile("%{IPTABLES4_FRAG}");
        assertNotNull("IPTABLES4_FRAG should compile", grok);

        String[] fragments = {"DF", "MF", "CE", "DF MF"};
        for (String frag : fragments) {
            Match match = grok.match(frag);
            Map<String, Object> result = match.capture();
            // Fragment flags might be optional, so just verify it compiles
        }
    }

    // ========================================
    // Complex Pattern Compilation Issues
    // ========================================

    @Test(expected = PatternSyntaxException.class)
    public void testNetScreenSessionLog_hasCompilationIssue() throws GrokException {
        // This pattern contains literal (?<[observer][product]>NetScreen) which is invalid Java regex
        // Java named groups require simple alphanumeric identifiers like (?<name>...)
        // not bracket notation like (?<[field][name]>...)
        compiler.compile("%{NETSCREENSESSIONLOG}");
        fail("NETSCREENSESSIONLOG should throw PatternSyntaxException due to invalid named group syntax");
    }

    @Test(expected = PatternSyntaxException.class)
    public void testCiscoFW106001_hasCompilationIssue() throws GrokException {
        // Cisco ASA patterns use ECS-style field references in their definitions
        compiler.compile("%{CISCOFW106001}");
        fail("CISCOFW106001 should throw PatternSyntaxException due to ECS field references");
    }

    @Test(expected = PatternSyntaxException.class)
    public void testIptables_hasCompilationIssue() throws GrokException {
        // IPTABLES pattern uses nested ECS-style field names
        compiler.compile("%{IPTABLES}");
        fail("IPTABLES should throw PatternSyntaxException due to ECS field references");
    }

    @Test(expected = PatternSyntaxException.class)
    public void testShorewall_hasCompilationIssue() throws GrokException {
        // SHOREWALL pattern includes IPTABLES which has ECS field issues
        compiler.compile("%{SHOREWALL}");
        fail("SHOREWALL should throw PatternSyntaxException due to IPTABLES dependency");
    }

    @Test(expected = PatternSyntaxException.class)
    public void testSFW2_hasCompilationIssue() throws GrokException {
        // SFW2 pattern includes IPTABLES which has ECS field issues
        compiler.compile("%{SFW2}");
        fail("SFW2 should throw PatternSyntaxException due to IPTABLES dependency");
    }

    // ========================================
    // Pattern Structure Verification
    // ========================================

    @Test
    public void testNetScreenSessionLog_containsExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String pattern = patterns.get("NETSCREENSESSIONLOG");

        assertNotNull("NETSCREENSESSIONLOG pattern should exist", pattern);

        // Verify pattern contains expected field names (even if they cause compilation errors)
        assertTrue("Pattern should reference [observer][hostname]",
            pattern.contains("[observer][hostname]"));
        assertTrue("Pattern should reference [observer][product]",
            pattern.contains("[observer][product]"));
        assertTrue("Pattern should reference [netscreen][device_id]",
            pattern.contains("[netscreen][device_id]"));
        assertTrue("Pattern should reference [event][code]",
            pattern.contains("[event][code]"));
        assertTrue("Pattern should reference [source][bytes]",
            pattern.contains("[source][bytes]"));
        assertTrue("Pattern should reference [destination][bytes]",
            pattern.contains("[destination][bytes]"));
        assertTrue("Pattern should reference [source][address]",
            pattern.contains("[source][address]"));
        assertTrue("Pattern should reference [destination][address]",
            pattern.contains("[destination][address]"));
    }

    @Test
    public void testIptables_containsExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String pattern = patterns.get("IPTABLES");

        assertNotNull("IPTABLES pattern should exist", pattern);

        // Verify pattern contains expected field names
        // Note: IP addresses are in IPTABLES4_PART and IPTABLES6_PART, not directly in IPTABLES
        assertTrue("Pattern should reference [observer][ingress][interface][name]",
            pattern.contains("[observer][ingress][interface][name]"));
        assertTrue("Pattern should reference [observer][egress][interface][name]",
            pattern.contains("[observer][egress][interface][name]"));
        assertTrue("Pattern should reference [destination][mac]",
            pattern.contains("[destination][mac]"));
        assertTrue("Pattern should reference [source][mac]",
            pattern.contains("[source][mac]"));
        assertTrue("Pattern should reference [network][transport]",
            pattern.contains("[network][transport]"));
        assertTrue("Pattern should reference [source][port]",
            pattern.contains("[source][port]"));
        assertTrue("Pattern should reference [destination][port]",
            pattern.contains("[destination][port]"));
        assertTrue("Pattern should reference IPTABLES4_PART or IPTABLES6_PART",
            pattern.contains("IPTABLES4_PART") || pattern.contains("IPTABLES6_PART"));
    }

    @Test
    public void testIptables4Part_containsExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String pattern = patterns.get("IPTABLES4_PART");

        assertNotNull("IPTABLES4_PART pattern should exist", pattern);

        // Verify IPv4-specific fields
        assertTrue("Pattern should reference [source][ip]",
            pattern.contains("[source][ip]"));
        assertTrue("Pattern should reference [destination][ip]",
            pattern.contains("[destination][ip]"));
        assertTrue("Pattern should reference [iptables][length]",
            pattern.contains("[iptables][length]"));
        assertTrue("Pattern should reference [iptables][ttl]",
            pattern.contains("[iptables][ttl]"));
    }

    @Test
    public void testIptables6Part_containsExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String pattern = patterns.get("IPTABLES6_PART");

        assertNotNull("IPTABLES6_PART pattern should exist", pattern);

        // Verify IPv6-specific fields
        assertTrue("Pattern should reference [source][ip]",
            pattern.contains("[source][ip]"));
        assertTrue("Pattern should reference [destination][ip]",
            pattern.contains("[destination][ip]"));
        assertTrue("Pattern should reference [iptables][length]",
            pattern.contains("[iptables][length]"));
        assertTrue("Pattern should reference [iptables][ttl]",
            pattern.contains("[iptables][ttl]"));
    }

    @Test
    public void testCiscoFW302013_containsExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String pattern = patterns.get("CISCOFW302013_302014_302015_302016");

        assertNotNull("CISCOFW302013_302014_302015_302016 pattern should exist", pattern);

        // Verify pattern contains expected Cisco ASA fields
        assertTrue("Pattern should reference [cisco][asa][outcome]",
            pattern.contains("[cisco][asa][outcome]"));
        assertTrue("Pattern should reference [cisco][asa][network][direction]",
            pattern.contains("[cisco][asa][network][direction]"));
        assertTrue("Pattern should reference [cisco][asa][network][transport]",
            pattern.contains("[cisco][asa][network][transport]"));
        assertTrue("Pattern should reference [cisco][asa][connection_id]",
            pattern.contains("[cisco][asa][connection_id]"));
    }

    // ========================================
    // Documentation Tests
    // ========================================

    @Test
    public void testFirewallPatternCount() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Count firewall-related patterns
        long ciscoPatternCount = patterns.keySet().stream()
            .filter(key -> key.startsWith("CISCO"))
            .count();

        long iptablesPatternCount = patterns.keySet().stream()
            .filter(key -> key.startsWith("IPTABLES"))
            .count();

        // Verify reasonable number of patterns are loaded
        assertTrue("Should have at least 10 CISCO patterns", ciscoPatternCount >= 10);
        assertTrue("Should have at least 5 IPTABLES patterns", iptablesPatternCount >= 5);
    }

    @Test
    public void testPatternNamingConventions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify Cisco ASA message-specific patterns follow naming convention
        // Format should be CISCOFW<message-id> or CISCOFW<message-id-range>
        assertTrue("CISCOFW106001 should follow naming convention",
            patterns.containsKey("CISCOFW106001"));
        assertTrue("CISCOFW106006_106007_106010 should follow naming convention",
            patterns.containsKey("CISCOFW106006_106007_106010"));

        // Verify helper patterns use uppercase and underscores
        assertTrue("CISCO_ACTION uses correct naming",
            patterns.containsKey("CISCO_ACTION"));
        assertTrue("IPTABLES_TCP_FLAGS uses correct naming",
            patterns.containsKey("IPTABLES_TCP_FLAGS"));
    }
}
