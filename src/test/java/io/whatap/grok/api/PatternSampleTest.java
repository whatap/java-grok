package io.whatap.grok.api;

import io.whatap.grok.api.exception.GrokException;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Verifies that all PatternType sample logs can be parsed by their corresponding patterns.
 */
public class PatternSampleTest {

    @Test
    public void testAllSampleLogsParseable() throws GrokException {
        PatternRepository repo = PatternRepository.getInstance();
        GrokCompiler compiler = GrokCompiler.newInstance();
        compiler.registerAllPatterns();

        int totalSamples = 0;
        int failedSamples = 0;

        for (PatternType type : PatternType.values()) {
            Map<String, String> samples = repo.getSampleLogs(type);
            for (Map.Entry<String, String> entry : samples.entrySet()) {
                totalSamples++;
                String patternName = entry.getKey();
                String sampleLog = entry.getValue();

                try {
                    Grok grok = compiler.compile("%{" + patternName + "}");
                    Match match = grok.match(sampleLog);
                    Map<String, Object> captured = match.capture();

                    if (captured == null || captured.isEmpty()) {
                        failedSamples++;
                        System.err.println("FAIL: " + type.getFileName() + "/" + patternName + " → no capture from: " + sampleLog);
                    }
                } catch (Exception e) {
                    failedSamples++;
                    System.err.println("ERROR: " + type.getFileName() + "/" + patternName + " → " + e.getMessage());
                }
            }
        }

        System.out.println("Total samples: " + totalSamples + ", Failed: " + failedSamples);
        assertEquals("Some sample logs failed to parse", 0, failedSamples);
    }

    @Test
    public void testSampleLogsMinimalNulls() {
        PatternRepository repo = PatternRepository.getInstance();
        GrokCompiler compiler = GrokCompiler.newInstance();
        compiler.registerAllPatterns();

        int totalWarnings = 0;

        for (PatternType type : PatternType.values()) {
            Map<String, String> samples = repo.getSampleLogs(type);
            for (Map.Entry<String, String> entry : samples.entrySet()) {
                String patternName = entry.getKey();
                String sampleLog = entry.getValue();

                try {
                    Grok grok = compiler.compile("%{" + patternName + "}");
                    Match match = grok.match(sampleLog);
                    Map<String, Object> captured = match.capture();
                    if (captured == null || captured.isEmpty()) continue;

                    long totalFields = captured.size();
                    long nullFields = 0;
                    for (Object v : captured.values()) {
                        if (v == null) nullFields++;
                    }
                    double nullRatio = (double) nullFields / totalFields;

                    if (nullRatio >= 0.75) {
                        totalWarnings++;
                        System.err.println("WARN null>=75%: " + type.getFileName() + "/" + patternName
                            + " nulls=" + nullFields + "/" + totalFields
                            + " sample=" + sampleLog.substring(0, Math.min(80, sampleLog.length())));
                    }
                } catch (Exception e) {
                    // compile failure checked in other tests
                }
            }
        }

        System.out.println("Total null>50% warnings: " + totalWarnings);
        assertEquals("Too many samples with >=75% null fields", 0, totalWarnings);
    }

    @Test
    public void testGetSampleLogsReturnsNonEmpty() {
        PatternRepository repo = PatternRepository.getInstance();
        int emptyCount = 0;
        for (PatternType type : PatternType.values()) {
            Map<String, String> samples = repo.getSampleLogs(type);
            if (samples.isEmpty()) {
                emptyCount++;
                System.out.println("WARNING: No samples for " + type.getFileName());
            }
        }
        // At least the majority of types should have samples
        assertTrue("Too many PatternTypes without samples: " + emptyCount,
                   emptyCount < PatternType.values().length / 2);
    }
}
