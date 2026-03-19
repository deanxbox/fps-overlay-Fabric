package net.hicham.fps_overlay;

import net.minecraft.client.MinecraftClient;

public class PerformanceTracker {
    private static final PerformanceTracker INSTANCE = new PerformanceTracker();

    private ModConfig config;

    // Performance data - simple primitives (cached between updates)
    private int currentFps = 0;
    private long usedMemory = 0;
    private long maxMemory = 0;
    private int currentPing = 0;
    private int onePercentLow = 0;
    private double currentMspt = 0;
    private double currentTps = 20.0;

    // Average FPS and 1% low tracking - Rolling Sum Circular Buffer
    private double averageFps = 0;
    private final long[] frameTimeBuffer = new long[1000];
    private long sumOfDeltasNanos = 0;
    private int frameBufferIndex = 0;
    private int frameBufferSize = 0;

    // Timing
    private long lastUpdateTime = 0;
    private long lastFrameTimeNano = 0;

    private PerformanceTracker() {}

    public static PerformanceTracker getInstance() {
        return INSTANCE;
    }

    public void setConfig(ModConfig config) {
        this.config = config;
    }

    // Periodic update logic called from ClientTickEvents
    public void update(MinecraftClient client) {
        if (config == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < config.general.updateIntervalMs) return;
        lastUpdateTime = currentTime;

        // 1. Update Core Data (FPS, Ping, 1% Low)
        this.currentFps = Math.max(0, client.getCurrentFps());
        this.averageFps = calculateAverageFps();
        this.currentPing = fetchCurrentPing(client);
        this.onePercentLow = calculateOnePercentLow();

        // 2. Update System/Tick Data (RAM, MSPT/TPS)
        MemoryData mem = fetchMemoryData();
        this.usedMemory = mem.used();
        this.maxMemory = mem.max();
        
        TickData tick = fetchTickData(client);
        this.currentMspt = tick.mspt();
        this.currentTps = tick.tps();
    }

    private double calculateAverageFps() {
        if (frameBufferSize == 0 || sumOfDeltasNanos == 0) return 0;
        return (frameBufferSize * 1_000_000_000.0) / sumOfDeltasNanos;
    }

    private MemoryData fetchMemoryData() {
        try {
            Runtime runtime = Runtime.getRuntime();
            return new MemoryData(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory());
        } catch (Exception e) {
            return new MemoryData(0, 0);
        }
    }

    private int fetchCurrentPing(MinecraftClient client) {
        var handler = client.getNetworkHandler();
        var player = client.player;
        if (handler == null || player == null) return 0;
        try {
            var entry = handler.getPlayerListEntry(player.getUuid());
            return entry != null ? Math.max(0, entry.getLatency()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculateOnePercentLow() {
        if (frameBufferSize < 10) return 0;
        
        long[] sortedTimes = new long[frameBufferSize];
        System.arraycopy(frameTimeBuffer, 0, sortedTimes, 0, frameBufferSize);
        java.util.Arrays.sort(sortedTimes);
        
        // 1% low in frame times means the LONGEST frames (last percentile)
        int index = Math.max(0, frameBufferSize - 1 - (frameBufferSize / 100));
        long onePercentFrameNanos = sortedTimes[index];
        
        if (onePercentFrameNanos == 0) return 0;
        return (int) (1_000_000_000.0 / onePercentFrameNanos);
    }

    // Record every frame-time delta to the rolling buffer for Avg/1% Low calculation
    public void recordFrame() {
        long currentNano = System.nanoTime();
        if (lastFrameTimeNano != 0) {
            long delta = currentNano - lastFrameTimeNano;
            
            // Deduct old value from rolling sum before overwriting (Efficient rolling average)
            sumOfDeltasNanos -= frameTimeBuffer[frameBufferIndex];
            
            frameTimeBuffer[frameBufferIndex] = delta;
            sumOfDeltasNanos += delta;
            
            frameBufferIndex = (frameBufferIndex + 1) % frameTimeBuffer.length;
            if (frameBufferSize < frameTimeBuffer.length) frameBufferSize++;
        }
        lastFrameTimeNano = currentNano;
    }

    private TickData fetchTickData(MinecraftClient client) {
        var server = client.getServer();
        if (server != null) {
            double mspt = server.getAverageTickTime();
            double tps = Math.min(20.0, 1000.0 / Math.max(1.0, mspt));
            return new TickData(mspt, tps);
        }
        return new TickData(0, 20.0);
    }

    public void clearAverageFpsData() {
        frameBufferIndex = 0;
        frameBufferSize = 0;
        averageFps = 0;
        sumOfDeltasNanos = 0;
        java.util.Arrays.fill(frameTimeBuffer, 0);
    }

    // Simple Getters
    public int getCurrentFps() { return currentFps; }
    public long getUsedMemory() { return usedMemory; }
    public long getMaxMemory() { return maxMemory; }
    public int getCurrentPing() { return currentPing; }
    public double getAverageFps() { return averageFps; }
    public int getOnePercentLow() { return onePercentLow; }
    public double getMspt() { return currentMspt; }
    public double getTps() { return currentTps; }

    // DTOs for cleaner internal data passing
    private record MemoryData(long used, long max) {}
    private record TickData(double mspt, double tps) {}
}
