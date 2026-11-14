package org.bitwisemadness.tensuradelight.tensura;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * Tensura integration facade.
 *
 * <p>This class deliberately contains <b>no</b> references to any Tensura
 * type. Every method checks {@link #isAvailable()} first and only then
 * delegates to {@link TensuraAccess}. Because the JVM resolves class
 * references lazily, {@code TensuraAccess} is never loaded — and so never
 * tries to resolve missing Tensura classes — when Tensura is absent. That is
 * what lets this mod boot and no-op cleanly without Tensura, even though the
 * toml declares it required.</p>
 */
public final class TensuraBridge {

    private TensuraBridge() {
    }

    /** Whether Tensura: Reincarnated is installed. */
    public static boolean isAvailable() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded("tensura");
    }

    /**
     * Adds {@code ep} to the entity's maximum EP and {@code magicule} to its
     * current pool.
     *
     * <p>Both amounts must be finite and non-negative, and at least one must
     * be positive: Tensura's maximum-EP writer adds blindly, so a negative
     * amount would silently shrink the eater instead. Anything Tensura refuses
     * logs a warning and answers {@code false} rather than failing the eat.</p>
     *
     * @return whether the grant reached Tensura
     */
    public static boolean grant(LivingEntity entity, double ep, double magicule) {
        if (entity == null || !isAvailable()) {
            return false;
        }
        if (!acceptable(ep) || !acceptable(magicule) || (ep <= 0.0 && magicule <= 0.0)) {
            return false;
        }
        try {
            TensuraAccess.grant(entity, ep, magicule);
            return true;
        } catch (Throwable failure) {
            TensuraDelight.LOGGER.warn("Could not grant {} EP and {} magicule", ep, magicule, failure);
            return false;
        }
    }

    private static boolean acceptable(double amount) {
        return Double.isFinite(amount) && amount >= 0.0;
    }
}
