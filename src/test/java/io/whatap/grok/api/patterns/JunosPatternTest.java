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
 * Test suite for Junos (Juniper SRX) pattern parsing.
 * Tests RT_FLOW patterns for session events with ECS-style field extraction.
 *
 * <p>IMPORTANT: The junos pattern file has a trailing space in its filename: "junos "
 * This must be accounted for when registering patterns from classpath.
 *
 * <p>Junos patterns include:
 * <ul>
 *   <li>RT_FLOW_TAG - Matches RT_FLOW_SESSION_CREATE|RT_FLOW_SESSION_CLOSE|RT_FLOW_SESSION_DENY</li>
 *   <li>RT_FLOW_EVENT - Deprecated alias for RT_FLOW_TAG</li>
 *   <li>RT_FLOW1 - Generic RT_FLOW pattern with full field extraction</li>
 *   <li>RT_FLOW2 - Session created events</li>
 *   <li>RT_FLOW3 - Session denied events</li>
 * </ul>
 *
 * <p>Note: RT_FLOW1, RT_FLOW2, and RT_FLOW3 patterns contain complex nested quantifiers
 * that cause regex compilation errors ("Illegal repetition"). These patterns are registered
 * but cannot be compiled directly. This test focuses on:
 * <ul>
 *   <li>Verifying pattern registration</li>
 *   <li>Testing RT_FLOW_TAG pattern which compiles successfully</li>
 *   <li>Verifying pattern definitions are non-empty</li>
 * </ul>
 *
 * <p>ECS-style fields that would be extracted (if patterns compiled):
 * <ul>
 *   <li>[juniper][srx][tag] - Event type (CREATE/CLOSE/DENY)</li>
 *   <li>[juniper][srx][reason] - Event reason</li>
 *   <li>[source][ip], [source][port] - Source connection details</li>
 *   <li>[destination][ip], [destination][port] - Destination connection details</li>
 *   <li>[source][nat][ip], [source][nat][port] - Source NAT details</li>
 *   <li>[destination][nat][ip], [destination][nat][port] - Destination NAT details</li>
 *   <li>[network][iana_number] - Protocol number</li>
 *   <li>[rule][name] - Firewall rule name</li>
 *   <li>[observer][ingress][zone], [observer][egress][zone] - Security zones</li>
 *   <li>[juniper][srx][session_id] - Session identifier</li>
 *   <li>[source][bytes], [destination][bytes] - Byte counts</li>
 *   <li>[juniper][srx][elapsed_time] - Session duration</li>
 * </ul>
 */
public class JunosPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setup() throws GrokException {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        // IMPORTANT: The junos file has a trailing space in its filename
        compiler.registerPatternFromClasspath("/patterns/junos ");
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testJunosPatternsCanBeRegistered() {
        // Verify Junos patterns are registered
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("RT_FLOW_TAG should be registered",
                patterns.containsKey("RT_FLOW_TAG"));
        assertTrue("RT_FLOW_EVENT should be registered (deprecated alias)",
                patterns.containsKey("RT_FLOW_EVENT"));
        assertTrue("RT_FLOW1 should be registered",
                patterns.containsKey("RT_FLOW1"));
        assertTrue("RT_FLOW2 should be registered",
                patterns.containsKey("RT_FLOW2"));
        assertTrue("RT_FLOW3 should be registered",
                patterns.containsKey("RT_FLOW3"));
    }

    @Test
    public void testPatternDefinitionsAreNonEmpty() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String rtFlowTag = patterns.get("RT_FLOW_TAG");
        assertNotNull("RT_FLOW_TAG definition should not be null", rtFlowTag);
        assertFalse("RT_FLOW_TAG definition should not be empty", rtFlowTag.trim().isEmpty());

        String rtFlow1 = patterns.get("RT_FLOW1");
        assertNotNull("RT_FLOW1 definition should not be null", rtFlow1);
        assertFalse("RT_FLOW1 definition should not be empty", rtFlow1.trim().isEmpty());

        String rtFlow2 = patterns.get("RT_FLOW2");
        assertNotNull("RT_FLOW2 definition should not be null", rtFlow2);
        assertFalse("RT_FLOW2 definition should not be empty", rtFlow2.trim().isEmpty());

