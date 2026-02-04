package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Comprehensive test class for Postfix mail server pattern definitions.
 * Tests various Postfix daemon patterns including smtpd, cleanup, qmgr, smtp, pipe, postscreen, and more.
 *
 * <h2>Patterns Tested</h2>
 * <ul>
 *   <li><b>Common Patterns:</b> POSTFIX_QUEUEID, POSTFIX_CLIENT_INFO, POSTFIX_RELAY_INFO</li>
 *   <li><b>Helper Patterns:</b> POSTFIX_SMTP_STAGE, POSTFIX_ACTION, POSTFIX_STATUS_CODE</li>
 *   <li><b>Daemon Patterns:</b> POSTFIX_SMTPD, POSTFIX_CLEANUP, POSTFIX_QMGR, POSTFIX_PIPE</li>
 *   <li><b>Security Patterns:</b> POSTFIX_POSTSCREEN, POSTFIX_DNSBLOG, POSTFIX_ANVIL</li>
 *   <li><b>Delivery Patterns:</b> POSTFIX_SMTP, POSTFIX_BOUNCE, POSTFIX_ERROR</li>
 *   <li><b>Management Patterns:</b> POSTFIX_MASTER, POSTFIX_POSTSUPER, POSTFIX_SCACHE</li>
 * </ul>
 *
 * <h2>Test Coverage</h2>
 * <ul>
 *   <li>Pattern registration verification</li>
 *   <li>Pattern definition correctness</li>
 *   <li>Simple helper pattern compilation</li>
 *   <li>Complex composite pattern matching</li>
 *   <li>Real-world Postfix log examples</li>
 *   <li>Edge cases and validation</li>
 * </ul>
 */
