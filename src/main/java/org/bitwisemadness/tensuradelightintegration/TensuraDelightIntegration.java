package org.bitwisemadness.tensuradelightintegration;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.bitwisemadness.tensuradelightintegration.datagen.JsonFilesProvider;
import org.bitwisemadness.tensuradelightintegration.datagen.MealGrantContent;
import org.bitwisemadness.tensuradelightintegration.event.MealEatenHandler;
import org.bitwisemadness.tensuradelightintegration.food.MealGrantsReloadListener;
import org.bitwisemadness.tensuradelightintegration.tensura.TensuraBridge;
import org.slf4j.Logger;

/**
 * Tensura - Delight Integration entry point.
 *
 * <p>Everything observable is wired here: datagen on the mod bus, the reload
 * listener and the eat handler on the game bus. Both game-bus handlers are
 * plain NeoForge events - no mixin is involved anywhere in this mod.</p>
 */
@Mod(TensuraDelightIntegration.MOD_ID)
public final class TensuraDelightIntegration {

    public static final String MOD_ID = "tensura_delight_integration";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraDelightIntegration(IEventBus modBus) {
        modBus.addListener(TensuraDelightIntegration::gatherData);

        NeoForge.EVENT_BUS.addListener(TensuraDelightIntegration::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(MealEatenHandler::onFinish);

        LOGGER.info("Tensura - Delight Integration initialized (tensura {}, farmersdelight {}, "
                + "brewinandchewin {}, dungeonsdelight {})",
                TensuraBridge.isAvailable(), loaded("farmersdelight"),
                loaded("brewinandchewin"), loaded("dungeonsdelight"));
    }

    /** Optional feature mods may be absent; the rest of the code gates on this. */
    private static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Loads the meal grant table on world load and on {@code /reload}. */
    private static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MealGrantsReloadListener());
    }

    /** Feeds the shipped meal grant table ({@link MealGrantContent}) into datagen. */
    private static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        event.addProvider(new JsonFilesProvider("meal_grants",
                event.getGenerator().getPackOutput(), MealGrantContent.build()));
    }
}
