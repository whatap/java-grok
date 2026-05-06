package io.whatap.grok.api.engine;

/**
 * Abstraction over a regex engine implementation.
 *
 * Two production engines are provided:
 *  - JavaRegexEngine  : java.util.regex with InterruptibleCharSequence timeout cap
 *  - Re2jEngine       : com.google.re2j (linear-time, no backtracking)
 *  - HybridEngine     : tries re2j first, falls back to java on compile failure
 */
public interface RegexEngine {

  String NAME_JAVA = "java";
  String NAME_RE2J = "re2j";
  String NAME_HYBRID = "hybrid";

  /**
   * Compile a regex into an engine-specific {@link CompiledPattern}.
   *
   * @throws RegexCompileException if the engine cannot compile this regex
   */
  CompiledPattern compile(String regex);

  String getName();

  /**
   * Resolve an engine by name. Falls back to {@link #NAME_HYBRID} on null/unknown.
   */
  static RegexEngine forName(String name, long timeoutMs, int checkInterval) {
    if (name == null) {
      return new HybridEngine(timeoutMs, checkInterval);
    }
    switch (name.toLowerCase()) {
      case NAME_JAVA:
        return new JavaRegexEngine(timeoutMs, checkInterval);
      case NAME_RE2J:
        return new Re2jEngine();
      case NAME_HYBRID:
      default:
        return new HybridEngine(timeoutMs, checkInterval);
    }
  }
}
