package io.whatap.grok.api;

import io.whatap.grok.api.engine.CompiledPattern;
import io.whatap.grok.api.engine.EngineMatcher;

/**
 * ThreadLocal pool for {@link EngineMatcher} instances. Reuses one matcher per
 * thread (per {@link CompiledPattern}) to avoid per-call allocation.
 *
 * @since 1.0.1
 */
public class MatcherPool {

    private final ThreadLocal<EngineMatcher> matcherCache = new ThreadLocal<>();
    private final CompiledPattern pattern;

    public MatcherPool(CompiledPattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Get an {@link EngineMatcher} for the current thread, reusing a cached
     * instance when possible.
     */
    public EngineMatcher getMatcher(CharSequence input) {
        EngineMatcher matcher = matcherCache.get();
        if (matcher == null) {
            matcher = pattern.matcher(input);
            matcherCache.set(matcher);
        } else {
            matcher.reset(input);
        }
        return matcher;
    }

    public void clearCache() {
        matcherCache.remove();
    }

    public CompiledPattern getPattern() {
        return pattern;
    }
}
