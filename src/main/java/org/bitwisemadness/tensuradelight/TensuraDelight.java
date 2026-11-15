package org.bitwisemadness.tensuradelight;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.bitwisemadness.tensuradelight.datagen.JsonFilesProvider;
import org.bitwisemadness.tensuradelight.datagen.KnifeGearContent;
import org.bitwisemadness.tensuradelight.datagen.MealGrantContent;
import org.bitwisemadness.tensuradelight.event.CuttingBoardHandler;
import org.bitwisemadness.tensuradelight.event.MealEatenHandler;
import org.bitwisemadness.tensuradelight.food.MealGrantsReloadListener;
import org.bitwisemadness.tensuradelight.gear.CuttingGrantsReloadListener;
import org.bitwisemadness.tensuradelight.tensura.TensuraBridge;
import org.slf4j.Logger;

/**
 * Tensura Delight entry point.
 *
 * <p>Everything observable is wired here: datagen on the mod bus, the reload
 * listeners and the eat and cut handlers on the game bus. All three game-bus
 * handlers are plain NeoForge events - no mixin is involved anywhere in this
 * mod.</p>
 */
@Mod(TensuraDelight.MOD_ID)
public final class TensuraDelight {

    public static final String MOD_ID = "tensura_delight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TensuraDelight(IEventBus modBus) {
        modBus.addListener(TensuraDelight::gatherData);

        NeoForge.EVENT_BUS.addListener(TensuraDelight::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(MealEatenHandler::onFinish);
        NeoForge.EVENT_BUS.addListener(CuttingBoardHandler::onRightClickBlock);

        LOGGER.info("Tensura Delight initialized (tensura {}, farmersdelight {}, "
                + "brewinandchewin {}, dungeonsdelight {})",
                TensuraBridge.isAvailable(), loaded("farmersdelight"),
                loaded("brewinandchewin"), loaded("dungeonsdelight"));
    }

    /** Optional feature mods may be absent; the rest of the code gates on this. */
    private static boolean loaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** Loads the meal grant and cutting grant tables on world load and on {@code /reload}. */
    private static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new MealGrantsReloadListener());
        event.addListener(new CuttingGrantsReloadListener());
    }

    /** Feeds the shipped tables ({@link MealGrantContent}, {@link KnifeGearContent}) into datagen. */
    private static void gatherData(GatherDataEvent event) {
        if (!event.includeServer()) {
            return;
        }
        event.addProvider(new JsonFilesProvider("meal_grants",
                event.getGenerator().getPackOutput(), MealGrantContent.build()));
        event.addProvider(new JsonFilesProvider("knife_gear",
                event.getGenerator().getPackOutput(), KnifeGearContent.build()));
    }
}
