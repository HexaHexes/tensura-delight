package org.bitwisemadness.tensuradelight.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The twenty-four station recipes: Tensura ingredients cooked at Farmer's
 * Delight's cooking pot and cutting board, Brewin' And Chewin's keg and
 * Dungeon's Delight's Monster Pot.
 *
 * <p>Each recipe mirrors the host mod's own recipe of the same dish
 * byte-for-byte in structure, swapping in Tensura ingredients and keeping the
 * host's experience, tab, category, temperature and tool. The recipes whose
 * host is optional carry a per-recipe {@code neoforge:mod_loaded} condition,
 * so their JSON is never decoded without that mod. Only gson and plain
 * strings appear here, so the table runs headless under JUnit;
 * {@code StationRecipesContent} ships it through datagen.</p>
 *
 * <p>The cutting board takes fourteen recipes - every raw monster consumable
 * of Tensura's {@code #tensura:raw_monster_consumables} tag - and bridges
 * each into a chain Farmer's Delight already cooks: the land meats fillet to
 * minced beef, the sea meats to salmon slice, the chitin legs crush to bone
 * meal and the slime congeals to a slime ball. Cutting grants gear EP on any
 * knife click, so these add processing rather than pay; the cutting board has
 * no recipe book, so none of them earns an unlock advancement.</p>
 */
public final class StationRecipes {

    private static final String FILE_PREFIX = "data/" + TensuraDelight.MOD_ID + "/recipe/";
    private static final String JSON_SUFFIX = ".json";

    private StationRecipes() {
    }

