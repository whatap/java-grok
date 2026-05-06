package io.whatap.grok.api.engine;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

/**
 * Google re2j backed engine. NFA simulation, linear time, no backtracking.
 *
 * Does NOT support: lookahead, lookbehind, backreference, atomic group,
 * possessive quantifiers. Patterns using those features fall back to
 * {@link JavaRegexEngine} when wrapped by {@link HybridEngine}.
 */
public final class Re2jEngine implements RegexEngine {

  /**
   * java.util.regex named group syntax `(?<name>...)` → re2j Python style `(?P<name>...)`.
   * Only matches when followed by an identifier (not `=` / `!` lookbehind, which re2j
   * rejects anyway).
   */
  private static final java.util.regex.Pattern NAMED_GROUP_REWRITE =
      java.util.regex.Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9_]*)>");

  /**
   * Atomic group `(?>...)` 는 re2j에서 의미가 없음 (backtracking 자체 없음). 단순
   * non-capturing group `(?:...)` 으로 변환하면 re2j가 컴파일 가능. 적용 대상:
   * BASE10NUM, QUOTEDSTRING, UNIXPATH, WINPATH, YEAR 등.
   */
  static String rewriteForRe2j(String regex) {
    if (regex == null) {
      return regex;
    }
    String result = regex;
    if (result.indexOf("(?<") >= 0) {
      result = NAMED_GROUP_REWRITE.matcher(result).replaceAll("(?P<$1>");
    }
    if (result.indexOf("(?>") >= 0) {
      // atomic group → non-capturing group. 단순 문자열 치환 (escape된 `(?>` 는 grok 패턴에 없음).
      result = result.replace("(?>", "(?:");
    }
    return result;
  }

  @Override
  public CompiledPattern compile(String regex) {
    String rewritten = rewriteForRe2j(regex);
    try {
      Pattern p = Pattern.compile(rewritten);
      return new Re2jCompiledPattern(p, regex);
    } catch (PatternSyntaxException e) {
      throw new RegexCompileException("re2j compile failed: " + e.getMessage(), e);
    } catch (Throwable e) {
      throw new RegexCompileException("re2j compile failed: " + e.getMessage(), e);
    }
  }

  @Override
  public String getName() {
    return NAME_RE2J;
  }

  static final class Re2jCompiledPattern implements CompiledPattern {

    private final Pattern pattern;
    private final String regex;

    Re2jCompiledPattern(Pattern pattern, String regex) {
      this.pattern = pattern;
      this.regex = regex;
    }

    @Override
    public EngineMatcher matcher(CharSequence input) {
      return new Re2jEngineMatcher(pattern.matcher(input));
    }

    @Override
    public String getEngineName() {
      return NAME_RE2J;
    }

    @Override
    public String getPattern() {
      return regex;
    }

    public Pattern getRawPattern() {
      return pattern;
    }
  }

  static final class Re2jEngineMatcher implements EngineMatcher {

    private Matcher matcher;

    Re2jEngineMatcher(Matcher matcher) {
      this.matcher = matcher;
    }

    @Override
    public void reset(CharSequence input) {
      matcher.reset(input);
    }

    @Override
    public boolean find() {
      return matcher.find();
    }

    @Override
    public int start() {
      return matcher.start();
    }

    @Override
    public int end() {
      return matcher.end();
    }

    @Override
    public int start(int group) {
      return matcher.start(group);
    }

    @Override
    public int end(int group) {
      return matcher.end(group);
    }

    @Override
    public String group() {
      return matcher.group();
    }

    @Override
    public String group(int group) {
      return matcher.group(group);
    }

    @Override
    public String group(String name) {
      return matcher.group(name);
    }

    @Override
    public int groupCount() {
      return matcher.groupCount();
    }
  }
}
