package io.whatap.grok.api.engine;

/**
 * Compiled regex pattern abstraction. Created by {@link RegexEngine#compile(String)}.
 */
public interface CompiledPattern {

  EngineMatcher matcher(CharSequence input);

  String getEngineName();

  String getPattern();
}
