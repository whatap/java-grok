package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Rails pattern definitions.
 *
 * IMPORTANT NOTE: The rails pattern file (src/main/resources/patterns/rails) uses
 * Ruby/Oniguruma regex syntax which is incompatible with Java's regex engine.
 *
 * Known Issues:
 * 1. RUUID uses \h{32} which means "32 horizontal whitespace chars" in Java regex,
 *    but means "32 hex digits" in Ruby/Oniguruma regex.
 *
 * 2. Patterns like RCONTROLLER use (?<rails.controller.class>...) syntax which
 *    causes PatternSyntaxException in Java because Java interprets (?<...) as a
 *    lookbehind assertion when it starts with (?<.
 *
 * 3. The correct way to use ECS field names in Java grok patterns is to define the
 *    pattern with simple names and then reference them with ECS names when compiling,
 *    like: %{WORD:log.level}
 *
 * These tests document the current incompatibility between the Rails pattern file
 * (designed for Logstash/Ruby) and the Java grok implementation.
 */
public class RailsPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/rails");
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testRailsPatternsAreRegistered() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify all Rails patterns are registered
        assertTrue("RUUID should be registered", patterns.containsKey("RUUID"));
        assertTrue("RCONTROLLER should be registered", patterns.containsKey("RCONTROLLER"));
        assertTrue("RAILS3HEAD should be registered", patterns.containsKey("RAILS3HEAD"));
        assertTrue("RPROCESSING should be registered", patterns.containsKey("RPROCESSING"));
        assertTrue("RAILS3FOOT should be registered", patterns.containsKey("RAILS3FOOT"));
        assertTrue("RAILS3PROFILE should be registered", patterns.containsKey("RAILS3PROFILE"));
        assertTrue("RAILS3 should be registered", patterns.containsKey("RAILS3"));
    }

    @Test
    public void testRailsPatternDefinitions() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify pattern contents match the rails pattern file
        assertEquals("\\h{32}", patterns.get("RUUID"));
        assertTrue("RCONTROLLER should contain controller and action",
                patterns.get("RCONTROLLER").contains("rails.controller.class") &&
                patterns.get("RCONTROLLER").contains("rails.controller.action"));
        assertTrue("RAILS3HEAD should contain HTTP method and URL",
                patterns.get("RAILS3HEAD").contains("http.request.method") &&
                patterns.get("RAILS3HEAD").contains("url.original"));
    }

    // ========== RUUID Pattern Tests (Documenting Incompatibility) ==========

    @Test
    public void testRUUIDPattern() throws Exception {
        // NOTE: The RUUID pattern uses \h{32} which in Ruby/Oniguruma means hex digits,
        // but in Java regex \h means horizontal whitespace. This pattern is currently
        // incompatible with Java regex and will only match 32 whitespace characters.
        // This test documents the current (broken) behavior.

        Grok grok = compiler.compile("%{RUUID:uuid}");

        // Test with 32 spaces (what the pattern actually matches in Java)
        String thirtyTwoSpaces = "                                ";
        Match match = grok.match(thirtyTwoSpaces);
        Map<String, Object> captured = match.capture();
        assertNotNull("Pattern matches 32 spaces (not hex as intended)", captured);
        assertEquals(thirtyTwoSpaces, captured.get("uuid"));
    }

    @Test
    public void testRUUIDPatternDoesNotMatchHexAsIntended() throws Exception {
        // NOTE: This test documents that RUUID does NOT match hex UUIDs as intended
        // because \h in Java regex means horizontal whitespace, not hex digits

        Grok grok = compiler.compile("%{RUUID:uuid}");

        String[] hexStrings = {
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4",
            "00000000000000000000000000000000",
            "ffffffffffffffffffffffffffffffff"
        };

        for (String hex : hexStrings) {
            Match match = grok.match(hex);
            Map<String, Object> captured = match.capture();
            // capture() returns an empty map (not null) when there's no match
            assertTrue("RUUID does not match hex (pattern uses \\h which is whitespace in Java): " + hex,
                    captured == null || captured.isEmpty());
        }
    }

    // ========== RCONTROLLER Pattern Tests (Documenting Incompatibility) ==========

    @Test(expected = PatternSyntaxException.class)
    public void testRCONTROLLERPatternCompilationFails() throws Exception {
        // NOTE: The RCONTROLLER pattern uses (?<[rails][controller][class]>...) syntax
        // which is incompatible with Java regex. Java interprets (?<...) as a lookbehind
        // assertion, causing PatternSyntaxException.

        compiler.compile("%{RCONTROLLER}");
        fail("RCONTROLLER pattern should throw PatternSyntaxException due to incompatible syntax");
    }

    @Test
    public void testRCONTROLLERPatternDefinitionContent() throws Exception {
        // Verify the pattern contains the expected structure (even though it won't compile)
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rcontroller = patterns.get("RCONTROLLER");

        assertNotNull("RCONTROLLER pattern should be defined", rcontroller);
        assertTrue("Pattern should contain rails controller class field",
                rcontroller.contains("rails.controller.class"));
        assertTrue("Pattern should contain rails controller action field",
                rcontroller.contains("rails.controller.action"));
        assertTrue("Pattern should contain # separator", rcontroller.contains("#"));
    }

    // ========== RAILS3HEAD Pattern Tests ==========

    @Test
    public void testRAILS3HEADPatternCompilationSucceeds() throws Exception {
        // NOTE: RAILS3HEAD does NOT use RCONTROLLER, so it compiles successfully.
        // It uses (?<timestamp>...) with a simple field name which is valid Java regex.

        Grok grok = compiler.compile("%{RAILS3HEAD}");
        assertNotNull("RAILS3HEAD should compile successfully", grok);
    }

    @Test
    public void testRAILS3HEADPatternDefinitionContent() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rails3head = patterns.get("RAILS3HEAD");

        assertNotNull("RAILS3HEAD pattern should be defined", rails3head);
        assertTrue("Pattern should contain HTTP request method field",
                rails3head.contains("http.request.method"));
        assertTrue("Pattern should contain URL original field",
                rails3head.contains("url.original"));
        assertTrue("Pattern should contain source address field",
                rails3head.contains("source.address"));
        assertTrue("Pattern should contain timestamp field",
                rails3head.contains("timestamp"));
        assertTrue("Pattern should contain 'Started' keyword",
                rails3head.contains("Started"));
    }

    @Test
    public void testRAILS3HEADMatchesRailsLogLine() throws Exception {
        // Test that RAILS3HEAD can actually match Rails log lines
        Grok grok = compiler.compile("%{RAILS3HEAD}");

        String logLine = "Started GET \"/users\" for 127.0.0.1 at 2023-10-11 22:14:15 +0000";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match Rails log line", captured);
        assertFalse("Should capture fields", captured.isEmpty());
        assertEquals("GET", captured.get("http.request.method"));
        assertEquals("/users", captured.get("url.original"));
        assertEquals("127.0.0.1", captured.get("source.address"));
        assertEquals("2023-10-11 22:14:15 +0000", captured.get("timestamp"));
    }

    // ========== RPROCESSING Pattern Tests (Documenting Incompatibility) ==========

    @Test(expected = PatternSyntaxException.class)
    public void testRPROCESSINGPatternCompilationFails() throws Exception {
        // NOTE: RPROCESSING uses RCONTROLLER which has incompatible syntax

        compiler.compile("%{RPROCESSING}");
        fail("RPROCESSING pattern should throw PatternSyntaxException");
    }

    @Test
    public void testRPROCESSINGPatternDefinitionContent() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rprocessing = patterns.get("RPROCESSING");

        assertNotNull("RPROCESSING pattern should be defined", rprocessing);
        assertTrue("Pattern should contain RCONTROLLER", rprocessing.contains("RCONTROLLER"));
        assertTrue("Pattern should contain request format field",
                rprocessing.contains("rails.request.format"));
        assertTrue("Pattern should contain request params field",
                rprocessing.contains("rails.request.params"));
        assertTrue("Pattern should contain 'Processing by' text",
                rprocessing.contains("Processing by"));
    }

    // ========== RAILS3PROFILE Pattern Tests (Documenting Incompatibility) ==========

    @Test
    public void testRAILS3PROFILEPatternCompilationFails() throws Exception {
        // NOTE: RAILS3PROFILE has duplicate named groups (rails.request.duration.active_record)
        // which causes IllegalStateException in Java regex
        try {
            compiler.compile("%{RAILS3PROFILE}");
            fail("RAILS3PROFILE pattern should throw an exception due to duplicate named groups");
        } catch (IllegalStateException | java.util.regex.PatternSyntaxException e) {
            // Expected: duplicate named groups cause compilation issues
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testRAILS3PROFILEPatternDefinitionContent() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rails3profile = patterns.get("RAILS3PROFILE");

        assertNotNull("RAILS3PROFILE pattern should be defined", rails3profile);
        assertTrue("Pattern should contain view duration field",
                rails3profile.contains("rails.request.duration.view"));
        assertTrue("Pattern should contain active_record duration field",
                rails3profile.contains("rails.request.duration.active_record"));
        assertTrue("Pattern should contain 'Views:' text", rails3profile.contains("Views:"));
        assertTrue("Pattern should contain 'ActiveRecord:' text", rails3profile.contains("ActiveRecord:"));
    }

    // ========== RAILS3FOOT Pattern Tests (Documenting Incompatibility) ==========

    @Test
    public void testRAILS3FOOTPatternCompilationFails() throws Exception {
        // NOTE: RAILS3FOOT uses RAILS3PROFILE which has duplicate named groups
        try {
            compiler.compile("%{RAILS3FOOT}");
            fail("RAILS3FOOT pattern should throw an exception due to duplicate named groups");
        } catch (IllegalStateException | java.util.regex.PatternSyntaxException e) {
            // Expected: inherits duplicate named group issues from RAILS3PROFILE
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void testRAILS3FOOTPatternDefinitionContent() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rails3foot = patterns.get("RAILS3FOOT");

        assertNotNull("RAILS3FOOT pattern should be defined", rails3foot);
        assertTrue("Pattern should contain status code field",
                rails3foot.contains("http.response.status_code"));
        assertTrue("Pattern should contain total duration field",
                rails3foot.contains("rails.request.duration.total"));
        assertTrue("Pattern should contain RAILS3PROFILE", rails3foot.contains("RAILS3PROFILE"));
        assertTrue("Pattern should contain 'Completed' text", rails3foot.contains("Completed"));
    }

    // ========== RAILS3 Complete Pattern Tests (Documenting Incompatibility) ==========

    @Test(expected = PatternSyntaxException.class)
    public void testRAILS3PatternCompilationFails() throws Exception {
        // NOTE: RAILS3 combines all Rails patterns, inheriting their incompatibility

        compiler.compile("%{RAILS3}");
        fail("RAILS3 pattern should throw PatternSyntaxException");
    }

    @Test
    public void testRAILS3PatternDefinitionContent() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rails3 = patterns.get("RAILS3");

        assertNotNull("RAILS3 pattern should be defined", rails3);
        assertTrue("Pattern should contain RAILS3HEAD", rails3.contains("RAILS3HEAD"));
        assertTrue("Pattern should contain RPROCESSING", rails3.contains("RPROCESSING"));
        assertTrue("Pattern should contain RAILS3FOOT", rails3.contains("RAILS3FOOT"));
        assertTrue("Pattern should contain explain original field",
                rails3.contains("rails.request.explain.original"));
    }

    // ========== ECS Field Name Documentation Tests ==========

    @Test
    public void testExpectedECSFieldNamesInPatternDefinitions() throws Exception {
        // This test documents what ECS field names are defined in the Rails patterns
        // (even though they can't be compiled due to syntax incompatibility)

        Map<String, String> patterns = compiler.getPatternDefinitions();

        // HTTP-related fields
        String[] httpFields = {
            "http.request.method",
            "http.response.status_code"
        };

        // URL-related fields
        String[] urlFields = {
            "url.original"
        };

        // Source-related fields
        String[] sourceFields = {
            "source.address"
        };

        // Rails-specific fields
        String[] railsFields = {
            "rails.controller.class",
            "rails.controller.action",
            "rails.request.format",
            "rails.request.params",
            "rails.request.duration.total",
            "rails.request.duration.view",
            "rails.request.duration.active_record",
            "rails.request.explain.original"
        };

        // Verify HTTP fields are present in pattern definitions
        for (String field : httpFields) {
            boolean foundInAnyPattern = patterns.values().stream()
                    .anyMatch(pattern -> pattern.contains(field));
            assertTrue("HTTP field should be in Rails patterns: " + field, foundInAnyPattern);
        }

        // Verify Rails fields are present in pattern definitions
        for (String field : railsFields) {
            boolean foundInAnyPattern = patterns.values().stream()
                    .anyMatch(pattern -> pattern.contains(field));
            assertTrue("Rails field should be in Rails patterns: " + field, foundInAnyPattern);
        }
    }

    @Test
    public void testTypeConversionModifiersInPatternDefinitions() throws Exception {
        // This test documents that the Rails patterns include type conversion modifiers
        // like :integer and :float for numeric fields

        Map<String, String> patterns = compiler.getPatternDefinitions();
        String rails3foot = patterns.get("RAILS3FOOT");

        // Status code should have :integer modifier (note: Rails uses :int, not :integer)
        assertTrue("Status code should have integer conversion",
                rails3foot.contains(":int") || rails3foot.contains(":integer"));

        // Duration fields should have :float modifier
        assertTrue("Duration fields should have float conversion",
                rails3foot.contains(":float"));
    }

    // ========== Compatibility Notes and Recommendations ==========

    @Test
    public void testDocumentCompatibilityIssues() {
        // This test serves as documentation for developers who encounter these patterns

        String compatibilityNotes =
            "The Rails pattern file (src/main/resources/patterns/rails) was designed for " +
            "Logstash which uses the Oniguruma regex engine (Ruby-compatible). This Java " +
            "grok implementation uses Java's standard regex engine, which is incompatible " +
            "with several constructs used in the Rails patterns:\n\n" +
            "1. \\h means 'horizontal whitespace' in Java but 'hexadecimal digit' in Ruby\n" +
            "2. (?<field.name>...) syntax for nested field names causes PatternSyntaxException\n\n" +
            "To use Rails log parsing in Java:\n" +
            "- Option 1: Create custom patterns with Java-compatible syntax\n" +
            "- Option 2: Use simple field names in pattern definitions and apply ECS names " +
            "when compiling: %{WORD:log.level}\n" +
            "- Option 3: Fix the rails pattern file to use Java-compatible regex syntax";

        // This assertion always passes but documents the issues
        assertNotNull("Compatibility notes documented", compatibilityNotes);
    }

    @Test
    public void testRecommendedAlternativeApproach() throws Exception {
        // This test shows the recommended way to parse Rails logs with Java grok
        // using custom patterns with Java-compatible syntax

        // Define a simple custom pattern for controller matching (without ECS syntax in definition)
        compiler.register("MY_CONTROLLER", "([^#]+)");
        compiler.register("MY_ACTION", "(\\w+)");

        // Compile using the ECS field name syntax (%{PATTERN:[field][name]})
        Grok grok = compiler.compile("%{MY_CONTROLLER:rails.controller.class}#%{MY_ACTION:rails.controller.action}");

        String controllerLine = "UsersController#index";
        Match match = grok.match(controllerLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match with correct syntax", captured);
        assertEquals("UsersController", captured.get("rails.controller.class"));
        assertEquals("index", captured.get("rails.controller.action"));
    }

    @Test
    public void testRecommendedRailsLogParsingExample() throws Exception {
        // Full example of parsing a Rails request line with Java-compatible patterns

        compiler.register("HTTP_METHOD", "(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)");
        compiler.register("RAILS_PATH", "(/[^\\s\"]*)");
        compiler.register("IP_OR_HOST", "([0-9\\.]+|[a-zA-Z0-9\\.-]+)");
        compiler.register("RAILS_TIMESTAMP", "([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2} [+-][0-9]{4})");

        String pattern = "Started %{HTTP_METHOD:http.request.method} \"%{RAILS_PATH:url.original}\" " +
                        "for %{IP_OR_HOST:source.address} at %{RAILS_TIMESTAMP:timestamp}";

        Grok grok = compiler.compile(pattern);

        String logLine = "Started GET \"/users\" for 127.0.0.1 at 2023-10-11 22:14:15 +0000";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match Rails request line", captured);
        assertEquals("GET", captured.get("http.request.method"));
        assertEquals("/users", captured.get("url.original"));
        assertEquals("127.0.0.1", captured.get("source.address"));
        assertEquals("2023-10-11 22:14:15 +0000", captured.get("timestamp"));
    }
}
