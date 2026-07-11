package net.hicham.fps_overlay.mixin;

import net.hicham.fps_overlay.FpsOverlayMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "renderFrame", at = @At("TAIL"))
    private void fpsOverlay$recordRenderedFrame(boolean advanceGameTime, CallbackInfo ci) {
        FpsOverlayMod.onFrameRendered((Minecraft) (Object) this);
    }
}
