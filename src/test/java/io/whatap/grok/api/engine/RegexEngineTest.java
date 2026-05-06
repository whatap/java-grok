package io.whatap.grok.api.engine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RegexEngineTest {

  @Test
  public void java_engine_basic_match() {
    JavaRegexEngine engine = new JavaRegexEngine();
    CompiledPattern p = engine.compile("(?<num>\\d+)");
    EngineMatcher m = p.matcher("hello 42 world");

    assertTrue(m.find());
    assertEquals("42", m.group());
    assertEquals("42", m.group("num"));
    assertEquals(6, m.start());
    assertEquals(8, m.end());
    assertEquals(RegexEngine.NAME_JAVA, p.getEngineName());
  }

  @Test
  public void re2j_engine_basic_match() {
    Re2jEngine engine = new Re2jEngine();
    CompiledPattern p = engine.compile("(?P<num>\\d+)");
    EngineMatcher m = p.matcher("hello 42 world");

    assertTrue(m.find());
    assertEquals("42", m.group());
    assertEquals("42", m.group("num"));
    assertEquals(6, m.start());
    assertEquals(8, m.end());
    assertEquals(RegexEngine.NAME_RE2J, p.getEngineName());
  }

  @Test
  public void java_and_re2j_produce_same_capture_groups() {
    String regex = "(?<word>\\w+)\\s+(?<num>\\d+)";
    String input = "answer 42";

    EngineMatcher j = new JavaRegexEngine().compile(regex).matcher(input);
    assertTrue(j.find());
    assertEquals("answer", j.group("word"));
    assertEquals("42", j.group("num"));

    String re2jRegex = regex.replace("(?<word>", "(?P<word>").replace("(?<num>", "(?P<num>");
    EngineMatcher r = new Re2jEngine().compile(re2jRegex).matcher(input);
    assertTrue(r.find());
    assertEquals("answer", r.group("word"));
    assertEquals("42", r.group("num"));
  }

  @Test
  public void re2j_rejects_lookbehind() {
    Re2jEngine engine = new Re2jEngine();
    try {
      engine.compile("(?<![0-9])\\d+");
      fail("expected RegexCompileException");
    } catch (RegexCompileException expected) {
      assertNotNull(expected.getMessage());
    }
  }

  @Test
  public void hybrid_prefers_re2j_when_compatible() {
    HybridEngine engine = new HybridEngine(500L, 1024);
    CompiledPattern p = engine.compile("(?P<num>\\d+)");

    assertEquals(RegexEngine.NAME_RE2J, p.getEngineName());
    EngineMatcher m = p.matcher("x 7 y");
    assertTrue(m.find());
    assertEquals("7", m.group("num"));
  }

  @Test
  public void hybrid_falls_back_to_java_for_lookbehind() {
    HybridEngine engine = new HybridEngine(500L, 1024);
    CompiledPattern p = engine.compile("(?<![0-9])(?<num>\\d+)");

    assertEquals("java_fallback", p.getEngineName());
    EngineMatcher m = p.matcher("a42 b");
    assertTrue(m.find());
    assertEquals("42", m.group("num"));
  }

  @Test
  public void java_engine_timeout_throws_on_catastrophic_backtracking() {
    JavaRegexEngine engine = new JavaRegexEngine(50L, 64);
    String pathological = "(a+)+b";
    CompiledPattern p = engine.compile(pathological);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 30; i++) {
      sb.append('a');
    }
    sb.append('!');

    EngineMatcher m = p.matcher(sb.toString());
    long t0 = System.currentTimeMillis();
    try {
      m.find();
      fail("expected RegexTimeoutException");
    } catch (RegexTimeoutException expected) {
      long elapsed = System.currentTimeMillis() - t0;
      assertTrue("timeout fired in " + elapsed + "ms (expected <500)", elapsed < 500);
    }
  }

  @Test
  public void re2j_handles_pathological_input_in_linear_time() {
    Re2jEngine engine = new Re2jEngine();
    CompiledPattern p = engine.compile("(a+)+b");

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 30; i++) {
      sb.append('a');
    }
    sb.append('!');

    EngineMatcher m = p.matcher(sb.toString());
    long t0 = System.currentTimeMillis();
    boolean found = m.find();
    long elapsed = System.currentTimeMillis() - t0;

    assertFalse(found);
    assertTrue("re2j took " + elapsed + "ms (expected <100)", elapsed < 100);
  }

  @Test
  public void interruptible_char_sequence_throws_after_deadline() {
    InterruptibleCharSequence seq = new InterruptibleCharSequence("abcdefghij", 10L, 4);

    try {
      Thread.sleep(15);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    try {
      for (int i = 0; i < seq.length(); i++) {
        seq.charAt(i);
      }
      fail("expected RegexTimeoutException");
    } catch (RegexTimeoutException expected) {
      assertTrue(expected.getElapsedMs() >= 10);
    }
  }

  @Test
  public void engine_for_name_resolves_correctly() {
    assertEquals(RegexEngine.NAME_JAVA, RegexEngine.forName("java", 100L, 1024).getName());
    assertEquals(RegexEngine.NAME_RE2J, RegexEngine.forName("re2j", 100L, 1024).getName());
    assertEquals(RegexEngine.NAME_HYBRID, RegexEngine.forName("hybrid", 100L, 1024).getName());
    assertEquals(RegexEngine.NAME_HYBRID, RegexEngine.forName(null, 100L, 1024).getName());
    assertEquals(RegexEngine.NAME_HYBRID, RegexEngine.forName("unknown", 100L, 1024).getName());
  }

  @Test
  public void re2j_rewrites_named_groups() {
    Re2jEngine engine = new Re2jEngine();
    CompiledPattern p = engine.compile("(?<num>\\d+)-(?<word>\\w+)");
    EngineMatcher m = p.matcher("42-hello");
    assertTrue(m.find());
    assertEquals("42", m.group("num"));
    assertEquals("hello", m.group("word"));
  }

  @Test
  public void re2j_rewrites_atomic_groups() {
    Re2jEngine engine = new Re2jEngine();
    CompiledPattern p = engine.compile("(?>\\d+)\\.(?>\\d+)");
    EngineMatcher m = p.matcher("123.456");
    assertTrue(m.find());
    assertEquals("123.456", m.group());
  }

  @Test
  public void re2j_handles_grok_year_pattern() {
    Re2jEngine engine = new Re2jEngine();
    CompiledPattern p = engine.compile("(?>\\d\\d){1,2}");
    EngineMatcher m = p.matcher("year 2026 here");
    assertTrue(m.find());
    assertEquals("2026", m.group());
  }
}
