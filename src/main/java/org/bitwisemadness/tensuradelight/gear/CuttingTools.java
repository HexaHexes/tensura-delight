package org.bitwisemadness.tensuradelight.gear;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The mod's cutting-tool tag: runtime membership for the cutting board grant.
 *
 * <p>Only plain strings live here as constants, so the headless datagen round
 * trip can read the id and the two values without loading a Minecraft class;
 * {@link #TAG} itself is built on first in-game use.</p>
 */
public final class CuttingTools {

    /** The tag's path inside {@code data/<mod id>/tags/item/}. */
    public static final String CUTTING_TOOLS_ID = "cutting_tools";

    /** The common tags the shipped file unions; the pack's four knife mods keep both. */
    public static final String KNIFE_TAG = "#c:tools/knife";
    public static final String CLEAVER_TAG = "#c:tools/cleaver";

    /** Membership test for knives and cleavers, resolved through the shipped tag file. */
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TensuraDelight.MOD_ID, CUTTING_TOOLS_ID));

    private CuttingTools() {
    }
}
