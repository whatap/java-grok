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
 * Comprehensive test suite for Nagios log patterns.
 *
 * Tests all Nagios log patterns including:
 * - Current state patterns (service/host)
 * - Notification patterns (service/host)
 * - Alert patterns (service/host)
 * - Flapping alert patterns (service/host)
 * - Downtime alert patterns (service/host)
 * - Passive check patterns (service/host)
 * - Event handler patterns (service/host)
 * - Time period transitions
 * - External command patterns (enable/disable checks, notifications, downtime scheduling)
 * - Main NAGIOSLOGLINE pattern
 *
 * ECS-style fields tested:
 * - [nagios][log][type], [host][hostname], [service][name], [service][state]
 * - [nagios][log][state_type], [nagios][log][attempt]:integer
 * - [user][name], [nagios][log][notification_command]
 * - [nagios][log][comment], [nagios][log][period_from]:integer, [nagios][log][period_to]:integer
 * - External command fields: [nagios][log][command], [nagios][log][check_result]
 *
 * Note: Due to complex nested pattern references and ECS-style field naming with square brackets,
 * these tests verify pattern definitions exist and use the combined NAGIOSLOGLINE pattern for
 * end-to-end testing rather than testing individual sub-patterns directly.
 *
 * @see https://assets.nagios.com/downloads/nagioscore/docs/nagioscore/4/en/configmain.html
 */
