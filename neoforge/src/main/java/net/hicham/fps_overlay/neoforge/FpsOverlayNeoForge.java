package net.hicham.fps_overlay.neoforge;

import net.hicham.fps_overlay.FpsOverlayMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.minecraft.client.Minecraft;

@Mod(FpsOverlayMod.MOD_ID)
public class FpsOverlayNeoForge {

    public FpsOverlayNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            FpsOverlayMod.init();
            NeoForge.EVENT_BUS.addListener(this::onClientTick);
            NeoForge.EVENT_BUS.addListener(this::onRenderGui);

            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (client, parent) -> net.hicham.fps_overlay.ConfigScreenFactory.createConfigScreen(parent));
        }
    }

    private void onClientTick(ClientTickEvent.Post event) {
        FpsOverlayMod.onClientTick(Minecraft.getInstance());
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        if (FpsOverlayMod.shouldRenderOverlay()) {
            net.hicham.fps_overlay.OverlayRenderer.render(event.getGuiGraphics(), Minecraft.getInstance());
        }
    }
}
