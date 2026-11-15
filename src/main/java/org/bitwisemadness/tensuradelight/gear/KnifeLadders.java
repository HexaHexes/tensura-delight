package org.bitwisemadness.tensuradelight.gear;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The shipped knife ladders: one band, climb rate and evolution target per rung.
 *
 * <p>{@code KnifeGearContent} turns this table into one
 * {@code data/<mod id>/gear_existence/<item namespace>/...} file per rung and
 * demands {@link #parse} reads those files back to this exact table, so
 * editing one side without the other fails the datagen run. Tensura loads the
 * generated files at runtime, indexing them by the {@code type} field's item
 * id, so each file's own namespace follows the item it names.</p>
 *
 * <p>No Minecraft types are touched beyond plain strings and numbers, so the
 * ladder invariants run headless under JUnit.</p>
 */
public final class KnifeLadders {

    /** Where the gear existence files live, inside {@code data/<mod id>/}. */
    public static final String DIRECTORY = "gear_existence";

    /**
     * Evolution targets outside this table. {@code tensura:kunai} is
     * tensura-iaf-growth's knife rung (0..52000 into
     * {@code tensura:pure_magisteel_kunai}), the hand-off every ladder tops out
     * at, mirroring how {@code netherite_sword} hands off to magisteel.
     */
    public static final Set<String> EXTERNAL_TARGETS = Set.of("tensura:kunai");

    private static final String ID_NAMESPACE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.-";
    private static final String ID_PATH_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_./-";

    private static final List<Ladder> LADDERS = buildLadders();
    private static final Map<String, Rung> BY_ITEM = index(LADDERS);

    private KnifeLadders() {
    }

    /** One rung: the item's band and what it climbs into. */
    public record Rung(String itemId, int minEp, Integer maxEp, Double epGain, String evolution) {
    }

    /** One material family's rungs in climb order. */
    public record Ladder(String name, List<Rung> rungs) {
    }

    /** The merged table keyed by item id, plus one problem line per skipped file. */
    public record ParseResult(Map<String, Rung> byItem, List<String> problems) {
    }

    /** Every ladder, in climb order. */
    public static List<Ladder> ladders() {
        return List.copyOf(LADDERS);
    }

    /** Every rung keyed by item id, in table order. */
    public static Map<String, Rung> byItem() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(BY_ITEM));
    }

    /** The shipped file for an item id, keyed by its path inside the jar. */
    public static String pathFor(String itemId) {
        int colon = itemId.indexOf(':');
        if (colon < 1 || colon == itemId.length() - 1) {
            throw new IllegalArgumentException("not a valid item id: " + itemId);
        }
        return "data/" + TensuraDelight.MOD_ID + "/" + DIRECTORY + "/"
                + itemId.substring(0, colon) + "/" + itemId.substring(colon + 1) + ".json";
    }

    /**
     * Merges every gear existence file into one table keyed by item id.
     *
     * <p>Files apply in path order, each fully or not at all: a file with a
     * JSON syntax error, an unknown key or an invalid value is reported in
     * {@link ParseResult#problems()} and skipped. A second file naming an item
     * that already has a rung is a problem too, because Tensura's registry
     * would silently keep only one of them.</p>
     */
    public static ParseResult parse(Map<String, String> files) {
        Map<String, Rung> byItem = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (String name : new TreeMap<>(files).keySet()) {
            try {
                Rung rung = readFile(files.get(name));
                if (byItem.putIfAbsent(rung.itemId(), rung) != null) {
                    throw new IllegalArgumentException("duplicate item id: " + rung.itemId());
                }
            } catch (JsonParseException | IllegalStateException | IllegalArgumentException failure) {
                problems.add(name + ": " + String.valueOf(failure.getMessage()));
            }
        }
        return new ParseResult(Collections.unmodifiableMap(byItem), List.copyOf(problems));
    }

    private static Rung readFile(String text) {
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        String type = null;
        Integer minEp = null;
        Integer maxEp = null;
        Double epGain = null;
        String evolution = null;
        for (Map.Entry<String, JsonElement> field : json.entrySet()) {
            switch (field.getKey()) {
                case "type" -> type = itemId(field.getValue(), "type");
                case "minEP" -> minEp = integer(field.getValue(), "minEP");
                case "maxEP" -> maxEp = integer(field.getValue(), "maxEP");
                case "epGain" -> epGain = amount(field.getValue(), "epGain");
                case "evolution" -> evolution = itemId(field.getValue(), "evolution");
                default -> throw new IllegalArgumentException("unknown key: " + field.getKey());
            }
        }
        return validated(type, minEp, maxEp, epGain, evolution);
    }

    private static Rung validated(String type, Integer minEp, Integer maxEp, Double epGain, String evolution) {
        if (type == null) {
            throw new IllegalArgumentException("missing type");
        }
        if (minEp == null || minEp < 0) {
            throw new IllegalArgumentException("minEP must be a non-negative integer");
        }
        if (maxEp != null && maxEp < 0) {
            throw new IllegalArgumentException("maxEP must be a non-negative integer");
        }
        return new Rung(type, minEp, maxEp, epGain, evolution);
    }

    private static int integer(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value != Math.floor(value)) {
            throw new IllegalArgumentException(key + " must be an integer, got " + value);
        }
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(key + " is too large: " + value);
        }
        return (int) value;
    }

    private static double amount(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be a number");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(key + " must be finite and at least 0, got " + value);
        }
        return value;
    }

    /** The same character rules {@code ResourceLocation} enforces, without loading it. */
    private static String itemId(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be an item id string");
        }
        String value = element.getAsString();
        int colon = value.indexOf(':');
        if (colon < 1 || !inSegment(value.substring(0, colon), ID_NAMESPACE_CHARS)
                || colon == value.length() - 1 || !inSegment(value.substring(colon + 1), ID_PATH_CHARS)) {
            throw new IllegalArgumentException(key + " is not a valid item id: " + value);
        }
        return value;
    }

    private static boolean inSegment(String value, String allowed) {
        for (int index = 0; index < value.length(); index++) {
            if (allowed.indexOf(value.charAt(index)) < 0) {
                return false;
            }
        }
        return !value.isEmpty();
    }

    private static Map<String, Rung> index(List<Ladder> ladders) {
        Map<String, Rung> byItem = new LinkedHashMap<>();
        for (Ladder ladder : ladders) {
            for (Rung rung : ladder.rungs()) {
                if (byItem.putIfAbsent(rung.itemId(), rung) != null) {
                    throw new IllegalStateException("duplicate ladder item: " + rung.itemId());
                }
            }
        }
        return Collections.unmodifiableMap(byItem);
    }

    /**
     * The ladders themselves: gates follow the pack's tool spine (500 entry,
     * 3200 iron-class, 10000 netherite-class, 52000 top hand-off), with rungs
     * between anchors spaced evenly and rounded to 100. Gold sits parallel
     * onto diamond like the pack's golden sword, and every ladder's top joins
     * tensura-iaf-growth's kunai rung.
     */
    private static List<Ladder> buildLadders() {
        return List.of(
                new Ladder("farmersdelight_knives", List.of(
                        rung("farmersdelight:flint_knife", 500, 0.003, "farmersdelight:iron_knife"),
                        rung("farmersdelight:iron_knife", 3200, 0.003, "farmersdelight:diamond_knife"),
                        rung("farmersdelight:golden_knife", 3200, 0.003, "farmersdelight:diamond_knife"),
                        rung("farmersdelight:diamond_knife", 10000, 0.003, "farmersdelight:netherite_knife"),
                        rung("farmersdelight:netherite_knife", 52000, 0.02, "tensura:kunai"))),
                new Ladder("aquaculture_fillet_knives", List.of(
                        rung("aquaculture:wooden_fillet_knife", 200, 0.003, "aquaculture:stone_fillet_knife"),
                        rung("aquaculture:stone_fillet_knife", 500, 0.003, "aquaculture:iron_fillet_knife"),
                        rung("aquaculture:iron_fillet_knife", 3200, 0.003, "aquaculture:diamond_fillet_knife"),
                        rung("aquaculture:gold_fillet_knife", 3200, 0.003, "aquaculture:diamond_fillet_knife"),
                        rung("aquaculture:diamond_fillet_knife", 10000, 0.003, "aquaculture:neptunium_fillet_knife"),
                        rung("aquaculture:neptunium_fillet_knife", 52000, 0.02, "tensura:kunai"))),
                new Ladder("aquaculturedelight_knives", List.of(
                        rung("aquaculturedelight:neptunium_knife", 52000, 0.02, "tensura:kunai"))),
                new Ladder("dungeonsdelight_knives", List.of(
                        rung("dungeonsdelight:zanite_knife", 500, 0.003, "dungeonsdelight:ironwood_knife"),
                        rung("dungeonsdelight:ironwood_knife", 3200, 0.003, "dungeonsdelight:steeleaf_knife"),
                        rung("dungeonsdelight:steeleaf_knife", 4900, 0.003, "dungeonsdelight:knightmetal_knife"),
                        rung("dungeonsdelight:knightmetal_knife", 6600, 0.003, "dungeonsdelight:gravitite_knife"),
                        rung("dungeonsdelight:gravitite_knife", 8300, 0.003, "dungeonsdelight:stained_knife"),
                        rung("dungeonsdelight:stained_knife", 10000, 0.003, "dungeonsdelight:fiery_knife"),
                        rung("dungeonsdelight:fiery_knife", 52000, 0.02, "tensura:kunai"))),
                new Ladder("dungeonsdelight_cleavers", List.of(
                        rung("dungeonsdelight:flint_cleaver", 500, 0.003, "dungeonsdelight:iron_cleaver"),
                        rung("dungeonsdelight:iron_cleaver", 3200, 0.003, "dungeonsdelight:zanite_cleaver"),
                        rung("dungeonsdelight:golden_cleaver", 3200, 0.003, "dungeonsdelight:diamond_cleaver"),
                        rung("dungeonsdelight:zanite_cleaver", 4100, 0.003, "dungeonsdelight:ironwood_cleaver"),
                        rung("dungeonsdelight:ironwood_cleaver", 4900, 0.003, "dungeonsdelight:steel_cleaver"),
                        rung("dungeonsdelight:steel_cleaver", 5800, 0.003, "dungeonsdelight:steeleaf_cleaver"),
                        rung("dungeonsdelight:steeleaf_cleaver", 6600, 0.003, "dungeonsdelight:knightmetal_cleaver"),
                        rung("dungeonsdelight:knightmetal_cleaver", 7500, 0.003, "dungeonsdelight:gravitite_cleaver"),
                        rung("dungeonsdelight:gravitite_cleaver", 8300, 0.003, "dungeonsdelight:diamond_cleaver"),
                        rung("dungeonsdelight:diamond_cleaver", 9200, 0.003, "dungeonsdelight:stained_cleaver"),
                        rung("dungeonsdelight:stained_cleaver", 10000, 0.003, "dungeonsdelight:fiery_cleaver"),
                        rung("dungeonsdelight:fiery_cleaver", 31000, 0.02, "dungeonsdelight:netherite_cleaver"),
                        rung("dungeonsdelight:netherite_cleaver", 52000, 0.02, "tensura:kunai"))));
    }

    private static Rung rung(String itemId, int maxEp, Double epGain, String evolution) {
        return new Rung(itemId, 0, maxEp, epGain, evolution);
    }
}
