package org.bitwisemadness.tensuradelight.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.appetite.Appetite;

/**
 * The shipped appetite tags, as datapack JSON built from the ids in
 * {@link Appetite}.
 *
 * <p>The runtime never reads this class: the generated files under
 * {@code src/generated/resources} are what ManasCore's race tags and the item
 * tag system load, so a datapack that replaces one changes the game instead of
 * being quietly restored by a Java fallback. The file paths are derived from
 * the very ids the runtime tests, so datagen and the code cannot name two
 * different tags, and {@link #validate()} fails the datagen run when a
 * default the code relies on would go missing - an empty {@code eats} or
 * {@code drinker} is a deliberate default, a dropped monster reference is
 * not.</p>
 */
public final class AppetiteTagsContent {

    /** Race tags live under the race registry's own directory, {@code manascore:races}. */
    private static final String RACE_TAG_DIRECTORY =
            "data/" + TensuraDelight.MOD_ID + "/tags/manascore_race/races/";
    private static final String ITEM_TAG_DIRECTORY = "data/" + TensuraDelight.MOD_ID + "/tags/item/";

    private static final String DOES_NOT_EAT_FILE = raceFile(Appetite.DOES_NOT_EAT);
    private static final String MONSTER_EATER_FILE = raceFile(Appetite.MONSTER_EATER);
    private static final String EATS_FILE = raceFile(Appetite.EATS);
    private static final String DRINKER_FILE = raceFile(Appetite.DRINKER);
    private static final String MONSTER_MEALS_FILE = itemFile(Appetite.MONSTER_MEALS);
    private static final String DRINKS_FILE = itemFile(Appetite.DRINKS);

    private AppetiteTagsContent() {
    }

    /** Every file of the shipped appetite pack: four race gates, two item gates. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> files = new LinkedHashMap<>();
        files.put(DOES_NOT_EAT_FILE, tag(optionalReference("#tensura:unable_to_heal_with_food")));
        files.put(MONSTER_EATER_FILE, tag(
                optionalReference("#tensura:undead"),
                optionalReference("#tensura:necromancer"),
                optionalReference("#tensura:daemon"),
                optionalReference("#tensura:slime"),
                optionalReference("#capital_tensura_reincarnated:monsters")));
        files.put(EATS_FILE, tag());
        files.put(DRINKER_FILE, tag());
        files.put(MONSTER_MEALS_FILE, tag(optionalReference("#dungeonsdelight:monster_foods")));
        // Both FD and DD write data/farmersdelight/tags/item/drinks.json, but
        // TagLoader unions a path's whole resource stack unless a file sets
        // "replace": true - so the two merge to all ten. See agents/REPO.md.
        files.put(DRINKS_FILE, tag(
                optionalReference("#brewinandchewin:fermented_drinks"),
                optionalReference("#farmersdelight:drinks")));
        validate(files);
        return files;
    }

    private static String raceFile(ResourceLocation gate) {
        return RACE_TAG_DIRECTORY + gate.getPath() + ".json";
    }

    private static String itemFile(TagKey<Item> tag) {
        return ITEM_TAG_DIRECTORY + tag.location().getPath() + ".json";
    }

    /**
     * One tag file: a {@code values} array in the order written and no
     * {@code replace} key, so the shipped files union with whatever a pack or
     * another mod contributes instead of overriding it.
     */
    private static JsonObject tag(JsonElement... values) {
        JsonObject json = new JsonObject();
        JsonArray array = new JsonArray();
        for (JsonElement value : values) {
            array.add(value);
        }
        json.add("values", array);
        return json;
    }

    /**
     * A reference a contributor may drop: {@code {"id": ..., "required":
     * false}} keeps a missing race, tag or optional mod's item from failing
     * the whole file, exactly the shape Dungeon's Delight uses itself.
     */
    private static JsonObject optionalReference(String id) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", id);
        entry.addProperty("required", false);
        return entry;
    }

    /** Fails the datagen run when the built files would not load as the code expects. */
    private static void validate(Map<String, JsonObject> files) {
        if (files.size() != 6) {
            throw new IllegalStateException("the appetite pack must ship exactly six tag files, got "
                    + files.keySet());
        }
        for (Map.Entry<String, JsonObject> file : files.entrySet()) {
            String path = file.getKey();
            boolean raceTag = path.startsWith(RACE_TAG_DIRECTORY);
            boolean itemTag = path.startsWith(ITEM_TAG_DIRECTORY);
            if (raceTag == itemTag) {
                throw new IllegalStateException(
                        "an appetite tag must live under exactly one tag directory: " + path);
            }
            if (file.getValue().has("replace")) {
                throw new IllegalStateException("appetite tags union, they never replace: " + path);
            }
        }
        requireEmpty(files.get(EATS_FILE), EATS_FILE);
        requireEmpty(files.get(DRINKER_FILE), DRINKER_FILE);
        requireReferences(files.get(DOES_NOT_EAT_FILE), "#tensura:unable_to_heal_with_food");
        requireReferences(files.get(MONSTER_EATER_FILE), "#tensura:undead", "#tensura:necromancer",
                "#tensura:daemon", "#tensura:slime");
        requireReferences(files.get(MONSTER_MEALS_FILE), "#dungeonsdelight:monster_foods");
        requireReferences(files.get(DRINKS_FILE), "#brewinandchewin:fermented_drinks",
                "#farmersdelight:drinks");
    }

    private static void requireEmpty(JsonObject tag, String path) {
        JsonArray values = tag.getAsJsonArray("values");
        if (values == null || !values.isEmpty()) {
            throw new IllegalStateException(path + " must ship empty, so it fails open as no restriction");
        }
    }

    private static void requireReferences(JsonObject tag, String... references) {
        JsonArray values = tag.getAsJsonArray("values");
        if (values == null) {
            throw new IllegalStateException("an appetite tag must carry a values array");
        }
        Set<String> found = new LinkedHashSet<>();
        for (JsonElement value : values) {
            if (value.isJsonObject()) {
                JsonElement id = value.getAsJsonObject().get("id");
                if (id != null) {
                    found.add(id.getAsString());
                }
            } else if (value.isJsonPrimitive()) {
                found.add(value.getAsString());
            }
        }
        for (String reference : references) {
            if (!found.contains(reference)) {
                throw new IllegalStateException(
                        "an appetite tag must reference " + reference + ", got " + found);
            }
        }
    }
}
