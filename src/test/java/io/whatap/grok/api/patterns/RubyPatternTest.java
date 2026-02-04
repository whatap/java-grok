package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Ruby pattern definitions.
 * Tests Ruby logger patterns including RUBY_LOGLEVEL and RUBY_LOGGER.
 */
public class RubyPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/ruby");
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testRubyPatternsAreRegistered() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify all Ruby patterns are registered
        assertTrue("RUBY_LOGLEVEL should be registered", patterns.containsKey("RUBY_LOGLEVEL"));
        assertTrue("RUBY_LOGGER should be registered", patterns.containsKey("RUBY_LOGGER"));
    }

    @Test
    public void testRubyPatternDefinitions() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify RUBY_LOGLEVEL pattern matches expected log levels
        String rubyLoglevel = patterns.get("RUBY_LOGLEVEL");
        assertNotNull("RUBY_LOGLEVEL should be defined", rubyLoglevel);
        assertTrue("RUBY_LOGLEVEL should contain DEBUG", rubyLoglevel.contains("DEBUG"));
        assertTrue("RUBY_LOGLEVEL should contain FATAL", rubyLoglevel.contains("FATAL"));
        assertTrue("RUBY_LOGLEVEL should contain ERROR", rubyLoglevel.contains("ERROR"));
        assertTrue("RUBY_LOGLEVEL should contain WARN", rubyLoglevel.contains("WARN"));
        assertTrue("RUBY_LOGLEVEL should contain INFO", rubyLoglevel.contains("INFO"));

        // Verify RUBY_LOGGER pattern structure
        String rubyLogger = patterns.get("RUBY_LOGGER");
        assertNotNull("RUBY_LOGGER should be defined", rubyLogger);
        assertTrue("RUBY_LOGGER should contain timestamp field", rubyLogger.contains("timestamp"));
        assertTrue("RUBY_LOGGER should contain pid field", rubyLogger.contains("pid"));
        assertTrue("RUBY_LOGGER should contain loglevel field", rubyLogger.contains("loglevel"));
        assertTrue("RUBY_LOGGER should contain progname field", rubyLogger.contains("progname"));
        assertTrue("RUBY_LOGGER should contain message field", rubyLogger.contains("message"));
    }

    // ========== RUBY_LOGLEVEL Pattern Tests ==========

    @Test
    public void testRubyLoglevelPattern() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGLEVEL:level}");

        String[] validLevels = {
            "DEBUG",
            "FATAL",
            "ERROR",
            "WARN",
            "INFO"
        };

        for (String level : validLevels) {
            Match match = grok.match(level);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log level: " + level, captured);
            assertFalse("Should capture fields for: " + level, captured.isEmpty());
            assertEquals(level, captured.get("level"));
        }
    }

    @Test
    public void testRubyLoglevelPatternRejectsInvalidLevels() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGLEVEL:level}");

        String[] invalidLevels = {
            "TRACE",
            "NOTICE",
            "CRITICAL",
            "VERBOSE",
            "debug",  // lowercase should not match
            "info"
        };

        for (String level : invalidLevels) {
            Match match = grok.match(level);
            Map<String, Object> captured = match.capture();
            // capture() returns an empty map (not null) when there's no match
            assertTrue("Should not match invalid level: " + level,
                    captured == null || captured.isEmpty());
        }
    }

    @Test
    public void testRubyLoglevelPatternInContext() throws Exception {
        // Test that RUBY_LOGLEVEL can match within a larger string
        Grok grok = compiler.compile("^\\[%{RUBY_LOGLEVEL:level}\\]");

        String logLine = "[ERROR] Something went wrong";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log level in context", captured);
        assertEquals("ERROR", captured.get("level"));
    }

    // ========== RUBY_LOGGER Pattern Tests - Compilation ==========

    @Test
    public void testRubyLoggerPatternCompiles() throws Exception {
        // Verify the pattern compiles without errors
        Grok grok = compiler.compile("%{RUBY_LOGGER}");
        assertNotNull("RUBY_LOGGER should compile successfully", grok);
    }

    // ========== RUBY_LOGGER Pattern Tests - DEBUG Level ==========

    @Test
    public void testRubyLoggerDebugLevel() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "D, [2023-10-11T22:14:15.123456 #12345]  DEBUG -- myapp: Debug message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match DEBUG log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());

        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("DEBUG", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("Debug message", captured.get("message"));
    }

    @Test
    public void testRubyLoggerDebugLevelMultiWordMessage() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "D, [2023-10-11T22:14:15.123456 #12345]  DEBUG -- myapp: This is a longer debug message with multiple words";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match DEBUG log with multi-word message", captured);
        assertEquals("This is a longer debug message with multiple words", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - INFO Level ==========

    @Test
    public void testRubyLoggerInfoLevel() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Info message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match INFO log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());

        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("INFO", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("Info message", captured.get("message"));
    }

    @Test
    public void testRubyLoggerInfoLevelWithSpecialChars() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: User logged in: user@example.com";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match INFO log with special characters", captured);
        assertEquals("User logged in: user@example.com", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - WARN Level ==========

    @Test
    public void testRubyLoggerWarnLevel() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "W, [2023-10-11T22:14:15.123456 #12345]   WARN -- myapp: Warning message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match WARN log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());

        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("WARN", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("Warning message", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - ERROR Level ==========

    @Test
    public void testRubyLoggerErrorLevel() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "E, [2023-10-11T22:14:15.123456 #12345]  ERROR -- myapp: Error message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match ERROR log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());

        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("ERROR", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("Error message", captured.get("message"));
    }

    @Test
    public void testRubyLoggerErrorLevelWithStackTrace() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "E, [2023-10-11T22:14:15.123456 #12345]  ERROR -- myapp: RuntimeError: Something went wrong";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match ERROR log with exception", captured);
        assertEquals("RuntimeError: Something went wrong", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - FATAL Level ==========

    @Test
    public void testRubyLoggerFatalLevel() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "F, [2023-10-11T22:14:15.123456 #12345]  FATAL -- myapp: Fatal error occurred";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match FATAL log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());

        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("FATAL", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("Fatal error occurred", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - Variations ==========

    @Test
    public void testRubyLoggerWithDifferentPids() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String[] pids = {
            "1",
            "99",
            "1234",
            "99999"
        };

        for (String pid : pids) {
            String logLine = String.format("I, [2023-10-11T22:14:15.123456 #%s]   INFO -- myapp: Test message", pid);
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();

            assertNotNull("Failed to match log with PID: " + pid, captured);
            assertEquals("Should capture PID: " + pid, pid, captured.get("pid"));
        }
    }

    @Test
    public void testRubyLoggerWithDifferentPrognames() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String[] prognames = {
            "myapp",
            "rails",
            "sidekiq",
            "my_app",
            "app-name",
            "App123"
        };

        for (String progname : prognames) {
            String logLine = String.format("I, [2023-10-11T22:14:15.123456 #12345]   INFO -- %s: Test message", progname);
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();

            assertNotNull("Failed to match log with progname: " + progname, captured);
            assertEquals("Should capture progname: " + progname, progname, captured.get("progname"));
        }
    }

    @Test
    public void testRubyLoggerWithDifferentTimestamps() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String[] timestamps = {
            "2023-10-11T22:14:15.123456",
            "2020-01-01T00:00:00.000000",
            "2025-12-31T23:59:59.999999",
            "2023-06-15T12:30:45.654321"
        };

        for (String timestamp : timestamps) {
            String logLine = String.format("I, [%s #12345]   INFO -- myapp: Test message", timestamp);
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();

            assertNotNull("Failed to match log with timestamp: " + timestamp, captured);
            assertEquals("Should capture timestamp: " + timestamp, timestamp, captured.get("timestamp"));
        }
    }

    @Test
    public void testRubyLoggerWithVaryingWhitespace() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        // Ruby logger can have varying amounts of whitespace before the log level
        String[] logLines = {
            "I, [2023-10-11T22:14:15.123456 #12345] INFO -- myapp: Message with minimal space",
            "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Message with extra spaces",
            "I, [2023-10-11T22:14:15.123456 #12345]    INFO -- myapp: Message with many spaces",
            "E, [2023-10-11T22:14:15.123456 #12345]  ERROR -- myapp: Error with two spaces"
        };

        for (String logLine : logLines) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log with varying whitespace: " + logLine, captured);
            assertNotNull("Should capture message", captured.get("message"));
        }
    }

    @Test
    public void testRubyLoggerWithVaryingWhitespaceBeforeProgname() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        // The pattern allows for one or more spaces/dashes before progname
        String[] logLines = {
            "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Message",
            "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- - myapp: Message",
            "I, [2023-10-11T22:14:15.123456 #12345]   INFO --  myapp: Message"
        };

        for (String logLine : logLines) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log with varying progname spacing: " + logLine, captured);
            assertNotNull("Should capture message", captured.get("message"));
        }
    }

    // ========== RUBY_LOGGER Pattern Tests - Empty Message ==========

    @Test
    public void testRubyLoggerWithEmptyMessage() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: ";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log with empty message", captured);
        assertEquals("", captured.get("message"));
    }

    // ========== RUBY_LOGGER Pattern Tests - Real-world Examples ==========

    @Test
    public void testRealWorldRailsLog() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- : Started GET \"/users\" for 127.0.0.1 at 2023-10-11 22:14:15 +0000";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Rails log", captured);
        assertEquals("INFO", captured.get("loglevel"));
        assertTrue("Should capture Rails request message",
                captured.get("message").toString().contains("Started GET"));
    }

    @Test
    public void testRealWorldSidekiqLog() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- : Sidekiq 6.5.0 starting";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Sidekiq log", captured);
        assertEquals("INFO", captured.get("loglevel"));
        assertTrue("Should capture Sidekiq startup message",
                captured.get("message").toString().contains("Sidekiq"));
    }

    @Test
    public void testRealWorldRakeTaskLog() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "D, [2023-10-11T22:14:15.123456 #12345]  DEBUG -- rake: Running database migration";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Rake task log", captured);
        assertEquals("DEBUG", captured.get("loglevel"));
        assertEquals("rake", captured.get("progname"));
        assertEquals("Running database migration", captured.get("message"));
    }

    @Test
    public void testRealWorldApplicationError() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "E, [2023-10-11T22:14:15.123456 #12345]  ERROR -- app: ActiveRecord::RecordNotFound: Couldn't find User with 'id'=999";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match application error log", captured);
        assertEquals("ERROR", captured.get("loglevel"));
        assertEquals("app", captured.get("progname"));
        assertTrue("Should capture ActiveRecord exception",
                captured.get("message").toString().contains("ActiveRecord::RecordNotFound"));
    }

    // ========== RUBY_LOGGER Pattern Tests - Edge Cases ==========

    @Test
    public void testRubyLoggerWithColonInMessage() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Key: value, Another: value2";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log with colons in message", captured);
        assertEquals("Key: value, Another: value2", captured.get("message"));
    }

    @Test
    public void testRubyLoggerWithUrlInMessage() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Fetching data from https://api.example.com/v1/users";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log with URL in message", captured);
        assertTrue("Should capture URL in message",
                captured.get("message").toString().contains("https://api.example.com/v1/users"));
    }

    @Test
    public void testRubyLoggerWithJsonInMessage() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Response: {\"status\":\"ok\",\"count\":42}";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log with JSON in message", captured);
        assertTrue("Should capture JSON in message",
                captured.get("message").toString().contains("{\"status\":\"ok\""));
    }

    // ========== Field Extraction Verification Tests ==========

    @Test
    public void testAllFieldsExtractedCorrectly() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "W, [2023-10-11T22:14:15.123456 #12345]   WARN -- myapp: This is a complete test message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match complete log line", captured);

        // Verify all fields are present
        assertTrue("Should have timestamp field", captured.containsKey("timestamp"));
        assertTrue("Should have pid field", captured.containsKey("pid"));
        assertTrue("Should have loglevel field", captured.containsKey("loglevel"));
        assertTrue("Should have progname field", captured.containsKey("progname"));
        assertTrue("Should have message field", captured.containsKey("message"));

        // Verify all fields have correct values
        assertEquals("2023-10-11T22:14:15.123456", captured.get("timestamp"));
        assertEquals("12345", captured.get("pid"));
        assertEquals("WARN", captured.get("loglevel"));
        assertEquals("myapp", captured.get("progname"));
        assertEquals("This is a complete test message", captured.get("message"));
    }

    @Test
    public void testFieldTypesAreStrings() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match log", captured);

        // Verify field types (all should be strings by default)
        assertTrue("timestamp should be String", captured.get("timestamp") instanceof String);
        assertTrue("pid should be String", captured.get("pid") instanceof String);
        assertTrue("loglevel should be String", captured.get("loglevel") instanceof String);
        assertTrue("progname should be String", captured.get("progname") instanceof String);
        assertTrue("message should be String", captured.get("message") instanceof String);
    }

    // ========== Pattern Combination Tests ==========

    @Test
    public void testRubyLoggerWithCustomFieldNames() throws Exception {
        // Test using RUBY_LOGGER with custom field names
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String logLine = "I, [2023-10-11T22:14:15.123456 #12345]   INFO -- myapp: Custom field test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match with default field names", captured);
        assertEquals("Custom field test", captured.get("message"));
    }

    @Test
    public void testMultipleRubyLogLines() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String[] logLines = {
            "D, [2023-10-11T22:14:15.123456 #12345]  DEBUG -- myapp: Debug message",
            "I, [2023-10-11T22:14:15.123457 #12345]   INFO -- myapp: Info message",
            "W, [2023-10-11T22:14:15.123458 #12345]   WARN -- myapp: Warning message",
            "E, [2023-10-11T22:14:15.123459 #12345]  ERROR -- myapp: Error message",
            "F, [2023-10-11T22:14:15.123460 #12345]  FATAL -- myapp: Fatal message"
        };

        for (String logLine : logLines) {
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLine, captured);
            assertNotNull("Should have timestamp", captured.get("timestamp"));
            assertNotNull("Should have loglevel", captured.get("loglevel"));
            assertNotNull("Should have message", captured.get("message"));
        }
    }

    // ========== Negative Tests ==========

    @Test
    public void testRubyLoggerDoesNotMatchInvalidFormat() throws Exception {
        Grok grok = compiler.compile("%{RUBY_LOGGER}");

        String[] invalidLines = {
            "Not a Ruby log line",
            "2023-10-11 22:14:15 INFO Some message",  // Different format
            "[2023-10-11T22:14:15.123456] INFO: Message",  // Missing required parts
            "I, 2023-10-11T22:14:15.123456 #12345 INFO -- myapp: Message",  // Missing brackets
            "I, [2023-10-11T22:14:15.123456] INFO -- myapp: Message"  // Missing PID
        };

        for (String invalidLine : invalidLines) {
            Match match = grok.match(invalidLine);
            Map<String, Object> captured = match.capture();
            assertTrue("Should not match invalid line: " + invalidLine,
                    captured == null || captured.isEmpty());
        }
    }
}
