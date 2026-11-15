package org.bitwisemadness.tensuradelight.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bitwisemadness.tensuradelight.datagen.StationRecipesContent;
import org.bitwisemadness.tensuradelight.food.MealGrants;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the station recipe table: the shipped set, the shape
 * each host reads, and the missing-host gating rule that keeps optional mods
 * optional. Gson only, so the suite runs on a bare JVM.
 */
class StationRecipesTest {

    private static final String RECIPE_DIR = "data/tensura_delight/recipe/";

    // c is NeoForge's common-tag namespace: present whenever NeoForge is,
    // and NeoForge itself is a required dependency of this mod.
    private static final Set<String> ALWAYS_PRESENT = Set.of(
            "minecraft", "c", "tensura", "farmersdelight", "tensura_delight");

    @Test
    void exactlyFiveRecipesUnderOurRecipeDirectory() {
        Map<String, JsonObject> files = StationRecipesContent.build();
        assertEquals(5, files.size(), "the station table ships five recipes");
        Set<String> ids = new LinkedHashSet<>();
        for (String path : files.keySet()) {
            assertTrue(path.startsWith(RECIPE_DIR), "path must live under " + RECIPE_DIR + ": " + path);
            ids.add(StationRecipes.recipeId(path));
        }
        assertEquals(5, ids.size(), "recipe ids must be unique");
    }