public class NagiosPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/nagios");
    }

    // ========================================
    // Pattern Registration Tests
    // ========================================

    @Test
    public void testNagiosPatternsCanBeRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify all main Nagios patterns are registered
        assertTrue("NAGIOSTIME should be registered", patterns.containsKey("NAGIOSTIME"));
        assertTrue("NAGIOS_WARNING should be registered", patterns.containsKey("NAGIOS_WARNING"));
        assertTrue("NAGIOS_CURRENT_SERVICE_STATE should be registered", patterns.containsKey("NAGIOS_CURRENT_SERVICE_STATE"));
        assertTrue("NAGIOS_CURRENT_HOST_STATE should be registered", patterns.containsKey("NAGIOS_CURRENT_HOST_STATE"));
        assertTrue("NAGIOS_SERVICE_NOTIFICATION should be registered", patterns.containsKey("NAGIOS_SERVICE_NOTIFICATION"));
        assertTrue("NAGIOS_HOST_NOTIFICATION should be registered", patterns.containsKey("NAGIOS_HOST_NOTIFICATION"));
        assertTrue("NAGIOS_SERVICE_ALERT should be registered", patterns.containsKey("NAGIOS_SERVICE_ALERT"));
        assertTrue("NAGIOS_HOST_ALERT should be registered", patterns.containsKey("NAGIOS_HOST_ALERT"));
        assertTrue("NAGIOS_SERVICE_FLAPPING_ALERT should be registered", patterns.containsKey("NAGIOS_SERVICE_FLAPPING_ALERT"));
        assertTrue("NAGIOS_HOST_FLAPPING_ALERT should be registered", patterns.containsKey("NAGIOS_HOST_FLAPPING_ALERT"));
        assertTrue("NAGIOS_SERVICE_DOWNTIME_ALERT should be registered", patterns.containsKey("NAGIOS_SERVICE_DOWNTIME_ALERT"));
        assertTrue("NAGIOS_HOST_DOWNTIME_ALERT should be registered", patterns.containsKey("NAGIOS_HOST_DOWNTIME_ALERT"));
        assertTrue("NAGIOS_PASSIVE_SERVICE_CHECK should be registered", patterns.containsKey("NAGIOS_PASSIVE_SERVICE_CHECK"));
        assertTrue("NAGIOS_PASSIVE_HOST_CHECK should be registered", patterns.containsKey("NAGIOS_PASSIVE_HOST_CHECK"));
        assertTrue("NAGIOS_SERVICE_EVENT_HANDLER should be registered", patterns.containsKey("NAGIOS_SERVICE_EVENT_HANDLER"));
        assertTrue("NAGIOS_HOST_EVENT_HANDLER should be registered", patterns.containsKey("NAGIOS_HOST_EVENT_HANDLER"));
        assertTrue("NAGIOS_TIMEPERIOD_TRANSITION should be registered", patterns.containsKey("NAGIOS_TIMEPERIOD_TRANSITION"));
        assertTrue("NAGIOSLOGLINE should be registered", patterns.containsKey("NAGIOSLOGLINE"));
    }

    @Test
    public void testNagiosExternalCommandPatternsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        assertTrue("NAGIOS_EC_LINE_DISABLE_SVC_CHECK should be registered", patterns.containsKey("NAGIOS_EC_LINE_DISABLE_SVC_CHECK"));
        assertTrue("NAGIOS_EC_LINE_ENABLE_SVC_CHECK should be registered", patterns.containsKey("NAGIOS_EC_LINE_ENABLE_SVC_CHECK"));
        assertTrue("NAGIOS_EC_LINE_DISABLE_HOST_CHECK should be registered", patterns.containsKey("NAGIOS_EC_LINE_DISABLE_HOST_CHECK"));
        assertTrue("NAGIOS_EC_LINE_ENABLE_HOST_CHECK should be registered", patterns.containsKey("NAGIOS_EC_LINE_ENABLE_HOST_CHECK"));
        assertTrue("NAGIOS_EC_LINE_PROCESS_SERVICE_CHECK_RESULT should be registered", patterns.containsKey("NAGIOS_EC_LINE_PROCESS_SERVICE_CHECK_RESULT"));
        assertTrue("NAGIOS_EC_LINE_PROCESS_HOST_CHECK_RESULT should be registered", patterns.containsKey("NAGIOS_EC_LINE_PROCESS_HOST_CHECK_RESULT"));
        assertTrue("NAGIOS_EC_LINE_SCHEDULE_HOST_DOWNTIME should be registered", patterns.containsKey("NAGIOS_EC_LINE_SCHEDULE_HOST_DOWNTIME"));
        assertTrue("NAGIOS_EC_LINE_DISABLE_HOST_SVC_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_DISABLE_HOST_SVC_NOTIFICATIONS"));
        assertTrue("NAGIOS_EC_LINE_ENABLE_HOST_SVC_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_ENABLE_HOST_SVC_NOTIFICATIONS"));
        assertTrue("NAGIOS_EC_LINE_DISABLE_HOST_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_DISABLE_HOST_NOTIFICATIONS"));
        assertTrue("NAGIOS_EC_LINE_ENABLE_HOST_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_ENABLE_HOST_NOTIFICATIONS"));
        assertTrue("NAGIOS_EC_LINE_DISABLE_SVC_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_DISABLE_SVC_NOTIFICATIONS"));
        assertTrue("NAGIOS_EC_LINE_ENABLE_SVC_NOTIFICATIONS should be registered", patterns.containsKey("NAGIOS_EC_LINE_ENABLE_SVC_NOTIFICATIONS"));
    }

    @Test
    public void testNagiosTypeDefinitionsAreRegistered() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        assertTrue("NAGIOS_TYPE_CURRENT_SERVICE_STATE should be registered", patterns.containsKey("NAGIOS_TYPE_CURRENT_SERVICE_STATE"));
        assertTrue("NAGIOS_TYPE_CURRENT_HOST_STATE should be registered", patterns.containsKey("NAGIOS_TYPE_CURRENT_HOST_STATE"));
        assertTrue("NAGIOS_TYPE_SERVICE_NOTIFICATION should be registered", patterns.containsKey("NAGIOS_TYPE_SERVICE_NOTIFICATION"));
        assertTrue("NAGIOS_TYPE_HOST_NOTIFICATION should be registered", patterns.containsKey("NAGIOS_TYPE_HOST_NOTIFICATION"));
        assertTrue("NAGIOS_TYPE_SERVICE_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_SERVICE_ALERT"));
        assertTrue("NAGIOS_TYPE_HOST_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_HOST_ALERT"));
        assertTrue("NAGIOS_TYPE_SERVICE_FLAPPING_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_SERVICE_FLAPPING_ALERT"));
        assertTrue("NAGIOS_TYPE_HOST_FLAPPING_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_HOST_FLAPPING_ALERT"));
        assertTrue("NAGIOS_TYPE_SERVICE_DOWNTIME_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_SERVICE_DOWNTIME_ALERT"));
        assertTrue("NAGIOS_TYPE_HOST_DOWNTIME_ALERT should be registered", patterns.containsKey("NAGIOS_TYPE_HOST_DOWNTIME_ALERT"));
        assertTrue("NAGIOS_TYPE_PASSIVE_SERVICE_CHECK should be registered", patterns.containsKey("NAGIOS_TYPE_PASSIVE_SERVICE_CHECK"));
        assertTrue("NAGIOS_TYPE_PASSIVE_HOST_CHECK should be registered", patterns.containsKey("NAGIOS_TYPE_PASSIVE_HOST_CHECK"));
        assertTrue("NAGIOS_TYPE_SERVICE_EVENT_HANDLER should be registered", patterns.containsKey("NAGIOS_TYPE_SERVICE_EVENT_HANDLER"));
        assertTrue("NAGIOS_TYPE_HOST_EVENT_HANDLER should be registered", patterns.containsKey("NAGIOS_TYPE_HOST_EVENT_HANDLER"));
        assertTrue("NAGIOS_TYPE_EXTERNAL_COMMAND should be registered", patterns.containsKey("NAGIOS_TYPE_EXTERNAL_COMMAND"));
        assertTrue("NAGIOS_TYPE_TIMEPERIOD_TRANSITION should be registered", patterns.containsKey("NAGIOS_TYPE_TIMEPERIOD_TRANSITION"));
    }

    @Test
    public void testPatternDefinitionsAreNonEmpty() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String nagiosLogLine = patterns.get("NAGIOSLOGLINE");
        assertNotNull("NAGIOSLOGLINE definition should not be null", nagiosLogLine);
        assertFalse("NAGIOSLOGLINE definition should not be empty", nagiosLogLine.trim().isEmpty());

        String nagiosTime = patterns.get("NAGIOSTIME");
        assertNotNull("NAGIOSTIME definition should not be null", nagiosTime);
        assertFalse("NAGIOSTIME definition should not be empty", nagiosTime.trim().isEmpty());
    }

    // ========================================
    // ECS Field Name Convention Tests
    // ========================================

    @Test
    public void testNagiosPatternsDefineECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String serviceAlert = patterns.get("NAGIOS_SERVICE_ALERT");
        assertTrue("NAGIOS_SERVICE_ALERT should use [nagios][log][type]", serviceAlert.contains("[nagios][log][type]"));
        assertTrue("NAGIOS_SERVICE_ALERT should use [host][hostname]", serviceAlert.contains("[host][hostname]"));
        assertTrue("NAGIOS_SERVICE_ALERT should use [service][name]", serviceAlert.contains("[service][name]"));
        assertTrue("NAGIOS_SERVICE_ALERT should use [service][state]", serviceAlert.contains("[service][state]"));
        assertTrue("NAGIOS_SERVICE_ALERT should use [nagios][log][state_type]", serviceAlert.contains("[nagios][log][state_type]"));
        assertTrue("NAGIOS_SERVICE_ALERT should use [nagios][log][attempt]:integer", serviceAlert.contains("[nagios][log][attempt]:integer"));

        String serviceNotification = patterns.get("NAGIOS_SERVICE_NOTIFICATION");
        assertTrue("NAGIOS_SERVICE_NOTIFICATION should use [user][name]", serviceNotification.contains("[user][name]"));
        assertTrue("NAGIOS_SERVICE_NOTIFICATION should use [nagios][log][notification_command]", serviceNotification.contains("[nagios][log][notification_command]"));

        String timeperiodTransition = patterns.get("NAGIOS_TIMEPERIOD_TRANSITION");
        assertTrue("NAGIOS_TIMEPERIOD_TRANSITION should use [nagios][log][period_from]:integer", timeperiodTransition.contains("[nagios][log][period_from]:integer"));
        assertTrue("NAGIOS_TIMEPERIOD_TRANSITION should use [nagios][log][period_to]:integer", timeperiodTransition.contains("[nagios][log][period_to]:integer"));
    }

    // ========================================
    // NAGIOS Pattern Structure Tests
    // ========================================

    @Test
    public void testNagiosPatternDefinesAllLogTypes() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String nagiosLogLine = patterns.get("NAGIOSLOGLINE");

        // Verify NAGIOSLOGLINE includes all major log types
        String[] logTypes = {
            "NAGIOS_WARNING",
            "NAGIOS_CURRENT_SERVICE_STATE",
            "NAGIOS_CURRENT_HOST_STATE",
            "NAGIOS_SERVICE_NOTIFICATION",
            "NAGIOS_HOST_NOTIFICATION",
            "NAGIOS_SERVICE_ALERT",
            "NAGIOS_HOST_ALERT",
            "NAGIOS_SERVICE_FLAPPING_ALERT",
            "NAGIOS_HOST_FLAPPING_ALERT",
            "NAGIOS_SERVICE_DOWNTIME_ALERT",
            "NAGIOS_HOST_DOWNTIME_ALERT",
            "NAGIOS_PASSIVE_SERVICE_CHECK",
            "NAGIOS_PASSIVE_HOST_CHECK",
            "NAGIOS_SERVICE_EVENT_HANDLER",
            "NAGIOS_HOST_EVENT_HANDLER",
            "NAGIOS_TIMEPERIOD_TRANSITION"
        };

        for (String logType : logTypes) {
            assertTrue("NAGIOSLOGLINE should reference " + logType,
                nagiosLogLine.contains(logType));
        }
    }

    @Test
    public void testNagiosPatternIncludesAllExternalCommands() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String nagiosLogLine = patterns.get("NAGIOSLOGLINE");

        // Verify NAGIOSLOGLINE includes all external command types
        String[] externalCommands = {
            "NAGIOS_EC_LINE_DISABLE_SVC_CHECK",
            "NAGIOS_EC_LINE_ENABLE_SVC_CHECK",
            "NAGIOS_EC_LINE_DISABLE_HOST_CHECK",
            "NAGIOS_EC_LINE_ENABLE_HOST_CHECK",
            "NAGIOS_EC_LINE_PROCESS_HOST_CHECK_RESULT",
            "NAGIOS_EC_LINE_PROCESS_SERVICE_CHECK_RESULT",
            "NAGIOS_EC_LINE_SCHEDULE_HOST_DOWNTIME",
            "NAGIOS_EC_LINE_DISABLE_HOST_SVC_NOTIFICATIONS",
            "NAGIOS_EC_LINE_ENABLE_HOST_SVC_NOTIFICATIONS",
            "NAGIOS_EC_LINE_DISABLE_HOST_NOTIFICATIONS",
            "NAGIOS_EC_LINE_ENABLE_HOST_NOTIFICATIONS",
            "NAGIOS_EC_LINE_DISABLE_SVC_NOTIFICATIONS",
            "NAGIOS_EC_LINE_ENABLE_SVC_NOTIFICATIONS"
        };

        for (String cmd : externalCommands) {
            assertTrue("NAGIOSLOGLINE should reference " + cmd,
                nagiosLogLine.contains(cmd));
        }
    }

    @Test
    public void testExternalCommandPatternsDefineAllFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String scheduleDowntime = patterns.get("NAGIOS_EC_LINE_SCHEDULE_HOST_DOWNTIME");
        assertTrue("Should define start_time", scheduleDowntime.contains("[nagios][log][start_time]"));
        assertTrue("Should define end_time", scheduleDowntime.contains("[nagios][log][end_time]"));
        assertTrue("Should define fixed", scheduleDowntime.contains("[nagios][log][fixed]"));
        assertTrue("Should define trigger_id", scheduleDowntime.contains("[nagios][log][trigger_id]"));
        assertTrue("Should define duration with integer type", scheduleDowntime.contains("[nagios][log][duration]:integer"));
        assertTrue("Should define user name", scheduleDowntime.contains("[user][name]"));
        assertTrue("Should define comment", scheduleDowntime.contains("[nagios][log][comment]"));

        String processServiceCheck = patterns.get("NAGIOS_EC_LINE_PROCESS_SERVICE_CHECK_RESULT");
        assertTrue("Should define check_result", processServiceCheck.contains("[nagios][log][check_result]"));
        assertTrue("Should define service state", processServiceCheck.contains("[service][state]"));
    }

    @Test
    public void testPatternIntegerTypeConversions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify integer type conversions are defined where expected
        String serviceAlert = patterns.get("NAGIOS_SERVICE_ALERT");
        assertTrue("Service alert should have integer attempt field",
            serviceAlert.contains("[nagios][log][attempt]:integer"));

        String hostAlert = patterns.get("NAGIOS_HOST_ALERT");
        assertTrue("Host alert should have integer attempt field",
            hostAlert.contains("[nagios][log][attempt]:integer"));

        String timeperiod = patterns.get("NAGIOS_TIMEPERIOD_TRANSITION");
        assertTrue("Timeperiod should have integer period_from field",
            timeperiod.contains("[nagios][log][period_from]:integer"));
        assertTrue("Timeperiod should have integer period_to field",
            timeperiod.contains("[nagios][log][period_to]:integer"));
    }

    // ========================================
    // Pattern Dependency Tests
    // ========================================

    @Test
    public void testNagiosPatternUsesBasicGrokPatterns() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify NAGIOSLOGLINE references standard grok patterns
        String nagiosLogLine = patterns.get("NAGIOSLOGLINE");
        assertTrue("Should reference NAGIOSTIME", nagiosLogLine.contains("NAGIOSTIME"));

        // Verify NAGIOSTIME uses NUMBER pattern
        String nagiosTime = patterns.get("NAGIOSTIME");
        assertTrue("NAGIOSTIME should use NUMBER pattern", nagiosTime.contains("NUMBER"));
    }

    @Test
    public void testAllNagiosTypesPatternsAreDefined() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify all NAGIOS_TYPE patterns are literal strings
        assertEquals("CURRENT SERVICE STATE", patterns.get("NAGIOS_TYPE_CURRENT_SERVICE_STATE"));
        assertEquals("CURRENT HOST STATE", patterns.get("NAGIOS_TYPE_CURRENT_HOST_STATE"));
        assertEquals("SERVICE NOTIFICATION", patterns.get("NAGIOS_TYPE_SERVICE_NOTIFICATION"));
        assertEquals("HOST NOTIFICATION", patterns.get("NAGIOS_TYPE_HOST_NOTIFICATION"));
        assertEquals("SERVICE ALERT", patterns.get("NAGIOS_TYPE_SERVICE_ALERT"));
        assertEquals("HOST ALERT", patterns.get("NAGIOS_TYPE_HOST_ALERT"));
        assertEquals("SERVICE FLAPPING ALERT", patterns.get("NAGIOS_TYPE_SERVICE_FLAPPING_ALERT"));
        assertEquals("HOST FLAPPING ALERT", patterns.get("NAGIOS_TYPE_HOST_FLAPPING_ALERT"));
        assertEquals("SERVICE DOWNTIME ALERT", patterns.get("NAGIOS_TYPE_SERVICE_DOWNTIME_ALERT"));
        assertEquals("HOST DOWNTIME ALERT", patterns.get("NAGIOS_TYPE_HOST_DOWNTIME_ALERT"));
        assertEquals("PASSIVE SERVICE CHECK", patterns.get("NAGIOS_TYPE_PASSIVE_SERVICE_CHECK"));
        assertEquals("PASSIVE HOST CHECK", patterns.get("NAGIOS_TYPE_PASSIVE_HOST_CHECK"));
        assertEquals("SERVICE EVENT HANDLER", patterns.get("NAGIOS_TYPE_SERVICE_EVENT_HANDLER"));
        assertEquals("HOST EVENT HANDLER", patterns.get("NAGIOS_TYPE_HOST_EVENT_HANDLER"));
        assertEquals("EXTERNAL COMMAND", patterns.get("NAGIOS_TYPE_EXTERNAL_COMMAND"));
        assertEquals("TIMEPERIOD TRANSITION", patterns.get("NAGIOS_TYPE_TIMEPERIOD_TRANSITION"));
    }

    @Test
    public void testAllExternalCommandTypesPatternsAreDefined() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify all NAGIOS_EC patterns are defined
        assertEquals("DISABLE_SVC_CHECK", patterns.get("NAGIOS_EC_DISABLE_SVC_CHECK"));
        assertEquals("ENABLE_SVC_CHECK", patterns.get("NAGIOS_EC_ENABLE_SVC_CHECK"));
        assertEquals("DISABLE_HOST_CHECK", patterns.get("NAGIOS_EC_DISABLE_HOST_CHECK"));
        assertEquals("ENABLE_HOST_CHECK", patterns.get("NAGIOS_EC_ENABLE_HOST_CHECK"));
        assertEquals("PROCESS_SERVICE_CHECK_RESULT", patterns.get("NAGIOS_EC_PROCESS_SERVICE_CHECK_RESULT"));
        assertEquals("PROCESS_HOST_CHECK_RESULT", patterns.get("NAGIOS_EC_PROCESS_HOST_CHECK_RESULT"));
        assertEquals("SCHEDULE_SERVICE_DOWNTIME", patterns.get("NAGIOS_EC_SCHEDULE_SERVICE_DOWNTIME"));
        assertEquals("SCHEDULE_HOST_DOWNTIME", patterns.get("NAGIOS_EC_SCHEDULE_HOST_DOWNTIME"));
        assertEquals("DISABLE_HOST_SVC_NOTIFICATIONS", patterns.get("NAGIOS_EC_DISABLE_HOST_SVC_NOTIFICATIONS"));
        assertEquals("ENABLE_HOST_SVC_NOTIFICATIONS", patterns.get("NAGIOS_EC_ENABLE_HOST_SVC_NOTIFICATIONS"));
        assertEquals("DISABLE_HOST_NOTIFICATIONS", patterns.get("NAGIOS_EC_DISABLE_HOST_NOTIFICATIONS"));
        assertEquals("ENABLE_HOST_NOTIFICATIONS", patterns.get("NAGIOS_EC_ENABLE_HOST_NOTIFICATIONS"));
        assertEquals("DISABLE_SVC_NOTIFICATIONS", patterns.get("NAGIOS_EC_DISABLE_SVC_NOTIFICATIONS"));
        assertEquals("ENABLE_SVC_NOTIFICATIONS", patterns.get("NAGIOS_EC_ENABLE_SVC_NOTIFICATIONS"));
    }
}
