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

public class KubernetesPatternTest {

    private GrokCompiler compiler;

    @Before
    public void setUp() throws Exception {
        compiler = GrokCompiler.newInstance();
        compiler.register(Resources.getResource(ResourceManager.PATTERNS).openStream());
        compiler.register(Resources.getResource("patterns/kubernetes").openStream());
    }

    @Test
    public void testContainerLog() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_CONTAINER_LOG}");

        String log = "2024-01-15T08:30:00.123456789Z stdout F {\"level\":\"info\",\"msg\":\"server started\"}";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get("log_timestamp").toString().startsWith("2024-01-15"));
        assertTrue(result.get("log_message").toString().contains("server started"));
    }

    @Test
    public void testContainerLog_stderr() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_CONTAINER_LOG}");

        String log = "2024-03-20T12:00:00.000000000Z stderr F Error: connection refused";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.get("log_message").toString().contains("connection refused"));
    }

    @Test
    public void testContainerLog_partialLine() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_CONTAINER_LOG}");

        String log = "2024-01-15T08:30:00.000Z stdout P partial log output";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    public void testKubeletLog() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_KUBELET}");

        String log = "I0115 08:30:00.123456 12345 kubelet.go:1234] Started kubelet";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(12345, result.get("process.pid"));
        assertTrue(result.get("log_message").toString().contains("Started kubelet"));
    }

    @Test
    public void testKubeletLog_warning() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_KUBELET}");

        String log = "W0320 14:22:10.654321 9876 pod_workers.go:567] Error syncing pod";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertEquals(9876, result.get("process.pid"));
    }

    @Test
    public void testApiServerLog() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_APISERVER}");

        String log = "I0115 10:00:00.000000 1 httplog.go:132] GET /api/v1/namespaces: (1.234ms) 200";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.get("process.pid"));
    }

    @Test
    public void testInvalidLog() throws GrokException {
        Grok grok = compiler.compile("%{KUBE_CONTAINER_LOG}");

        String log = "not a kubernetes log";
        Match match = grok.match(log);
        Map<String, Object> result = match.capture();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testAllPatternsCompile() throws GrokException {
        String[] patterns = {"KUBE_CONTAINER_LOG", "KUBE_KUBELET", "KUBE_APISERVER"};
        for (String pattern : patterns) {
            Grok grok = compiler.compile("%{" + pattern + "}");
            assertNotNull("Pattern " + pattern + " should compile", grok);
        }
    }
}