    @Test
    void eachStationCarriesTheShapeItsHostReads() {
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            JsonObject json = entry.getValue();
            switch (stationOf(entry.getKey())) {
                case "cooking" -> {
                    assertEquals("farmersdelight:cooking", typeOf(json));
                    assertKeys(json, "experience", "ingredients", "recipe_book_tab", "result");
                }
                case "cutting" -> {
                    assertEquals("farmersdelight:cutting", typeOf(json));
                    assertKeys(json, "ingredients", "result", "tool");
                }
                case "fermenting" -> {
                    assertEquals("brewinandchewin:fermenting", typeOf(json));
                    assertKeys(json, "category", "experience", "ingredients", "result", "temperature");
                }
                case "monster_cooking" -> {
                    assertEquals("dungeonsdelight:monster_cooking", typeOf(json));
                    assertKeys(json, "experience", "ingredients", "recipe_book_tab", "result");
                }
                default -> fail("unknown station directory in " + entry.getKey());
            }
        }
    }

    @Test
    void everyForeignNamespaceIsGatedOnItsHost() {
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            Set<String> ids = new LinkedHashSet<>();
            collectIds(entry.getValue(), ids);
            for (String id : ids) {
                int colon = id.indexOf(':');
                assertTrue(colon > 0, "referenced id must be namespaced: " + id);
                String namespace = id.substring(0, colon);
                if (ALWAYS_PRESENT.contains(namespace)) {
                    continue;
                }
                assertTrue(hasModLoaded(entry.getValue(), namespace),
                        entry.getKey() + " must be gated on its host mod " + namespace);
            }
        }
    }

    @Test
    void conditionsAreOnlyModLoadedChecks() {
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
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
    void resultsAreSizedStacks() {
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            JsonObject json = entry.getValue();
            JsonElement result = json.get("result");
            if ("farmersdelight:cutting".equals(typeOf(json))) {
                assertTrue(result.isJsonArray(), entry.getKey() + " cutting result must be a list");
                for (JsonElement rolled : result.getAsJsonArray()) {
                    assertTrue(rolled.isJsonObject(), entry.getKey() + " rolled result must be an object");
                    assertSizedStack(rolled.getAsJsonObject().getAsJsonObject("item"), entry.getKey());
                }
            } else {
                assertSizedStack(result.getAsJsonObject(), entry.getKey());
            }
        }
    }

    @Test
    void mealResultsJoinThePhaseOneTable() {
        Set<String> meals = MealGrants.defaults().meals();
        String cuttingResult = null;
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            JsonObject json = entry.getValue();
            if ("farmersdelight:cutting".equals(typeOf(json))) {
                cuttingResult = resultId(json);
                continue;
            }
            String meal = resultId(json);
            assertTrue(meals.contains(meal), meal + " must be listed as a meal");
        }
        assertEquals("farmersdelight:minced_beef", cuttingResult,
                "the cutting recipe must yield Farmer's Delight minced beef");
        assertFalse(meals.contains(cuttingResult),
                "minced beef is a cutting ingredient, not a meal");
    }

    @Test
    void fermentingRecipesEndWithAnEmptyKegSlot() {
        boolean found = false;
        for (Map.Entry<String, JsonObject> entry : StationRecipesContent.build().entrySet()) {
            if (!"brewinandchewin:fermenting".equals(typeOf(entry.getValue()))) {
                continue;
            }
            found = true;
            JsonArray ingredients = entry.getValue().getAsJsonArray("ingredients");
            JsonElement last = ingredients.get(ingredients.size() - 1);
            assertTrue(last.isJsonArray() && last.getAsJsonArray().size() == 0,
                    entry.getKey() + " must end with the empty keg slot");
        }
        assertTrue(found, "the table must ship a keg recipe");
    }

    private static String stationOf(String path) {
        String rest = path.substring(RECIPE_DIR.length());
        return rest.substring(0, rest.indexOf('/'));
    }

    private static String typeOf(JsonObject json) {
        JsonElement type = json.get("type");
        assertTrue(type != null && type.isJsonPrimitive(), "every recipe must declare its type");
        return type.getAsString();
    }

    private static void assertKeys(JsonObject json, String... keys) {
        for (String key : keys) {
            assertTrue(json.has(key), "missing key: " + key);
        }
    }

    private static void assertSizedStack(JsonObject stack, String path) {
        assertTrue(stack != null, path + " must have a result stack");
        JsonElement id = stack.get("id");
        assertTrue(id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString(),
                path + " result needs an id string");
        JsonElement count = stack.get("count");
        assertTrue(count != null && count.isJsonPrimitive() && count.getAsJsonPrimitive().isNumber(),
                path + " result needs a numeric count");
        assertTrue(count.getAsInt() >= 1, path + " result count must be at least 1");
    }

    private static String resultId(JsonObject json) {
        JsonElement result = json.get("result");
        if (result.isJsonObject()) {
            return result.getAsJsonObject().get("id").getAsString();
        }
        JsonObject first = result.getAsJsonArray().get(0).getAsJsonObject();
        return first.getAsJsonObject("item").get("id").getAsString();
    }

    /** Collects every item, tag and result id the recipe references. */
    private static void collectIds(JsonElement element, Set<String> ids) {
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                collectIds(entry, ids);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
            collectField(field.getKey(), field.getValue(), ids);
        }
    }

    private static void collectField(String key, JsonElement value, Set<String> ids) {
        if (("item".equals(key) || "tag".equals(key)) && value.isJsonPrimitive()) {
            ids.add(value.getAsString());
        } else if ("result".equals(key)) {
            collectResultIds(value, ids);
        } else if (value.isJsonObject() || value.isJsonArray()) {
            collectIds(value, ids);
        }
    }

    private static void collectResultIds(JsonElement result, Set<String> ids) {
        if (result.isJsonObject()) {
            JsonElement id = result.getAsJsonObject().get("id");
            if (id != null && id.isJsonPrimitive()) {
                ids.add(id.getAsString());
            }
            return;
        }
        for (JsonElement rolled : result.getAsJsonArray()) {
            if (rolled.isJsonObject() && rolled.getAsJsonObject().has("item")) {
                collectResultIds(rolled.getAsJsonObject().get("item"), ids);
            }
        }
    }

    private static boolean hasModLoaded(JsonObject json, String modId) {
        JsonElement conditions = json.get("neoforge:conditions");
        if (conditions == null || !conditions.isJsonArray()) {
            return false;
        }
        for (JsonElement element : conditions.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject condition = element.getAsJsonObject();
            JsonElement type = condition.get("type");
            JsonElement modid = condition.get("modid");
            if (type != null && modid != null
                    && "neoforge:mod_loaded".equals(type.getAsString())
                    && modId.equals(modid.getAsString())) {
                return true;
            }
        }
        return false;
    }
}
