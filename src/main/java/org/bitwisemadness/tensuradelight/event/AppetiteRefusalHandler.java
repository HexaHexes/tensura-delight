package org.bitwisemadness.tensuradelight.event;

import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import org.bitwisemadness.tensuradelight.appetite.Appetite;

/**
 * Cancels the use of delight food for a race that refuses it.
 *
 * <p>Subscribed to NeoForge's {@code LivingEntityUseItemEvent.Start} on the
 * game bus. Refusal has to happen here and not at {@code Finish}: the finish
 * event fires after the food has already been eaten - nutrition, effects and
 * all - and is not cancellable, so cancelling {@code Start} is the only point
 * where "refused" still means nothing was consumed. The handler runs on both
 * sides without a client check: the decision is a pure function of synced
 * state, so cancelling everywhere keeps the eating animation honest while the
 * server stays authoritative either way.</p>
 */
public final class AppetiteRefusalHandler {

    private AppetiteRefusalHandler() {
    }

    /** Refuses the item outright, or lets the use start untouched. */
    public static void onStart(LivingEntityUseItemEvent.Start event) {
        if (Appetite.refusesUse(event.getEntity(), event.getItem())) {
            event.setCanceled(true);
        }
    }
}
