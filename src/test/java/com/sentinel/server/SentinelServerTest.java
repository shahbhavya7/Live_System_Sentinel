package com.sentinel.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SentinelServerTest {

    @Test
    void testCpuCritical() {
        assertEquals(40, ThreatAnalyzer.cpuScore(96.0));
    }

    @Test
    void testCpuHigh() {
        assertEquals(20, ThreatAnalyzer.cpuScore(85.0));
    }

    @Test
    void testCpuNormal() {
        assertEquals(0, ThreatAnalyzer.cpuScore(50.0));
    }
}