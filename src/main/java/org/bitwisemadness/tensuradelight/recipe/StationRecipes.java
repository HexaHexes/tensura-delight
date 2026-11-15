package org.bitwisemadness.tensuradelight.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The five station recipes: Tensura ingredients cooked at Farmer's Delight's
 * cooking pot and cutting board, Brewin' And Chewin's keg and Dungeon's
 * Delight's Monster Pot.
 *
 * <p>Each recipe mirrors the host mod's own recipe of the same dish
 * byte-for-byte in structure, swapping in Tensura ingredients and keeping the
 * host's experience, tab, category, temperature and tool. The two recipes
 * whose host is optional carry a per-recipe {@code neoforge:mod_loaded}
 * condition, so their JSON is never decoded without that mod. Only gson and
 * plain strings appear here, so the table runs headless under JUnit;
 * {@code StationRecipesContent} ships it through datagen.</p>
 */
public final class StationRecipes {

    private static final String FILE_PREFIX = "data/" + TensuraDelight.MOD_ID + "/recipe/";
    private static final String JSON_SUFFIX = ".json";

    private StationRecipes() {
    }

    /** Every shipped recipe file, keyed by its path inside the jar. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        files.put(path("cooking/cattledeer_stew"), cattledeerStew());
        files.put(path("cooking/spear_toro_fish_stew"), spearToroFishStew());
        files.put(path("cutting/cattledeer_beef"), cattledeerBeefCutting());
        files.put(path("fermenting/kippers_from_spear_toro"), kippersFromSpearToro());
        files.put(path("monster_cooking/spider_bisque_from_knight_spider"), spiderBisque());
        return files;
    }

    /** The recipe id a shipped file loads under, e.g. {@code tensura_delight:cooking/cattledeer_stew}. */
    public static String recipeId(String path) {
        if (!path.startsWith(FILE_PREFIX) || !path.endsWith(JSON_SUFFIX)) {
            throw new IllegalArgumentException("not a station recipe path: " + path);
        }
        int end = path.length() - JSON_SUFFIX.length();
        return TensuraDelight.MOD_ID + ":" + path.substring(FILE_PREFIX.length(), end);
    }

    /** Every shipped recipe id, in table order. */
    public static List<String> recipeIds() {
        return build().keySet().stream().map(StationRecipes::recipeId).toList();
    }

    private static String path(String recipePath) {
        return FILE_PREFIX + recipePath + JSON_SUFFIX;
    }

    /** Field order matches Farmer's Delight's own beef_stew, so the diff stays readable. */
    private static JsonObject cattledeerStew() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("tensura:cattledeer_beef"),
                tag("c:crops/carrot"), tag("c:crops/potato")));
        json.addProperty("recipe_book_tab", "meals");
        json.add("result", stack("farmersdelight:beef_stew", 1));
        return json;
    }

    private static JsonObject spearToroFishStew() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("tensura:raw_spear_toro_meat"),
                item("farmersdelight:tomato_sauce"), tag("c:crops/onion")));
        json.addProperty("recipe_book_tab", "meals");
        json.add("result", stack("farmersdelight:fish_stew", 1));
        return json;
    }

    private static JsonObject cattledeerBeefCutting() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cutting");
        json.add("ingredients", entries(item("tensura:cattledeer_beef")));
        JsonObject rolled = new JsonObject();
        rolled.add("item", stack("farmersdelight:minced_beef", 2));
        JsonArray results = new JsonArray();
        results.add(rolled);
        json.add("result", results);
        JsonObject ability = new JsonObject();
        ability.addProperty("type", "farmersdelight:item_ability");
        ability.addProperty("action", "knife_dig");
        json.add("tool", entries(ability, tag("c:tools/knife")));
        return json;
    }

    /** Field order matches Brewin' And Chewin's own kippers, empty keg slot included. */
    private static JsonObject kippersFromSpearToro() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "brewinandchewin");
        json.addProperty("type", "brewinandchewin:fermenting");
        json.addProperty("category", "meals");
        json.addProperty("experience", 1.0);
        JsonArray slots = entries(item("tensura:raw_spear_toro_meat"),
                item("tensura:raw_spear_toro_meat"), item("minecraft:dried_kelp"));
        slots.add(new JsonArray());
        json.add("ingredients", slots);
        json.add("result", stack("brewinandchewin:kippers", 2));
        json.addProperty("temperature", 4);
        return json;
    }

    private static JsonObject spiderBisque() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "dungeonsdelight");
        json.addProperty("type", "dungeonsdelight:monster_cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("tensura:knight_spider_leg"),
                item("tensura:knight_spider_leg"), item("dungeonsdelight:rotgourd_slice"),
                item("dungeonsdelight:spider_extract")));
        json.addProperty("recipe_book_tab", "monster_meals");
        json.add("result", stack("dungeonsdelight:spider_bisque", 1));
        return json;
    }

    private static JsonObject item(String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("item", id);
        return entry;
    }

    private static JsonObject tag(String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("tag", id);
        return entry;
    }

    private static JsonArray entries(JsonObject... entries) {
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            array.add(entry);
        }
        return array;
    }

    private static JsonObject stack(String id, int count) {
        JsonObject stack = new JsonObject();
        stack.addProperty("count", count);
        stack.addProperty("id", id);
        return stack;
    }

    /** Conditions gate the recipe codec at load time, so an absent host never parses the JSON. */
    private static void addModLoadedCondition(JsonObject json, String modId) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:mod_loaded");
        condition.addProperty("modid", modId);
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        json.add("neoforge:conditions", conditions);
    }
}
