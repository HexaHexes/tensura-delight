package org.bitwisemadness.tensuradelightintegration;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/**
 * Tensura - Delight Integration entry point.
 */
@Mod(TensuraDelightIntegration.MOD_ID)
public final class TensuraDelightIntegration {

    public static final String MOD_ID = "tensura_delight_integration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraDelightIntegration(IEventBus modBus) {
        LOGGER.info("Tensura - Delight Integration initialized ({}{}{})",
                loaded("brewinandchewin") ? "brewinandchewin " : "",
                loaded("dungeonsdelight") ? "dungeonsdelight " : "",
                loaded("farmersdelight") ? "farmersdelight" : "");
    }

    /** Optional feature mods may be absent; the rest of the code gates on this. */
    private static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