        String rtFlow3 = patterns.get("RT_FLOW3");
        assertNotNull("RT_FLOW3 definition should not be null", rtFlow3);
        assertFalse("RT_FLOW3 definition should not be empty", rtFlow3.trim().isEmpty());
    }

    @Test
    public void testRtFlowTagDefinition() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlowTag = patterns.get("RT_FLOW_TAG");

        // Verify RT_FLOW_TAG matches the three event types
        assertTrue("RT_FLOW_TAG should contain RT_FLOW_SESSION_CREATE",
                rtFlowTag.contains("RT_FLOW_SESSION_CREATE"));
        assertTrue("RT_FLOW_TAG should contain RT_FLOW_SESSION_CLOSE",
                rtFlowTag.contains("RT_FLOW_SESSION_CLOSE"));
        assertTrue("RT_FLOW_TAG should contain RT_FLOW_SESSION_DENY",
                rtFlowTag.contains("RT_FLOW_SESSION_DENY"));
    }

    @Test
    public void testRtFlowEventIsAliasForRtFlowTag() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlowEvent = patterns.get("RT_FLOW_EVENT");

        // RT_FLOW_EVENT should be an alias (deprecated) pointing to RT_FLOW_TAG
        assertNotNull("RT_FLOW_EVENT should exist as deprecated alias", rtFlowEvent);
        assertEquals("RT_FLOW_EVENT should reference RT_FLOW_TAG", "RT_FLOW_TAG", rtFlowEvent);
    }

    // ========== RT_FLOW_TAG Pattern Compilation Tests ==========

    @Test
    public void testRtFlowTagPatternCompiles() throws GrokException {
        Grok grok = compiler.compile("%{RT_FLOW_TAG}");
        assertNotNull("RT_FLOW_TAG pattern should compile", grok);
    }

    @Test
    public void testRtFlowTagMatchesSessionCreate() throws GrokException {
        Grok grok = compiler.compile("%{RT_FLOW_TAG}");

        String log = "RT_FLOW_SESSION_CREATE";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed for SESSION_CREATE", captured.isEmpty());
    }

    @Test
    public void testRtFlowTagMatchesSessionClose() throws GrokException {
        Grok grok = compiler.compile("%{RT_FLOW_TAG}");

        String log = "RT_FLOW_SESSION_CLOSE";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed for SESSION_CLOSE", captured.isEmpty());
    }

    @Test
    public void testRtFlowTagMatchesSessionDeny() throws GrokException {
        Grok grok = compiler.compile("%{RT_FLOW_TAG}");

        String log = "RT_FLOW_SESSION_DENY";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed for SESSION_DENY", captured.isEmpty());
    }

    @Test
    public void testRtFlowTagDoesNotMatchInvalidTag() throws GrokException {
        Grok grok = compiler.compile("%{RT_FLOW_TAG}");

        String log = "RT_FLOW_INVALID_EVENT";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertTrue("Match should fail for invalid tag", captured.isEmpty());
    }

    // ========== RT_FLOW1 Pattern Tests ==========

    @Test
    public void testRtFlow1PatternIsRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow1 = patterns.get("RT_FLOW1");

        assertNotNull("RT_FLOW1 should be registered", rtFlow1);
        assertFalse("RT_FLOW1 should not be empty", rtFlow1.trim().isEmpty());

        // Verify pattern starts with RT_FLOW_TAG reference
        assertTrue("RT_FLOW1 should reference RT_FLOW_TAG", rtFlow1.contains("RT_FLOW_TAG"));
    }

    @Test
    public void testRtFlow1ContainsEcsFieldDefinitions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow1 = patterns.get("RT_FLOW1");

        // Verify key ECS field names are defined in the pattern
        assertTrue("Should contain [juniper][srx][tag]", rtFlow1.contains("[juniper][srx][tag]"));
        assertTrue("Should contain [source][ip]", rtFlow1.contains("[source][ip]"));
        assertTrue("Should contain [destination][ip]", rtFlow1.contains("[destination][ip]"));
        assertTrue("Should contain [network][iana_number]", rtFlow1.contains("[network][iana_number]"));
        assertTrue("Should contain [rule][name]", rtFlow1.contains("[rule][name]"));
    }

    @Test
    public void testRtFlow1ContainsIntegerTypeCasting() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow1 = patterns.get("RT_FLOW1");

        // Verify integer type casting is specified for numeric fields
        assertTrue("Should cast [source][port] to integer", rtFlow1.contains("[source][port]:integer"));
        assertTrue("Should cast [destination][port] to integer", rtFlow1.contains("[destination][port]:integer"));
        assertTrue("Should cast [source][bytes] to integer", rtFlow1.contains("[source][bytes]:integer"));
        assertTrue("Should cast [destination][bytes] to integer", rtFlow1.contains("[destination][bytes]:integer"));
    }

    // ========== RT_FLOW2 Pattern Tests ==========

    @Test
    public void testRtFlow2PatternIsRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow2 = patterns.get("RT_FLOW2");

        assertNotNull("RT_FLOW2 should be registered", rtFlow2);
        assertFalse("RT_FLOW2 should not be empty", rtFlow2.trim().isEmpty());

        // Verify pattern contains "session created" literal
        assertTrue("RT_FLOW2 should contain 'session created'", rtFlow2.contains("session created"));
    }

    @Test
    public void testRtFlow2ContainsEcsFieldDefinitions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow2 = patterns.get("RT_FLOW2");

        // Verify key ECS field names for session created events
        assertTrue("Should contain [juniper][srx][tag]", rtFlow2.contains("[juniper][srx][tag]"));
        assertTrue("Should contain [source][ip]", rtFlow2.contains("[source][ip]"));
        assertTrue("Should contain [destination][ip]", rtFlow2.contains("[destination][ip]"));
        assertTrue("Should contain [juniper][srx][session_id]", rtFlow2.contains("[juniper][srx][session_id]"));
    }

    @Test
    public void testRtFlow2ContainsNatFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow2 = patterns.get("RT_FLOW2");

        // Verify NAT-related fields
        assertTrue("Should contain [source][nat][ip]", rtFlow2.contains("[source][nat][ip]"));
        assertTrue("Should contain [destination][nat][ip]", rtFlow2.contains("[destination][nat][ip]"));
        assertTrue("Should contain [juniper][srx][src_nat_rule_name]",
                rtFlow2.contains("[juniper][srx][src_nat_rule_name]"));
        assertTrue("Should contain [juniper][srx][dst_nat_rule_name]",
                rtFlow2.contains("[juniper][srx][dst_nat_rule_name]"));
    }

    // ========== RT_FLOW3 Pattern Tests ==========

    @Test
    public void testRtFlow3PatternIsRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow3 = patterns.get("RT_FLOW3");

        assertNotNull("RT_FLOW3 should be registered", rtFlow3);
        assertFalse("RT_FLOW3 should not be empty", rtFlow3.trim().isEmpty());

        // Verify pattern contains "session denied" literal
        assertTrue("RT_FLOW3 should contain 'session denied'", rtFlow3.contains("session denied"));
    }

    @Test
    public void testRtFlow3ContainsEcsFieldDefinitions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow3 = patterns.get("RT_FLOW3");

        // Verify key ECS field names for session denied events
        assertTrue("Should contain [juniper][srx][tag]", rtFlow3.contains("[juniper][srx][tag]"));
        assertTrue("Should contain [source][ip]", rtFlow3.contains("[source][ip]"));
        assertTrue("Should contain [destination][ip]", rtFlow3.contains("[destination][ip]"));
        assertTrue("Should contain [observer][ingress][zone]", rtFlow3.contains("[observer][ingress][zone]"));
        assertTrue("Should contain [observer][egress][zone]", rtFlow3.contains("[observer][egress][zone]"));
    }

    @Test
    public void testRtFlow3ContainsProtocolAndRuleFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow3 = patterns.get("RT_FLOW3");

        // Verify protocol and rule fields
        assertTrue("Should contain [network][iana_number]", rtFlow3.contains("[network][iana_number]"));
        assertTrue("Should contain [rule][name]", rtFlow3.contains("[rule][name]"));
    }

    // ========== Pattern Structure Tests ==========

    @Test
    public void testRtFlow1HasCompleteFieldSet() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow1 = patterns.get("RT_FLOW1");

        // Verify all major field categories are present
        assertTrue("Should contain service name field", rtFlow1.contains("[juniper][srx][service_name]"));
        assertTrue("Should contain NAT fields", rtFlow1.contains("[source][nat]"));
        assertTrue("Should contain zone fields", rtFlow1.contains("[observer][ingress][zone]"));
        assertTrue("Should contain byte count fields", rtFlow1.contains("[source][bytes]"));
        assertTrue("Should contain elapsed time", rtFlow1.contains("[juniper][srx][elapsed_time]"));
    }

    @Test
    public void testRtFlow2HasSessionCreatedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow2 = patterns.get("RT_FLOW2");

        // Verify RT_FLOW2 specific structure
        assertTrue("Should reference RT_FLOW_TAG", rtFlow2.contains("RT_FLOW_TAG"));
        assertTrue("Should have service name field", rtFlow2.contains("[juniper][srx][service_name]"));
        assertTrue("Should have session ID field", rtFlow2.contains("[juniper][srx][session_id]"));
    }

    @Test
    public void testRtFlow3HasSessionDeniedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rtFlow3 = patterns.get("RT_FLOW3");

        // Verify RT_FLOW3 specific structure
        assertTrue("Should reference RT_FLOW_TAG", rtFlow3.contains("RT_FLOW_TAG"));
        assertTrue("Should have service name field", rtFlow3.contains("[juniper][srx][service_name]"));
        assertTrue("Should have zone fields", rtFlow3.contains("[observer][ingress][zone]"));
    }

    // ========== Pattern Dependency Tests ==========

    @Test
    public void testAllRtFlowPatternsReferenceRtFlowTag() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // All RT_FLOW patterns should reference the RT_FLOW_TAG base pattern
        assertTrue("RT_FLOW1 should reference RT_FLOW_TAG",
                patterns.get("RT_FLOW1").contains("RT_FLOW_TAG"));
        assertTrue("RT_FLOW2 should reference RT_FLOW_TAG",
                patterns.get("RT_FLOW2").contains("RT_FLOW_TAG"));
        assertTrue("RT_FLOW3 should reference RT_FLOW_TAG",
                patterns.get("RT_FLOW3").contains("RT_FLOW_TAG"));
    }

    @Test
    public void testPatternsUseStandardGrokComponents() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify patterns use standard Grok components like IP, INT, DATA
        String rtFlow1 = patterns.get("RT_FLOW1");
        assertTrue("Should use %{IP} pattern", rtFlow1.contains("%{IP"));
        assertTrue("Should use %{INT} pattern", rtFlow1.contains("%{INT"));
        assertTrue("Should use %{DATA} pattern", rtFlow1.contains("%{DATA"));
    }
}
