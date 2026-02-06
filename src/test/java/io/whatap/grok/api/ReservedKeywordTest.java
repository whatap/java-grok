package io.whatap.grok.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * TDD tests for reserved keyword substitution at compile time.
 *
 * WhaTap Log Monitoring reserves these field names for internal processing:
 *   timestamp -> log_timestamp
 *   time      -> log_time
 *   message   -> log_message
 *   content   -> log_content
 *   category  -> log_category
 *   pcode     -> log_pcode
 *   logContent -> log_body
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReservedKeywordTest {

  GrokCompiler compiler;

  @Before
  public void setUp() throws Exception {
    compiler = GrokCompiler.newInstance();
    compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
  }

  @Test
  public void test001_timestampReservedKeywordIsRenamed() {
    compiler.register("MYLOG", "\\d{4}-\\d{2}-\\d{2}");
    Grok grok = compiler.compile("%{MYLOG:timestamp}");
    Map<String, Object> result = grok.capture("2024-01-15");

    assertNull("'timestamp' should not exist as key", result.get("timestamp"));
    assertEquals("2024-01-15", result.get("log_timestamp"));
  }

  @Test
  public void test002_timeReservedKeywordIsRenamed() {
    compiler.register("MYTIME", "\\d{2}:\\d{2}:\\d{2}");
    Grok grok = compiler.compile("%{MYTIME:time}");
    Map<String, Object> result = grok.capture("12:30:45");

    assertNull("'time' should not exist as key", result.get("time"));
    assertEquals("12:30:45", result.get("log_time"));
  }

  @Test
  public void test003_messageReservedKeywordIsRenamed() {
    compiler.register("MSG", ".+");
    Grok grok = compiler.compile("%{MSG:message}");
    Map<String, Object> result = grok.capture("hello world");

    assertNull("'message' should not exist as key", result.get("message"));
    assertEquals("hello world", result.get("log_message"));
  }

  @Test
  public void test004_contentReservedKeywordIsRenamed() {
    compiler.register("BODY", ".+");
    Grok grok = compiler.compile("%{BODY:content}");
    Map<String, Object> result = grok.capture("some content here");

    assertNull("'content' should not exist as key", result.get("content"));
    assertEquals("some content here", result.get("log_content"));
  }

  @Test
  public void test005_categoryReservedKeywordIsRenamed() {
    compiler.register("CAT", "\\w+");
    Grok grok = compiler.compile("%{CAT:category}");
    Map<String, Object> result = grok.capture("ERROR");

    assertNull("'category' should not exist as key", result.get("category"));
    assertEquals("ERROR", result.get("log_category"));
  }

  @Test
  public void test006_pcodeReservedKeywordIsRenamed() {
    compiler.register("CODE", "\\d+");
    Grok grok = compiler.compile("%{CODE:pcode}");
    Map<String, Object> result = grok.capture("12345");

    assertNull("'pcode' should not exist as key", result.get("pcode"));
    assertEquals("12345", result.get("log_pcode"));
  }

  @Test
  public void test007_logContentReservedKeywordIsRenamed() {
    compiler.register("BODY", ".+");
    Grok grok = compiler.compile("%{BODY:logContent}");
    Map<String, Object> result = grok.capture("log body text");

    assertNull("'logContent' should not exist as key", result.get("logContent"));
    assertEquals("log body text", result.get("log_body"));
  }

  @Test
  public void test008_nonReservedKeywordIsNotRenamed() {
    compiler.register("WORD", "\\w+");
    Grok grok = compiler.compile("%{WORD:username}");
    Map<String, Object> result = grok.capture("john");

    assertEquals("john", result.get("username"));
    assertNull("'log_username' should not exist", result.get("log_username"));
  }

  @Test
  public void test009_multipleReservedKeywordsInOnePattern() {
    compiler.register("TS", "\\S+");
    compiler.register("MSG", ".+");
    Grok grok = compiler.compile("%{TS:timestamp} %{MSG:message}");
    Map<String, Object> result = grok.capture("2024-01-15 hello world");

    assertNull(result.get("timestamp"));
    assertNull(result.get("message"));
    assertEquals("2024-01-15", result.get("log_timestamp"));
    assertEquals("hello world", result.get("log_message"));
  }

  @Test
  public void test010_mixedReservedAndNonReservedKeys() {
    compiler.register("TS", "\\S+");
    compiler.register("LVL", "\\w+");
    compiler.register("MSG", ".+");
    Grok grok = compiler.compile("%{TS:timestamp} %{LVL:level} %{MSG:message}");
    Map<String, Object> result = grok.capture("2024-01-15 ERROR something broke");

    assertNull(result.get("timestamp"));
    assertNull(result.get("message"));
    assertEquals("2024-01-15", result.get("log_timestamp"));
    assertEquals("ERROR", result.get("level"));
    assertEquals("something broke", result.get("log_message"));
  }

  @Test
  public void test011_alreadyRenamedFieldIsNotDoubleRenamed() {
    compiler.register("TS", "\\S+");
    Grok grok = compiler.compile("%{TS:log_timestamp}");
    Map<String, Object> result = grok.capture("2024-01-15");

    assertEquals("2024-01-15", result.get("log_timestamp"));
  }

  @Test
  public void test012_reservedKeywordDisabledDoesNotRename() {
    GrokCompiler noRenameCompiler = GrokCompiler.newInstance();
    noRenameCompiler.setReservedKeywordRenaming(false);
    noRenameCompiler.register("WORD", "\\w+");
    Grok grok = noRenameCompiler.compile("%{WORD:timestamp}");
    Map<String, Object> result = grok.capture("hello");

    assertEquals("hello", result.get("timestamp"));
    assertNull(result.get("log_timestamp"));
  }

  @Test
  public void test013_reservedKeywordEnabledByDefault() {
    GrokCompiler freshCompiler = GrokCompiler.newInstance();
    freshCompiler.register("WORD", "\\w+");
    Grok grok = freshCompiler.compile("%{WORD:timestamp}");
    Map<String, Object> result = grok.capture("hello");

    assertNull(result.get("timestamp"));
    assertEquals("hello", result.get("log_timestamp"));
  }
}
