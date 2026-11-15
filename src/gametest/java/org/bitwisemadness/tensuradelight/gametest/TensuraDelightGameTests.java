package org.bitwisemadness.tensuradelight.gametest;

import io.github.manasmods.tensura.data.existence.gear.GearExistenceData;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.food.MealGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingTools;
import org.bitwisemadness.tensuradelight.gear.KnifeLadders;
import org.bitwisemadness.tensuradelight.tensura.TensuraBridge;

/**
 * Headless proof of the shipped wiring: the mod boots, the reload listeners
 * have installed the generated tables, Tensura is really on the run closure
 * with an entry for every knife, and the cutting tag resolves.
 *
 * <p>Keep {@code expectedTests} in {@code build.gradle} in sync with the
 * number of {@code @GameTest} methods here, or the harness fails the run.</p>
 */
@GameTestHolder(TensuraDelight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TensuraDelightGameTests {

    private TensuraDelightGameTests() {
    }

    /** The mod is loaded and the empty structure template resolves. */
    @GameTest(template = "empty")
    public static void mod_loads(GameTestHelper helper) {
        helper.succeed();
    }

    /** The shipped defaults are in force, so the generated JSON loaded and parsed. */
    @GameTest(template = "empty")
    public static void meal_grant_table_is_loaded(GameTestHelper helper) {
        MealGrants.Table table = MealGrants.current();
        helper.assertTrue(MealGrants.hasLoaded(), "the meal_grant reload listener has not run");
        helper.assertTrue(table.equals(MealGrants.defaults()),
                "the loaded table must equal the shipped defaults, meals: " + table.meals());
        helper.assertTrue(table.meals().contains("farmersdelight:beef_stew"),
                "Farmer's Delight meals must be listed");
        helper.assertTrue(table.meals().contains("brewinandchewin:cheesy_pasta"),
                "Brewin' And Chewin' meals must be listed");
        helper.assertTrue(table.meals().contains("dungeonsdelight:ghoulash"),
                "Dungeon's Delight meals must be listed");
        helper.succeed();
    }

    /** Tensura is staged, so {@code TensuraBridge} answers with the real API. */
    @GameTest(template = "empty")
    public static void tensura_is_present(GameTestHelper helper) {
        helper.assertTrue(TensuraBridge.isAvailable(),
                "Tensura: Reincarnated must be on the run closure for this test to mean anything");
        helper.succeed();
    }

    /** The shipped cut rate loaded, so the generated ep_per_cut file parsed. */
    @GameTest(template = "empty")
    public static void cutting_grant_table_is_loaded(GameTestHelper helper) {
        CuttingGrants.Table table = CuttingGrants.current();
        helper.assertTrue(CuttingGrants.hasLoaded(), "the ep_per_cut reload listener has not run");
        helper.assertTrue(table.equals(CuttingGrants.defaults()),
                "the loaded table must equal the shipped default, ep per cut: " + table.epPerCut());
        helper.succeed();
    }

    /** Every covered knife has an entry in Tensura's gear existence registry. */
    @GameTest(template = "empty")
    public static void gear_existence_covers_every_knife(GameTestHelper helper) {
        Registry<GearExistenceData> registry =
                helper.getLevel().registryAccess().registryOrThrow(TensuraCustomData.GEAR_EXISTENCE);
        Set<String> covered = new HashSet<>();
        registry.stream().forEach(data -> covered.add(data.gear().toString()));
        for (String itemId : KnifeLadders.byItem().keySet()) {
            helper.assertTrue(covered.contains(itemId), "no gear_existence entry for " + itemId);
        }
        helper.succeed();
    }

    /** The cutting tag resolves through the common tags, and only for real tools. */
    @GameTest(template = "empty")
    public static void cutting_tag_resolves_to_pack_knives(GameTestHelper helper) {
        Item knife = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("farmersdelight", "flint_knife"));
        helper.assertTrue(knife != Items.AIR, "Farmer's Delight must be staged for this test");
        helper.assertTrue(new ItemStack(knife).is(CuttingTools.TAG),
                "flint_knife must join the cutting tag through c:tools/knife");
        helper.assertFalse(new ItemStack(Items.DIRT).is(CuttingTools.TAG),
                "dirt must not count as a cutting tool");
        helper.succeed();
    }
}
