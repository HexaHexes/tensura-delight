package org.bitwisemadness.tensuradelight.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.datagen.StationAdvancementsContent;
import org.bitwisemadness.tensuradelight.datagen.StationRecipesContent;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the recipe-book unlock table: the shipped set, the
 * recipe each file rewards, the missing-host gating rule, and the invariant
 * that an advancement never outlives its recipe. Gson only, so the suite
 * runs on a bare JVM.
 */
class StationAdvancementsTest {

    private static final String ADVANCEMENT_DIR = "data/tensura_delight/advancement/recipes/";

    // c is NeoForge's common-tag namespace: present whenever NeoForge is,
    // and NeoForge itself is a required dependency of this mod.
    private static final Set<String> ALWAYS_PRESENT = Set.of(
            "minecraft", "c", "tensura", "farmersdelight", "tensura_delight");

    @Test
    void exactlyTenUnlocksUnderOurRecipesDirectory() {
        Map<String, JsonObject> files = StationAdvancementsContent.build();
        assertEquals(10, files.size(), "the table ships one unlock per recipe-book recipe");
        Set<String> ids = new LinkedHashSet<>();
        for (String path : files.keySet()) {
            assertTrue(path.startsWith(ADVANCEMENT_DIR),
                    "path must live under " + ADVANCEMENT_DIR + ": " + path);
            assertTrue(path.endsWith(".json"), "advancement file must be JSON: " + path);
            String recipePath = path.substring(ADVANCEMENT_DIR.length(), path.length() - ".json".length());
            assertFalse(recipePath.startsWith("cutting/"),
                    "the cutting board has no recipe book, so it gets no unlock: " + path);
            ids.add(StationAdvancements.advancementId(path));
        }
        assertEquals(10, ids.size(), "advancement ids must be unique");
    }

