package org.bitwisemadness.tensuradelight.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The recipe-book unlock advancements for the station recipes: one file per
 * dish a station's recipe book can display, so holding an ingredient learns
 * it the way the host mods' own recipes are learned.
 *
 * <p>Farmer's Delight, Brewin' And Chewin' and Dungeon's Delight each ship an
 * unlock advancement for every recipe their stations show, and all three
 * stations are {@code RecipeBookMenu}s - without these, their books list the
 * hosts' dishes but never ours. Each entry mirrors the host advancement
 * shape: parent {@code minecraft:recipes/root}, an ingredient criterion OR
 * {@code recipe_unlocked}, and {@code rewards.recipes} naming the recipe. The
 * cutting board is deliberately absent - it has no recipe book, and Farmer's
 * Delight ships no cutting advancements either. An entry names its recipe's
 * host as a {@code neoforge:mod_loaded} condition exactly when the recipe
 * itself is gated (pinned by {@code StationAdvancementsTest}), so an
 * advancement can never outlive its recipe. Only gson and plain strings
 * appear here, so the table runs headless under JUnit;
 * {@code StationAdvancementsContent} ships it through datagen.</p>
 */
public final class StationAdvancements {

    private static final String FILE_PREFIX = "data/" + TensuraDelight.MOD_ID + "/advancement/";
    private static final String RECIPES_DIR = FILE_PREFIX + "recipes/";
    private static final String JSON_SUFFIX = ".json";
    private static final String PARENT = "minecraft:recipes/root";

    /**
     * One unlock: the recipe it rewards (path under the recipe directory),
     * the mod its file is gated on ({@code null} when the recipe ships
     * unconditionally), and the ingredient items that teach it - concrete
     * ids, deduplicated, the way the hosts write them.
     */
    private record Unlock(String recipePath, String requiredMod, List<String> ingredients) {
    }

    /** The four recipe-book dishes, in table order: cooking twice, then the two gated stations. */
    private static final List<Unlock> UNLOCKS = List.of(
            new Unlock("cooking/cattledeer_stew", null,
                    List.of("tensura:cattledeer_beef", "minecraft:carrot", "minecraft:potato")),
            new Unlock("cooking/spear_toro_fish_stew", null,
                    List.of("tensura:raw_spear_toro_meat", "farmersdelight:tomato_sauce",
                            "farmersdelight:onion")),
            new Unlock("fermenting/kippers_from_spear_toro", "brewinandchewin",
                    List.of("tensura:raw_spear_toro_meat", "minecraft:dried_kelp")),
            new Unlock("monster_cooking/spider_bisque_from_knight_spider", "dungeonsdelight",
                    List.of("tensura:knight_spider_leg", "dungeonsdelight:rotgourd_slice",
                            "dungeonsdelight:spider_extract")));

    private StationAdvancements() {
    }

    /** Every shipped unlock file, keyed by its path inside the jar. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> recipes = StationRecipes.build();
        Map<String, JsonObject> files = new LinkedHashMap<>();
        for (Unlock unlock : UNLOCKS) {
            JsonObject recipe = recipeFor(recipes, unlock);
            files.put(path(unlock.recipePath()), advancement(unlock, recipe));
        }
        return files;
    }

    /** The advancement id a shipped file loads under, e.g. {@code tensura_delight:recipes/cooking/cattledeer_stew}. */
    public static String advancementId(String path) {
        if (!path.startsWith(FILE_PREFIX) || !path.endsWith(JSON_SUFFIX)) {
            throw new IllegalArgumentException("not a station advancement path: " + path);
        }
        int end = path.length() - JSON_SUFFIX.length();
        return TensuraDelight.MOD_ID + ":" + path.substring(FILE_PREFIX.length(), end);
    }

    /** Every shipped advancement id, in table order. */
    public static List<String> advancementIds() {
        return build().keySet().stream().map(StationAdvancements::advancementId).toList();
    }

    private static String path(String recipePath) {
        return RECIPES_DIR + recipePath + JSON_SUFFIX;
    }

    /** The shipped recipe this unlock rewards, or a failure naming the orphaned entry. */
    private static JsonObject recipeFor(Map<String, JsonObject> recipes, Unlock unlock) {
        String recipeId = TensuraDelight.MOD_ID + ":" + unlock.recipePath();
        for (Map.Entry<String, JsonObject> entry : recipes.entrySet()) {
            if (recipeId.equals(StationRecipes.recipeId(entry.getKey()))) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException(
                "unlock " + unlock.recipePath() + " rewards no shipped station recipe " + recipeId);
    }

    /** Field order matches the hosts' unlock files, conditions first when the recipe is gated. */
    private static JsonObject advancement(Unlock unlock, JsonObject recipe) {
        String recipeId = TensuraDelight.MOD_ID + ":" + unlock.recipePath();
        JsonObject json = new JsonObject();
        if (unlock.requiredMod() != null) {
            addModLoadedCondition(json, unlock.requiredMod());
        }
        json.addProperty("parent", PARENT);
        json.add("criteria", criteria(recipeId, unlock.ingredients()));
        JsonArray inner = new JsonArray();
        inner.add("has_the_recipe");
        inner.add("has_any_ingredient");
        JsonArray requirements = new JsonArray();
        requirements.add(inner);
        json.add("requirements", requirements);
        JsonArray rewarded = new JsonArray();
        rewarded.add(recipeId);
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", rewarded);
        json.add("rewards", rewards);

        // An advancement may never outlive its recipe: gate exactly where the
        // recipe gates, or the reward would name a recipe the manager dropped.
        JsonElement gated = json.get("neoforge:conditions");
        if (!Objects.equals(gated, recipe.get("neoforge:conditions"))) {
            throw new IllegalStateException("unlock " + unlock.recipePath()
                    + " must gate exactly where its recipe gates: unlock " + gated
                    + ", recipe " + recipe.get("neoforge:conditions"));
        }
        return json;
    }

    /**
     * The two criteria every host unlock carries: holding any ingredient OR
     * the recipe being unlocked, in that order.
     */
    private static JsonObject criteria(String recipeId, List<String> ingredients) {
        JsonArray ids = new JsonArray();
        for (String ingredient : ingredients) {
            ids.add(ingredient);
        }
        JsonObject predicate = new JsonObject();
        predicate.add("items", ids);
        JsonArray predicates = new JsonArray();
        predicates.add(predicate);
        JsonObject anyIngredientConditions = new JsonObject();
        anyIngredientConditions.add("items", predicates);
        JsonObject anyIngredient = new JsonObject();
        anyIngredient.add("conditions", anyIngredientConditions);
        anyIngredient.addProperty("trigger", "minecraft:inventory_changed");

        JsonObject unlockedConditions = new JsonObject();
        unlockedConditions.addProperty("recipe", recipeId);
        JsonObject hasRecipe = new JsonObject();
        hasRecipe.add("conditions", unlockedConditions);
        hasRecipe.addProperty("trigger", "minecraft:recipe_unlocked");

        JsonObject criteria = new JsonObject();
        criteria.add("has_any_ingredient", anyIngredient);
        criteria.add("has_the_recipe", hasRecipe);
        return criteria;
    }

    /** Conditions gate the advancement codec at load time, so an absent host never parses it. */
    private static void addModLoadedCondition(JsonObject json, String modId) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", modId);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        json.add("neoforge:conditions", conditions);
    }
}
