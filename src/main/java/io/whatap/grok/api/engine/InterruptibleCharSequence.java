package io.whatap.grok.api.engine;

/**
 * CharSequence wrapper that periodically checks elapsed time and throws
 * {@link RegexTimeoutException} once the deadline is exceeded.
 *
 * Uses an opcount mask so System.currentTimeMillis is invoked once every
 * checkInterval character accesses (default 1024). This avoids the per-char
 * syscall overhead while still bounding worst-case backtracking spend.
 *
 * Required because java.util.regex.Matcher.find() is not interruptible.
 */
public final class InterruptibleCharSequence implements CharSequence {

  private static final int DEFAULT_CHECK_INTERVAL = 1024;

  private final CharSequence inner;
  private final long deadlineMs;
  private final int checkMask;
  private final long startMs;
  private final long timeoutMs;
  private long ops;

  public InterruptibleCharSequence(CharSequence inner, long timeoutMs) {
    this(inner, timeoutMs, DEFAULT_CHECK_INTERVAL);
  }

  public InterruptibleCharSequence(CharSequence inner, long timeoutMs, int checkInterval) {
    this.inner = inner;
    this.timeoutMs = timeoutMs;
    this.startMs = System.currentTimeMillis();
    this.deadlineMs = startMs + timeoutMs;
    int interval = nextPowerOfTwo(checkInterval);
    this.checkMask = interval - 1;
  }

  private static int nextPowerOfTwo(int v) {
    if (v <= 1) {
      return 1;
    }
    int n = 1;
    while (n < v) {
      n <<= 1;
    }
    return n;
  }

  @Override
  public char charAt(int index) {
    if ((++ops & checkMask) == 0L) {
      long now = System.currentTimeMillis();
      if (now > deadlineMs) {
        throw new RegexTimeoutException(now - startMs, timeoutMs);
      }
    }
    return inner.charAt(index);
  }

  @Override
  public int length() {
    return inner.length();
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return inner.subSequence(start, end);
  }

  @Override
  public String toString() {
    return inner.toString();
  }

  public CharSequence unwrap() {
    return inner;
  }

  public long getOps() {
    return ops;
  }
}
