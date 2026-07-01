package net.hicham.fps_overlay.mixin;

import net.hicham.fps_overlay.PerformanceTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftRenderMixin {

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void onRenderFrame(boolean advanceGameTime, CallbackInfo ci) {
        PerformanceTracker.getInstance().recordFrame();
    }
}