    /** Every shipped recipe file, keyed by its path inside the jar. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        files.put(path("cooking/baked_cod_stew"), bakedCodStew());
        files.put(path("cooking/cattledeer_stew"), cattledeerStew());
        files.put(path("cooking/spear_toro_fish_stew"), spearToroFishStew());
        files.put(path("cutting/cattledeer_beef"),
                cutting("tensura:cattledeer_beef", "farmersdelight:minced_beef", 2));
        files.put(path("cutting/chilled_slime"), cutting("tensura:chilled_slime", "minecraft:slime_ball", 1));
        files.put(path("cutting/giant_ant_leg"), cutting("tensura:giant_ant_leg", "minecraft:bone_meal", 1));
        files.put(path("cutting/knight_spider_leg"),
                cutting("tensura:knight_spider_leg", "minecraft:bone_meal", 1));
        files.put(path("cutting/raw_armorsaurus_meat"),
                cutting("tensura:raw_armorsaurus_meat", "farmersdelight:minced_beef", 2));
        files.put(path("cutting/raw_blade_tiger_meat"),
                cutting("tensura:raw_blade_tiger_meat", "farmersdelight:minced_beef", 2));
        files.put(path("cutting/raw_charybdis_meat"),
                cutting("tensura:raw_charybdis_meat", "farmersdelight:salmon_slice", 2));
        files.put(path("cutting/raw_giant_bat_meat"),
                cutting("tensura:raw_giant_bat_meat", "farmersdelight:minced_beef", 2));
        files.put(path("cutting/raw_megalodon_meat"),
                cutting("tensura:raw_megalodon_meat", "farmersdelight:salmon_slice", 2));
        files.put(path("cutting/raw_serpent_meat"),
                cutting("tensura:raw_serpent_meat", "farmersdelight:minced_beef", 2));
        files.put(path("cutting/raw_sissie_meat"),
                cutting("tensura:raw_sissie_meat", "farmersdelight:salmon_slice", 2));
        files.put(path("cutting/raw_spear_toro_meat"),
                cutting("tensura:raw_spear_toro_meat", "farmersdelight:salmon_slice", 2));
        files.put(path("cutting/sissie_fin"), cutting("tensura:sissie_fin", "farmersdelight:salmon_slice", 1));
        files.put(path("cutting/spear_toro_fin"),
                cutting("tensura:spear_toro_fin", "farmersdelight:salmon_slice", 1));
        files.put(path("fermenting/jerky_from_blade_tiger"), jerkyFromBladeTiger());
        files.put(path("fermenting/kippers_from_spear_toro"), kippersFromSpearToro());
        files.put(path("monster_cooking/ghoulash_from_giant_bat"), ghoulash());
        files.put(path("monster_cooking/gyudon_from_armorsaurus"), gyudon());
        files.put(path("monster_cooking/salt_soaked_stew_from_serpent"), saltSoakedStew());
        files.put(path("monster_cooking/spider_bisque_from_knight_spider"), spiderBisque());
        files.put(path("monster_cooking/spider_salmagundi_from_knight_spider"), spiderSalmagundi());
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

    /**
     * Farmer's Delight's knife cut, byte-for-byte from its own
     * {@code cutting/beef}: one ingredient, one rolled result, and the
     * item-ability tool shape the board reads.
     */
    private static JsonObject cutting(String ingredient, String result, int count) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cutting");
        json.add("ingredients", entries(item(ingredient)));
        JsonObject rolled = new JsonObject();
        rolled.add("item", stack(result, count));
        JsonArray results = new JsonArray();
        results.add(rolled);
        json.add("result", results);
        JsonObject ability = new JsonObject();
        ability.addProperty("type", "farmersdelight:item_ability");
        ability.addProperty("action", "knife_dig");
        json.add("tool", entries(ability, tag("c:tools/knife")));
        return json;
    }

    /**
     * Field order matches Farmer's Delight's own baked_cod_stew, whose
     * {@code c:foods/raw_cod} slot names no Tensura item - the six sea
     * meats sit in {@code c:foods/raw_fish} instead, so the swap is explicit.
     */
    private static JsonObject bakedCodStew() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("tensura:raw_megalodon_meat"),
                tag("c:crops/potato"), tag("c:eggs"), tag("c:crops/tomato")));
        json.addProperty("recipe_book_tab", "meals");
        json.add("result", stack("farmersdelight:baked_cod_stew", 1));
        return json;
    }

    /**
     * Field order matches Brewin' And Chewin's own jerky, empty keg slot
     * included. Its {@code jerky_meat} tag reaches only vanilla meat, so all
     * three slots swap rather than keep a tag this mod's ingredients never
     * satisfy - which is also what makes the recipe new instead of a
     * restatement of the host's.
     */
    private static JsonObject jerkyFromBladeTiger() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "brewinandchewin");
        json.addProperty("type", "brewinandchewin:fermenting");
        json.addProperty("category", "meals");
        json.addProperty("experience", 1.0);
        JsonArray slots = entries(item("tensura:raw_blade_tiger_meat"),
                item("tensura:raw_blade_tiger_meat"), item("tensura:raw_blade_tiger_meat"));
        slots.add(new JsonArray());
        json.add("ingredients", slots);
        json.add("result", stack("brewinandchewin:jerky", 3));
        json.addProperty("temperature", 4);
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

    /** Field order matches Dungeon's Delight's own spider_salmagundi, spider meat swapped for the leg. */
    private static JsonObject spiderSalmagundi() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "dungeonsdelight");
        json.addProperty("type", "dungeonsdelight:monster_cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("minecraft:fermented_spider_eye"),
                item("tensura:knight_spider_leg"), item("tensura:knight_spider_leg"),
                item("farmersdelight:onion")));
        json.addProperty("recipe_book_tab", "monster_meals");
        json.add("result", stack("dungeonsdelight:spider_salmagundi", 1));
        return json;
    }

    /**
     * Field order matches Dungeon's Delight's own ghoulash. Its
     * {@code fleshes} slot is the tag no Tensura item joins, so the swap is
     * what makes the dish reachable from this mod's ingredients at all.
     */
    private static JsonObject ghoulash() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "dungeonsdelight");
        json.addProperty("type", "dungeonsdelight:monster_cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("dungeonsdelight:slime_noodles"),
                item("tensura:raw_giant_bat_meat"), item("farmersdelight:rotten_tomato")));
        json.addProperty("recipe_book_tab", "monster_meals");
        json.add("result", stack("dungeonsdelight:ghoulash", 1));
        return json;
    }

    /** Field order matches Dungeon's Delight's own gyudon, gritty flesh swapped for Tensura meat. */
    private static JsonObject gyudon() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "dungeonsdelight");
        json.addProperty("type", "dungeonsdelight:monster_cooking");
        json.addProperty("experience", 2.0);
        json.add("ingredients", entries(item("tensura:raw_armorsaurus_meat"),
                tag("dungeonsdelight:sculk_cheese"), item("farmersdelight:cooked_rice"),
                item("farmersdelight:fried_egg"), item("farmersdelight:onion")));
        json.addProperty("recipe_book_tab", "monster_meals");
        json.add("result", stack("dungeonsdelight:gyudon", 1));
        return json;
    }

    /** Field order matches Dungeon's Delight's own salt_soaked_stew, brined flesh swapped for serpent. */
    private static JsonObject saltSoakedStew() {
        JsonObject json = new JsonObject();
        addModLoadedCondition(json, "dungeonsdelight");
        json.addProperty("type", "dungeonsdelight:monster_cooking");
        json.addProperty("experience", 1.0);
        json.add("ingredients", entries(item("tensura:raw_serpent_meat"),
                item("tensura:raw_serpent_meat"), item("minecraft:salmon"),
                item("minecraft:beetroot")));
        json.addProperty("recipe_book_tab", "monster_meals");
        json.add("result", stack("dungeonsdelight:salt_soaked_stew", 1));
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
