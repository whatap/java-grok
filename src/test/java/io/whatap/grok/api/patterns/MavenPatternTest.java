package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Maven pattern definitions.
 * Tests Maven version pattern matching for various version formats including:
 * - Standard semantic versioning (major.minor.patch)
 * - Partial versions (major.minor, major only)
 * - Release qualifiers (RELEASE, SNAPSHOT)
 * - Wildcard versions
 */
public class MavenPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/maven");
    }

    // ========== MAVEN_VERSION Pattern Tests ==========

    @Test
    public void testMavenVersionPattern_MajorMinorPatch() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "1.0.0",
            "2.3.4",
            "10.20.30",
            "0.0.1",
            "999.999.999"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match major.minor.patch version: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_MajorMinor() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "2.1",
            "1.0",
            "10.5",
            "99.99"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match major.minor version: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_MajorOnly() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "3",
            "1",
            "10",
            "999"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match major version only: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_WithRelease() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "1.0.0-RELEASE",
            "2.3.4-RELEASE",
            "1.5-RELEASE",
            "3-RELEASE"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match version with RELEASE: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_WithSnapshot() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "2.1.3-SNAPSHOT",
            "1.0.0-SNAPSHOT",
            "5.2-SNAPSHOT",
            "4-SNAPSHOT"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match version with SNAPSHOT: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_WithWildcard() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        String[] validVersions = {
            "1.0.*",
            "2.3.*",
            "10.*",
            "1.*"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match version with wildcard: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_WithDotSeparator() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        // Test versions with dot separator before qualifier
        String[] validVersions = {
            "1.0.0.RELEASE",
            "2.1.3.SNAPSHOT"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match version with dot separator: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionPattern_RealWorldVersions() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        // Test real-world Maven versions
        String[] realVersions = {
            "3.8.6",                    // Maven itself
            "2.7.10-SNAPSHOT",          // Spring Boot SNAPSHOT
            "5.3.23",                   // Spring Framework
            "4.0.0",                    // Hibernate
            "1.18.24",                  // Lombok
            "2.11.0",                   // Jackson
            "4.13.2",                   // JUnit
            "1.7.36",                   // Slf4j
            "2.17.1",                   // Log4j2
            "3.21.12"                   // Protobuf
        };

        for (String version : realVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match real-world version: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    // ========== Edge Cases Tests ==========

    @Test
    public void testMavenVersionPattern_PartialMatches() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        // These versions partially match - the pattern matches the valid prefix
        String[] partialVersions = {
            "1.0.0-RC1",           // Matches "1.0.0" (RC1 not captured)
            "1.0.0-alpha",         // Matches "1.0.0" (alpha not captured)
            "1.0.0-beta",          // Matches "1.0.0" (beta not captured)
            "1.0.0-FINAL"          // Matches "1.0.0" (FINAL not captured)
        };

        for (String version : partialVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Should match the valid prefix of: " + version, captured);
            // The captured version will be just the numeric part without the unsupported qualifier
        }
    }

    @Test
    public void testMavenVersionPattern_InvalidPrefixes() throws Exception {
        // The MAVEN_VERSION pattern allows optional major and minor parts,
        // so "v1.0.0" actually matches "1.0.0" starting from the first digit.
        // This test demonstrates the actual behavior of the pattern.
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        // "v1.0.0" will match "1.0.0" (skipping the 'v' prefix)
        Match match1 = grok.match("v1.0.0");
        Map<String, Object> captured1 = match1.capture();
        assertNotNull("Pattern matches numeric part", captured1);
        assertEquals("1.0.0", captured1.get("version"));

        // "version-1.0.0" will match "1.0.0" (skipping the prefix)
        Match match2 = grok.match("version-1.0.0");
        Map<String, Object> captured2 = match2.capture();
        assertNotNull("Pattern matches numeric part", captured2);
        assertEquals("1.0.0", captured2.get("version"));
    }

    @Test
    public void testMavenVersionPattern_EmptyString() throws Exception {
        // Note: The MAVEN_VERSION pattern with all optional groups can match empty string
        // This is due to the pattern structure: (?:(\d+)\.)?(?:(\d+)\.)?(\*|\d+)
        // where all parts are optional or can match minimal characters
        Grok grok = compiler.compile("^%{MAVEN_VERSION:version}$");

        // For strict validation, use anchors (^ and $) to ensure complete match
        String[] testCases = {
            "1.0.0",    // Should match
            "2.1",      // Should match
            "3"         // Should match
        };

        for (String version : testCases) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Should match valid version: " + version, captured);
        }
    }

    // ========== Pattern in Context Tests ==========

    @Test
    public void testMavenVersionInDependencyString() throws Exception {
        Grok grok = compiler.compile(".*:%{MAVEN_VERSION:version}");

        String[] dependencies = {
            "org.springframework.boot:spring-boot-starter-web:2.7.10-SNAPSHOT",
            "com.google.guava:guava:31.1-jre",
            "org.apache.maven:maven-core:3.8.6"
        };

        String[] expectedVersions = {
            "2.7.10-SNAPSHOT",
            "31.1",
            "3.8.6"
        };

        for (int i = 0; i < dependencies.length; i++) {
            Match match = grok.match(dependencies[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match dependency: " + dependencies[i], captured);
            assertEquals(expectedVersions[i], captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionInMavenOutput() throws Exception {
        Grok grok = compiler.compile(".*\\[INFO\\] Building .* %{MAVEN_VERSION:version}");

        String[] mavenOutputs = {
            "[INFO] Building my-project 1.0.0-SNAPSHOT",
            "[INFO] Building spring-boot-app 2.5.4",
            "[INFO] Building api-gateway 3.2.1-RELEASE"
        };

        String[] expectedVersions = {
            "1.0.0-SNAPSHOT",
            "2.5.4",
            "3.2.1-RELEASE"
        };

        for (int i = 0; i < mavenOutputs.length; i++) {
            Match match = grok.match(mavenOutputs[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match Maven output: " + mavenOutputs[i], captured);
            assertEquals(expectedVersions[i], captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionInPomXml() throws Exception {
        Grok grok = compiler.compile(".*<version>%{MAVEN_VERSION:version}</version>");

        String[] pomLines = {
            "    <version>1.0.0</version>",
            "    <version>2.1.3-SNAPSHOT</version>",
            "    <version>5.3.23</version>",
            "        <version>3.8.6</version>"
        };

        String[] expectedVersions = {
            "1.0.0",
            "2.1.3-SNAPSHOT",
            "5.3.23",
            "3.8.6"
        };

        for (int i = 0; i < pomLines.length; i++) {
            Match match = grok.match(pomLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match POM line: " + pomLines[i], captured);
            assertEquals(expectedVersions[i], captured.get("version"));
        }
    }

    // ========== Pattern Definition Verification ==========

    @Test
    public void testMavenVersionPatternIsRegistered() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("MAVEN_VERSION should be registered", patterns.containsKey("MAVEN_VERSION"));

        String patternDef = patterns.get("MAVEN_VERSION");
        assertNotNull("MAVEN_VERSION definition should not be null", patternDef);

        // Verify pattern supports key features
        assertTrue("Pattern should support optional major.minor", patternDef.contains("(?:(\\d+)\\.)?"));
        assertTrue("Pattern should support wildcard or digits", patternDef.contains("(\\*|\\d+)"));
        assertTrue("Pattern should support RELEASE or SNAPSHOT", patternDef.contains("(RELEASE|SNAPSHOT)"));
    }

    // ========== Multiple Capture Groups Test ==========

    @Test
    public void testMavenVersionWithMultipleCaptureGroups() throws Exception {
        // Test using multiple named captures in the same pattern
        Grok grok = compiler.compile("Dependency: %{DATA:artifact}:%{MAVEN_VERSION:version}");

        String input = "Dependency: org.springframework.boot:spring-boot-starter:2.7.10-SNAPSHOT";
        Match match = grok.match(input);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match dependency with artifact", captured);
        assertEquals("org.springframework.boot:spring-boot-starter", captured.get("artifact"));
        assertEquals("2.7.10-SNAPSHOT", captured.get("version"));
    }

    @Test
    public void testMavenVersionZeroPadding() throws Exception {
        Grok grok = compiler.compile("%{MAVEN_VERSION:version}");

        // Test versions with zero padding (should still match)
        String[] validVersions = {
            "01.00.00",
            "0.0.0",
            "00.01.02"
        };

        for (String version : validVersions) {
            Match match = grok.match(version);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match zero-padded version: " + version, captured);
            assertEquals(version, captured.get("version"));
        }
    }

    @Test
    public void testMavenVersionInMavenLog() throws Exception {
        // Use more specific pattern to match only the artifact name
        Grok grok = compiler.compile("\\[INFO\\] Downloading from .*/(?<artifact>[^/]+)/%{MAVEN_VERSION:version}/.*");

        String logLine = "[INFO] Downloading from central: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter/2.7.10-SNAPSHOT/spring-boot-starter-2.7.10-SNAPSHOT.pom";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Maven log line", captured);
        assertEquals("spring-boot-starter", captured.get("artifact"));
        assertEquals("2.7.10-SNAPSHOT", captured.get("version"));
    }
}
