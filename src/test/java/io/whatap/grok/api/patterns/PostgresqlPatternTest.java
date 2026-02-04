package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for PostgreSQL pattern definitions.
 * Tests PostgreSQL log format with structured field extraction.
 *
 * Note: Due to grok pattern compiler limitations with escaped brackets, the pattern uses
 * simplified field names ('username' and 'connid') instead of ECS-style nested names.
 */
public class PostgresqlPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/postgresql");
    }

    // ========== Pattern Registration and Compilation Tests ==========

    @Test
    public void testPostgresqlPatternIsRegistered() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("POSTGRESQL pattern should be registered", patterns.containsKey("POSTGRESQL"));

        String patternDef = patterns.get("POSTGRESQL");
        assertNotNull("POSTGRESQL definition should not be null", patternDef);

        // Verify the pattern contains expected field references
        assertTrue("Pattern should contain timestamp", patternDef.contains("timestamp"));
        assertTrue("Pattern should contain [event][timezone]", patternDef.contains("[event][timezone]"));
        assertTrue("Pattern should contain username", patternDef.contains("username"));
        assertTrue("Pattern should contain connid", patternDef.contains("connid"));
    }

    @Test
    public void testPostgresqlPatternCompiles() throws Exception {
        // Verify that the POSTGRESQL pattern compiles without errors
        Grok grok = compiler.compile("%{POSTGRESQL}");
        assertNotNull("POSTGRESQL pattern should compile successfully", grok);
    }

    // ========== Basic PostgreSQL Log Pattern Tests ==========

    @Test
    public void testBasicPostgresqlLogWithLOG() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: statement: SELECT * FROM users;";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match basic PostgreSQL log", captured);

        assertEquals("2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("EST", captured.get("[event][timezone]"));
        assertEquals("postgres user@mydb", captured.get("username"));
        assertEquals("12345", captured.get("connid"));
    }

    @Test
    public void testBasicPostgresqlLogWithERROR() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 UTC admin system@production[67890] ERROR: connection refused";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match PostgreSQL error log", captured);

        assertEquals("2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("UTC", captured.get("[event][timezone]"));
        assertEquals("admin system@production", captured.get("username"));
        assertEquals("67890", captured.get("connid"));
    }

    // ========== Different Timezone Tests ==========

    @Test
    public void testPostgresqlLogWithDifferentTimezones() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] logLines = {
            "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: test",
            "2023-10-11 22:14:15 UTC admin db@prod[67890] LOG: test",
            "2023-10-11 22:14:15 PST app user@webapp[11111] LOG: test",
            "2023-10-11 22:14:15 CST database admin@system[22222] LOG: test",
            "2023-10-11 22:14:15 EDT postgres test@local[33333] LOG: test"
        };

        String[] expectedTimezones = { "EST", "UTC", "PST", "CST", "EDT" };

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line with timezone: " + expectedTimezones[i], captured);
            assertEquals(expectedTimezones[i], captured.get("[event][timezone]"));
        }
    }

    // ========== Different User/Database Format Tests ==========

    @Test
    public void testPostgresqlLogWithDifferentUserFormats() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] logLines = {
            "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: test",
            "2023-10-11 22:14:15 EST admin_user application@database[12345] LOG: test",
            "2023-10-11 22:14:15 EST root system_db@production[12345] LOG: test",
            "2023-10-11 22:14:15 EST app_user webapp@testdb[12345] LOG: test"
        };

        String[] expectedUsers = {
            "postgres user@mydb",
            "admin_user application@database",
            "root system_db@production",
            "app_user webapp@testdb"
        };

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line with user: " + expectedUsers[i], captured);
            assertEquals(expectedUsers[i], captured.get("username"));
        }
    }

    // ========== Different Connection ID Tests ==========

    @Test
    public void testPostgresqlLogWithDifferentConnectionIds() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] logLines = {
            "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: test",
            "2023-10-11 22:14:15 EST postgres user@mydb[1] LOG: test",
            "2023-10-11 22:14:15 EST postgres user@mydb[999999] LOG: test",
            "2023-10-11 22:14:15 EST postgres user@mydb[54321] LOG: test"
        };

        String[] expectedConnectionIds = { "12345", "1", "999999", "54321" };

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line with connection ID: " + expectedConnectionIds[i], captured);
            assertEquals(expectedConnectionIds[i], captured.get("connid"));
        }
    }

    // ========== Connection ID Type Tests ==========

    @Test
    public void testConnectionIdIsString() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        Object connId = captured.get("connid");
        assertNotNull("Connection ID should be captured", connId);
        assertTrue("Connection ID should be String type", connId instanceof String);
        assertEquals("12345", connId);
    }

    @Test
    public void testConnectionIdWithDifferentValues() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] testIds = { "1", "100", "12345", "67890", "999999" };

        for (String expectedId : testIds) {
            String logLine = String.format(
                "2023-10-11 22:14:15 EST postgres user@mydb[%s] LOG: test", expectedId);
            Match match = grok.match(logLine);
            Map<String, Object> captured = match.capture();

            assertNotNull("Failed to match log with connection ID: " + expectedId, captured);

            Object connId = captured.get("connid");
            assertTrue("Connection ID should be String for value: " + expectedId, connId instanceof String);
            assertEquals(expectedId, connId);
        }
    }

    // ========== Field Name Tests ==========

    @Test
    public void testAllFieldsAreExtracted() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: statement: SELECT * FROM users;";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify all expected fields are present
        assertTrue("Missing timestamp", captured.containsKey("timestamp"));
        assertTrue("Missing [event][timezone]", captured.containsKey("[event][timezone]"));
        assertTrue("Missing username", captured.containsKey("username"));
        assertTrue("Missing connid", captured.containsKey("connid"));
    }

    @Test
    public void testFieldValuesMatchExpected() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: test message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify field values are correctly extracted
        assertEquals("Timestamp mismatch", "2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("Timezone mismatch", "EST", captured.get("[event][timezone]"));
        assertEquals("Username mismatch", "postgres user@mydb", captured.get("username"));
        assertEquals("Connection ID mismatch", "12345", captured.get("connid"));
    }

    // ========== Real-world PostgreSQL Log Examples ==========

    @Test
    public void testRealWorldPostgresqlSelectStatement() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: statement: SELECT * FROM users WHERE active = true;";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world SELECT statement", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("EST", captured.get("[event][timezone]"));
        assertEquals("postgres user@mydb", captured.get("username"));
        assertEquals("12345", captured.get("connid"));
    }

    @Test
    public void testRealWorldPostgresqlConnectionError() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 UTC admin system@production[67890] ERROR: connection refused";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world connection error", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("UTC", captured.get("[event][timezone]"));
        assertEquals("admin system@production", captured.get("username"));
        assertEquals("67890", captured.get("connid"));
    }

    @Test
    public void testRealWorldPostgresqlDatabaseStartup() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 08:00:00 UTC postgres postgres@template1[1234] LOG: database system is ready to accept connections";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world database startup", captured);
        assertEquals("2023-10-11 08:00:00", captured.get("timestamp"));
        assertEquals("UTC", captured.get("[event][timezone]"));
        assertEquals("postgres postgres@template1", captured.get("username"));
        assertEquals("1234", captured.get("connid"));
    }

    @Test
    public void testRealWorldPostgresqlQueryExecution() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 14:30:22 PST appuser myapp@production_db[98765] LOG: duration: 123.456 ms  statement: UPDATE users SET last_login = NOW() WHERE id = 42;";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world query execution", captured);
        assertEquals("2023-10-11 14:30:22", captured.get("timestamp"));
        assertEquals("PST", captured.get("[event][timezone]"));
        assertEquals("appuser myapp@production_db", captured.get("username"));
        assertEquals("98765", captured.get("connid"));
    }

    @Test
    public void testRealWorldPostgresqlFatalError() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres admin@postgres[11111] FATAL: database \"nonexistent\" does not exist";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world FATAL error", captured);
        assertEquals("2023-10-11 22:14:15", captured.get("timestamp"));
        assertEquals("EST", captured.get("[event][timezone]"));
        assertEquals("postgres admin@postgres", captured.get("username"));
        assertEquals("11111", captured.get("connid"));
    }

    @Test
    public void testRealWorldPostgresqlAutovacuum() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 03:00:00 UTC postgres autovacuum@mydb[55555] LOG: automatic vacuum of table \"mydb.public.users\": index scans: 1";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real-world autovacuum log", captured);
        assertEquals("2023-10-11 03:00:00", captured.get("timestamp"));
        assertEquals("UTC", captured.get("[event][timezone]"));
        assertEquals("postgres autovacuum@mydb", captured.get("username"));
        assertEquals("55555", captured.get("connid"));
    }

    // ========== Multiple Log Lines Tests ==========

    @Test
    public void testMultiplePostgresqlLogLines() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] logLines = {
            "2023-10-11 08:00:00 UTC postgres postgres@template1[1000] LOG: database system was shut down at 2023-10-11 08:00:00 UTC",
            "2023-10-11 08:00:01 UTC postgres postgres@template1[1001] LOG: database system is ready to accept connections",
            "2023-10-11 08:00:02 EST appuser myapp@prod[2000] LOG: connection authorized: user=appuser database=prod",
            "2023-10-11 08:00:03 PST admin system@postgres[3000] ERROR: role \"baduser\" does not exist",
            "2023-10-11 08:00:04 CST webapp app@webdb[4000] LOG: statement: SELECT COUNT(*) FROM sessions WHERE expires_at > NOW()",
            "2023-10-11 08:00:05 EDT postgres autovacuum@mydb[5000] LOG: automatic analyze of table \"mydb.public.logs\" system usage"
        };

        for (String line : logLines) {
            Match match = grok.match(line);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + line, captured);
            assertNotNull("Missing timestamp in: " + line, captured.get("timestamp"));
            assertNotNull("Missing timezone in: " + line, captured.get("[event][timezone]"));
            assertNotNull("Missing username in: " + line, captured.get("username"));
            assertNotNull("Missing connection ID in: " + line, captured.get("connid"));
        }
    }

    // ========== Edge Cases and Boundary Tests ==========

    @Test
    public void testPostgresqlLogWithMinimalConnectionId() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[1] LOG: test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log with minimal connection ID", captured);
        assertEquals("1", captured.get("connid"));
    }

    @Test
    public void testPostgresqlLogWithLargeConnectionId() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[2147483647] LOG: test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log with large connection ID", captured);
        assertEquals("2147483647", captured.get("connid"));
    }

    @Test
    public void testPostgresqlLogWithComplexUsername() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST app_user-2023 webapp_v2@production-db-01[12345] LOG: test";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log with complex username", captured);
        assertEquals("app_user-2023 webapp_v2@production-db-01", captured.get("username"));
    }

    @Test
    public void testPostgresqlLogWithDifferentDateFormats() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String[] logLines = {
            "2023-01-01 00:00:00 UTC postgres user@mydb[12345] LOG: test",
            "2023-12-31 23:59:59 EST postgres user@mydb[12345] LOG: test",
            "2023-06-15 12:30:45 PST postgres user@mydb[12345] LOG: test"
        };

        String[] expectedTimestamps = {
            "2023-01-01 00:00:00",
            "2023-12-31 23:59:59",
            "2023-06-15 12:30:45"
        };

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log with timestamp: " + expectedTimestamps[i], captured);
            assertEquals(expectedTimestamps[i], captured.get("timestamp"));
        }
    }

    // ========== Pattern Structure Validation Tests ==========

    @Test
    public void testExpectedFieldCount() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345] LOG: complete test message";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match complete log", captured);

        // Count primary extracted fields (not intermediate pattern fields)
        int primaryFieldCount = 0;
        if (captured.containsKey("timestamp")) primaryFieldCount++;
        if (captured.containsKey("[event][timezone]")) primaryFieldCount++;
        if (captured.containsKey("username")) primaryFieldCount++;
        if (captured.containsKey("connid")) primaryFieldCount++;

        assertEquals("Expected 4 primary fields to be captured", 4, primaryFieldCount);
    }

    @Test
    public void testPatternMatchesCompleteLogLine() throws Exception {
        Grok grok = compiler.compile("%{POSTGRESQL}");

        String logLine = "2023-10-11 22:14:15 EST postgres user@mydb[12345]";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match complete log line", captured);

        // Verify the pattern captured the essential fields
        assertNotNull("Timestamp should be captured", captured.get("timestamp"));
        assertNotNull("Timezone should be captured", captured.get("[event][timezone]"));
        assertNotNull("Username should be captured", captured.get("username"));
        assertNotNull("Connection ID should be captured", captured.get("connid"));
    }
}
