package net.hicham.fps_overlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTracker {
    private static final PerformanceTracker INSTANCE = new PerformanceTracker();
    private static final Logger LOGGER = LogManager.getLogger("FPS Overlay");
    private static final int GRAPH_SAMPLE_CAPACITY = 600;
    private static final long GRAPH_SAMPLE_INTERVAL_MS = 100L;
    private static final int MAX_FRAME_SAMPLES = 1000;
    private static final int MIN_POINT_ONE_PERCENT_SAMPLES = 120;
    private static final int PING_HISTORY_CAPACITY = 60;

    private ModConfig config;

    private final Object frameTimeLock = new Object();
    private final long[] frameTimeRingBuffer = new long[MAX_FRAME_SAMPLES];
    private final long[] frameTimeScratch = new long[MAX_FRAME_SAMPLES];
    private final AtomicLong sumOfDeltasNanos = new AtomicLong(0);
    private volatile int ringHead = 0;
    private volatile int ringSize = 0;
    private final EnumMap<OverlayMetric, Long> lastMetricUpdateTimes = new EnumMap<>(OverlayMetric.class);

    private int currentFps = 0;
    private volatile double rawFrameTimeMs = 0;
    private double displayedFrameTimeMs = 0;
    private long usedMemory = 0;
    private long maxMemory = 0;
    private int currentPing = 0;
    private int onePercentLow = 0;
    private int pointOnePercentLow = 0;
    private double currentMspt = 0;
    private double currentTps = 20.0;
    private int loadedChunks = 0;
    private int visibleChunks = 0;
    private int completedChunks = 0;
    private String coordinatesText = "0 64 0";
    private String biomeText = "Unknown";
    private String dimensionText = "Overworld";
    private String facingText = "North (0°)";
    private String chunkCoordsText = "0, 0";
    private String lightText = "0 / 0";
    private String realTimeText = "12:00 PM";
    private String dayCountText = "0";
    private String dayTimeText = "6:00 AM";
    private double currentJitterMs = 0;
    private double currentPacketLossPercent = 0;

    private ResourceKey<Biome> cachedBiomeKey = null;
    private String cachedBiomeName = "Unknown";

    private int minFps = Integer.MAX_VALUE;
    private int maxFps = 0;
    private int minPing = Integer.MAX_VALUE;
    private int maxPing = 0;
    private int robustMinFps = 1;
    private int robustMaxFps = 0;
    private int robustMinPing = 0;
    private int robustMaxPing = 0;
    private double averageFps = 0;

    private final int[] fpsGraphBuffer = new int[GRAPH_SAMPLE_CAPACITY];
    private final int[] graphCopyBuffer = new int[GRAPH_SAMPLE_CAPACITY];
    private final int[] fpsRangeScratch = new int[GRAPH_SAMPLE_CAPACITY];
    private final Object graphLock = new Object();
    private int graphIndex = 0;
    private int graphSize = 0;

    private final int[] pingHistoryBuffer = new int[PING_HISTORY_CAPACITY];
    private final int[] pingGraphCopyBuffer = new int[PING_HISTORY_CAPACITY];
    private final int[] pingRangeScratch = new int[PING_HISTORY_CAPACITY];
    private final Object pingHistoryLock = new Object();
    private int pingHistoryIndex = 0;
    private int pingHistorySize = 0;

    private volatile long dataVersion = 0;
    private volatile long frameDataVersion = 0;
    private volatile long lastFrameTimeNano = 0;
    private volatile long lastGraphSampleTime = 0;
    private volatile long lastSignificantChangeTimeMs = System.currentTimeMillis();

    private PerformanceTracker() {
    }

    public static PerformanceTracker getInstance() {
        return INSTANCE;
    }

    public void setConfig(ModConfig config) {
        this.config = config;
    }

    public void update(Minecraft client) {
        if (config == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean changed = false;

        if (shouldRefreshAny(currentTime, OverlayMetric.REAL_TIME, OverlayMetric.DAY_COUNT, OverlayMetric.DAY_TIME)) {
            TimeData timeData = fetchTimeData(client);
            boolean timeChanged = !realTimeText.equals(timeData.realTime())
                    || !dayCountText.equals(timeData.dayCount())
                    || !dayTimeText.equals(timeData.dayTime());
            realTimeText = timeData.realTime();
            dayCountText = timeData.dayCount();
            dayTimeText = timeData.dayTime();
            markRefreshed(currentTime, OverlayMetric.REAL_TIME, OverlayMetric.DAY_COUNT, OverlayMetric.DAY_TIME);
            changed |= timeChanged;
        }

        if (shouldRefreshAny(currentTime, OverlayMetric.COORDS, OverlayMetric.BIOME, OverlayMetric.DIMENSION,
                OverlayMetric.FACING, OverlayMetric.CHUNK_COORDS, OverlayMetric.LIGHT)) {
            ExtendedLocationData location = fetchLocationData(client);
            boolean locationChanged = !coordinatesText.equals(location.coordinates())
                    || !biomeText.equals(location.biome())
                    || !dimensionText.equals(location.dimension())
                    || !facingText.equals(location.facing())
                    || !chunkCoordsText.equals(location.chunkCoords())
                    || !lightText.equals(location.light());
            coordinatesText = location.coordinates();
            biomeText = location.biome();
            dimensionText = location.dimension();
            facingText = location.facing();
            chunkCoordsText = location.chunkCoords();
            lightText = location.light();
            markRefreshed(currentTime, OverlayMetric.COORDS, OverlayMetric.BIOME, OverlayMetric.DIMENSION,
                    OverlayMetric.FACING, OverlayMetric.CHUNK_COORDS, OverlayMetric.LIGHT);
            changed |= locationChanged;
        }

        if (config.hud.showGraph) {
            sampleGraph(currentTime, Math.max(0, client.getFps()));
        }

        if (shouldRefreshMetric(OverlayMetric.FPS, currentTime)) {
            int previousFps = currentFps;
            currentFps = Math.max(0, client.getFps());
            markRefreshed(currentTime, OverlayMetric.FPS);
            updateFpsMinMax();
            changed |= Math.abs(currentFps - previousFps) > 2;
        }
        if (shouldRefreshMetric(OverlayMetric.AVG_FPS, currentTime)) {
            double previousAverageFps = averageFps;
            averageFps = calculateAverageFps();
            markRefreshed(currentTime, OverlayMetric.AVG_FPS);
            changed |= Math.abs(previousAverageFps - averageFps) > 1.0;
        }
        if (shouldRefreshMetric(OverlayMetric.LOW_1, currentTime)) {
            int previousOnePercentLow = onePercentLow;
            onePercentLow = calculateOnePercentLow();
            markRefreshed(currentTime, OverlayMetric.LOW_1);
            changed |= Math.abs(previousOnePercentLow - onePercentLow) > 1;
        }
        if (shouldRefreshMetric(OverlayMetric.LOW_01, currentTime)) {
            int previousPointOnePercentLow = pointOnePercentLow;
            pointOnePercentLow = calculatePointOnePercentLow();
            markRefreshed(currentTime, OverlayMetric.LOW_01);
            changed |= Math.abs(previousPointOnePercentLow - pointOnePercentLow) > 1;
        }
        if (shouldRefreshMetric(OverlayMetric.FRAME_TIME, currentTime)) {
            double previousFrameTime = displayedFrameTimeMs;
            displayedFrameTimeMs = rawFrameTimeMs;
            markRefreshed(currentTime, OverlayMetric.FRAME_TIME);
            changed |= Math.abs(previousFrameTime - displayedFrameTimeMs) > 0.3;
        }

        if (shouldRefreshMetric(OverlayMetric.MEMORY, currentTime)) {
            long previousUsedMemory = usedMemory;
            MemoryData memory = fetchMemoryData();
            usedMemory = memory.used();
            maxMemory = memory.max();
            markRefreshed(currentTime, OverlayMetric.MEMORY);
            changed |= Math.abs(previousUsedMemory - usedMemory) > (8L * 1024L * 1024L);
        }

        boolean refreshNetwork = config.hud.showPingGraph
                || shouldRefreshAny(currentTime, OverlayMetric.PING, OverlayMetric.JITTER, OverlayMetric.PACKET_LOSS);
        if (refreshNetwork) {
            int sampledPing = fetchCurrentPing(client);
            boolean refreshPingStats = shouldRefreshMetric(OverlayMetric.JITTER, currentTime)
                    || shouldRefreshMetric(OverlayMetric.PACKET_LOSS, currentTime);
            if (config.hud.showPingGraph || refreshPingStats) {
                samplePingHistory(sampledPing);
            }
            if (shouldRefreshMetric(OverlayMetric.PING, currentTime)) {
                int previousPing = currentPing;
                currentPing = sampledPing;
                updatePingMinMax();
                markRefreshed(currentTime, OverlayMetric.PING);
                changed |= Math.abs(currentPing - previousPing) > 5;
            }
            if (refreshPingStats) {
                int[] pingSamples = copyPingSamples();
                if (shouldRefreshMetric(OverlayMetric.JITTER, currentTime)) {
                    double previousJitter = currentJitterMs;
                    currentJitterMs = calculateJitter(pingSamples);
                    markRefreshed(currentTime, OverlayMetric.JITTER);
                    changed |= Math.abs(currentJitterMs - previousJitter) > 1.0;
                }
                if (shouldRefreshMetric(OverlayMetric.PACKET_LOSS, currentTime)) {
                    double previousPacketLoss = currentPacketLossPercent;
                    currentPacketLossPercent = calculatePingSpikeRate(pingSamples);
                    markRefreshed(currentTime, OverlayMetric.PACKET_LOSS);
                    changed |= Math.abs(currentPacketLossPercent - previousPacketLoss) > 0.5;
                }
            }
        }

        boolean refreshTick = shouldRefreshAny(currentTime, OverlayMetric.MSPT, OverlayMetric.TPS);
        if (refreshTick) {
            TickData tick = fetchTickData(client);
            if (shouldRefreshMetric(OverlayMetric.MSPT, currentTime)) {
                double previousMspt = currentMspt;
                currentMspt = tick.mspt();
                markRefreshed(currentTime, OverlayMetric.MSPT);
                changed |= Math.abs(currentMspt - previousMspt) > 0.5;
            }
            if (shouldRefreshMetric(OverlayMetric.TPS, currentTime)) {
                double previousTps = currentTps;
                currentTps = tick.tps();
                markRefreshed(currentTime, OverlayMetric.TPS);
                changed |= Math.abs(currentTps - previousTps) > 0.2;
            }
        }

        if (shouldRefreshMetric(OverlayMetric.CHUNKS, currentTime)) {
            int previousLoaded = loadedChunks;
            int previousVisibleChunks = visibleChunks;
            int previousCompletedChunks = completedChunks;
            ChunkData chunks = fetchChunkData(client);
            loadedChunks = chunks.loaded();
            visibleChunks = chunks.visible();
            completedChunks = chunks.completed();
            markRefreshed(currentTime, OverlayMetric.CHUNKS);
            changed |= loadedChunks != previousLoaded
                    || visibleChunks != previousVisibleChunks
                    || completedChunks != previousCompletedChunks;
        }

        if (changed) {
            lastSignificantChangeTimeMs = currentTime;
            dataVersion++;
        }
    }

    public void recordFrame() {
        long currentNano = System.nanoTime();
        if (lastFrameTimeNano != 0) {
            long delta = currentNano - lastFrameTimeNano;
            rawFrameTimeMs = delta / 1_000_000.0;
            synchronized (frameTimeLock) {
                long evictedValue = 0;
                if (ringSize == MAX_FRAME_SAMPLES) {
                    evictedValue = frameTimeRingBuffer[ringHead];
                } else {
                    ringSize++;
                }

                frameTimeRingBuffer[ringHead] = delta;
                ringHead = (ringHead + 1) % MAX_FRAME_SAMPLES;
                sumOfDeltasNanos.addAndGet(delta - evictedValue);
            }
            frameDataVersion++;
        }
        lastFrameTimeNano = currentNano;
    }

    public void clearAverageFpsData() {
        resetSessionStats();
    }

    public void resetSessionStats() {
        averageFps = 0;
        displayedFrameTimeMs = 0;
        onePercentLow = 0;
        pointOnePercentLow = 0;
        sumOfDeltasNanos.set(0);
        synchronized (frameTimeLock) {
            ringHead = 0;
            ringSize = 0;
            Arrays.fill(frameTimeRingBuffer, 0);
        }

        synchronized (graphLock) {
            graphIndex = 0;
            graphSize = 0;
            Arrays.fill(fpsGraphBuffer, 0);
        }

        synchronized (pingHistoryLock) {
            pingHistoryIndex = 0;
            pingHistorySize = 0;
            Arrays.fill(pingHistoryBuffer, 0);
        }

        minFps = Integer.MAX_VALUE;
        maxFps = 0;
        minPing = Integer.MAX_VALUE;
        maxPing = 0;
        robustMinFps = 1;
        robustMaxFps = 0;
        robustMinPing = 0;
        robustMaxPing = 0;
        currentJitterMs = 0;
        currentPacketLossPercent = 0;
        lastMetricUpdateTimes.clear();
        cachedBiomeKey = null;
        cachedBiomeName = "Unknown";
        lastSignificantChangeTimeMs = System.currentTimeMillis();
        dataVersion++;
        frameDataVersion++;
    }

    public int[] copyGraphValues() {
        synchronized (graphLock) {
            int size = graphSize;
            for (int i = 0; i < size; i++) {
                int sourceIndex = (graphIndex - size + i + fpsGraphBuffer.length) % fpsGraphBuffer.length;
                graphCopyBuffer[i] = fpsGraphBuffer[sourceIndex];
            }

            if (size == graphCopyBuffer.length) {
                return graphCopyBuffer;
            }

            int[] values = new int[size];
            System.arraycopy(graphCopyBuffer, 0, values, 0, size);
            return values;
        }
    }

    public int[] copyPingGraphValues() {
        synchronized (pingHistoryLock) {
            int size = pingHistorySize;
            for (int i = 0; i < size; i++) {
                int sourceIndex = (pingHistoryIndex - size + i + pingHistoryBuffer.length) % pingHistoryBuffer.length;
                pingGraphCopyBuffer[i] = pingHistoryBuffer[sourceIndex];
            }

            if (size == pingGraphCopyBuffer.length) {
                return pingGraphCopyBuffer;
            }

            int[] values = new int[size];
            System.arraycopy(pingGraphCopyBuffer, 0, values, 0, size);
            return values;
        }
    }

    public int getCurrentFps() {
        return currentFps;
    }

    public double getCurrentFrameTimeMs() {
        return displayedFrameTimeMs;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    public int getCurrentPing() {
        return currentPing;
    }

    public double getAverageFps() {
        return averageFps;
    }

    public double getLiveAverageFps() {
        return calculateAverageFps();
    }

    public int getOnePercentLow() {
        return onePercentLow;
    }

    public int getLiveOnePercentLow() {
        return calculateOnePercentLow();
    }

    public int getPointOnePercentLow() {
        return pointOnePercentLow;
    }

    public int getLivePointOnePercentLow() {
        return calculatePointOnePercentLow();
    }

    public double getMspt() {
        return currentMspt;
    }

    public double getTps() {
        return currentTps;
    }

    public int getLoadedChunks() {
        return loadedChunks;
    }

    public int getVisibleChunks() {
        return visibleChunks;
    }

    public int getCompletedChunks() {
        return completedChunks;
    }

    public String getCoordinatesText() {
        return coordinatesText;
    }

    public String getBiomeText() {
        return biomeText;
    }

    public String getDimensionText() {
        return dimensionText;
    }

    public String getFacingText() {
        return facingText;
    }

    public String getChunkCoordsText() {
        return chunkCoordsText;
    }

    public String getLightText() {
        return lightText;
    }

    public String getRealTimeText() {
        return realTimeText;
    }

    public String getDayCountText() {
        return dayCountText;
    }

    public String getDayTimeText() {
        return dayTimeText;
    }

    public double getJitterMs() {
        return currentJitterMs;
    }

    public double getPacketLossPercent() {
        return currentPacketLossPercent;
    }

    public long getDataVersion() {
        return dataVersion;
    }

    public long getFrameDataVersion() {
        return frameDataVersion;
    }

    public int getMinFps() {
        return robustMinFps > 0 ? robustMinFps
                : (minFps == Integer.MAX_VALUE ? Math.max(1, currentFps) : Math.max(1, minFps));
    }

    public int getMaxFps() {
        return robustMaxFps > 0 ? robustMaxFps : maxFps;
    }

    public int getMinPing() {
        return robustMinPing > 0 ? robustMinPing
                : (minPing == Integer.MAX_VALUE ? Math.max(0, currentPing) : minPing);
    }

    public int getMaxPing() {
        return robustMaxPing > 0 ? robustMaxPing : maxPing;
    }

    public float getFadeAlpha(ModConfig.AutoHideRules rules) {
        if (rules == null || !rules.fadeOnIdle) {
            return 1.0f;
        }

        long elapsed = System.currentTimeMillis() - lastSignificantChangeTimeMs;
        long delayMs = rules.fadeDelaySeconds * 1000L;
        if (elapsed <= delayMs) {
            return 1.0f;
        }

        float fadeProgress = Math.min(1.0f, (elapsed - delayMs) / 1000.0f);
        return 1.0f + ((rules.fadeOpacity - 1.0f) * fadeProgress);
    }

    private void updateFpsMinMax() {
        minFps = Math.min(minFps, currentFps);
        maxFps = Math.max(maxFps, currentFps);
    }

    private void updatePingMinMax() {
        if (currentPing > 0) {
            minPing = Math.min(minPing, currentPing);
            maxPing = Math.max(maxPing, currentPing);
        }
    }

    private void sampleGraph(long currentTime, int fpsValue) {
        if (currentTime - lastGraphSampleTime < GRAPH_SAMPLE_INTERVAL_MS) {
            return;
        }
        lastGraphSampleTime = currentTime;

        synchronized (graphLock) {
            fpsGraphBuffer[graphIndex] = fpsValue;
            graphIndex = (graphIndex + 1) % fpsGraphBuffer.length;
            if (graphSize < fpsGraphBuffer.length) {
                graphSize++;
            }
            RobustRange range = calculateRobustRange(fpsGraphBuffer, graphSize, graphIndex, false, fpsRangeScratch);
            if (range != null) {
                robustMinFps = Math.max(1, range.min());
                robustMaxFps = Math.max(robustMinFps, range.max());
            }
        }
    }

    private void samplePingHistory(int pingValue) {
        synchronized (pingHistoryLock) {
            pingHistoryBuffer[pingHistoryIndex] = pingValue;
            pingHistoryIndex = (pingHistoryIndex + 1) % pingHistoryBuffer.length;
            if (pingHistorySize < pingHistoryBuffer.length) {
                pingHistorySize++;
            }
            RobustRange range = calculateRobustRange(pingHistoryBuffer, pingHistorySize, pingHistoryIndex, true, pingRangeScratch);
            if (range != null) {
                robustMinPing = Math.max(0, range.min());
                robustMaxPing = Math.max(robustMinPing, range.max());
            }
        }
    }

    private double calculateAverageFps() {
        int size;
        long sum;
        synchronized (frameTimeLock) {
            size = ringSize;
            sum = sumOfDeltasNanos.get();
        }
        if (size == 0 || sum <= 0) {
            return 0;
        }
        return (size * 1_000_000_000.0) / sum;
    }

    private MemoryData fetchMemoryData() {
        try {
            Runtime runtime = Runtime.getRuntime();
            return new MemoryData(runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory());
        } catch (Exception e) {
            return new MemoryData(0, 0);
        }
    }

    private int fetchCurrentPing(Minecraft client) {
        var handler = client.getConnection();
        var player = client.player;
        if (handler == null || player == null) {
            return 0;
        }

        try {
            var entry = handler.getPlayerInfo(player.getUUID());
            return entry != null ? Math.max(0, entry.getLatency()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculateOnePercentLow() {
        return calculateLowPercentileFps(10, 100);
    }

    private int calculatePointOnePercentLow() {
        return calculateLowPercentileFps(MIN_POINT_ONE_PERCENT_SAMPLES, 1000);
    }

    private int calculateLowPercentileFps(int minimumSamples, int divisor) {
        int sampleCount;
        synchronized (frameTimeLock) {
            if (ringSize < minimumSamples) {
                return 0;
            }

            sampleCount = ringSize;
            int start = (ringHead - ringSize + MAX_FRAME_SAMPLES) % MAX_FRAME_SAMPLES;
            for (int i = 0; i < ringSize; i++) {
                frameTimeScratch[i] = frameTimeRingBuffer[(start + i) % MAX_FRAME_SAMPLES];
            }
        }

        int index = Math.max(0, sampleCount - 1 - Math.max(1, sampleCount / divisor));
        long percentileFrameNanos = quickSelect(frameTimeScratch, 0, sampleCount - 1, index);
        if (percentileFrameNanos <= 0) {
            return 0;
        }
        return (int) (1_000_000_000.0 / percentileFrameNanos);
    }

    private double calculateJitter(int[] samples) {
        int validCount = countPositiveSamples(samples);
        if (validCount < 2) {
            return 0;
        }

        double mean = 0;
        for (int sample : samples) {
            if (sample > 0) {
                mean += sample;
            }
        }
        mean /= validCount;

        double variance = 0;
        for (int sample : samples) {
            if (sample > 0) {
                double delta = sample - mean;
                variance += delta * delta;
            }
        }
        variance /= validCount;
        return Math.sqrt(variance);
    }

    private double calculatePingSpikeRate(int[] samples) {
        int validCount = countPositiveSamples(samples);
        if (validCount < 4) {
            return 0;
        }

        int[] sorted = new int[validCount];
        int writeIndex = 0;
        for (int sample : samples) {
            if (sample > 0) {
                sorted[writeIndex++] = sample;
            }
        }

        Arrays.sort(sorted);
        int median = sorted[validCount / 2];
        if (median <= 0) {
            return 0;
        }

        int spikeCount = 0;
        for (int sample : samples) {
            if (sample > (median * 3L)) {
                spikeCount++;
            }
        }
        return Math.min(100.0, (spikeCount * 100.0) / validCount);
    }

    private int countPositiveSamples(int[] samples) {
        int count = 0;
        for (int sample : samples) {
            if (sample > 0) {
                count++;
            }
        }
        return count;
    }

    private int[] copyPingSamples() {
        synchronized (pingHistoryLock) {
            int size = pingHistorySize;
            if (size == 0) {
                return new int[0];
            }

            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                int sourceIndex = (pingHistoryIndex - size + i + pingHistoryBuffer.length) % pingHistoryBuffer.length;
                values[i] = pingHistoryBuffer[sourceIndex];
            }
            return values;
        }
    }

    private RobustRange calculateRobustRange(int[] ringBuffer, int size, int writeIndex, boolean ignoreZero, int[] scratch) {
        if (size <= 0) {
            return null;
        }

        int validCount = 0;
        for (int i = 0; i < size; i++) {
            int sourceIndex = (writeIndex - size + i + ringBuffer.length) % ringBuffer.length;
            int sample = ringBuffer[sourceIndex];
            if (!ignoreZero || sample > 0) {
                scratch[validCount++] = sample;
            }
        }
        if (validCount == 0) {
            return null;
        }

        Arrays.sort(scratch, 0, validCount);
        if (validCount < 8) {
            return new RobustRange(scratch[0], scratch[validCount - 1]);
        }

        int minIndex = Math.min(validCount - 1, Math.max(0, (int) Math.floor(validCount * 0.05)));
        int maxIndex = Math.max(minIndex, Math.min(validCount - 1, (int) Math.ceil(validCount * 0.95) - 1));
        return new RobustRange(scratch[minIndex], scratch[maxIndex]);
    }

    private TickData fetchTickData(Minecraft client) {
        var server = client.getSingleplayerServer();
        if (server != null) {
            double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
            double tps = Math.min(20.0, 1000.0 / Math.max(1.0, mspt));
            return new TickData(mspt, tps);
        }

        return new TickData(-1, -1);
    }

    private ChunkData fetchChunkData(Minecraft client) {
        if (client.level == null || client.levelRenderer == null) {
            return new ChunkData(0, 0, 0);
        }

        int loaded = 0;
        int visible = 0;
        int completed = 0;

        try {
            loaded = Math.max(0, client.level.getChunkSource().getLoadedChunksCount());
        } catch (Exception ignored) {
        }

        try {
            visible = Math.max(0, client.levelRenderer.countRenderedSections());
        } catch (Exception ignored) {
        }

        try {
            completed = Math.max(0, client.levelRenderer.getVisibleSections().size());
        } catch (Exception ignored) {
            completed = visible;
        }

        return new ChunkData(loaded, visible, completed);
    }

    private TimeData fetchTimeData(Minecraft client) {
        ModConfig.ClockFormat clockFormat = config != null && config.hud != null && config.hud.clockFormat != null
                ? config.hud.clockFormat
                : ModConfig.ClockFormat.AM_PM;

        java.time.LocalTime now = java.time.LocalTime.now();
        String realTime = formatClock(now.getHour(), now.getMinute(), clockFormat);
        if (client.level == null) {
            return new TimeData(realTime, "0", formatClock(6, 0, clockFormat));
        }

        long dayTime = client.level.getDayTime();
        long worldTicks = Math.floorMod(dayTime, 24000L);
        long worldDay = Math.floorDiv(dayTime, 24000L);
        int hours = (int) ((worldTicks / 1000L + 6L) % 24L);
        int minutes = (int) (((worldTicks % 1000L) * 60L) / 1000L);
        return new TimeData(realTime, String.valueOf(worldDay), formatClock(hours, minutes, clockFormat));
    }

    private ExtendedLocationData fetchLocationData(Minecraft client) {
        if (client.player == null || client.level == null) {
            return new ExtendedLocationData("0 64 0", "Unknown", "Overworld", "North (0°)", "0, 0", "0 / 0");
        }

        BlockPos pos = client.player.blockPosition();
        String coordinates = pos.getX() + " " + pos.getY() + " " + pos.getZ();
        String biome = "Unknown";
        String dimension = formatDimension(client.level.dimension().identifier().getPath());
        String facing = formatFacing(client.player.getDirection(), client.player.getYRot());
        String chunkCoords = (pos.getX() >> 4) + ", " + (pos.getZ() >> 4);
        String light = "0 / 0";

        try {
            ResourceKey<Biome> biomeKey = client.level.getBiome(pos).unwrapKey().orElse(null);
            biome = biomeKey != null ? resolveBiomeName(biomeKey) : "Unknown";
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve biome for FPS overlay location data", e);
        }

        try {
            int blockLight = client.level.getBrightness(LightLayer.BLOCK, pos);
            int skyLight = client.level.getBrightness(LightLayer.SKY, pos);
            light = blockLight + " / " + skyLight;
        } catch (Exception e) {
            LOGGER.debug("Failed to resolve light levels for FPS overlay location data", e);
        }

        return new ExtendedLocationData(coordinates, biome, dimension, facing, chunkCoords, light);
    }

    private String resolveBiomeName(ResourceKey<Biome> biomeKey) {
        if (biomeKey.equals(cachedBiomeKey)) {
            return cachedBiomeName;
        }

        cachedBiomeKey = biomeKey;
        cachedBiomeName = Component.translatable(biomeKey.identifier().toLanguageKey("biome")).getString();
        return cachedBiomeName;
    }

    private String formatDimension(String path) {
        return switch (path) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "Nether";
            case "the_end" -> "End";
            default -> path.replace('_', ' ');
        };
    }

    private String formatFacing(Direction direction, float yaw) {
        float normalizedYaw = yaw % 360.0f;
        if (normalizedYaw < 0) {
            normalizedYaw += 360.0f;
        }
        return direction.getName() + " (" + Math.round(normalizedYaw) + "\u00B0)";
    }

    private String formatClock(int hours, int minutes, ModConfig.ClockFormat clockFormat) {
        if (clockFormat == ModConfig.ClockFormat.HOUR_24) {
            return String.format(Locale.ROOT, "%02d:%02d", hours, minutes);
        }

        int displayHour = hours % 12;
        if (displayHour == 0) {
            displayHour = 12;
        }
        return String.format(Locale.ROOT, "%d:%02d %s", displayHour, minutes, hours < 12 ? "AM" : "PM");
    }

    private long quickSelect(long[] values, int left, int right, int targetIndex) {
        if (values == null || values.length == 0 || left < 0 || right >= values.length
                || left > right || targetIndex < left || targetIndex > right) {
            return 0;
        }

        while (left < right) {
            int pivotIndex = medianOfThree(values, left, right);
            pivotIndex = partition(values, left, right, pivotIndex);

            if (pivotIndex == targetIndex) {
                return values[pivotIndex];
            }
            if (targetIndex < pivotIndex) {
                right = pivotIndex - 1;
            } else {
                left = pivotIndex + 1;
            }
        }

        return values[left];
    }

    private int partition(long[] values, int left, int right, int pivotIndex) {
        long pivotValue = values[pivotIndex];
        swap(values, pivotIndex, right);
        int storeIndex = left;

        for (int i = left; i < right; i++) {
            if (values[i] < pivotValue) {
                swap(values, storeIndex, i);
                storeIndex++;
            }
        }

        swap(values, storeIndex, right);
        return storeIndex;
    }

    private int medianOfThree(long[] values, int left, int right) {
        int mid = left + ((right - left) / 2);

        if (values[left] > values[mid]) {
            swap(values, left, mid);
        }
        if (values[left] > values[right]) {
            swap(values, left, right);
        }
        if (values[mid] > values[right]) {
            swap(values, mid, right);
        }

        return mid;
    }

    private void swap(long[] values, int left, int right) {
        if (left == right) {
            return;
        }

        long temp = values[left];
        values[left] = values[right];
        values[right] = temp;
    }

    private boolean shouldRefreshMetric(OverlayMetric metric, long currentTime) {
        int interval = config.hud.getMetricUpdateInterval(metric, config.general.updateIntervalMs);
        if (interval <= 0) {
            return true;
        }
        Long lastRefresh = lastMetricUpdateTimes.get(metric);
        return lastRefresh == null || currentTime - lastRefresh >= interval;
    }

    private boolean shouldRefreshAny(long currentTime, OverlayMetric... metrics) {
        for (OverlayMetric metric : metrics) {
            if (shouldRefreshMetric(metric, currentTime)) {
                return true;
            }
        }
        return false;
    }

    private void markRefreshed(long currentTime, OverlayMetric... metrics) {
        for (OverlayMetric metric : metrics) {
            lastMetricUpdateTimes.put(metric, currentTime);
        }
    }

    private record RobustRange(int min, int max) {
    }

    private record MemoryData(long used, long max) {
    }

    private record TickData(double mspt, double tps) {
    }

    private record ChunkData(int loaded, int visible, int completed) {
    }

    private record ExtendedLocationData(String coordinates, String biome, String dimension,
                                        String facing, String chunkCoords, String light) {
    }

    private record TimeData(String realTime, String dayCount, String dayTime) {
    }
}