public class PostfixPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatternFromClasspath("/patterns/postfix");
    }

    // ========== Common Pattern Tests ==========

    @Test
    public void testPostfixQueueIdPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QUEUEID:queueid}");

        String[] validQueueIds = {
            "ABC123",           // 6 hex chars
            "DEADBEEF",        // 8 hex chars
            "1A2B3C4D5E6F7",   // 15 alphanumeric
            "abcdef123456789"  // 15+ alphanumeric lowercase
        };

        for (String queueId : validQueueIds) {
            Match match = grok.match(queueId);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match queue ID: " + queueId, captured);
            // Pattern uses alternation so result may be in an array
            Object queueIdValue = captured.get("queueid");
            if (queueIdValue instanceof java.util.List) {
                java.util.List queueIds = (java.util.List) queueIdValue;
                boolean found = false;
                for (Object id : queueIds) {
                    if (queueId.equals(id)) {
                        found = true;
                        break;
                    }
                }
                assertTrue("Queue ID should be in list: " + queueId, found);
            } else {
                assertEquals(queueId, queueIdValue);
            }
        }
    }

    @Test
    public void testPostfixClientInfoPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_CLIENT_INFO}");

        String[] validClientInfo = {
            "[192.168.1.100]",
            "mail.example.com[10.0.0.1]",
            "[192.168.1.100]:25",
            "smtp.client.org[203.0.113.5]:54321",
            "unknown[172.16.0.1]"
        };

        for (String clientInfo : validClientInfo) {
            Match match = grok.match(clientInfo);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match client info: " + clientInfo, captured);
            assertTrue("Should contain IP", captured.containsKey("postfix_client_ip"));
        }
    }

    @Test
    public void testPostfixRelayInfoPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_RELAY_INFO}");

        String[] validRelayInfo = {
            "[192.168.1.100]",
            "relay.example.com[10.0.0.1]:25",
            "[smtp]:25",
            "none",
            "local",
            "relay.host.com[203.0.113.5]:587"
        };

        for (String relayInfo : validRelayInfo) {
            Match match = grok.match(relayInfo);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match relay info: " + relayInfo, captured);
        }
    }

    @Test
    public void testPostfixSmtpStagePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_STAGE:stage}");

        String[] validStages = {
            "CONNECT",
            "HELO",
            "EHLO",
            "STARTTLS",
            "AUTH",
            "MAIL",
            "MAIL FROM",
            "RCPT",
            "RCPT TO",
            "DATA",
            "end of DATA",
            "RSET",
            "UNKNOWN",
            "END-OF-MESSAGE",
            "VRFY",
            "."
        };

        for (String stage : validStages) {
            Match match = grok.match(stage);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match SMTP stage: " + stage, captured);
            assertEquals(stage, captured.get("stage"));
        }
    }

    @Test
    public void testPostfixActionPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_ACTION:action}");

        String[] validActions = {
            "accept",
            "defer",
            "discard",
            "filter",
            "header-redirect",
            "reject"
        };

        for (String action : validActions) {
            Match match = grok.match(action);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match action: " + action, captured);
            assertEquals(action, captured.get("action"));
        }
    }

    @Test
    public void testPostfixStatusCodePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_STATUS_CODE:code}");

        String[] validStatusCodes = {
            "200",
            "250",
            "421",
            "450",
            "500",
            "550"
        };

        for (String code : validStatusCodes) {
            Match match = grok.match(code);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match status code: " + code, captured);
            assertEquals(code, captured.get("code"));
        }
    }

    @Test
    public void testPostfixStatusCodeEnhancedPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_STATUS_CODE_ENHANCED:code}");

        String[] validEnhancedCodes = {
            "2.0.0",
            "4.7.1",
            "5.1.1",
            "5.7.0",
            "4.4.2"
        };

        for (String code : validEnhancedCodes) {
            Match match = grok.match(code);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match enhanced status code: " + code, captured);
            assertEquals(code, captured.get("code"));
        }
    }

    @Test
    public void testPostfixTimeUnitPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_TIME_UNIT:time}");

        String[] validTimeUnits = {
            "60s",
            "5m",
            "2h",
            "7d",
            "0.5s",
            "1.5h"
        };

        for (String time : validTimeUnits) {
            Match match = grok.match(time);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match time unit: " + time, captured);
            assertEquals(time, captured.get("time"));
        }
    }

    @Test
    public void testPostfixWarningLevelPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_WARNING_LEVEL:level}");

        String[] validLevels = {
            "warning",
            "fatal",
            "info"
        };

        for (String level : validLevels) {
            Match match = grok.match(level);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match warning level: " + level, captured);
            assertEquals(level, captured.get("level"));
        }
    }

    // ========== SMTPD Pattern Tests ==========

    @Test
    public void testPostfixSmtpdConnectPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTPD_CONNECT}");

        String logLine = "connect from mail.example.com[192.168.1.100]";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTPD connect", captured);
        assertEquals("mail.example.com", captured.get("postfix_client_hostname"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
    }

    @Test
    public void testPostfixSmtpdDisconnectPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTPD_DISCONNECT}");

        String logLine = "disconnect from unknown[10.0.0.1]";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTPD disconnect", captured);
        assertEquals("unknown", captured.get("postfix_client_hostname"));
        assertEquals("10.0.0.1", captured.get("postfix_client_ip"));
    }

    @Test
    public void testPostfixSmtpdNoqueuePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTPD_NOQUEUE}");

        String logLine = "NOQUEUE: reject: RCPT from unknown[192.168.1.100]: 554 5.7.1 <test@example.com>: Relay access denied; from=<sender@test.com> to=<test@example.com> proto=ESMTP helo=<test>";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTPD NOQUEUE", captured);
        assertEquals("reject", captured.get("postfix_action"));
        assertEquals("RCPT", captured.get("postfix_smtp_stage"));
        assertEquals("unknown", captured.get("postfix_client_hostname"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
    }

    @Test
    public void testPostfixTlsConnPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_TLSCONN}");

        String logLine = "Anonymous TLS connection established from mail.example.com[192.168.1.100]: TLSv1.2 with cipher ECDHE-RSA-AES256-GCM-SHA384 (256/256 bits)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match TLS connection", captured);
        assertEquals("mail.example.com", captured.get("postfix_client_hostname"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
        assertEquals("TLSv1.2", captured.get("postfix_tls_version"));
        assertEquals("ECDHE-RSA-AES256-GCM-SHA384", captured.get("postfix_tls_cipher"));
        assertEquals("256/256", captured.get("postfix_tls_cipher_size"));
    }

    // ========== Cleanup Pattern Tests ==========

    @Test
    public void testPostfixCleanupMilterPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_CLEANUP_MILTER}");

        String logLine = "ABC123: milter-reject: END-OF-MESSAGE from mail.example.com[192.168.1.100]: 5.7.1 Message rejected; from=<sender@test.com> to=<recipient@example.com>";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match cleanup milter", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("reject", captured.get("postfix_milter_result"));
    }

    // ========== QMGR Pattern Tests ==========

    @Test
    public void testPostfixQmgrRemovedPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QMGR_REMOVED}");

        String logLine = "ABC123: removed";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match QMGR removed", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
    }

    @Test
    public void testPostfixQmgrActivePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QMGR_ACTIVE}");

        String logLine = "ABC123: from=<sender@example.com>, size=1234, nrcpt=1 (queue active)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match QMGR active", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertTrue("Should contain keyvalue data", captured.containsKey("postfix_keyvalue_data"));
    }

    @Test
    public void testPostfixQmgrExpiredPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QMGR_EXPIRED}");

        String logLine = "ABC123: from=<sender@example.com>, status=expired, returned to sender";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match QMGR expired", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("sender@example.com", captured.get("postfix_from"));
        assertEquals("expired", captured.get("postfix_status"));
    }

    // ========== Pipe Pattern Tests ==========

    @Test
    public void testPostfixPipePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_PIPE_ANY}");

        String logLine = "ABC123: to=<user@example.com>, relay=dovecot, delay=0.5, status=sent (delivered via dovecot service)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match pipe delivery", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("sent", captured.get("postfix_status"));
        assertTrue("Should contain pipe response", captured.containsKey("postfix_pipe_response"));
    }

    // ========== Error Pattern Tests ==========

    @Test
    public void testPostfixErrorPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_ERROR_ANY}");

        String logLine = "ABC123: to=<user@example.com>, relay=none, delay=0, status=bounced (mail system configuration error)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match error", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("bounced", captured.get("postfix_status"));
        assertTrue("Should contain error response", captured.containsKey("postfix_error_response"));
    }

    // ========== Postsuper Pattern Tests ==========

    @Test
    public void testPostfixPostsuperActionPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_POSTSUPER_ACTION}");

        // Test individual actions that don't have spaces
        String[] simpleActions = {
            "ABC123: removed",
            "DEF456: requeued"
        };

        for (String action : simpleActions) {
            Match match = grok.match(action);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match postsuper action: " + action, captured);
            // Check for queueid with alternation handling
            Object queueIdValue = captured.get("postfix_queueid");
            assertNotNull("Should contain queueid for: " + action, queueIdValue);
            assertTrue("Should contain action", captured.containsKey("postfix_postsuper_action"));
        }

        // Note: "placed on hold" and "released from hold" contain spaces
        // These need to be tested with more context or escaped properly
    }

    @Test
    public void testPostfixPostsuperSummaryPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_POSTSUPER_SUMMARY}");

        String[] validSummaries = {
            "Deleted: 10 messages",
            "Requeued: 5 message",
            "Placed on hold: 3 messages",
            "Released from hold: 1 message"
        };

        for (String summary : validSummaries) {
            Match match = grok.match(summary);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match postsuper summary: " + summary, captured);
            assertTrue("Should contain summary action", captured.containsKey("postfix_postsuper_summary_action"));
            assertTrue("Should contain count", captured.containsKey("postfix_postsuper_summary_count"));
        }
    }

    // ========== Postscreen Pattern Tests ==========

    @Test
    public void testPostfixPostscreenConnectPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_PS_CONNECT}");

        String logLine = "CONNECT from mail.example.com[192.168.1.100] to [10.0.0.1]:25";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match postscreen connect", captured);
        assertEquals("mail.example.com", captured.get("postfix_client_hostname"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
        assertEquals("10.0.0.1", captured.get("postfix_server_ip"));
        assertEquals("25", captured.get("postfix_server_port"));
    }

    @Test
    public void testPostfixPostscreenAccessPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_PS_ACCESS}");

        String[] validAccess = {
            "DISCONNECT unknown[192.168.1.100]",
            "BLACKLISTED mail.example.com[10.0.0.1]",
            "WHITELISTED smtp.trusted.com[203.0.113.5]",
            "PASS NEW unknown[172.16.0.1]",
            "PASS OLD mail.known.com[192.168.1.200]"
        };

        for (String access : validAccess) {
            Match match = grok.match(access);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match postscreen access: " + access, captured);
            assertTrue("Should contain access action", captured.containsKey("postfix_postscreen_access"));
        }
    }

    @Test
    public void testPostfixPostscreenDnsblPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_PS_DNSBL}");

        String logLine = "DNSBL rank 2 for mail.example.com[192.168.1.100]";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match postscreen DNSBL", captured);
        assertEquals("DNSBL", captured.get("postfix_postscreen_violation"));
        assertEquals("2", captured.get("postfix_postscreen_dnsbl_rank"));
    }

    // ========== DNSblog Pattern Tests ==========

    @Test
    public void testPostfixDnsblogListingPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_DNSBLOG_LISTING}");

        String logLine = "addr 192.168.1.100 listed by domain zen.spamhaus.org as 127.0.0.2";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match dnsblog listing", captured);
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
        assertEquals("zen.spamhaus.org", captured.get("postfix_dnsbl_domain"));
        assertEquals("127.0.0.2", captured.get("postfix_dnsbl_result"));
    }

    // ========== Anvil Pattern Tests ==========

    @Test
    public void testPostfixAnvilConnRatePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_ANVIL_CONN_RATE}");

        String logLine = "statistics: max connection rate 10/60s for (smtp:192.168.1.100) at Oct 11 22:14:15";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match anvil connection rate", captured);
        assertEquals("10", captured.get("postfix_anvil_conn_rate"));
        assertEquals("60s", captured.get("postfix_anvil_conn_period"));
        assertEquals("smtp", captured.get("postfix_service"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
    }

    @Test
    public void testPostfixAnvilConnCachePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_ANVIL_CONN_CACHE}");

        String logLine = "statistics: max cache size 256 at Oct 11 22:14:15";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match anvil cache", captured);
        assertEquals("256", captured.get("postfix_anvil_cache_size"));
        assertTrue("Should contain timestamp", captured.containsKey("postfix_anvil_timestamp"));
    }

    @Test
    public void testPostfixAnvilConnCountPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_ANVIL_CONN_COUNT}");

        String logLine = "statistics: max connection count 50 for (smtp:192.168.1.100) at Oct 11 22:14:15";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match anvil connection count", captured);
        assertEquals("50", captured.get("postfix_anvil_conn_count"));
        assertEquals("smtp", captured.get("postfix_service"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
    }

    // ========== SMTP Pattern Tests ==========

    @Test
    public void testPostfixSmtpDeliveryPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_DELIVERY}");

        String logLine = "ABC123: to=<user@example.com>, relay=mail.example.com[192.168.1.100]:25, delay=0.5, delays=0.1/0.1/0.2/0.1, dsn=2.0.0, status=sent (250 2.0.0 OK)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTP delivery", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("sent", captured.get("postfix_status"));
    }

    @Test
    public void testPostfixSmtpConnerrPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_CONNERR}");

        String[] validErrors = {
            "connect to mail.example.com[192.168.1.100]: Connection timed out",
            "connect to relay.test.com[10.0.0.1]: No route to host",
            "connect to smtp.fail.com[172.16.0.1]: Connection refused",
            "connect to server.down.com[203.0.113.5]: Network is unreachable"
        };

        for (String error : validErrors) {
            Match match = grok.match(error);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match SMTP connection error: " + error, captured);
        }
    }

    @Test
    public void testPostfixSmtpLostconnPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_LOSTCONN}");

        String logLine = "ABC123: lost connection with mail.example.com[192.168.1.100] while sending message body";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTP lost connection", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertTrue("Should contain lostconn data", captured.containsKey("postfix_smtp_lostconn_data"));
    }

    @Test
    public void testPostfixSmtpTimeoutPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_TIMEOUT}");

        String logLine = "ABC123: conversation with mail.example.com[192.168.1.100] timed out while receiving the initial server greeting";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTP timeout", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
    }

    @Test
    public void testPostfixSmtpRelayerrPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_RELAYERR}");

        String logLine = "ABC123: host mail.example.com[192.168.1.100] said: 550 5.1.1 <user@example.com>: Recipient address rejected: User unknown (in reply to RCPT TO command)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match SMTP relay error", captured);
        assertEquals("ABC123", captured.get("postfix_queueid"));
        assertEquals("RCPT TO", captured.get("postfix_smtp_stage"));
    }

    // ========== Master Pattern Tests ==========

    @Test
    public void testPostfixMasterStartPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_MASTER_START}");

        String logLine = "daemon started -- version 3.4.14, configuration /etc/postfix";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match master start", captured);
        assertEquals("3.4.14", captured.get("postfix_version"));
        assertEquals("/etc/postfix", captured.get("postfix_config_path"));
    }

    @Test
    public void testPostfixMasterExitPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_MASTER_EXIT}");

        String logLine = "terminating on signal 15";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match master exit", captured);
        assertEquals("15", captured.get("postfix_termination_signal"));
    }

    // ========== Bounce Pattern Tests ==========

    @Test
    public void testPostfixBounceNotificationPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_BOUNCE_NOTIFICATION}");

        // Test bounce notification - note: pattern matches "non-delivery", "delivery status", or "delay"
        String bounce = "ABC123: sender non-delivery notification: DEF456";
        Match match = grok.match(bounce);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match bounce notification", captured);
        // Check for queueid with alternation handling
        Object queueIdValue = captured.get("postfix_queueid");
        assertNotNull("Should contain queueid", queueIdValue);
        Object bounceQueueIdValue = captured.get("postfix_bounce_queueid");
        assertNotNull("Should contain bounce queueid", bounceQueueIdValue);
    }

    // ========== Scache Pattern Tests ==========

    @Test
    public void testPostfixScacheLookupsPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SCACHE_LOOKUPS}");

        String[] validLookups = {
            "statistics: address lookup hits=100 miss=10 success=90%",
            "statistics: domain lookup hits=50 miss=5 success=91%"
        };

        for (String lookup : validLookups) {
            Match match = grok.match(lookup);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match scache lookup: " + lookup, captured);
            assertTrue("Should contain hits", captured.containsKey("postfix_scache_hits"));
            assertTrue("Should contain miss", captured.containsKey("postfix_scache_miss"));
            assertTrue("Should contain success", captured.containsKey("postfix_scache_success"));
        }
    }

    @Test
    public void testPostfixScacheSimultaneousPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SCACHE_SIMULTANEOUS}");

        String logLine = "statistics: max simultaneous domains=10 addresses=100 connection=5";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match scache simultaneous", captured);
        assertEquals("10", captured.get("postfix_scache_domains"));
        assertEquals("100", captured.get("postfix_scache_addresses"));
        assertEquals("5", captured.get("postfix_scache_connection"));
    }

    // ========== Delays Pattern Tests ==========

    @Test
    public void testPostfixDelaysPattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_DELAYS}");

        String delayString = "0.01/0.02/0.5/0.1";
        Match match = grok.match(delayString);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match delays", captured);
        assertEquals("0.01", captured.get("postfix_delay_before_qmgr"));
        assertEquals("0.02", captured.get("postfix_delay_in_qmgr"));
        assertEquals("0.5", captured.get("postfix_delay_conn_setup"));
        assertEquals("0.1", captured.get("postfix_delay_transmission"));
    }

    // ========== Aggregate Pattern Tests ==========

    @Test
    public void testPostfixSmtpdAggregatePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTPD}");

        String[] validSmtpdLogs = {
            "connect from mail.example.com[192.168.1.100]",
            "disconnect from mail.example.com[192.168.1.100]",
            "ABC123: client=mail.example.com[192.168.1.100]"
        };

        for (String log : validSmtpdLogs) {
            Match match = grok.match(log);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match SMTPD aggregate: " + log, captured);
        }
    }

    @Test
    public void testPostfixQmgrAggregatePattern() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QMGR}");

        String[] validQmgrLogs = {
            "ABC123: removed",
            "DEF456: from=<sender@example.com>, size=1234, nrcpt=1 (queue active)",
            "GHI789: from=<sender@example.com>, status=expired, returned to sender"
        };

        for (String log : validQmgrLogs) {
            Match match = grok.match(log);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match QMGR aggregate: " + log, captured);
        }
    }

    // ========== Real-world Log Examples ==========

    @Test
    public void testRealWorldSmtpDelivery() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTP_DELIVERY}");

        String logLine = "A1B2C3D4E5F6: to=<recipient@example.com>, relay=mail.example.com[192.168.1.100]:25, delay=2.5, delays=0.01/0.02/1.0/1.47, dsn=2.0.0, status=sent (250 2.0.0 Ok: queued as FEDCBA)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real SMTP delivery", captured);
        assertEquals("A1B2C3D4E5F6", captured.get("postfix_queueid"));
        assertEquals("sent", captured.get("postfix_status"));
    }

    @Test
    public void testRealWorldSmtpReject() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_SMTPD_NOQUEUE}");

        String logLine = "NOQUEUE: reject: RCPT from unknown[192.0.2.1]: 554 5.7.1 <spam@badhost.com>: Relay access denied; from=<sender@test.com> to=<victim@example.com> proto=ESMTP helo=<badhost.com>";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real SMTP reject", captured);
        assertEquals("reject", captured.get("postfix_action"));
        assertEquals("RCPT", captured.get("postfix_smtp_stage"));
    }

    @Test
    public void testRealWorldTlsConnection() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_TLSCONN}");

        String logLine = "Trusted TLS connection established to relay.example.com[203.0.113.5]:587: TLSv1.3 with cipher TLS_AES_256_GCM_SHA384 (256/256 bits)";
        Match match = grok.match(logLine);
        Map<String, Object> captured = match.capture();

        assertNotNull("Failed to match real TLS connection", captured);
        assertEquals("relay.example.com", captured.get("postfix_relay_hostname"));
        assertEquals("203.0.113.5", captured.get("postfix_relay_ip"));
        assertEquals("TLSv1.3", captured.get("postfix_tls_version"));
    }

    @Test
    public void testRealWorldQueueManagement() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QMGR}");

        String[] realLogs = {
            "1A2B3C4D: from=<sender@example.com>, size=2048, nrcpt=2 (queue active)",
            "5E6F7G8H: removed"
        };

        for (String log : realLogs) {
            Match match = grok.match(log);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match real QMGR log: " + log, captured);
        }
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testPatternRegistration() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String[] requiredPatterns = {
            "POSTFIX_QUEUEID",
            "POSTFIX_CLIENT_INFO",
            "POSTFIX_RELAY_INFO",
            "POSTFIX_SMTP_STAGE",
            "POSTFIX_ACTION",
            "POSTFIX_STATUS_CODE",
            "POSTFIX_STATUS_CODE_ENHANCED",
            "POSTFIX_SMTPD",
            "POSTFIX_CLEANUP",
            "POSTFIX_QMGR",
            "POSTFIX_PIPE",
            "POSTFIX_POSTSCREEN",
            "POSTFIX_DNSBLOG",
            "POSTFIX_ANVIL",
            "POSTFIX_SMTP",
            "POSTFIX_MASTER",
            "POSTFIX_BOUNCE",
            "POSTFIX_DISCARD",
            "POSTFIX_LMTP",
            "POSTFIX_PICKUP",
            "POSTFIX_TLSPROXY",
            "POSTFIX_SENDMAIL",
            "POSTFIX_POSTDROP",
            "POSTFIX_SCACHE",
            "POSTFIX_TRIVIAL_REWRITE",
            "POSTFIX_TLSMGR",
            "POSTFIX_LOCAL",
            "POSTFIX_VIRTUAL",
            "POSTFIX_ERROR",
            "POSTFIX_POSTSUPER"
        };

        for (String patternName : requiredPatterns) {
            assertTrue("Pattern should be registered: " + patternName, patterns.containsKey(patternName));
            assertNotNull("Pattern definition should not be null: " + patternName, patterns.get(patternName));
        }
    }

    @Test
    public void testPatternCompilation() throws Exception {
        String[] patterns = {
            "%{POSTFIX_QUEUEID}",
            "%{POSTFIX_CLIENT_INFO}",
            "%{POSTFIX_RELAY_INFO}",
            "%{POSTFIX_SMTP_STAGE}",
            "%{POSTFIX_ACTION}",
            "%{POSTFIX_STATUS_CODE}",
            "%{POSTFIX_STATUS_CODE_ENHANCED}",
            "%{POSTFIX_TIME_UNIT}",
            "%{POSTFIX_WARNING_LEVEL}",
            "%{POSTFIX_DELAYS}",
            "%{POSTFIX_KEYVALUE}",
            "%{POSTFIX_SMTPD_CONNECT}",
            "%{POSTFIX_SMTPD_DISCONNECT}",
            "%{POSTFIX_QMGR_REMOVED}",
            "%{POSTFIX_QMGR_ACTIVE}",
            "%{POSTFIX_MASTER_START}",
            "%{POSTFIX_MASTER_EXIT}",
            "%{POSTFIX_ANVIL_CONN_RATE}",
            "%{POSTFIX_DNSBLOG_LISTING}",
            "%{POSTFIX_POSTSUPER_ACTION}",
            "%{POSTFIX_POSTSUPER_SUMMARY}"
        };

        for (String pattern : patterns) {
            try {
                Grok grok = compiler.compile(pattern);
                assertNotNull("Pattern should compile: " + pattern, grok);
            } catch (Exception e) {
                fail("Failed to compile pattern: " + pattern + " - " + e.getMessage());
            }
        }
    }

    @Test
    public void testHelperPatternDefinitions() throws Exception {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        // Verify helper patterns exist and have correct definitions
        assertTrue("GREEDYDATA_NO_COLON should be registered", patterns.containsKey("GREEDYDATA_NO_COLON"));
        assertEquals("[^:]*", patterns.get("GREEDYDATA_NO_COLON"));

        assertTrue("GREEDYDATA_NO_SEMICOLON should be registered", patterns.containsKey("GREEDYDATA_NO_SEMICOLON"));
        assertEquals("[^;]*", patterns.get("GREEDYDATA_NO_SEMICOLON"));
    }

    // ========== Edge Cases and Validation ==========

    @Test
    public void testQueueIdVariations() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_QUEUEID:queueid}");

        String[] queueIds = {
            "ABCDEF",          // Minimum 6 hex
            "123456",          // All numbers hex
            "AABBCCDDEEFF00",  // 14 hex chars
            "abcdefghijklmno"  // 15 lowercase alphanumeric
        };

        for (String queueId : queueIds) {
            Match match = grok.match(queueId);
            Map<String, Object> captured = match.capture();
            assertNotNull("Failed to match queue ID variation: " + queueId, captured);
        }
    }

    @Test
    public void testClientInfoWithoutHostname() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_CLIENT_INFO}");

        String clientInfo = "[192.168.1.100]";
        Match match = grok.match(clientInfo);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match client info without hostname", captured);
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
    }

    @Test
    public void testClientInfoWithPort() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_CLIENT_INFO}");

        String clientInfo = "mail.example.com[192.168.1.100]:12345";
        Match match = grok.match(clientInfo);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match client info with port", captured);
        assertEquals("mail.example.com", captured.get("postfix_client_hostname"));
        assertEquals("192.168.1.100", captured.get("postfix_client_ip"));
        assertEquals("12345", captured.get("postfix_client_port"));
    }

    @Test
    public void testRelayInfoServiceName() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_RELAY_INFO}");

        String relayInfo = "dovecot";
        Match match = grok.match(relayInfo);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match relay service name", captured);
        // Pattern uses alternation so result may be in an array
        Object serviceValue = captured.get("postfix_relay_service");
        if (serviceValue instanceof java.util.List) {
            java.util.List services = (java.util.List) serviceValue;
            boolean found = false;
            for (Object service : services) {
                if ("dovecot".equals(service)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Service name should be in list", found);
        } else {
            assertEquals("dovecot", serviceValue);
        }
    }

    @Test
    public void testZeroDelays() throws Exception {
        Grok grok = compiler.compile("%{POSTFIX_DELAYS}");

        String delayString = "0/0/0/0";
        Match match = grok.match(delayString);
        Map<String, Object> captured = match.capture();

        assertNotNull("Should match zero delays", captured);
        assertEquals("0", captured.get("postfix_delay_before_qmgr"));
        assertEquals("0", captured.get("postfix_delay_in_qmgr"));
        assertEquals("0", captured.get("postfix_delay_conn_setup"));
        assertEquals("0", captured.get("postfix_delay_transmission"));
    }
}
