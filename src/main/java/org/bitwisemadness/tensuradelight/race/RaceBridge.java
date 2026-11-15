package org.bitwisemadness.tensuradelight.race;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * Race appetite facade: does this race refuse delight food, and may it be
 * paid for this gate.
 *
 * <p>This class deliberately contains <b>no</b> references to any ManasCore
 * type, exactly like {@code TensuraBridge}: every method checks
 * {@link #isAvailable()} first and only then delegates to {@link RaceAccess},
 * because the JVM resolves class references lazily and {@code RaceAccess} is
 * never loaded - and so never tries to resolve missing ManasCore classes -
 * when ManasCore is absent. Both answers fail open on any failure: a race
 * read that cannot happen must not turn into a silent refusal of food, nor
 * into a grant the pack meant to withhold.</p>
 */
public final class RaceBridge {

    private RaceBridge() {
    }

    /** Whether ManasCore is installed; it ships with Tensura and is gated anyway. */
    public static boolean isAvailable() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("manascore");
    }

    /**
     * Whether the entity's race refuses delight food outright.
     *
     * <p>The refusal is membership of the shipped {@code does_not_eat} tag
     * only: nothing else about the entity, the stack or the world changes
     * the answer, which is what lets the use-start handler cancel on both
     * sides of the connection.</p>
     *
     * @return whether the race is known to be a non-eater
     */
    public static boolean refusesFood(LivingEntity entity) {
        if (entity == null || !isAvailable()) {
            return false;
        }
        try {
            return RaceAccess.refusesFood(entity);
        } catch (Throwable failure) {
            TensuraDelight.LOGGER.debug("Could not read {} for a food refusal", entity, failure);
            return false;
        }
    }

    /**
     * Whether the entity's race may be paid a grant for this appetite gate.
     *
     * <p>An empty or unbound gate is no restriction at all, so a pack that
     * ships nothing gets the old behaviour for free; a gate with members
     * pays only the races listed in it.</p>
     *
     * @param gate one of the four ids in {@code Appetite}
     * @return whether the race passes the gate
     */
    public static boolean allowsAppetite(LivingEntity entity, ResourceLocation gate) {
        if (entity == null || !isAvailable()) {
            return true;
        }
        try {
            return RaceAccess.allowsAppetite(entity, gate);
        } catch (Throwable failure) {
            TensuraDelight.LOGGER.debug("Could not read {} for the {} gate", entity, gate, failure);
            return true;
        }
    }
}
