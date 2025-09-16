package io.whatap.grok.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThreadLocal pool for Matcher objects to improve performance
 * by reusing Matcher instances per thread.
 * 
 * @since 1.0.1
 */
public class MatcherPool {
    
    private final ThreadLocal<Matcher> matcherCache = new ThreadLocal<>();
    private final Pattern pattern;
    
    public MatcherPool(Pattern pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Get a Matcher instance for the current thread.
     * Reuses existing Matcher if available, otherwise creates new one.
     */
    public Matcher getMatcher(CharSequence input) {
        Matcher matcher = matcherCache.get();
        if (matcher == null) {
            matcher = pattern.matcher(input);
            matcherCache.set(matcher);
        } else {
            matcher.reset(input);
        }
        return matcher;
    }
    
    /**
     * Clear the ThreadLocal cache for the current thread.
     */
    public void clearCache() {
        matcherCache.remove();
    }
    
    /**
     * Get the underlying Pattern.
     */
    public Pattern getPattern() {
        return pattern;
    }
}