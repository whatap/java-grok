package io.whatap.grok.api.engine;

import com.google.common.io.Resources;
import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.ResourceManager;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 이랜드 패턴이 실제로 어떤 엔진으로 매칭되는지 확인.
 */
public class ElandPatternEngineTest {

  private static final String ELAND_PATTERN =
      "\\[%{DATA:thread}\\] %{LOGLEVEL:loglevel} %{DATA:class} - %{GREEDYDATA:log}";

  private GrokCompiler compiler;

  @Before
  public void setup() throws Exception {
    compiler = GrokCompiler.newInstance();
    compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
  }

  @Test
  public void eland_pattern_compiles_with_hybrid_engine() {
    HybridEngine engine = new HybridEngine(500L, 1024);
    Grok grok = compiler.compile(ELAND_PATTERN, engine);

    System.out.println("=== Eland pattern compiled engine = " + grok.getEngineName());
    assertNotNull(grok.getEngineName());
  }

  @Test
  public void eland_pattern_compiles_with_re2j_directly() {
    Re2jEngine engine = new Re2jEngine();
    try {
      Grok grok = compiler.compile(ELAND_PATTERN, engine);
      System.out.println("=== re2j direct compile OK, engine=" + grok.getEngineName());
    } catch (RegexCompileException e) {
      System.out.println("=== re2j direct compile FAILED: " + e.getMessage());
      throw e;
    }
  }

  @Test
  public void hybrid_engine_matches_pathological_line_quickly() {
    HybridEngine engine = new HybridEngine(500L, 1024);
    Grok grok = compiler.compile(ELAND_PATTERN, engine);

    StringBuilder sb = new StringBuilder("[public boolean com.zaxxer.hikari.pool.HikariProxy] ");
    for (int i = 0; i < 80; i++) {
      sb.append("[col_").append(i).append("=v]");
    }

    long t0 = System.currentTimeMillis();
    Map<String, Object> r = grok.capture(sb.toString());
    long elapsed = System.currentTimeMillis() - t0;

    System.out.println("=== hybrid match elapsed=" + elapsed + "ms engine=" + grok.getEngineName()
        + " result=" + (r == null || r.isEmpty() ? "no_match" : "match"));
    assertTrue("hybrid took " + elapsed + "ms (should be <100ms)", elapsed < 100);
  }

  @Test
  public void benchmark_real_demo_line_throughput() {
    // 데모 앱의 ElandLoggingService가 생성하는 실제 라인 재현 (~5KB)
    StringBuilder sb = new StringBuilder(8192);
    sb.append("[public boolean com.zaxxer.hikari.pool.HikariProxyPreparedStatement.execute()")
        .append(" throws java.sql.SQLException,비밀번호 필드 조회]")
        .append(" HikariProxyPreparedStatement@1446461757 wrapping")
        .append(" /* LEE_JINSU01 2022-06-28 TokenMapper - getApprUserInfo */")
        .append(" SELECT b.user_id, a.oa_id, b.use_start_dt, b.use_end_dt");
    for (int i = 0; i < 30; i++) sb.append(" [col_").append(i).append("=val_").append(i).append("]");
    sb.append(" FROM ma_openapiapprokey_m a INNER JOIN ur_user_m b ON a.user_id = b.user_id")
        .append(" WHERE 1=1 AND a.oa_id = 'L251013211'")
        .append(" AND a.oa_pwd_enc = 'D7FRLMYi88YbRjilnpv1yQ=='")
        .append(" AND a.use_yn = 'Y' AND b.use_yn = 'Y'");
    for (int i = 0; i < 50; i++) sb.append(" [bind_").append(i).append("=java.lang.String:value-with-some-padding-text-").append(i).append("]");
    String line = sb.toString();
    System.out.println("=== line length = " + line.length() + " chars");

    // re2j 단독
    Grok re2j = compiler.compile(ELAND_PATTERN, new Re2jEngine());
    int iters = 1000;
    long t0 = System.nanoTime();
    for (int i = 0; i < iters; i++) {
      re2j.capture(line);
    }
    long elapsedNs = System.nanoTime() - t0;
    System.out.println("=== re2j: " + iters + " iters in " + (elapsedNs / 1_000_000) + "ms"
        + " → " + (elapsedNs / iters / 1000) + "μs/iter, " + (1_000_000_000L * iters / elapsedNs) + " ops/s");

    // java + 500ms timeout
    Grok java = compiler.compile(ELAND_PATTERN, new JavaRegexEngine(500L, 1024));
    long jt0 = System.nanoTime();
    for (int i = 0; i < iters; i++) {
      java.capture(line);
    }
    long jElapsedNs = System.nanoTime() - jt0;
    System.out.println("=== java+timeout: " + iters + " iters in " + (jElapsedNs / 1_000_000) + "ms"
        + " → " + (jElapsedNs / iters / 1000) + "μs/iter, " + (1_000_000_000L * iters / jElapsedNs) + " ops/s");

    // java without timeout
    Grok jraw = compiler.compile(ELAND_PATTERN, new JavaRegexEngine(0L, 1024));
    long jrt0 = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      jraw.capture(line);
    }
    long jrElapsedNs = System.nanoTime() - jrt0;
    System.out.println("=== java(no timeout): 100 iters in " + (jrElapsedNs / 1_000_000) + "ms"
        + " → " + (jrElapsedNs / 100 / 1000) + "μs/iter, " + (100_000_000_000L / jrElapsedNs) + " ops/s");
  }
}
