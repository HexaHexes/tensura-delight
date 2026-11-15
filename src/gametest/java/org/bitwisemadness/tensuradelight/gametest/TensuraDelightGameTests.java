package org.bitwisemadness.tensuradelight.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.manasmods.manascore.race.api.RaceAPI;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.yirmiri.dungeonsdelight.common.block.monster_pot.MonsterPotRecipe;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.appetite.Appetite;
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
 * with an entry for every knife, the cutting tag resolves, the station
 * recipes load through the recipe manager and match their stations' inputs,
 * and race appetite refuses and gates delight food through the shipped tags.
 *
 * <p>Keep {@code expectedTests} in {@code build.gradle} in sync with the
 * number of {@code @GameTest} methods here (16 today), or the harness fails
 * the run.</p>
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

    /** The shipped race tags bind: a ghoul refuses delight food, a human and a slime do not. */
    @GameTest(template = "empty")
    public static void appetite_race_classifies_refusal(GameTestHelper helper) {
        ItemStack stew = stack("farmersdelight:beef_stew");
        helper.assertTrue(Appetite.refusesUse(eaterWith(helper, "tensura:ghoul"), stew),
                "a ghoul must refuse delight food");
        helper.assertFalse(Appetite.refusesUse(eaterWith(helper, "tensura:human"), stew),
                "a human must eat delight food");
        helper.assertFalse(Appetite.refusesUse(eaterWith(helper, "tensura:slime"), stew),
                "a slime is a monster eater, not a non-eater");
        helper.succeed();
    }

    /** A non-eater never gets as far as using delight food: the use start cancels. */
    @GameTest(template = "empty")
    public static void non_eater_refuses_delight_food_at_use_start(GameTestHelper helper) {
        Player ghoul = eaterWith(helper, "tensura:ghoul");
        int started = EventHooks.onItemUseStart(ghoul, stack("farmersdelight:beef_stew"),
                InteractionHand.MAIN_HAND, 32);
        helper.assertTrue(started == -1,
                "a ghoul must be refused delight food at use start, got " + started);
        helper.succeed();
    }

    /** An unrefused eater is completely unaffected: food and drink both start with their duration. */
    @GameTest(template = "empty")
    public static void eater_starts_delight_food_normally(GameTestHelper helper) {
        Player human = eaterWith(helper, "tensura:human");
        int stew = EventHooks.onItemUseStart(human, stack("farmersdelight:beef_stew"),
                InteractionHand.MAIN_HAND, 32);
        helper.assertTrue(stew == 32, "a human must start beef stew, got " + stew);
        int beer = EventHooks.onItemUseStart(human, stack("brewinandchewin:beer"),
                InteractionHand.MAIN_HAND, 32);
        helper.assertTrue(beer == 32, "a human must start a drink, got " + beer);
        helper.succeed();
    }

    /** Refusal only ever reaches delight food: another namespace and a non-food stay usable. */
    @GameTest(template = "empty")
    public static void refusal_is_scoped_to_delight_food(GameTestHelper helper) {
        Player ghoul = eaterWith(helper, "tensura:ghoul");
        int beef = EventHooks.onItemUseStart(ghoul, stack("minecraft:beef"),
                InteractionHand.MAIN_HAND, 32);
        helper.assertTrue(beef == 32, "vanilla beef must stay usable, got " + beef);
        int knife = EventHooks.onItemUseStart(ghoul, stack("farmersdelight:flint_knife"),
                InteractionHand.MAIN_HAND, 32);
        helper.assertTrue(knife == 32, "a delight knife is not food, got " + knife);
        helper.succeed();
    }

    /** The two shipped item gates resolve from the generated tag files. */
    @GameTest(template = "empty")
    public static void appetite_item_tags_resolve(GameTestHelper helper) {
        helper.assertTrue(stack("brewinandchewin:beer").is(Appetite.DRINKS),
                "beer must be a drink");
        helper.assertTrue(stack("farmersdelight:apple_cider").is(Appetite.DRINKS),
                "apple cider must be a drink");
        helper.assertTrue(stack("dungeonsdelight:ghoulash").is(Appetite.MONSTER_MEALS),
                "ghoulash must be a monster meal");
        helper.assertTrue(stack("dungeonsdelight:spider_bisque").is(Appetite.MONSTER_MEALS),
                "spider bisque must be a monster meal");
        helper.assertFalse(stack("farmersdelight:beef_stew").is(Appetite.DRINKS)
                        || stack("farmersdelight:beef_stew").is(Appetite.MONSTER_MEALS),
                "beef stew must be in neither gate");
        helper.assertFalse(stack("farmersdelight:flint_knife").is(Appetite.DRINKS)
                        || stack("farmersdelight:flint_knife").is(Appetite.MONSTER_MEALS),
                "a knife must be in neither gate");
        helper.succeed();
    }

    /** Each gate pays its own races, and the monster gate is really bound rather than fail-open. */
    @GameTest(template = "empty")
    public static void grant_gate_by_appetite_class(GameTestHelper helper) {
        Player human = eaterWith(helper, "tensura:human");
        Player ghoul = eaterWith(helper, "tensura:ghoul");
        Player slime = eaterWith(helper, "tensura:slime");
        helper.assertTrue(Appetite.grantAllowed(human, stack("farmersdelight:beef_stew")),
                "a human must be paid for an ordinary meal");
        helper.assertFalse(Appetite.grantAllowed(ghoul, stack("farmersdelight:beef_stew")),
                "a ghoul must not be paid for anything");
        helper.assertTrue(Appetite.grantAllowed(slime, stack("dungeonsdelight:ghoulash")),
                "a slime must be paid for a monster meal");
        helper.assertFalse(Appetite.grantAllowed(human, stack("dungeonsdelight:ghoulash")),
                "a human must not be paid for a monster meal; a broken root-registry lookup "
                        + "would fail open and pass this assertion the wrong way round");
        helper.assertTrue(Appetite.grantAllowed(human, stack("brewinandchewin:beer")),
                "the empty drinker gate must pay every race");
        helper.succeed();
    }

    /** The drinks joined the shipped table as fixed grants, which beats their zero nutrition. */
    @GameTest(template = "empty")
    public static void drink_grants_are_tabled(GameTestHelper helper) {
        MealGrants.Grant beer = MealGrants.current().overrides().get("brewinandchewin:beer");
        helper.assertTrue(new MealGrants.Grant(2.0, 10.0).equals(beer),
                "beer must carry the fixed 2 EP and 10 magicule grant, got " + beer);
        helper.assertTrue(MealGrants.grantFor("brewinandchewin:kombucha", 0).isPresent(),
                "kombucha must pay despite its zero nutrition");
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

    /**
     * A fresh mock player wearing this race, for the appetite decisions.
     *
     * <p>Deliberately {@code makeMockPlayer} and not
     * {@code makeMockServerPlayerInLevel}: the latter runs
     * {@code PlayerList.placeNewPlayer}, which fires {@code playerLogin}, and
     * ManasCore's login listener answers it by sending
     * {@code manascore_config:sync_config_to_client} to a connection that
     * never negotiated any channel - NeoForge's {@code NetworkRegistry} then
     * throws "Payload ... may not be sent to the client!" out of the test. The
     * plain mock player is only constructed, never placed, so no login event
     * fires; every appetite decision here takes a {@code LivingEntity} anyway,
     * which is what makes that substitution honest. The registration is
     * asserted, so an unknown id fails here instead of silently reading as
     * "no race set".
     */
    private static Player eaterWith(GameTestHelper helper, String raceId) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        boolean set = RaceAPI.getRaceFrom(player).setRace(ResourceLocation.parse(raceId), false);
        helper.assertTrue(set, raceId + " must be a registered race of the pinned Tensura jar");
        return player;
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
