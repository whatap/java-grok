package io.whatap.grok.api.patterns;

import io.whatap.grok.api.Grok;
import io.whatap.grok.api.GrokCompiler;
import io.whatap.grok.api.Match;
import io.whatap.grok.api.ResourceManager;
import io.whatap.grok.api.exception.GrokException;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ElasticsearchPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/elasticsearch").openStream());
    }

    @Test
    public void testEsLog_info() throws GrokException {
        Grok grok = compiler.compile("%{ES_LOG}");

        String log = "[2024-01-15T08:30:00,123][INFO ][o.e.c.m.MetadataCreateIndexService] [node-1] [my-index] creating index";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("INFO", result.get("log.level"));
        assertEquals("node-1", result.get("elasticsearch.node.name"));
        assertTrue(result.get("log_message").toString().contains("creating index"));
    }

    @Test
    public void testEsLog_warn() throws GrokException {
        Grok grok = compiler.compile("%{ES_LOG}");

        String log = "[2024-03-20T12:00:00,456][WARN ][o.e.d.ShardsAllocator     ] [node-2] cluster health is yellow";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("WARN", result.get("log.level"));
        assertEquals("node-2", result.get("elasticsearch.node.name"));
    }

    @Test
    public void testEsSlowLog() throws GrokException {
        Grok grok = compiler.compile("%{ES_SLOWLOG}");

        String log = "[2024-01-15T08:30:00,123][WARN ][i.s.s.query              ] [node-1] [my-index][0] took[5.2s], took_millis[5200], search query slow";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("WARN", result.get("log.level"));
        assertEquals("my-index", result.get("elasticsearch.index.name"));
        assertEquals(0, result.get("elasticsearch.shard.id"));
        assertEquals(5200, result.get("elasticsearch.slowlog.took_millis"));
    }

    @Test
    public void testEsDeprecation() throws GrokException {
        Grok grok = compiler.compile("%{ES_DEPRECATION}");

        String log = "[2024-01-15T08:30:00,123][DEPRECATION][o.e.d.a.b.BulkAction    ] [node-1] use of _type in bulk requests is deprecated";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals("node-1", result.get("elasticsearch.node.name"));
        assertTrue(result.get("log_message").toString().contains("deprecated"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{ES_LOG}");

        String log = "not an elasticsearch log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"ES_LOG", "ES_SLOWLOG", "ES_DEPRECATION", "ES_GC"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
