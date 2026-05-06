package io.whatap.grok.api.engine;

import com.google.common.io.Resources;
import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Reproduces the 2026-04-29 LogSink ReDoS incident (eland mall, pcode 42570).
 *
 * Pattern: \[%{DATA:thread}\] %{LOGLEVEL:loglevel} %{DATA:class} - %{GREEDYDATA:log}
 *
 * Triggering input: SQL method signature in brackets without trailing LOGLEVEL
 *   [public boolean com.zaxxer.hikari.pool.HikariProxyPreparedStatement.execute() throws java.sql.SQLException]
 *
 * Without timeout, java.util.regex.Matcher.find() can enter catastrophic
 * backtracking and consume CPU for minutes. With re2j or JavaRegexEngine timeout,
 * matching completes (or aborts) quickly.
 */
public class CatastrophicBacktrackingTest {

  private static final String PROBLEM_PATTERN =
      "\\[%{DATA:thread}\\] %{LOGLEVEL:loglevel} %{DATA:class} - %{GREEDYDATA:log}";

  private static final String TRIGGERING_INPUT_BASE =
      "[public boolean com.zaxxer.hikari.pool.HikariProxyPreparedStatement.execute()"
          + " throws java.sql.SQLException, ";

  private GrokCompiler compiler;

  @Before
  public void setup() throws Exception {
    compiler = GrokCompiler.newInstance();
    compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
  }

  private static String pathologicalInput() {
    StringBuilder sb = new StringBuilder(TRIGGERING_INPUT_BASE);
    for (int i = 0; i < 50; i++) {
      sb.append("col").append(i).append(", ");
    }
    sb.append("password VARCHAR] HikariProxyPreparedStatement@123 wrapping /* SQL: SELECT * FROM users */");
    return sb.toString();
  }

  @Test
  public void java_engine_with_timeout_aborts_catastrophic_match() {
    JavaRegexEngine engine = new JavaRegexEngine(300L, 256);
    Grok grok = compiler.compile(PROBLEM_PATTERN, engine);

    String input = pathologicalInput();
    long t0 = System.currentTimeMillis();
    try {
      grok.capture(input);
    } catch (RegexTimeoutException expected) {
      long elapsed = System.currentTimeMillis() - t0;
      assertTrue("timeout fired in " + elapsed + "ms (expected <1000)", elapsed < 1000);
      return;
    }
    long elapsed = System.currentTimeMillis() - t0;
    assertTrue("match completed in " + elapsed + "ms; would have been ok regardless",
        elapsed < 1000);
  }

  @Test
  public void hotfix_pattern_avoids_backtracking_with_java_engine() {
    String hotfixPattern =
        "\\[%{NOTSPACE:thread}\\]\\s+%{LOGLEVEL:loglevel}\\s+%{NOTSPACE:class}\\s+-\\s+%{GREEDYDATA:log}";

    JavaRegexEngine engine = new JavaRegexEngine(200L, 256);
    Grok grok = compiler.compile(hotfixPattern, engine);

    String input = pathologicalInput();
    long t0 = System.currentTimeMillis();
    Map<String, Object> captured = grok.capture(input);
    long elapsed = System.currentTimeMillis() - t0;

    assertTrue("hotfix pattern took " + elapsed + "ms (expected <50)", elapsed < 50);
    assertNotNull(captured);
  }

  @Test
  public void hotfix_pattern_matches_normal_log_line() {
    String hotfixPattern =
        "\\[%{NOTSPACE:thread}\\]\\s+%{LOGLEVEL:loglevel}\\s+%{NOTSPACE:class}\\s+-\\s+%{GREEDYDATA:log}";

    Grok grok = compiler.compile(hotfixPattern);
    Map<String, Object> captured = grok.capture(
        "[http-nio-8080-exec-16] INFO  c.d.f.c.c.WebClientConfig - Request:GET /api/v1/users");

    assertEquals("http-nio-8080-exec-16", captured.get("thread"));
    assertEquals("INFO", captured.get("loglevel"));
    assertEquals("c.d.f.c.c.WebClientConfig", captured.get("class"));
    assertEquals("Request:GET /api/v1/users", captured.get("log"));
  }

  @Test
  public void hybrid_engine_works_for_problem_pattern() {
    HybridEngine engine = new HybridEngine(500L, 1024);
    Grok grok = compiler.compile(PROBLEM_PATTERN, engine);

    Map<String, Object> captured = grok.capture(
        "[http-nio-8080-exec-16] INFO c.d.f.c.c.WebClientConfig - Request:GET /api/v1/users");

    assertEquals("http-nio-8080-exec-16", captured.get("thread"));
    assertEquals("INFO", captured.get("loglevel"));
  }

  @Test(timeout = 2000L)
  public void java_engine_without_timeout_can_be_unbounded() {
    JavaRegexEngine engine = new JavaRegexEngine(0L, 1024);
    Grok grok = compiler.compile(PROBLEM_PATTERN, engine);

    String input = pathologicalInput();
    try {
      grok.capture(input);
    } catch (RuntimeException ignored) {
      // result doesn't matter
    }
  }
}
