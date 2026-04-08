package com.sentinel.server;

public class ThreatAnalyzer {

    public static int cpuScore(double cpu) {

        if (cpu > 95.0) return 40;
        else if (cpu > 80.0) return 20;

        return 0;
    }
}