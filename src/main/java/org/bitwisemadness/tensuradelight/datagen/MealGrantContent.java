package org.bitwisemadness.tensuradelight.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.food.MealGrants;

/**
 * The shipped meal grant table, as datapack JSON built from
 * {@link MealGrants#defaults()}.
 *
 * <p>The runtime never reads this class: the generated file under
 * {@code src/generated/resources} is what the reload listener loads, so a
 * datapack that omits or replaces it changes the game instead of being
 * quietly restored by a Java fallback. {@link #build()} round-trips the
 * output through {@link MealGrants#parse} and demands it equals the defaults,
 * so editing one side without the other fails the datagen run rather than
 * shipping a table the parser would skip.</p>
 */
public final class MealGrantContent {

    /** The single shipped file, keyed by its path inside the jar. */
    private static final String FILE = "data/" + TensuraDelight.MOD_ID + "/"
            + MealGrants.DIRECTORY + "/meals.json";

    private MealGrantContent() {
    }

    /** Every file of the shipped pack: one table, round-trip validated. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        files.put(FILE, mealsJson(MealGrants.defaults()));
        validate(files);
        return files;
    }

    /** Field order matches what {@link MealGrants#parse} reads, so the diff stays honest. */
    private static JsonObject mealsJson(MealGrants.Table table) {
        JsonObject json = new JsonObject();
        json.addProperty("ep_per_nutrition", table.epPerNutrition());
        json.addProperty("magicule_per_nutrition", table.magiculePerNutrition());
        json.addProperty("max_meal_ep", table.maxMealEp());
        JsonArray meals = new JsonArray();
        table.meals().forEach(meals::add);
        json.add("meals", meals);
        JsonObject overrides = new JsonObject();
        table.overrides().forEach((id, grant) -> {
            JsonObject value = new JsonObject();
            value.addProperty("ep", grant.ep());
            value.addProperty("magicule", grant.magicule());
            overrides.add(id, value);
        });
        json.add("overrides", overrides);
        return json;
    }

    /** Fails the datagen run when the built file would not load as intended. */
    private static void validate(Map<String, JsonObject> files) {
        Map<String, String> text = new LinkedHashMap<>();
        files.forEach((path, json) -> text.put(path, json.toString()));
        MealGrants.ParseResult result = MealGrants.parse(text);
        if (!result.problems().isEmpty()) {
            throw new IllegalStateException(
                    "the parser rejects the generated meal grant table: " + result.problems());
        }
        if (!result.table().equals(MealGrants.defaults())) {
            throw new IllegalStateException(
                    "the generated meal grant table does not round-trip to the built-in defaults");
        }
    }
}
