package org.bitwisemadness.tensuradelight.tensura;

import io.github.manasmods.tensura.handler.GearHandler;
import io.github.manasmods.tensura.registry.item.misc.TensuraDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The only class in this mod that imports Tensura gear types.
 *
 * <p>Never reference this class directly - always go through
 * {@link TensuraBridge}, which checks {@code ModList} first. Loading this class
 * without Tensura installed would throw {@link NoClassDefFoundError}.</p>
 */
final class GearAccess {

    private GearAccess() {
    }

    /**
     * Adds {@code ep} to the stack's gear EP, then runs Tensura's evolution
     * pass over the new total.
     *
     * <p>Initialising first is safe and idempotent: it returns once the stack
     * carries a maximum, so earned EP is never reset, and it no-ops for
     * stackable stacks. The evolution calls act only while the stack's entry
     * declares a positive band, and the milestone pass re-reads the EP that the
     * gain just wrote.</p>
     */
    static void grant(Level level, ItemStack stack, double ep) {
        GearHandler.initiateGearExistence(level, stack);
        DataComponentType<Double> epComponent = TensuraDataComponents.EP.get();
        Double before = stack.get(epComponent);
        stack.set(epComponent, (before == null ? 0.0 : before) + ep);
        GearHandler.initiateGearEvolution(level, stack);
        Double after = stack.get(epComponent);
        GearHandler.applyUniqueGearEvolution(stack, after == null ? 0.0 : after);
    }
}
