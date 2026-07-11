package net.hicham.fps_overlay.mixin;

import net.hicham.fps_overlay.ServerTickMetrics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleDebugSample", at = @At("HEAD"))
    private void onHandleDebugSample(ClientboundDebugSamplePacket packet, CallbackInfo ci) {
        if (packet.debugSampleType() == net.minecraft.util.debugchart.RemoteDebugSampleType.TICK_TIME) {
            ServerTickMetrics.onDebugSample(packet.sample());
        }
    }

    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void onHandleSetTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerTickMetrics.onSetTime();
    }
}
