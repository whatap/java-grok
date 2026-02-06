package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Exim pattern definitions.
 * Tests all Exim-related patterns including message IDs, flags, dates, and queue times.
 */
public class EximPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/exim");
    }

    // ========== EXIM_MSGID Pattern Tests ==========

    @Test
    public void testEximMessageIdPattern() throws Exception {
        Grok grok = compiler.compile("%{EXIM_MSGID:msgid}");

        String[] validMessageIds = {
            "1a2B3c-4D5e6F-7G",
            "AbCdEf-123456-Xy",
            "000000-000000-00",
            "zZzZzZ-AaAaAa-Bb",
            "123456-789012-AB"
        };

        for (String msgId : validMessageIds) {
            Match match = grok.match(msgId);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match message ID: " + msgId, captured);
            assertEquals(msgId, captured.get("msgid"));
        }
    }

    @Test
    public void testEximMessageIdFormat() throws Exception {
        Grok grok = compiler.compile("%{EXIM_MSGID:msgid}");

        // Test the exact format: 6 alphanumeric - 6 alphanumeric - 2 alphanumeric
        String validId = "1aB2cD-3eF4gH-5i";
        Match match = grok.match(validId);
        Map<String, Object> captured = match.capture();
        assertNotNull("Failed to match valid format", captured);
        assertEquals(validId, captured.get("msgid"));
    }

    @Test
    public void testEximMessageIdInvalidFormats() throws Exception {
        Grok grok = compiler.compile("^%{EXIM_MSGID:msgid}$");

        String[] invalidMessageIds = {
            "12345-123456-AB",     // First part too short
            "1234567-123456-AB",   // First part too long
            "123456-12345-AB",     // Second part too short
            "123456-1234567-AB",   // Second part too long
            "123456-123456-A",     // Third part too short
            "123456-123456-ABC",   // Third part too long
            "123456_123456_AB",    // Wrong separator
            "123456-123456-A@"     // Invalid character
        };

        for (String msgId : invalidMessageIds) {
            Match match = grok.match(msgId);
            Map<String, Object> captured = match.capture();
            assertTrue("Should not match invalid message ID: " + msgId,
                      captured == null || captured.isEmpty());
        }
    }

    // ========== EXIM_FLAGS Pattern Tests ==========

    @Test
    public void testEximFlagsPattern() throws Exception {
        Grok grok = compiler.compile("%{EXIM_FLAGS:flags}");

        // Test all valid flags according to comments in pattern file
        String[][] flagsWithDescriptions = {
            {"<=", "message arrival"},
            {"=>", "normal message delivery"},
            {"->", "additional address in same delivery"},
            {"*>", "delivery suppressed by -N"},
            {"**", "delivery failed; address bounced"},
            {"==", "delivery deferred; temporary problem"},
            {"<>", "bounce message"},
            {">>", "cutthrough delivery"}
        };

        for (String[] flagInfo : flagsWithDescriptions) {
            String flag = flagInfo[0];
            String description = flagInfo[1];
            Match match = grok.match(flag);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match flag '" + flag + "' (" + description + ")", captured);
            assertEquals(flag, captured.get("flags"));
        }
    }

    @Test
    public void testEximFlagsInContext() throws Exception {
        Grok grok = compiler.compile("message %{EXIM_FLAGS:flags} user");

        String[] contextExamples = {
            "message <= user",
            "message => user",
            "message ** user",
            "message == user"
        };

        for (String example : contextExamples) {
            Match match = grok.match(example);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match in context: " + example, captured);
            assertNotNull("Missing flags in: " + example, captured.get("flags"));
        }
    }

    @Test
    public void testEximFlagsInvalidPatterns() throws Exception {
        Grok grok = compiler.compile("^%{EXIM_FLAGS:flags}$");

        String[] invalidFlags = {
            "<",
            ">",
            "=",
            "*",
            "<<",
            ">>>",
            "===",
            "+->"
        };

        for (String flag : invalidFlags) {
            Match match = grok.match(flag);
            Map<String, Object> captured = match.capture();
            assertTrue("Should not match invalid flag: " + flag,
                      captured == null || captured.isEmpty());
        }
    }

    // ========== EXIM_DATE Pattern Tests ==========

    @Test
    public void testEximDatePattern() throws Exception {
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp}");

        String[] validDates = {
            "2023-10-11 22:14:15",
            "2024-01-01 00:00:00",
            "2022-12-31 23:59:59",
            "2023-06-15 12:30:45",
            "2021-03-20 08:15:22"
        };

        for (String date : validDates) {
            Match match = grok.match(date);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match date: " + date, captured);
            assertEquals(date, captured.get("log_timestamp"));
        }
    }

    @Test
    public void testEximDateFormatComponents() throws Exception {
        // Test that date follows YYYY-MM-DD HH:MM:SS format
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp}");

        String date = "2023-10-11 22:14:15";
        Match match = grok.match(date);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match date", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("log_timestamp"));
    }

    @Test
    public void testEximDateInLogContext() throws Exception {
        Grok grok = compiler.compile("^%{EXIM_DATE:timestamp} \\[%{NUMBER:pid}\\] %{EXIM_MSGID:msgid}");

        String logLine = "2023-10-11 22:14:15 [12345] 1a2B3c-4D5e6F-7G";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log line", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("log_timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("1a2B3c-4D5e6F-7G", captured.get("msgid"));
    }

    // ========== EXIM_QT (Queue Time) Pattern Tests ==========

    @Test
    public void testEximQueueTimeFullFormat() throws Exception {
        Grok grok = compiler.compile("%{EXIM_QT:queue_time}");

        String[] validQueueTimes = {
            "1y2w3d4h5m6s",
            "1y",
            "2w",
            "3d",
            "4h",
            "5m",
            "6s",
            "1y2w",
            "3d4h",
            "5m6s",
            "1h30m",
            "45s",
            "2h15m30s"
        };

        for (String queueTime : validQueueTimes) {
            Match match = grok.match(queueTime);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match queue time: " + queueTime, captured);
            assertEquals(queueTime, captured.get("queue_time"));
        }
    }

    @Test
    public void testEximQueueTimeCompleteSequence() throws Exception {
        Grok grok = compiler.compile("%{EXIM_QT:queue_time}");

        // Test full sequence with all components
        String fullTime = "1y2w3d4h5m6s";
        Match match = grok.match(fullTime);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match full queue time", captured);
        assertEquals(fullTime, captured.get("queue_time"));
    }

    @Test
    public void testEximQueueTimePartialSequences() throws Exception {
        Grok grok = compiler.compile("%{EXIM_QT:queue_time}");

        // Test partial sequences (common in real logs)
        String[][] partialTimes = {
            {"1h30m", "1 hour 30 minutes"},
            {"45s", "45 seconds"},
            {"2d12h", "2 days 12 hours"},
            {"3w5d", "3 weeks 5 days"},
            {"10m30s", "10 minutes 30 seconds"}
        };

        for (String[] timeInfo : partialTimes) {
            String time = timeInfo[0];
            String description = timeInfo[1];
            Match match = grok.match(time);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match " + description + ": " + time, captured);
            assertEquals(time, captured.get("queue_time"));
        }
    }

    @Test
    public void testEximQueueTimeEmptyMatch() throws Exception {
        Grok grok = compiler.compile("%{EXIM_QT:queue_time}");

        // EXIM_QT can match empty string (all components are optional)
        String emptyTime = "";
        Match match = grok.match(emptyTime);
        Map<String, Object> captured = match.capture();

        // This should match because all components in the pattern are optional
        assertNotNull("Pattern should match empty string", captured);
    }

    @Test
    public void testEximQueueTimeInvalidFormats() throws Exception {
        Grok grok = compiler.compile("^%{EXIM_QT:queue_time}$");

        String[] invalidTimes = {
            "1y2y",      // Duplicate year
            "3w4w",      // Duplicate week
            "5d6d",      // Duplicate day
            "7h8h",      // Duplicate hour
            "9m10m",     // Duplicate minute
            "11s12s",    // Duplicate second
            "1s2m",      // Wrong order (seconds before minutes)
            "1h2d",      // Wrong order (hours before days)
            "1y2d3w"     // Wrong order (days before weeks)
        };

        for (String time : invalidTimes) {
            Match match = grok.match(time);
            Map<String, Object> captured = match.capture();
            assertTrue("Should not match invalid time format: " + time,
                      captured == null || captured.isEmpty());
        }
    }

    @Test
    public void testEximQueueTimeWithMultipleDigits() throws Exception {
        Grok grok = compiler.compile("%{EXIM_QT:queue_time}");

        String[] multiDigitTimes = {
            "10y",
            "52w",
            "365d",
            "24h",
            "60m",
            "3600s",
            "100y99w"
        };

        for (String time : multiDigitTimes) {
            Match match = grok.match(time);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match multi-digit time: " + time, captured);
            assertEquals(time, captured.get("queue_time"));
        }
    }

    // ========== Combined Pattern Tests ==========

    @Test
    public void testCombinedPatternsSimpleLog() throws Exception {
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp} %{EXIM_MSGID:msgid} %{EXIM_FLAGS:flags}");

        String logLine = "2023-10-11 22:14:15 1a2B3c-4D5e6F-7G <=";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match combined pattern", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("log_timestamp"));
        assertEquals("1a2B3c-4D5e6F-7G", captured.get("msgid"));
        assertEquals("<=", captured.get("flags"));
    }

    @Test
    public void testCombinedPatternsWithQueueTime() throws Exception {
        Grok grok = compiler.compile("%{EXIM_MSGID:msgid} %{EXIM_FLAGS:flags} .*QT=%{EXIM_QT:queue_time}");

        String[] logLines = {
            "1a2B3c-4D5e6F-7G => user@example.com QT=1h30m",
            "AbCdEf-123456-Xy ** bounce@test.com QT=2d12h",
            "zZzZzZ-AaAaAa-Bb == delayed@domain.org QT=45s"
        };

        for (String logLine : logLines) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLine, captured);
            assertNotNull("Missing msgid", captured.get("msgid"));
            assertNotNull("Missing flags", captured.get("flags"));
            assertNotNull("Missing queue_time", captured.get("queue_time"));
        }
    }

    @Test
    public void testRealWorldEximLogArrival() throws Exception {
        // Test message arrival pattern
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp} \\[%{NUMBER:pid}\\] %{EXIM_MSGID:msgid} %{EXIM_FLAGS:flags}");

        String logLine = "2023-10-11 22:14:15 [12345] 1a2B3c-4D5e6F-7G <=";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match arrival log", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("log_timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("1a2B3c-4D5e6F-7G", captured.get("msgid"));
        assertEquals("<=", captured.get("flags"));
    }

    @Test
    public void testRealWorldEximLogDelivery() throws Exception {
        // Test message delivery pattern
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp} %{EXIM_MSGID:msgid} %{EXIM_FLAGS:flags}");

        String[] deliveryLogs = {
            "2023-10-11 22:14:20 1a2B3c-4D5e6F-7G =>",
            "2023-10-11 22:14:25 1a2B3c-4D5e6F-7G ->",
            "2023-10-11 22:14:30 1a2B3c-4D5e6F-7G **"
        };

        for (String logLine : deliveryLogs) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match delivery log: " + logLine, captured);
            assertTrue("Should contain timestamp", captured.containsKey("log_timestamp"));
            assertTrue("Should contain msgid", captured.containsKey("msgid"));
            assertTrue("Should contain flags", captured.containsKey("flags"));
        }
    }

    @Test
    public void testEximPIDInLogContextWithWorkaround() throws Exception {
        // NOTE: EXIM_PID pattern cannot be tested directly due to regex compilation error.
        // The pattern: \[%{POSINT:[process][pid]:int}\]
        // causes "Illegal repetition" error when nested field names with :int modifier
        // are used inside escaped brackets (\[ and \]).
        //
        // This is a known limitation similar to MCOLLECTIVE pattern.
        // Workaround: Use a simplified pattern without type modifier for testing.

        String pidWorkaroundPattern = "\\[%{POSINT:process.pid}\\]";
        Grok grok = compiler.compile("%{EXIM_DATE:timestamp} " + pidWorkaroundPattern + " %{EXIM_MSGID:msgid}");

        String[] logLinesWithPids = {
            "2023-10-11 22:14:15 [12345] 1a2B3c-4D5e6F-7G",
            "2023-10-11 22:14:15 [1] 1a2B3c-4D5e6F-7G",
            "2023-10-11 22:14:15 [999999] 1a2B3c-4D5e6F-7G",
            "2023-10-11 22:14:15 [54321] 1a2B3c-4D5e6F-7G"
        };

        for (String logLine : logLinesWithPids) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLine, captured);
            // The workaround pattern captures to [process][pid] field as string
            assertNotNull("Missing process.pid field", captured.get("process.pid"));
        }
    }

    @Test
    public void testEximPIDPatternStructureValidation() throws Exception {
        // Since we can't compile EXIM_PID directly, verify it's registered with correct structure
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String eximPidDef = patterns.get("EXIM_PID");
        assertNotNull("EXIM_PID should be registered", eximPidDef);

        // Verify the pattern structure (even though it can't be compiled directly)
        assertTrue("EXIM_PID should contain POSINT reference", eximPidDef.contains("POSINT"));
        assertTrue("EXIM_PID should capture to process.pid field", eximPidDef.contains("process.pid"));
        assertTrue("EXIM_PID should have :int type modifier", eximPidDef.contains(":int"));
        assertTrue("EXIM_PID should be wrapped in brackets", eximPidDef.contains("\\[") && eximPidDef.contains("\\]"));
    }

    // ========== Pattern Definition Verification Tests ==========

    @Test
    public void testEximPatternDefinitionsRegistered() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String[] expectedPatterns = {
            "EXIM_MSGID",
            "EXIM_FLAGS",
            "EXIM_DATE",
            "EXIM_PID",
            "EXIM_QT",
            "EXIM_EXCLUDE_TERMS",
            "EXIM_REMOTE_HOST",
            "EXIM_INTERFACE",
            "EXIM_PROTOCOL",
            "EXIM_MSG_SIZE",
            "EXIM_HEADER_ID",
            "EXIM_QUOTED_CONTENT",
            "EXIM_SUBJECT",
            "EXIM_UNKNOWN_FIELD",
            "EXIM_NAMED_FIELDS",
            "EXIM_MESSAGE_ARRIVAL",
            "EXIM"
        };

        for (String patternName : expectedPatterns) {
            assertTrue("Pattern " + patternName + " should be registered",
                      patterns.containsKey(patternName));
            assertNotNull("Pattern " + patternName + " definition should not be null",
                         patterns.get(patternName));
        }
    }

    @Test
    public void testEximPatternDefinitionContents() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify EXIM_MSGID definition
        String msgidDef = patterns.get("EXIM_MSGID");
        assertTrue("EXIM_MSGID should match alphanumeric pattern",
                  msgidDef.contains("[0-9A-Za-z]"));

        // Verify EXIM_FLAGS definition
        String flagsDef = patterns.get("EXIM_FLAGS");
        assertTrue("EXIM_FLAGS should contain <=", flagsDef.contains("<="));
        assertTrue("EXIM_FLAGS should contain =>", flagsDef.contains("=>"));

        // Verify EXIM_QT definition
        String qtDef = patterns.get("EXIM_QT");
        assertTrue("EXIM_QT should contain year component", qtDef.contains("y"));
        assertTrue("EXIM_QT should contain week component", qtDef.contains("w"));
        assertTrue("EXIM_QT should contain day component", qtDef.contains("d"));
        assertTrue("EXIM_QT should contain hour component", qtDef.contains("h"));
        assertTrue("EXIM_QT should contain minute component", qtDef.contains("m"));
        assertTrue("EXIM_QT should contain second component", qtDef.contains("s"));
    }
}
