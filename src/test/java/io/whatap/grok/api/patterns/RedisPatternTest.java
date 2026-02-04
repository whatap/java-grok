package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Redis pattern definitions.
 * Tests all Redis-related patterns including timestamps, logs, and monitoring logs.
 *
 * Note: Due to regex limitations with the combination of escaped brackets, ECS-style field names,
 * and type semantics, this implementation uses simplified field names:
 * - redis_pid instead of [process][pid]
 * - redis_db_id instead of [redis][database][id]
 * - client_ip instead of [client][ip]
 * - client_port instead of [client][port]
 * - redis_command instead of [redis][command][name]
 * - redis_args instead of [redis][command][args]
 */
public class RedisPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/redis");
    }

    // ========== REDISTIMESTAMP Pattern Tests ==========

    @Test
    public void testRedisTimestampPattern() throws Exception {
        Grok grok = compiler.compile("%{REDISTIMESTAMP:timestamp}");

        String[] validTimestamps = {
            "11 Feb 22:14:15",
            "01 Jan 00:00:00",
            "31 Dec 23:59:59",
            "15 Mar 12:30:45",
            "28 Aug 08:15:30"
        };

        for (String timestamp : validTimestamps) {
            Match match = grok.match(timestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match timestamp: " + timestamp, captured);
            assertEquals(timestamp, captured.get("timestamp"));
        }
    }

    @Test
    public void testRedisTimestampWithLeadingZero() throws Exception {
        Grok grok = compiler.compile("%{REDISTIMESTAMP:timestamp}");

        String[] timestamps = {
            "01 Jan 00:00:00",
            "02 Feb 01:23:45",
            "09 Sep 09:09:09"
        };

        for (String timestamp : timestamps) {
            Match match = grok.match(timestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match timestamp with leading zero: " + timestamp, captured);
            assertEquals(timestamp, captured.get("timestamp"));
        }
    }

    @Test
    public void testRedisTimestampWithoutLeadingZero() throws Exception {
        Grok grok = compiler.compile("%{REDISTIMESTAMP:timestamp}");

        String[] timestamps = {
            "1 Jan 00:00:00",
            "2 Feb 01:23:45",
            "9 Sep 09:09:09"
        };

        for (String timestamp : timestamps) {
            Match match = grok.match(timestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match timestamp without leading zero: " + timestamp, captured);
            assertEquals(timestamp, captured.get("timestamp"));
        }
    }

    @Test
    public void testRedisTimestampAllMonths() throws Exception {
        Grok grok = compiler.compile("%{REDISTIMESTAMP:timestamp}");

        String[] months = {
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        };

        for (String month : months) {
            String timestamp = "15 " + month + " 12:00:00";
            Match match = grok.match(timestamp);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match timestamp with month: " + month, captured);
            assertEquals(timestamp, captured.get("timestamp"));
        }
    }

    // ========== REDISLOG Pattern Tests ==========

    @Test
    public void testRedisLogPattern() throws Exception {
        Grok grok = compiler.compile("%{REDISLOG}");

        String logLine = "[12345] 11 Feb 22:14:15 *";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Redis log", captured);
        assertEquals(12345, captured.get("redis_pid"));
        assertEquals("11 Feb 22:14:15", captured.get("timestamp"));
    }

    @Test
    public void testRedisLogWithVariousPids() throws Exception {
        Grok grok = compiler.compile("%{REDISLOG}");

        String[] logLines = {
            "[1] 11 Feb 22:14:15 *",
            "[12345] 11 Feb 22:14:15 *",
            "[999999] 11 Feb 22:14:15 *",
            "[54321] 01 Jan 00:00:00 *"
        };

        Integer[] expectedPids = {1, 12345, 999999, 54321};

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLines[i], captured);
            assertEquals(expectedPids[i], captured.get("redis_pid"));
        }
    }

    @Test
    public void testRedisLogFieldNames() throws Exception {
        Grok grok = compiler.compile("%{REDISLOG}");

        String logLine = "[12345] 11 Feb 22:14:15 *";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify field names
        assertTrue("Missing redis_pid", captured.containsKey("redis_pid"));
        assertTrue("Missing timestamp", captured.containsKey("timestamp"));

        assertEquals(12345, captured.get("redis_pid"));
    }

    @Test
    public void testRedisLogPidIsInteger() throws Exception {
        Grok grok = compiler.compile("%{REDISLOG}");

        String logLine = "[12345] 11 Feb 22:14:15 *";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify the :integer semantic converts to Integer type
        Object pid = captured.get("redis_pid");
        assertNotNull("PID should not be null", pid);
        assertTrue("PID should be Integer type", pid instanceof Integer);
        assertEquals(12345, pid);
    }

    // ========== REDISMONLOG Pattern Tests ==========

    @Test
    public void testRedisMonLogPattern() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match Redis monitor log", captured);
        assertEquals("1612345678.123456", captured.get("timestamp"));
        assertEquals("0", captured.get("redis_db_id"));
        assertEquals("127.0.0.1", captured.get("client_ip"));
        assertEquals(6379, captured.get("client_port"));
        assertEquals("GET", captured.get("redis_command"));
        assertEquals("key1", captured.get("redis_args"));
    }

    @Test
    public void testRedisMonLogWithDifferentCommands() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] logLines = {
            "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1",
            "1612345678.234567 [0 127.0.0.1:6379] \"SET\" key2 value2",
            "1612345678.345678 [0 127.0.0.1:6379] \"DEL\" key1 key2 key3",
            "1612345678.456789 [0 127.0.0.1:6379] \"HGET\" myhash field1",
            "1612345678.567890 [0 127.0.0.1:6379] \"ZADD\" myzset 1 member1"
        };

        String[] expectedCommands = {"GET", "SET", "DEL", "HGET", "ZADD"};
        String[] expectedArgs = {
            "key1",
            "key2 value2",
            "key1 key2 key3",
            "myhash field1",
            "myzset 1 member1"
        };

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLines[i], captured);
            assertEquals(expectedCommands[i], captured.get("redis_command"));
            assertEquals(expectedArgs[i], captured.get("redis_args"));
        }
    }

    @Test
    public void testRedisMonLogWithDifferentDatabases() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] logLines = {
            "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1",
            "1612345678.123456 [1 127.0.0.1:6379] \"GET\" key1",
            "1612345678.123456 [15 127.0.0.1:6379] \"GET\" key1",
            "1612345678.123456 [99 127.0.0.1:6379] \"GET\" key1"
        };

        String[] expectedDbIds = {"0", "1", "15", "99"};

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLines[i], captured);
            assertEquals(expectedDbIds[i], captured.get("redis_db_id"));
        }
    }

    @Test
    public void testRedisMonLogWithDifferentIPs() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] logLines = {
            "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1",
            "1612345678.123456 [0 192.168.1.100:6379] \"GET\" key1",
            "1612345678.123456 [0 10.0.0.5:6379] \"GET\" key1",
            "1612345678.123456 [0 172.16.0.1:6379] \"GET\" key1"
        };

        String[] expectedIPs = {"127.0.0.1", "192.168.1.100", "10.0.0.5", "172.16.0.1"};

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLines[i], captured);
            assertEquals(expectedIPs[i], captured.get("client_ip"));
        }
    }

    @Test
    public void testRedisMonLogWithDifferentPorts() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] logLines = {
            "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1",
            "1612345678.123456 [0 127.0.0.1:6380] \"GET\" key1",
            "1612345678.123456 [0 127.0.0.1:7000] \"GET\" key1",
            "1612345678.123456 [0 127.0.0.1:16379] \"GET\" key1"
        };

        Integer[] expectedPorts = {6379, 6380, 7000, 16379};

        for (int i = 0; i < logLines.length; i++) {
            Match match = grok.match(logLines[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match log line: " + logLines[i], captured);
            assertEquals(expectedPorts[i], captured.get("client_port"));
        }
    }

    @Test
    public void testRedisMonLogFieldNames() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify all field names are captured correctly
        assertTrue("Missing timestamp", captured.containsKey("timestamp"));
        assertTrue("Missing redis_db_id", captured.containsKey("redis_db_id"));
        assertTrue("Missing client_ip", captured.containsKey("client_ip"));
        assertTrue("Missing client_port", captured.containsKey("client_port"));
        assertTrue("Missing redis_command", captured.containsKey("redis_command"));
        assertTrue("Missing redis_args", captured.containsKey("redis_args"));

        assertEquals("1612345678.123456", captured.get("timestamp"));
        assertEquals("0", captured.get("redis_db_id"));
        assertEquals("127.0.0.1", captured.get("client_ip"));
        assertEquals(6379, captured.get("client_port"));
        assertEquals("GET", captured.get("redis_command"));
        assertEquals("key1", captured.get("redis_args"));
    }

    @Test
    public void testRedisMonLogIntegerTypes() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log", captured);

        // Verify fields marked as :integer are Integer type
        Object port = captured.get("client_port");
        assertNotNull("Port should not be null", port);
        assertTrue("Port should be Integer type", port instanceof Integer);
        assertEquals(6379, port);
    }

    @Test
    public void testRedisMonLogWithCommandWithoutArgs() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.123456 [0 127.0.0.1:6379] \"PING\"";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match log without command args", captured);
        assertEquals("PING", captured.get("redis_command"));

        // Args might be empty string or null
        Object args = captured.get("redis_args");
        assertTrue("Args should be empty or null for PING",
            args == null || args.toString().trim().isEmpty());
    }

    // ========== Real-world Log Examples ==========

    @Test
    public void testRealWorldRedisStartupLog() throws Exception {
        Grok grok = compiler.compile("%{REDISLOG}");

        String logLine = "[1234] 11 Feb 22:14:15 *";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis startup log", captured);
        assertEquals(1234, captured.get("redis_pid"));
        assertEquals("11 Feb 22:14:15", captured.get("timestamp"));
    }

    @Test
    public void testRealWorldRedisMonitorGetCommand() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" mykey";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis GET command", captured);
        assertEquals("1612345678.123456", captured.get("timestamp"));
        assertEquals("0", captured.get("redis_db_id"));
        assertEquals("127.0.0.1", captured.get("client_ip"));
        assertEquals(6379, captured.get("client_port"));
        assertEquals("GET", captured.get("redis_command"));
        assertEquals("mykey", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorSetCommand() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.234567 [0 127.0.0.1:6379] \"SET\" user:1000 {\"name\":\"John\",\"age\":30}";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis SET command with JSON", captured);
        assertEquals("SET", captured.get("redis_command"));
        assertEquals("user:1000 {\"name\":\"John\",\"age\":30}", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorMultiKeyCommand() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.345678 [0 127.0.0.1:6379] \"MGET\" key1 key2 key3 key4";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis MGET command", captured);
        assertEquals("MGET", captured.get("redis_command"));
        assertEquals("key1 key2 key3 key4", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorHashCommand() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.456789 [0 127.0.0.1:6379] \"HSET\" user:1000:profile name \"John Doe\"";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis HSET command", captured);
        assertEquals("HSET", captured.get("redis_command"));
        assertEquals("user:1000:profile name \"John Doe\"", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorSortedSetCommand() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.567890 [0 127.0.0.1:6379] \"ZADD\" leaderboard 100 player1 200 player2 300 player3";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis ZADD command", captured);
        assertEquals("ZADD", captured.get("redis_command"));
        assertEquals("leaderboard 100 player1 200 player2 300 player3", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorFromRemoteClient() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String logLine = "1612345678.678901 [1 192.168.1.100:54321] \"GET\" session:abc123";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real Redis command from remote client", captured);
        assertEquals("1", captured.get("redis_db_id"));
        assertEquals("192.168.1.100", captured.get("client_ip"));
        assertEquals(54321, captured.get("client_port"));
        assertEquals("GET", captured.get("redis_command"));
        assertEquals("session:abc123", captured.get("redis_args"));
    }

    @Test
    public void testRealWorldRedisMonitorPipelineCommands() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] pipelineCommands = {
            "1612345678.789012 [0 127.0.0.1:6379] \"MULTI\"",
            "1612345678.789123 [0 127.0.0.1:6379] \"SET\" key1 value1",
            "1612345678.789234 [0 127.0.0.1:6379] \"SET\" key2 value2",
            "1612345678.789345 [0 127.0.0.1:6379] \"EXEC\""
        };

        String[] expectedCommands = {"MULTI", "SET", "SET", "EXEC"};

        for (int i = 0; i < pipelineCommands.length; i++) {
            Match match = grok.match(pipelineCommands[i]);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match pipeline command: " + pipelineCommands[i], captured);
            assertEquals(expectedCommands[i], captured.get("redis_command"));
        }
    }

    @Test
    public void testMultipleRedisLogFormats() throws Exception {
        // Test that we can handle both REDISLOG and REDISMONLOG in a session
        Grok redisLogGrok = compiler.compile("%{REDISLOG}");
        Grok monLogGrok = compiler.compile("%{REDISMONLOG}");

        String redisLog = "[12345] 11 Feb 22:14:15 *";
        String monLog = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1";

        Match redisMatch = redisLogGrok.match(redisLog);
        Match monMatch = monLogGrok.match(monLog);

        Map<String, Object> redisCapture = redisMatch.capture();
        Map<String, Object> monCapture = monMatch.capture();

        assertNotNull("Failed to match REDISLOG", redisCapture);
        assertNotNull("Failed to match REDISMONLOG", monCapture);

        assertEquals(12345, redisCapture.get("redis_pid"));
        assertEquals("GET", monCapture.get("redis_command"));
    }

    @Test
    public void testTimestampFormats() throws Exception {
        // Test different timestamp formats used in Redis patterns
        Grok redistsGrok = compiler.compile("%{REDISTIMESTAMP:ts}");
        Grok monlogGrok = compiler.compile("%{REDISMONLOG}");

        // REDISTIMESTAMP format
        String redistimestamp = "11 Feb 22:14:15";
        Match tsMatch = redistsGrok.match(redistimestamp);
        assertNotNull("Failed to match REDISTIMESTAMP", tsMatch.capture());

        // Unix timestamp with microseconds in REDISMONLOG
        String monlog = "1612345678.123456 [0 127.0.0.1:6379] \"GET\" key1";
        Match monMatch = monlogGrok.match(monlog);
        Map<String, Object> monCapture = monMatch.capture();
        assertNotNull("Failed to match REDISMONLOG timestamp", monCapture);
        assertEquals("1612345678.123456", monCapture.get("timestamp"));
    }

    @Test
    public void testEdgeCaseEmptyCommandArgs() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] commandsWithoutArgs = {
            "1612345678.123456 [0 127.0.0.1:6379] \"PING\"",
            "1612345678.123456 [0 127.0.0.1:6379] \"DBSIZE\"",
            "1612345678.123456 [0 127.0.0.1:6379] \"INFO\""
        };

        for (String command : commandsWithoutArgs) {
            Match match = grok.match(command);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match command without args: " + command, captured);
            assertNotNull("Command name should be captured", captured.get("redis_command"));
        }
    }

    @Test
    public void testComplexCommandArguments() throws Exception {
        Grok grok = compiler.compile("%{REDISMONLOG}");

        String[] complexCommands = {
            "1612345678.123456 [0 127.0.0.1:6379] \"SET\" mykey \"value with spaces\"",
            "1612345678.123456 [0 127.0.0.1:6379] \"SET\" mykey value EX 3600",
            "1612345678.123456 [0 127.0.0.1:6379] \"ZADD\" myset NX 1.5 member1",
            "1612345678.123456 [0 127.0.0.1:6379] \"GEORADIUS\" locations 15.087269 37.502669 200 km"
        };

        for (String command : complexCommands) {
            Match match = grok.match(command);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match complex command: " + command, captured);
            assertNotNull("Command name should be captured", captured.get("redis_command"));
            assertNotNull("Command args should be captured", captured.get("redis_args"));
        }
    }
}
