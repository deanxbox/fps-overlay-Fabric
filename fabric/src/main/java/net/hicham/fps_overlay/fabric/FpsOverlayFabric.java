package net.hicham.fps_overlay.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.hicham.fps_overlay.FpsOverlayMod;
import net.hicham.fps_overlay.OverlayRenderer;
import net.minecraft.client.Minecraft;

/**
 * Fabric entrypoint. Registers Fabric-specific events then delegates to common logic.
 */
public class FpsOverlayFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FpsOverlayMod.init();

        ClientTickEvents.END_CLIENT_TICK.register(FpsOverlayMod::onClientTick);

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            if (!FpsOverlayMod.shouldRenderOverlay())
                return;

            Minecraft client = Minecraft.getInstance();
            if (client == null || client.font == null)
                return;

            OverlayRenderer.render(guiGraphics, client);
        });
    }
}
