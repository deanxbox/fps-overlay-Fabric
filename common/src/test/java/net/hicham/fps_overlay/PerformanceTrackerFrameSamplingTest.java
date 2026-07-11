package net.hicham.fps_overlay;

public final class PerformanceTrackerFrameSamplingTest {
    private PerformanceTrackerFrameSamplingTest() {
    }

    public static void main(String[] args) {
        PerformanceTracker tracker = PerformanceTracker.getInstance();
        tracker.resetSessionStats();

        tracker.recordFrame(1_000_000_000L, false);
        tracker.recordFrame(1_010_000_000L, true);
        tracker.recordFrame(1_020_000_000L, true);
        assertClose(100.0, tracker.getLiveAverageFps(), "regular frame samples");

        tracker.recordFrame(2_000_000_000L, false);
        tracker.recordFrame(2_010_000_000L, true);
        assertClose(100.0, tracker.getLiveAverageFps(), "resume after skipped HUD frames");

        tracker.resetSessionStats();
        long frameTime = 1_000_000_000L;
        tracker.recordFrame(frameTime, false);
        for (int i = 0; i < 118; i++) {
            frameTime += 10_000_000L;
            tracker.recordFrame(frameTime, true);
        }
        for (int i = 0; i < 2; i++) {
            frameTime += 50_000_000L;
            tracker.recordFrame(frameTime, true);
        }

        assertClose(10.6666666667, tracker.getLiveAverageFrameTimeMs(), "average frame time");
        assertClose(50.0, tracker.getLiveOnePercentFrameTimeHighMs(), "1% frame time high");
        assertClose(50.0, tracker.getLivePointOnePercentFrameTimeHighMs(), "0.1% frame time high");

        ServerTickMetrics.onJoinServer(null);
        if (ServerTickMetrics.isTier1Active() || ServerTickMetrics.isTier2Active()) {
            throw new AssertionError("server metrics must be inactive until a packet is received");
        }
    }

    private static void assertClose(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.001) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }
}
