package org.bitwisemadness.tensuradelight.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.bitwisemadness.tensuradelight.gear.CuttingGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingTools;
import org.bitwisemadness.tensuradelight.tensura.TensuraBridge;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

/**
 * Grants gear EP to the held knife when a cut lands on a cutting board.
 *
 * <p>Farmer's Delight publishes no cutting event, so this observes the same
 * {@code PlayerInteractEvent.RightClickBlock} its own carve listens to, server
 * side only. The block identity is a string id rather than a Farmer's Delight
 * class, and the grant is gated on the board's public ingredient accessors:
 * the event fires before Farmer's Delight's carve consumes the stored item, so
 * an empty or display-only board costs nothing. Sneak clicks are skipped
 * because Farmer's Delight uses them to place a tool on the board, not to cut.</p>
 */
public final class CuttingBoardHandler {

    private static final String CUTTING_BOARD = "farmersdelight:cutting_board";

    private CuttingBoardHandler() {
    }

    /** One grant per main-hand click that reaches a loaded board while holding a tagged knife. */
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity().isSecondaryUseActive()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(CuttingTools.TAG)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!CUTTING_BOARD.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString())) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof CuttingBoardBlockEntity board)
                || board.isEmpty() || board.isItemCarvingBoard()) {
            return;
        }
        double ep = CuttingGrants.current().epPerCut();
        if (ep > 0.0) {
            TensuraBridge.grantGearEp(level, stack, ep);
        }
    }
}