    @Test
    void unlocksCoverExactlyTheRecipeBookRecipes() {
        Set<String> shipped = new LinkedHashSet<>(StationRecipes.recipeIds());
        Set<String> rewarded = new LinkedHashSet<>();
        for (Map.Entry<String, JsonObject> entry : StationAdvancementsContent.build().entrySet()) {
            String path = entry.getKey();
            String recipePath = path.substring(ADVANCEMENT_DIR.length(), path.length() - ".json".length());
            String recipe = rewardRecipe(entry.getValue());
            assertEquals(TensuraDelight.MOD_ID + ":" + recipePath, recipe,
                    path + " must reward the recipe its file name mirrors");
            assertTrue(shipped.contains(recipe), recipe + " must be a shipped station recipe");
            assertTrue(rewarded.add(recipe), "two unlocks must not reward " + recipe);
        }

        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            String id = StationRecipes.recipeId(entry.getKey());
            boolean cutting = "farmersdelight:cutting".equals(entry.getValue().get("type").getAsString());
            if (cutting) {
                assertFalse(rewarded.contains(id), id + " must ship no unlock");
            } else {
                assertTrue(rewarded.contains(id),
                        id + " must ship an unlock so its recipe book can list it");
            }
        }
    }

    @Test
    void conditionsMirrorTheirRecipe() {
        Map<String, JsonObject> recipes = StationRecipesContent.build();
        for (Map.Entry<String, JsonObject> entry : StationAdvancementsContent.build().entrySet()) {
            String recipeId = rewardRecipe(entry.getValue());
            JsonObject recipe = null;
            for (Map.Entry<String, JsonObject> candidate : recipes.entrySet()) {
                if (recipeId.equals(StationRecipes.recipeId(candidate.getKey()))) {
                    recipe = candidate.getValue();
                    break;
                }
            }
            assertTrue(recipe != null, "the unlock must reward a shipped recipe: " + recipeId);
            assertEquals(conditionModIds(recipe), conditionModIds(entry.getValue()),
                    entry.getKey() + " must gate exactly where " + recipeId + " gates, "
                            + "or the advancement would outlive its recipe");
        }
    }

    @Test
    void conditionsAreOnlyModLoadedChecks() {
        for (Map.Entry<String, JsonObject> entry : StationAdvancementsContent.build().entrySet()) {
            JsonElement conditions = entry.getValue().get("neoforge:conditions");
            if (conditions == null) {
                continue;
            }
            assertTrue(conditions.isJsonArray(), entry.getKey() + " conditions must be a list");
            for (JsonElement element : conditions.getAsJsonArray()) {
                assertTrue(element.isJsonObject(), entry.getKey() + " condition must be an object");
                JsonObject condition = element.getAsJsonObject();
                assertEquals(Set.of("type", "modid"), condition.keySet(),
                        entry.getKey() + " condition may only carry type and modid");
                assertEquals("neoforge:mod_loaded", condition.get("type").getAsString(),
                        entry.getKey() + " may only gate on mod_loaded");
                assertTrue(condition.get("modid").isJsonPrimitive()
                                && condition.get("modid").getAsJsonPrimitive().isString(),
                        entry.getKey() + " condition needs a modid string");
            }
        }
    }

    @Test
    void everyForeignNamespaceIsGatedOnItsHost() {
        for (Map.Entry<String, JsonObject> entry : StationAdvancementsContent.build().entrySet()) {
            for (String id : ingredientIds(entry.getValue())) {
                int colon = id.indexOf(':');
                assertTrue(colon > 0, "referenced id must be namespaced: " + id);
                String namespace = id.substring(0, colon);
                if (ALWAYS_PRESENT.contains(namespace)) {
                    continue;
                }
                assertTrue(conditionModIds(entry.getValue()).contains(namespace),
                        entry.getKey() + " must be gated on its host mod " + namespace);
            }
        }
    }

    @Test
    void shapeMatchesTheHostsRecipeUnlocks() {
        for (Map.Entry<String, JsonObject> entry : StationAdvancementsContent.build().entrySet()) {
            JsonObject json = entry.getValue();
            if (json.has("neoforge:conditions")) {
                assertEquals("neoforge:conditions", json.keySet().iterator().next(),
                        entry.getKey() + " must lead with its condition, like the hosts");
            }
            assertEquals("minecraft:recipes/root", json.get("parent").getAsString(),
                    entry.getKey() + " must hang under the recipes root");

            JsonObject criteria = json.getAsJsonObject("criteria");
            assertEquals(List.of("has_any_ingredient", "has_the_recipe"),
                    List.copyOf(criteria.keySet()),
                    entry.getKey() + " must carry the hosts' two criteria in order");
            assertEquals("minecraft:inventory_changed",
                    criteria.getAsJsonObject("has_any_ingredient").get("trigger").getAsString(),
                    entry.getKey() + " teaches through the inventory trigger");
            assertEquals("minecraft:recipe_unlocked",
                    criteria.getAsJsonObject("has_the_recipe").get("trigger").getAsString(),
                    entry.getKey() + " also watches the recipe unlock");
            assertEquals(rewardRecipe(json),
                    criteria.getAsJsonObject("has_the_recipe").getAsJsonObject("conditions")
                            .get("recipe").getAsString(),
                    entry.getKey() + " must watch its own recipe");

            JsonArray requirements = json.getAsJsonArray("requirements");
            assertEquals(1, requirements.size(),
                    entry.getKey() + " must be one requirement of either criterion");
            assertEquals(List.of("has_the_recipe", "has_any_ingredient"),
                    stringList(requirements.get(0)),
                    entry.getKey() + " unlocks from the recipe or from holding an ingredient");
        }
    }

    /** The single recipe a file rewards. */
    private static String rewardRecipe(JsonObject advancement) {
        JsonArray recipes = advancement.getAsJsonObject("rewards").getAsJsonArray("recipes");
        assertEquals(1, recipes.size(), "an unlock must reward exactly one recipe");
        return recipes.get(0).getAsString();
    }

    /** The mods an advancement gates on, in order; empty when it ships unconditionally. */
    private static List<String> conditionModIds(JsonObject advancement) {
        List<String> modIds = new ArrayList<>();
        JsonElement conditions = advancement.get("neoforge:conditions");
        if (conditions == null) {
            return modIds;
        }
        for (JsonElement element : conditions.getAsJsonArray()) {
            modIds.add(element.getAsJsonObject().get("modid").getAsString());
        }
        return modIds;
    }

    /** Every concrete item the ingredient criterion watches. */
    private static List<String> ingredientIds(JsonObject advancement) {
        JsonObject criteria = advancement.getAsJsonObject("criteria");
        assertTrue(criteria.has("has_any_ingredient"),
                "every unlock needs an ingredient criterion");
        JsonArray predicates = criteria.getAsJsonObject("has_any_ingredient")
                .getAsJsonObject("conditions").getAsJsonArray("items");
        assertFalse(predicates.isEmpty(), "the ingredient criterion must watch a predicate");
        return stringList(predicates.get(0).getAsJsonObject().get("items"));
    }

    private static List<String> stringList(JsonElement array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array.getAsJsonArray()) {
            values.add(element.getAsString());
        }
        return values;
    }
}
