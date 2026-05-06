package io.whatap.grok.api.engine;

/**
 * Tries to compile with re2j first; falls back to {@link JavaRegexEngine} when re2j
 * cannot handle the pattern (lookahead/lookbehind/backreference/atomic group).
 *
 * The compiled pattern carries its actual engine name via {@link CompiledPattern#getEngineName()}
 * so callers can route metrics by realised engine.
 */
public final class HybridEngine implements RegexEngine {

  private final Re2jEngine re2j;
  private final JavaRegexEngine java;

  public HybridEngine(long timeoutMs, int checkInterval) {
    this.re2j = new Re2jEngine();
    this.java = new JavaRegexEngine(timeoutMs, checkInterval);
  }

  @Override
  public CompiledPattern compile(String regex) {
    try {
      return re2j.compile(regex);
    } catch (RegexCompileException re2jErr) {
      try {
        CompiledPattern javaPattern = java.compile(regex);
        return new HybridFallbackPattern(javaPattern, re2jErr.getMessage());
      } catch (RegexCompileException javaErr) {
        throw new RegexCompileException(
            "Both re2j and java.util.regex failed. re2j=" + re2jErr.getMessage()
                + " java=" + javaErr.getMessage(), javaErr);
      }
    }
  }

  @Override
  public String getName() {
    return NAME_HYBRID;
  }

  static final class HybridFallbackPattern implements CompiledPattern {

    private final CompiledPattern delegate;
    private final String re2jFailureReason;

    HybridFallbackPattern(CompiledPattern delegate, String re2jFailureReason) {
      this.delegate = delegate;
      this.re2jFailureReason = re2jFailureReason;
    }

    @Override
    public EngineMatcher matcher(CharSequence input) {
      return delegate.matcher(input);
    }

    @Override
    public String getEngineName() {
      return "java_fallback";
    }

    @Override
    public String getPattern() {
      return delegate.getPattern();
    }

    public String getRe2jFailureReason() {
      return re2jFailureReason;
    }

    public CompiledPattern getDelegate() {
      return delegate;
    }
  }
}
