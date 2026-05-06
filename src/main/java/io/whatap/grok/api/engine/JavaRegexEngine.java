package io.whatap.grok.api.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * java.util.regex backed engine. Wraps the input in {@link InterruptibleCharSequence}
 * to bound worst-case backtracking. Required because java.util.regex.Matcher.find()
 * is not interruptible via Thread.interrupt().
 *
 * timeoutMs &lt;= 0 disables the timeout (legacy behaviour).
 */
public final class JavaRegexEngine implements RegexEngine {

  private final long timeoutMs;
  private final int checkInterval;

  public JavaRegexEngine(long timeoutMs, int checkInterval) {
    this.timeoutMs = timeoutMs;
    this.checkInterval = checkInterval > 0 ? checkInterval : 1024;
  }

  public JavaRegexEngine() {
    this(0L, 1024);
  }

  @Override
  public CompiledPattern compile(String regex) {
    Pattern p = Pattern.compile(regex);
    return new JavaCompiledPattern(p, regex, timeoutMs, checkInterval);
  }

  @Override
  public String getName() {
    return NAME_JAVA;
  }

  public long getTimeoutMs() {
    return timeoutMs;
  }

  public int getCheckInterval() {
    return checkInterval;
  }

  static final class JavaCompiledPattern implements CompiledPattern {

    private final Pattern pattern;
    private final String regex;
    private final long timeoutMs;
    private final int checkInterval;

    JavaCompiledPattern(Pattern pattern, String regex, long timeoutMs, int checkInterval) {
      this.pattern = pattern;
      this.regex = regex;
      this.timeoutMs = timeoutMs;
      this.checkInterval = checkInterval;
    }

    @Override
    public EngineMatcher matcher(CharSequence input) {
      CharSequence wrapped = (timeoutMs > 0)
          ? new InterruptibleCharSequence(input, timeoutMs, checkInterval)
          : input;
      return new JavaEngineMatcher(pattern.matcher(wrapped));
    }

    @Override
    public String getEngineName() {
      return NAME_JAVA;
    }

    @Override
    public String getPattern() {
      return regex;
    }

    public Pattern getRawPattern() {
      return pattern;
    }
  }

  static final class JavaEngineMatcher implements EngineMatcher {

    private Matcher matcher;

    JavaEngineMatcher(Matcher matcher) {
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
