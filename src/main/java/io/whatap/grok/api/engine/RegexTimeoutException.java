package io.whatap.grok.api.engine;

public class RegexTimeoutException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final long elapsedMs;
  private final long timeoutMs;

  public RegexTimeoutException(long elapsedMs, long timeoutMs) {
    super("Regex matching exceeded " + timeoutMs + "ms (elapsed=" + elapsedMs + "ms)");
    this.elapsedMs = elapsedMs;
    this.timeoutMs = timeoutMs;
  }

  public RegexTimeoutException(String message) {
    super(message);
    this.elapsedMs = -1;
    this.timeoutMs = -1;
  }

  public long getElapsedMs() {
    return elapsedMs;
  }

  public long getTimeoutMs() {
    return timeoutMs;
  }
}
