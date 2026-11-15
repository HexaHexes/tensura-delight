package org.bitwisemadness.tensuradelight.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.manasmods.tensura.data.existence.gear.GearExistenceData;
import io.github.manasmods.tensura.registry.data.TensuraCustomData;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.yirmiri.dungeonsdelight.common.block.monster_pot.MonsterPotRecipe;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.food.MealGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingTools;
import org.bitwisemadness.tensuradelight.gear.KnifeLadders;
import org.bitwisemadness.tensuradelight.recipe.StationRecipes;
import org.bitwisemadness.tensuradelight.tensura.TensuraBridge;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.utility.AbstractedFluidStack;
import umpaz.brewinandchewin.common.utility.KegRecipeWrapper;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipeInput;

/**
 * Headless proof of the shipped wiring: the mod boots, the reload listeners
 * have installed the generated tables, Tensura is really on the run closure
 * with an entry for every knife, the cutting tag resolves, and the station
 * recipes load through the recipe manager and match their stations' inputs.
 *
 * <p>Keep {@code expectedTests} in {@code build.gradle} in sync with the
 * number of {@code @GameTest} methods here, or the harness fails the run.</p>
 */
@GameTestHolder(TensuraDelight.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TensuraDelightGameTests {

    /** Expected recipe type registry id per shipped recipe path. */
    private static final Map<String, String> STATION_TYPES = Map.of(
            "cooking/cattledeer_stew", "farmersdelight:cooking",
            "cooking/spear_toro_fish_stew", "farmersdelight:cooking",
            "cutting/cattledeer_beef", "farmersdelight:cutting",
            "fermenting/kippers_from_spear_toro", "brewinandchewin:fermenting",
            "monster_cooking/spider_bisque_from_knight_spider", "dungeonsdelight:monster_cooking");

    /** Expected result item id per shipped recipe path. */
    private static final Map<String, String> STATION_RESULTS = Map.of(
            "cooking/cattledeer_stew", "farmersdelight:beef_stew",
            "cooking/spear_toro_fish_stew", "farmersdelight:fish_stew",
            "cutting/cattledeer_beef", "farmersdelight:minced_beef",
            "fermenting/kippers_from_spear_toro", "brewinandchewin:kippers",
            "monster_cooking/spider_bisque_from_knight_spider", "dungeonsdelight:spider_bisque");

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

    /** The table loads through the recipe manager, and every Tensura id is a registered item. */
    @GameTest(template = "empty")
    public static void station_recipes_resolve_in_recipe_manager(GameTestHelper helper) {
        for (String id : StationRecipes.recipeIds()) {
            ResourceLocation location = ResourceLocation.parse(id);
            Optional<RecipeHolder<?>> found = helper.getLevel().getRecipeManager().byKey(location);
            helper.assertTrue(found.isPresent(), "the recipe manager must load " + id);
            Recipe<?> recipe = found.get().value();

            String path = location.getPath();
            String expectedType = STATION_TYPES.get(path);
            helper.assertTrue(expectedType != null, "no expected station type recorded for " + id);
            ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
            helper.assertTrue(type != null && expectedType.equals(type.toString()),
                    id + " must be a " + expectedType + " recipe, got " + type);

            String expectedResult = STATION_RESULTS.get(path);
            helper.assertTrue(expectedResult != null, "no expected result recorded for " + id);
            ResourceLocation resultId =
                    BuiltInRegistries.ITEM.getKey(recipe.getResultItem(helper.getLevel().registryAccess()).getItem());
            helper.assertTrue(expectedResult.equals(String.valueOf(resultId)),
                    id + " must yield " + expectedResult + ", got " + resultId);
        }

        Set<String> tensuraIds = tensuraIngredientIds();
        helper.assertFalse(tensuraIds.isEmpty(), "the table must reference Tensura ingredients");
        for (String tensuraId : tensuraIds) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(tensuraId)),
                    tensuraId + " must be a registered item of the pinned Tensura jar");
        }
        helper.succeed();
    }

    /** Each station's own recipe class accepts the Tensura inputs the recipe lists. */
    @GameTest(template = "empty")
    public static void station_recipes_match_their_station_inputs(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Recipe<?> stew = recipeById(helper, "tensura_delight:cooking/cattledeer_stew");
        helper.assertTrue(stew instanceof CookingPotRecipe,
                "cattledeer stew must decode as a cooking pot recipe");
        helper.assertTrue(((CookingPotRecipe) stew).matches(
                        potInput("tensura:cattledeer_beef", "minecraft:carrot", "minecraft:potato"), level),
                "the cooking pot must accept cattledeer beef with carrot and potato");

        Recipe<?> fishStew = recipeById(helper, "tensura_delight:cooking/spear_toro_fish_stew");
        helper.assertTrue(fishStew instanceof CookingPotRecipe,
                "spear toro stew must decode as a cooking pot recipe");
        helper.assertTrue(((CookingPotRecipe) fishStew).matches(potInput(
                        "tensura:raw_spear_toro_meat", "farmersdelight:tomato_sauce",
                        "farmersdelight:onion"), level),
                "the cooking pot must accept spear toro with tomato sauce and onion");

        Recipe<?> cutting = recipeById(helper, "tensura_delight:cutting/cattledeer_beef");
        helper.assertTrue(cutting instanceof CuttingBoardRecipe,
                "cattledeer beef must decode as a cutting board recipe");
        helper.assertTrue(((CuttingBoardRecipe) cutting).matches(new CuttingBoardRecipeInput(
                        stack("tensura:cattledeer_beef"), stack("farmersdelight:flint_knife")), level),
                "the cutting board must accept cattledeer beef under a knife");

        Recipe<?> monster = recipeById(helper,
                "tensura_delight:monster_cooking/spider_bisque_from_knight_spider");
        helper.assertTrue(monster instanceof MonsterPotRecipe,
                "spider bisque must decode as a monster pot recipe");
        helper.assertTrue(((MonsterPotRecipe) monster).matches(potInput(
                        "tensura:knight_spider_leg", "tensura:knight_spider_leg",
                        "dungeonsdelight:rotgourd_slice", "dungeonsdelight:spider_extract"), level),
                "the monster pot must accept knight spider legs with rotgourd and extract");

        Recipe<?> fermenting = recipeById(helper,
                "tensura_delight:fermenting/kippers_from_spear_toro");
        helper.assertTrue(fermenting instanceof KegFermentingRecipe,
                "kippers must decode as a keg fermenting recipe");
        helper.assertTrue(((KegFermentingRecipe) fermenting).matches(kegInput(
                        "tensura:raw_spear_toro_meat", "tensura:raw_spear_toro_meat",
                        "minecraft:dried_kelp"), level),
                "the keg must accept two spear toro and dried kelp");

        helper.succeed();
    }

    /** The four meal results are real foods on the Phase 1 table; minced beef stays an ingredient. */
    @GameTest(template = "empty")
    public static void station_results_are_normal_food(GameTestHelper helper) {
        Set<String> meals = MealGrants.defaults().meals();
        for (String id : List.of("farmersdelight:beef_stew", "farmersdelight:fish_stew",
                "brewinandchewin:kippers", "dungeonsdelight:spider_bisque")) {
            ItemStack food = stack(id);
            helper.assertFalse(food.isEmpty(), id + " must be a registered item");
            helper.assertTrue(food.getComponents().has(DataComponents.FOOD), id + " must be food");
            helper.assertTrue(meals.contains(id), id + " must be listed on the meal table");
        }

        ItemStack minced = stack("farmersdelight:minced_beef");
        helper.assertFalse(minced.isEmpty(), "minced beef must be a registered item");
        helper.assertTrue(minced.getComponents().has(DataComponents.FOOD),
                "minced beef is edible even though it is an ingredient");
        helper.succeed();
    }

    private static Recipe<?> recipeById(GameTestHelper helper, String id) {
        Optional<RecipeHolder<?>> found =
                helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), "the recipe manager must load " + id);
        return found.get().value();
    }

    /** The cooking pot and the Monster Pot both scan exactly six slots when matching. */
    private static RecipeWrapper potInput(String... items) {
        ItemStackHandler slots = new ItemStackHandler(6);
        for (int slot = 0; slot < items.length; slot++) {
            slots.setStackInSlot(slot, stack(items[slot]));
        }
        return new RecipeWrapper(slots);
    }

    /** Four item slots and no fluid: a recipe without a fluid ingredient demands an empty keg. */
    private static KegRecipeWrapper kegInput(String... items) {
        ItemStackHandler slots = new ItemStackHandler(4);
        for (int slot = 0; slot < items.length; slot++) {
            slots.setStackInSlot(slot, stack(items[slot]));
        }
        return new KegRecipeWrapper() {
            @Override
            public AbstractedFluidStack getFluid() {
                return AbstractedFluidStack.EMPTY;
            }

            @Override
            public long getTankCapacity() {
                return 0L;
            }

            @Override
            public int size() {
                return slots.getSlots();
            }

            @Override
            public ItemStack getItem(int index) {
                return slots.getStackInSlot(index);
            }
        };
    }

    private static ItemStack stack(String itemId) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId)));
    }

    /** Every {@code tensura:} item id the shipped table references, walked from the JSON. */
    private static Set<String> tensuraIngredientIds() {
        Set<String> ids = new HashSet<>();
        for (JsonObject recipe : StationRecipes.build().values()) {
            collectTensuraIds(recipe, ids);
        }
        return ids;
    }

    private static void collectTensuraIds(JsonElement element, Set<String> ids) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
                JsonElement value = field.getValue();
                if ("item".equals(field.getKey()) && value.isJsonPrimitive()
                        && value.getAsString().startsWith("tensura:")) {
                    ids.add(value.getAsString());
                } else if (value.isJsonObject() || value.isJsonArray()) {
                    collectTensuraIds(value, ids);
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                collectTensuraIds(entry, ids);
            }
        }
    }
}
