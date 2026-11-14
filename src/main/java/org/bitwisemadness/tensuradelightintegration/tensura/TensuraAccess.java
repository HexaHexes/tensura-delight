package org.bitwisemadness.tensuradelightintegration.tensura;

import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.world.entity.LivingEntity;

/**
 * The only class in this mod that imports Tensura: Reincarnated types.
 *
 * <p>Never reference this class directly — always go through
 * {@link TensuraBridge}, which checks {@code ModList} first. Loading this class
 * without Tensura installed would throw {@link NoClassDefFoundError}.</p>
 */
final class TensuraAccess {

    private TensuraAccess() {
    }

    /**
     * Adds {@code ep} to the entity's maximum EP and {@code magicule} to its
     * current pool.
     *
     * <p>Both are Tensura's own writers: {@code increaseMaxEP} raises the two
     * maximum attributes the evolution EP requirement measures, and
     * {@code gainMagicule} clamps to the magicule maximum and marks the
     * existence dirty itself, so nothing here persists the change.</p>
     */
    static void grant(LivingEntity entity, double ep, double magicule) {
        EnergyHelper.increaseMaxEP(entity, ep);
        EnergyHelper.gainMagicule(entity, magicule, EnergyHelper.GainType.NORMAL);
    }
}
