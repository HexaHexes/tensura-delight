package org.bitwisemadness.tensuradelight.event;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.food.MealGrants;
import org.bitwisemadness.tensuradelight.tensura.TensuraBridge;

/**
 * Turns a finished eat into a grant from {@link MealGrants}.
 *
 * <p>Subscribed to NeoForge's {@code LivingEntityUseItemEvent.Finish} on the
 * game bus: Tensura has no eating event of its own, and the observation must
 * stay on public API rather than a mixin. Only a server-side player finishing
 * a food stack is considered, and an item the table does not list changes
 * nothing.</p>
 */
public final class MealEatenHandler {

    private MealEatenHandler() {
    }

    /** Grants the table's amount for this food, or does nothing. */
    public static void onFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItem();
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Optional<MealGrants.Grant> grant = MealGrants.grantFor(itemId, food.nutrition());
        if (grant.isEmpty()) {
            return;
        }
        MealGrants.Grant value = grant.get();
        if (value.ep() <= 0.0 && value.magicule() <= 0.0) {
            return;
        }
        if (TensuraBridge.grant(player, value.ep(), value.magicule())) {
            TensuraDelight.LOGGER.debug("Granted {} EP and {} magicule to {} for {}",
                    value.ep(), value.magicule(), player.getScoreboardName(), itemId);
        }
    }
}
