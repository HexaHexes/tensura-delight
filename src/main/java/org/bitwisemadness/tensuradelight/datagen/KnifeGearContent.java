package org.bitwisemadness.tensuradelight.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.gear.CuttingGrants;
import org.bitwisemadness.tensuradelight.gear.CuttingTools;
import org.bitwisemadness.tensuradelight.gear.KnifeLadders;

/**
 * The shipped knife gear files, as datapack JSON built from
 * {@link KnifeLadders}, {@link CuttingGrants} and the cutting-tool tag.
 *
 * <p>The runtime never reads this class: the generated files under
 * {@code src/generated/resources} are what Tensura, the tag system and the
 * reload listener load, so a datapack that replaces one changes the game
 * instead of being quietly restored by a Java fallback. {@link #build()}
 * round-trips every file through its parser and demands it equals the source
 * table, so editing one side without the other fails the datagen run rather
 * than shipping content the parser would skip.</p>
 */
public final class KnifeGearContent {

    private static final String TAG_FILE = "data/" + TensuraDelight.MOD_ID + "/tags/item/"
            + CuttingTools.CUTTING_TOOLS_ID + ".json";
    private static final String CUT_FILE = "data/" + TensuraDelight.MOD_ID + "/"
            + CuttingGrants.DIRECTORY + "/cutting.json";

    private KnifeGearContent() {
    }

    /** Every file of the shipped knife gear: ladders, tag and cut rate, round-trip validated. */
    public static Map<String, JsonObject> build() {
        Map<String, JsonObject> gearFiles = new LinkedHashMap<>();
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            for (KnifeLadders.Rung rung : ladder.rungs()) {
                gearFiles.put(KnifeLadders.pathFor(rung.itemId()), rungJson(rung));
            }
        }
        validateGear(gearFiles);

        JsonObject tag = cuttingToolsTag();
        validateTag(tag);

        JsonObject rate = cuttingRateJson();
        validateRate(rate);

        Map<String, JsonObject> files = new LinkedHashMap<>(gearFiles);
        files.put(TAG_FILE, tag);
        files.put(CUT_FILE, rate);
        return files;
    }

    /** Field order matches what {@link KnifeLadders#parse} reads, so the diff stays honest. */
    private static JsonObject rungJson(KnifeLadders.Rung rung) {
        JsonObject json = new JsonObject();
        json.addProperty("type", rung.itemId());
        json.addProperty("minEP", rung.minEp());
        if (rung.maxEp() != null) {
            json.addProperty("maxEP", rung.maxEp());
        }
        if (rung.epGain() != null) {
            json.addProperty("epGain", rung.epGain());
        }
        if (rung.evolution() != null) {
            json.addProperty("evolution", rung.evolution());
        }
        return json;
    }

    /**
     * No {@code replace} key: the common tags stay unioned, not overridden.
     * Both references are optional, so removing a contributor shrinks the
     * union instead of failing the tag load with a missing reference.
     */
    private static JsonObject cuttingToolsTag() {
        JsonObject json = new JsonObject();
        JsonArray values = new JsonArray();
        values.add(optionalReference(CuttingTools.KNIFE_TAG));
        values.add(optionalReference(CuttingTools.CLEAVER_TAG));
        json.add("values", values);
        return json;
    }

    private static JsonObject optionalReference(String tagId) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", tagId);
        entry.addProperty("required", false);
        return entry;
    }

    private static JsonObject cuttingRateJson() {
        JsonObject json = new JsonObject();
        json.addProperty("ep_per_cut", CuttingGrants.defaults().epPerCut());
        return json;
    }

    /** Fails the datagen run when the built gear files would not load as intended. */
    private static void validateGear(Map<String, JsonObject> files) {
        Map<String, String> text = new LinkedHashMap<>();
        files.forEach((path, json) -> text.put(path, json.toString()));
        KnifeLadders.ParseResult result = KnifeLadders.parse(text);
        if (!result.problems().isEmpty()) {
            throw new IllegalStateException(
                    "the parser rejects the generated gear files: " + result.problems());
        }
        if (!result.byItem().equals(KnifeLadders.byItem())) {
            throw new IllegalStateException(
                    "the generated gear files do not round-trip to the ladder table");
        }
    }

    private static void validateTag(JsonObject json) {
        JsonArray values = json.getAsJsonArray("values");
        if (values == null || values.size() != 2
                || !optionalReference(values.get(0), CuttingTools.KNIFE_TAG)
                || !optionalReference(values.get(1), CuttingTools.CLEAVER_TAG)) {
            throw new IllegalStateException(
                    "the generated cutting_tools tag must optionally union the two common knife tags");
        }
    }

    private static boolean optionalReference(JsonElement element, String tagId) {
        if (!element.isJsonObject()) {
            return false;
        }
        JsonObject entry = element.getAsJsonObject();
        JsonElement id = entry.get("id");
        return id != null && tagId.equals(id.getAsString())
                && entry.has("required") && !entry.get("required").getAsBoolean();
    }

    private static void validateRate(JsonObject json) {
        CuttingGrants.ParseResult result = CuttingGrants.parse(Map.of(CUT_FILE, json.toString()));
        if (!result.problems().isEmpty()) {
            throw new IllegalStateException(
                    "the parser rejects the generated ep_per_cut file: " + result.problems());
        }
        if (!result.table().equals(CuttingGrants.defaults())) {
            throw new IllegalStateException(
                    "the generated ep_per_cut file does not round-trip to the built-in default");
        }
    }
}
