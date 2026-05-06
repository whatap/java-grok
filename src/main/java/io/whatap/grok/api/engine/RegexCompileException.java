package io.whatap.grok.api.engine;

public class RegexCompileException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RegexCompileException(String message, Throwable cause) {
    super(message, cause);
  }

  public RegexCompileException(String message) {
    super(message);
  }
}
