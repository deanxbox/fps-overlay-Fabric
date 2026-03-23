package net.hicham.fps_overlay.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Fabric-only ModMenu integration for the config screen button.
 */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return net.hicham.fps_overlay.ConfigScreenFactory::createConfigScreen;
    }
}
