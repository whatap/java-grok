package io.whatap.grok.api.engine;

/**
 * Engine-agnostic matcher abstraction. Implementations wrap either
 * java.util.regex.Matcher or com.google.re2j.Matcher.
 */
public interface EngineMatcher {

  void reset(CharSequence input);

  boolean find();

  int start();

  int end();

  int start(int group);

  int end(int group);

  String group();

  String group(int group);

  String group(String name);

  int groupCount();
}
