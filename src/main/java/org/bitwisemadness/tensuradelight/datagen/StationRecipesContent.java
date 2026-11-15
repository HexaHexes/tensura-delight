package org.bitwisemadness.tensuradelight.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import org.bitwisemadness.tensuradelight.recipe.StationRecipes;

/**
 * The shipped station recipes, as datapack JSON built from
 * {@link StationRecipes}.
 *
 * <p>The runtime never reads this class: the generated files under
 * {@code src/generated/resources} are what the recipe manager loads, so a
 * datapack that replaces one changes the game instead of being quietly
 * restored by a Java fallback. {@link #build()} round-trips every file
 * through gson and demands it equals the built table, so the table and the
 * datagen output cannot drift apart.</p>
 */
public final class StationRecipesContent {

    private StationRecipesContent() {
    }

    /** Every station recipe file: built from the table, round-trip validated. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = StationRecipes.build();
        validate(files);
        return files;
    }

    /** Fails the datagen run when a built file would not parse back to itself. */
    private static void validate(Map<String, JsonObject> files) {
        files.forEach((path, json) -> {
            JsonElement parsed = JsonParser.parseString(json.toString());
            if (!json.equals(parsed)) {
                throw new IllegalStateException(
                        "the generated station recipe does not round-trip: " + path);
            }
        });
    }
}
