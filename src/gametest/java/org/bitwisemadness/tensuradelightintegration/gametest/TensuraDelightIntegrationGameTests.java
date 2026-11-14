package org.bitwisemadness.tensuradelightintegration.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.bitwisemadness.tensuradelightintegration.TensuraDelightIntegration;
import org.bitwisemadness.tensuradelightintegration.food.MealGrants;
import org.bitwisemadness.tensuradelightintegration.tensura.TensuraBridge;

/**
 * Headless proof of the meal grant wiring: the mod boots, the reload listener
 * has installed the shipped table, and Tensura is really on the run closure so
 * the grant path is not a mock.
 *
 * <p>Keep {@code expectedTests} in {@code build.gradle} in sync with the
 * number of {@code @GameTest} methods here, or the harness fails the run.</p>
 */
@GameTestHolder(TensuraDelightIntegration.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TensuraDelightIntegrationGameTests {

    private TensuraDelightIntegrationGameTests() {
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
}
