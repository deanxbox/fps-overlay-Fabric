package net.hicham.fps_overlay;

import net.minecraft.client.MinecraftClient;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTracker {
    private static final PerformanceTracker INSTANCE = new PerformanceTracker();

    // Config reference
    private ModConfig config;

    // Performance data - thread-safe atomic references
    private final AtomicInteger currentFps = new AtomicInteger(0);
    private final AtomicLong usedMemory = new AtomicLong(0);
    private final AtomicLong maxMemory = new AtomicLong(0);
    private final AtomicInteger currentPing = new AtomicInteger(0);





    // Average FPS tracking - thread-safe structures
    private final AtomicLong averageFps = new AtomicLong(0);
    private final ConcurrentLinkedQueue<FpsSample> fpsSamples = new ConcurrentLinkedQueue<>();

    // Timing optimization
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final AtomicLong lastAverageUpdateTime = new AtomicLong(0);
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private static class FpsSample {
        final long timestamp;
        final int fps;

        FpsSample(long timestamp, int fps) {
            this.timestamp = timestamp;
            this.fps = fps;
        }
    }

    private PerformanceTracker() {
    }

    public static PerformanceTracker getInstance() {
        return INSTANCE;
    }

    public void setConfig(ModConfig config) {
        this.config = config;
    }

    public void update(MinecraftClient client) {
        if (config == null)
            return;

        long currentTime = System.nanoTime() / NANOS_PER_MILLI;
        long timeSinceLastUpdate = currentTime - lastUpdateTime.get();

        if (timeSinceLastUpdate < config.general.updateIntervalMs)
            return;

        lastUpdateTime.set(currentTime);

        // Update FPS
        currentFps.set(Math.max(0, client.getCurrentFps()));

        // Update Average FPS
        updateAverageFps(currentTime);

        // Update Memory
        try {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            usedMemory.set(total - free);
            maxMemory.set(runtime.maxMemory());
        } catch (Exception e) {
            // Ignore memory update errors
        }

        // Update Ping
        currentPing.set(getCurrentPing(client));


    }

    private void updateAverageFps(long currentTime) {
        if (!config.hud.showAverageFps)
            return;

        try {
            // Add new sample
            fpsSamples.offer(new FpsSample(currentTime, currentFps.get()));

            // Remove old samples outside the window
            long cutoffTime = currentTime - config.hud.averageWindowMs;
            while (!fpsSamples.isEmpty() && fpsSamples.peek().timestamp < cutoffTime) {
                fpsSamples.poll();
            }

            // Calculate average every 10 seconds or on first run
            long timeSinceLastAverageUpdate = currentTime - lastAverageUpdateTime.get();
            if (timeSinceLastAverageUpdate >= 10000 || lastAverageUpdateTime.get() == 0) {
                lastAverageUpdateTime.set(currentTime);

                if (!fpsSamples.isEmpty()) {
                    long sum = 0;
                    int count = 0;

                    for (FpsSample sample : fpsSamples) {
                        if (sample.timestamp >= cutoffTime) {
                            sum += sample.fps;
                            count++;
                        }
                    }

                    if (count > 0) {
                        double newAverage = sum / (double) count;
                        long newAverageLong = (long) (newAverage * 10); // Store with 1 decimal precision
                        averageFps.set(newAverageLong);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore average FPS errors
        }
    }

    private int getCurrentPing(MinecraftClient client) {
        var player = client.player;
        if (client.getNetworkHandler() == null || player == null)
            return 0;

        try {
            var handler = client.getNetworkHandler();
            if (handler == null) return 0;
            var entry = handler.getPlayerListEntry(player.getUuid());
            return entry != null ? Math.max(0, entry.getLatency()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }





    public void clearAverageFpsData() {
        fpsSamples.clear();
        averageFps.set(0);
        lastAverageUpdateTime.set(0);
    }

    // Getters
    public int getCurrentFps() {
        return currentFps.get();
    }

    public long getUsedMemory() {
        return usedMemory.get();
    }

    public long getMaxMemory() {
        return maxMemory.get();
    }

    public int getCurrentPing() {
        return currentPing.get();
    }

    public double getAverageFps() {
        return averageFps.get() / 10.0;
    }


}
