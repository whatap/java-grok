package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import io.whatap.grok.api.PatternType;
import io.whatap.grok.api.exception.GrokException;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test suite for AWS pattern parsing.
 * Tests S3, ELB, and CloudFront access log patterns with ECS-style field extraction.
 *
 * <p>Note: AWS patterns currently have issues with pattern compilation due to:
 * <ul>
 *   <li>Missing URIQUERY pattern (now added as alias)</li>
 *   <li>Complex nested quantifiers causing regex compilation errors</li>
 *   <li>ECS field naming with square brackets may cause parsing issues</li>
 * </ul>
 *
 * <p>This test focuses on validating that:
 * <ul>
 *   <li>AWS patterns can be registered without errors</li>
 *   <li>Simple S3_REQUEST_LINE pattern compiles and works</li>
 *   <li>Pattern structure and field naming follows ECS conventions</li>
 * </ul>
 */
public class AwsPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setup() throws GrokException {
        compiler = GrokCompiler.newInstance();
        compiler.registerDefaultPatterns();
        compiler.registerPatterns(PatternType.AWS);
    }

    // ========== Pattern Registration Tests ==========

    @Test
    public void testAwsPatternsCanBeRegistered() {
        // Verify AWS patterns are registered
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("S3_REQUEST_LINE should be registered",
                patterns.containsKey("S3_REQUEST_LINE"));
        assertTrue("S3_ACCESS_LOG should be registered",
                patterns.containsKey("S3_ACCESS_LOG"));
        assertTrue("ELB_V1_HTTP_LOG should be registered",
                patterns.containsKey("ELB_V1_HTTP_LOG"));
        assertTrue("ELB_ACCESS_LOG should be registered",
                patterns.containsKey("ELB_ACCESS_LOG"));
        assertTrue("CLOUDFRONT_ACCESS_LOG should be registered",
                patterns.containsKey("CLOUDFRONT_ACCESS_LOG"));
        assertTrue("URIQUERY should be registered (added for AWS compatibility)",
                patterns.containsKey("URIQUERY"));
    }

    @Test
    public void testPatternDefinitionsAreNonEmpty() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String s3RequestLine = patterns.get("S3_REQUEST_LINE");
        assertNotNull("S3_REQUEST_LINE definition should not be null", s3RequestLine);
        assertFalse("S3_REQUEST_LINE definition should not be empty", s3RequestLine.trim().isEmpty());

        String s3AccessLog = patterns.get("S3_ACCESS_LOG");
        assertNotNull("S3_ACCESS_LOG definition should not be null", s3AccessLog);
        assertFalse("S3_ACCESS_LOG definition should not be empty", s3AccessLog.trim().isEmpty());
    }

    // ========== S3 REQUEST_LINE Pattern Tests ==========

    @Test
    public void testS3RequestLinePatternCompiles() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");
        assertNotNull("S3_REQUEST_LINE pattern should compile", grok);
    }

    @Test
    public void testS3RequestLineWithFullHttpRequest() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");

        String log = "GET /my-bucket/path/to/file.jpg HTTP/1.1";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed", captured.isEmpty());
        assertEquals("GET", captured.get("http.request.method"));
        assertEquals("/my-bucket/path/to/file.jpg", captured.get("url.original"));
        assertEquals("1.1", captured.get("http.version"));
    }

    @Test
    public void testS3RequestLineWithoutHttpVersion() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");

        String log = "PUT /bucket/key";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed", captured.isEmpty());
        assertEquals("PUT", captured.get("http.request.method"));
        assertEquals("/bucket/key", captured.get("url.original"));
    }

    @Test
    public void testS3RequestLineVariousMethods() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");

        String[] methods = {"GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"};
        for (String method : methods) {
            String log = method + " /bucket/object HTTP/1.1";
            Match match = grok.match(log);
            Map<String, Object> captured = match.capture();

            assertFalse("Match should succeed for " + method, captured.isEmpty());
            assertEquals(method, captured.get("http.request.method"));
        }
    }

    @Test
    public void testS3RequestLineWithQueryString() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");

        String log = "GET /bucket/object?versioning&acl HTTP/1.1";
        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        assertFalse("Match should succeed", captured.isEmpty());
        assertEquals("GET", captured.get("http.request.method"));
        assertEquals("/bucket/object?versioning&acl", captured.get("url.original"));
        assertEquals("1.1", captured.get("http.version"));
    }

    // ========== ECS Field Name Convention Tests ==========

    @Test
    public void testS3RequestLineUsesECSFieldNames() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");
        String log = "GET /file HTTP/1.1";

        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        // Verify ECS-compliant nested field names with square brackets
        assertTrue("Should use http.request.method field name",
                captured.containsKey("http.request.method"));
        assertTrue("Should use url.original field name",
                captured.containsKey("url.original"));
        assertTrue("Should use http.version field name",
                captured.containsKey("http.version"));
    }

    @Test
    public void testAwsPatternsDefineECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String s3AccessLog = patterns.get("S3_ACCESS_LOG");

        // Verify pattern definitions contain ECS-style field references
        assertTrue("S3_ACCESS_LOG should reference aws.s3access.bucket_owner",
                s3AccessLog.contains("aws.s3access.bucket_owner"));
        assertTrue("S3_ACCESS_LOG should reference client.ip",
                s3AccessLog.contains("client.ip"));
        assertTrue("S3_ACCESS_LOG should reference http.response.status_code",
                s3AccessLog.contains("http.response.status_code"));
        assertTrue("S3_ACCESS_LOG should reference user_agent.original",
                s3AccessLog.contains("user_agent.original"));
    }

    @Test
    public void testELBPatternsDefineECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String elbLog = patterns.get("ELB_V1_HTTP_LOG");

        // Verify ELB patterns use ECS field names
        assertTrue("ELB_V1_HTTP_LOG should reference aws.elb.name",
                elbLog.contains("aws.elb.name"));
        assertTrue("ELB_V1_HTTP_LOG should reference source.ip",
                elbLog.contains("source.ip"));
        assertTrue("ELB_V1_HTTP_LOG should reference source.port",
                elbLog.contains("source.port"));
        assertTrue("ELB_V1_HTTP_LOG should reference aws.elb.backend.ip",
                elbLog.contains("aws.elb.backend.ip"));
    }

    @Test
    public void testCloudFrontPatternsDefineECSFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String cfLog = patterns.get("CLOUDFRONT_ACCESS_LOG");

        // Verify CloudFront patterns use ECS field names
        assertTrue("CLOUDFRONT_ACCESS_LOG should reference aws.cloudfront.x_edge_location",
                cfLog.contains("aws.cloudfront.x_edge_location"));
        assertTrue("CLOUDFRONT_ACCESS_LOG should reference destination.bytes",
                cfLog.contains("destination.bytes"));
        assertTrue("CLOUDFRONT_ACCESS_LOG should reference source.ip",
                cfLog.contains("source.ip"));
        assertTrue("CLOUDFRONT_ACCESS_LOG should reference network.protocol",
                cfLog.contains("network.protocol"));
    }

    // ========== Pattern Structure Tests ==========

    @Test
    public void testS3AccessLogDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String s3AccessLog = patterns.get("S3_ACCESS_LOG");

        // Verify key S3 fields are defined in the pattern
        String[] expectedFields = {
            "aws.s3access.bucket_owner",
            "aws.s3access.bucket",
            "aws.s3access.request_id",
            "aws.s3access.operation",
            "aws.s3access.key",
            "aws.s3access.bytes_sent",
            "aws.s3access.object_size",
            "aws.s3access.total_time",
            "aws.s3access.turn_around_time",
            "tls.cipher"
        };

        for (String field : expectedFields) {
            assertTrue("S3_ACCESS_LOG should define field: " + field,
                    s3AccessLog.contains(field));
        }
    }

    @Test
    public void testELBV1HttpLogDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String elbLog = patterns.get("ELB_V1_HTTP_LOG");

        // Verify key ELB fields are defined in the pattern
        String[] expectedFields = {
            "aws.elb.name",
            "source.ip",
            "source.port",
            "aws.elb.backend.ip",
            "aws.elb.backend.port",
            "aws.elb.request_processing_time.sec",
            "aws.elb.backend_processing_time.sec",
            "aws.elb.response_processing_time.sec",
            "http.response.status_code",
            "http.request.body.bytes",
            "http.response.body.bytes",
            "tls.cipher",
            "aws.elb.ssl_protocol"
        };

        for (String field : expectedFields) {
            assertTrue("ELB_V1_HTTP_LOG should define field: " + field,
                    elbLog.contains(field));
        }
    }

    @Test
    public void testCloudFrontAccessLogDefinesAllExpectedFields() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        String cfLog = patterns.get("CLOUDFRONT_ACCESS_LOG");

        // Verify key CloudFront fields are defined in the pattern
        String[] expectedFields = {
            "aws.cloudfront.x_edge_location",
            "destination.bytes",
            "source.ip",
            "source.port",
            "source.bytes",
            "http.request.method",
            "url.domain",
            "url.path",
            "url.query",
            "http.response.status_code",
            "http.request.referrer",
            "user_agent.original",
            "aws.cloudfront.x_edge_result_type",
            "aws.cloudfront.x_edge_request_id",
            "network.protocol",
            "aws.cloudfront.time_taken",
            "network.forwarded_ip",
            "aws.cloudfront.ssl_protocol",
            "tls.cipher",
            "aws.cloudfront.time_to_first_byte",
            "http.request.mime_type",
            "aws.cloudfront.http.request.size"
        };

        for (String field : expectedFields) {
            assertTrue("CLOUDFRONT_ACCESS_LOG should define field: " + field,
                    cfLog.contains(field));
        }
    }

    // ========== Helper Pattern Tests ==========

    @Test
    public void testURIQUERYPatternExists() {
        Map<String, String> patterns = compiler.getPatternDefinitions();
        assertTrue("URIQUERY pattern should be defined (required by AWS patterns)",
                patterns.containsKey("URIQUERY"));

        String uriquery = patterns.get("URIQUERY");
        assertNotNull("URIQUERY definition should not be null", uriquery);
        assertFalse("URIQUERY definition should not be empty", uriquery.trim().isEmpty());
    }

    @Test
    public void testELBHelperPatternsAreDefined() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        assertTrue("ELB_URIHOST should be defined", patterns.containsKey("ELB_URIHOST"));
        assertTrue("ELB_URIPATHQUERY should be defined", patterns.containsKey("ELB_URIPATHQUERY"));
        assertTrue("ELB_URIPATHPARAM should be defined", patterns.containsKey("ELB_URIPATHPARAM"));
        assertTrue("ELB_URI should be defined", patterns.containsKey("ELB_URI"));
        assertTrue("ELB_REQUEST_LINE should be defined", patterns.containsKey("ELB_REQUEST_LINE"));
    }

    // ========== Data Type Conversion Tests ==========

    @Test
    public void testS3RequestLineExtractsStrings() throws GrokException {
        Grok grok = compiler.compile("%{S3_REQUEST_LINE}");
        String log = "GET /path HTTP/1.1";

        Match match = grok.match(log);
        Map<String, Object> captured = match.capture();

        // All S3_REQUEST_LINE fields should be strings
        Object method = captured.get("http.request.method");
        assertTrue("Method should be String", method instanceof String);

        Object url = captured.get("url.original");
        assertTrue("URL should be String", url instanceof String);

        Object version = captured.get("http.version");
        assertTrue("HTTP version should be String", version instanceof String);
    }

    @Test
    public void testPatternDefinitionsIncludeTypeConversions() {
        Map<String, String> patterns = compiler.getPatternDefinitions();

        String s3AccessLog = patterns.get("S3_ACCESS_LOG");

        // Verify integer type conversions are defined
        assertTrue("S3_ACCESS_LOG should have :integer type for status_code",
                s3AccessLog.contains("http.response.status_code:integer"));
        assertTrue("S3_ACCESS_LOG should have :integer type for bytes_sent",
                s3AccessLog.contains("aws.s3access.bytes_sent:integer"));
        assertTrue("S3_ACCESS_LOG should have :integer type for object_size",
                s3AccessLog.contains("aws.s3access.object_size:integer"));
    }
}
