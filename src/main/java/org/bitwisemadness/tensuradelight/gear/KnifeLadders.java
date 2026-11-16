package org.bitwisemadness.tensuradelight.gear;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.bitwisemadness.tensuradelight.TensuraDelight;

/**
 * The shipped knife ladders: one band, climb rate, evolution target and host
 * mod per rung.
 *
 * <p>{@code KnifeGearContent} turns this table into one
 * {@code data/<mod id>/gear_existence/<item namespace>/...} file per rung and
 * demands {@link #parse} reads those files back to this exact table, so
 * editing one side without the other fails the datagen run. Tensura loads the
 * generated files at runtime, indexing them by the {@code type} field's item
 * id, so each file's own namespace follows the item it names.</p>
 *
 * <p>Gates are the pack's own sword values, read straight off the ladder the
 * swords climb: gearevolution's bundled chain as tensura-iaf-growth overrides
 * it (netherite at 10000 instead of the bundled 13500, stone at 500), so a
 * knife spends the same EP as the sword beside it. Dungeon's Delight's own
 * materials interleave between iron and diamond - and between diamond and
 * netherite - in the mod's own order. EP is cumulative and carries across an
 * evolution, so no two rungs may sit in the same band in sequence: gold runs
 * parallel to iron and both climb into diamond, like the pack's golden
 * sword.</p>
 *
 * <p>A family hosted by an optional mod carries that mod id, and
 * {@code KnifeGearContent} writes it out as a {@code neoforge:conditions}
 * entry, so while the host is absent the file never loads instead of naming
 * items that do not exist. Farmer's Delight is required by the pack, so its
 * knives are unconditional. No Minecraft types are touched beyond plain
 * strings and numbers, so the ladder invariants run headless under JUnit.</p>
 */
public final class KnifeLadders {

    /** Where the gear existence files live, inside {@code data/<mod id>}/. */
    public static final String DIRECTORY = "gear_existence";

    /**
     * The band a netherite- or neptunium-topped family stops at: the
     * netherite sword's own 10000 EP gate (tensura-iaf-growth replaces the
     * bundled 13500 outright). A top rung fills its band to here and stops -
     * the kunai is deliberately not a rung, since no mod in the pack tags
     * {@code tensura:kunai} as a knife.
     */
    public static final int TOP_GATE = 10000;

    // The pack's sword gates, verbatim. gearevolution's bundled ladder
    // supplies wood 200, iron/golden 2600, diamond 7000; tensura-iaf-growth
    // overrides stone to 500 and netherite to 10000.
    private static final int WOOD_GATE = 200;
    private static final int STONE_GATE = 500;
    private static final int IRON_GATE = 2600;
    private static final int GOLD_GATE = 2600;
    private static final int DIAMOND_GATE = 7000;

    // Dungeon's Delight's own materials, interleaved between iron and
    // diamond (six rungs, evenly spaced and rounded to 100) and between
    // diamond and netherite, in the mod's own material order.
    private static final int ZANITE_GATE = 3200;
    private static final int IRONWOOD_GATE = 3900;
    private static final int STEEL_GATE = 4500;
    private static final int STEELEAF_GATE = 5100;
    private static final int KNIGHTMETAL_GATE = 5700;
    private static final int GRAVITITE_GATE = 6400;
    private static final int STAINED_GATE = 8000;
    private static final int FIERY_GATE = 9000;

    /** The pack's spine: every sword, and so every knife, climbs at this rate. */
    private static final double EP_GAIN = 0.003;

    private static final String ID_NAMESPACE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.-";
    private static final String ID_PATH_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_./-";

    private static final List<Ladder> LADDERS = buildLadders();
    private static final Map<String, Rung> BY_ITEM = index(LADDERS);

    private KnifeLadders() {
    }

    /** One rung: the item's band, what it climbs into, and the mod its file needs. */
    public record Rung(String itemId, int minEp, Integer maxEp, Double epGain, String evolution,
                       String requiredMod) {
    }

    /** One material family's rungs in climb order, and the mod that hosts them. */
    public record Ladder(String name, String requiredMod, List<Rung> rungs) {
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
     * JSON syntax error, an unknown key, a malformed condition or an invalid
     * value is reported in {@link ParseResult#problems()} and skipped. A
     * second file naming an item that already has a rung is a problem too,
     * because Tensura's registry would silently keep only one of them.</p>
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
        String requiredMod = null;
        for (Map.Entry<String, JsonElement> field : json.entrySet()) {
            switch (field.getKey()) {
                case "type" -> type = itemId(field.getValue(), "type");
                case "minEP" -> minEp = integer(field.getValue(), "minEP");
                case "maxEP" -> maxEp = integer(field.getValue(), "maxEP");
                case "epGain" -> epGain = amount(field.getValue(), "epGain");
                case "evolution" -> evolution = itemId(field.getValue(), "evolution");
                case "neoforge:conditions" -> requiredMod = modLoadedCondition(field.getValue());
                default -> throw new IllegalArgumentException("unknown key: " + field.getKey());
            }
        }
        return validated(type, minEp, maxEp, epGain, evolution, requiredMod);
    }

    /**
     * The host mod named by a {@code neoforge:conditions} block: exactly one
     * {@code neoforge:mod_loaded} entry, since a gear file belongs to one
     * family and therefore to one mod.
     */
    private static String modLoadedCondition(JsonElement element) {
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("neoforge:conditions must be an array");
        }
        JsonArray entries = element.getAsJsonArray();
        if (entries.size() != 1) {
            throw new IllegalArgumentException(
                    "neoforge:conditions must hold exactly one entry, got " + entries.size());
        }
        JsonElement first = entries.get(0);
        if (!first.isJsonObject()) {
            throw new IllegalArgumentException("the neoforge:conditions entry must be an object");
        }
        String type = null;
        String modId = null;
        for (Map.Entry<String, JsonElement> field : first.getAsJsonObject().entrySet()) {
            switch (field.getKey()) {
                case "type" -> type = string(field.getValue(), "condition type");
                case "modid" -> modId = namespace(field.getValue(), "modid");
                default -> throw new IllegalArgumentException(
                        "unknown condition key: " + field.getKey());
            }
        }
        if (!"neoforge:mod_loaded".equals(type)) {
            throw new IllegalArgumentException(
                    "only neoforge:mod_loaded conditions are shipped, got " + type);
        }
        if (modId == null) {
            throw new IllegalArgumentException("the mod_loaded condition must name a modid");
        }
        return modId;
    }

    private static Rung validated(String type, Integer minEp, Integer maxEp, Double epGain,
            String evolution, String requiredMod) {
        if (type == null) {
            throw new IllegalArgumentException("missing type");
        }
        if (minEp == null || minEp < 0) {
            throw new IllegalArgumentException("minEP must be a non-negative integer");
        }
        if (maxEp != null && maxEp < 0) {
            throw new IllegalArgumentException("maxEP must be a non-negative integer");
        }
        return new Rung(type, minEp, maxEp, epGain, evolution, requiredMod);
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
        String value = string(element, key);
        int colon = value.indexOf(':');
        if (colon < 1 || !inSegment(value.substring(0, colon), ID_NAMESPACE_CHARS)
                || colon == value.length() - 1 || !inSegment(value.substring(colon + 1), ID_PATH_CHARS)) {
            throw new IllegalArgumentException(key + " is not a valid item id: " + value);
        }
        return value;
    }

    private static String string(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return element.getAsString();
    }

    /** A mod id is a namespace, not a full item id: the same characters, no colon. */
    private static String namespace(JsonElement element, String key) {
        String value = string(element, key);
        if (!inSegment(value, ID_NAMESPACE_CHARS)) {
            throw new IllegalArgumentException(key + " is not a valid mod id: " + value);
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
                if (!Objects.equals(ladder.requiredMod(), rung.requiredMod())) {
                    throw new IllegalStateException(ladder.name() + " runs " + rung.itemId()
                            + " under " + rung.requiredMod() + " but the family declares "
                            + ladder.requiredMod());
                }
                if (byItem.putIfAbsent(rung.itemId(), rung) != null) {
                    throw new IllegalStateException("duplicate ladder item: " + rung.itemId());
                }
            }
        }
        return Collections.unmodifiableMap(byItem);
    }

    /**
     * The ladders themselves: every family climbs the pack's sword material
     * ladder - wood/flint, stone, iron, gold, diamond, netherite - with
     * Dungeon's Delight's own materials interleaved between iron and diamond
     * and between diamond and netherite in the mod's own order. Gates are
     * global per material, so an iron knife, an iron fillet knife and an
     * iron-tier cleaver all evolve at the sword's own 2600 EP, and each
     * family's final rung terminals at its material's gate: netherite or
     * neptunium at the sword's 10000, Dungeon's Delight's knife line at its
     * fiery 9000. Gold runs parallel onto diamond like the pack's golden
     * sword - never after iron, because EP carries across an evolution and
     * equal adjacent bands would promote on the spot. Families hosted by an
     * optional mod declare that host so their files load only while it does;
     * Farmer's Delight is required by the pack, so its knives are
     * unconditional.
     */
    private static List<Ladder> buildLadders() {
        return List.of(
                ladder("farmersdelight_knives", null, List.of(
                        rung("farmersdelight:flint_knife", WOOD_GATE,
                                "farmersdelight:iron_knife"),
                        rung("farmersdelight:iron_knife", IRON_GATE,
                                "farmersdelight:diamond_knife"),
                        rung("farmersdelight:golden_knife", GOLD_GATE,
                                "farmersdelight:diamond_knife"),
                        rung("farmersdelight:diamond_knife", DIAMOND_GATE,
                                "farmersdelight:netherite_knife"),
                        terminal("farmersdelight:netherite_knife", TOP_GATE))),
                ladder("aquaculture_fillet_knives", "aquaculture", List.of(
                        rung("aquaculture:wooden_fillet_knife", WOOD_GATE,
                                "aquaculture:stone_fillet_knife"),
                        rung("aquaculture:stone_fillet_knife", STONE_GATE,
                                "aquaculture:iron_fillet_knife"),
                        rung("aquaculture:iron_fillet_knife", IRON_GATE,
                                "aquaculture:diamond_fillet_knife"),
                        rung("aquaculture:gold_fillet_knife", GOLD_GATE,
                                "aquaculture:diamond_fillet_knife"),
                        rung("aquaculture:diamond_fillet_knife", DIAMOND_GATE,
                                "aquaculture:neptunium_fillet_knife"),
                        terminal("aquaculture:neptunium_fillet_knife", TOP_GATE))),
                ladder("aquaculturedelight_knives", "aquaculturedelight", List.of(
                        terminal("aquaculturedelight:neptunium_knife", TOP_GATE))),
                ladder("dungeonsdelight_knives", "dungeonsdelight", List.of(
                        rung("dungeonsdelight:zanite_knife", ZANITE_GATE,
                                "dungeonsdelight:ironwood_knife"),
                        rung("dungeonsdelight:ironwood_knife", IRONWOOD_GATE,
                                "dungeonsdelight:steeleaf_knife"),
                        rung("dungeonsdelight:steeleaf_knife", STEELEAF_GATE,
                                "dungeonsdelight:knightmetal_knife"),
                        rung("dungeonsdelight:knightmetal_knife", KNIGHTMETAL_GATE,
                                "dungeonsdelight:gravitite_knife"),
                        rung("dungeonsdelight:gravitite_knife", GRAVITITE_GATE,
                                "dungeonsdelight:stained_knife"),
                        rung("dungeonsdelight:stained_knife", STAINED_GATE,
                                "dungeonsdelight:fiery_knife"),
                        terminal("dungeonsdelight:fiery_knife", FIERY_GATE))),
                ladder("dungeonsdelight_cleavers", "dungeonsdelight", List.of(
                        rung("dungeonsdelight:flint_cleaver", WOOD_GATE,
                                "dungeonsdelight:iron_cleaver"),
                        rung("dungeonsdelight:iron_cleaver", IRON_GATE,
                                "dungeonsdelight:zanite_cleaver"),
                        rung("dungeonsdelight:golden_cleaver", GOLD_GATE,
                                "dungeonsdelight:diamond_cleaver"),
                        rung("dungeonsdelight:zanite_cleaver", ZANITE_GATE,
                                "dungeonsdelight:ironwood_cleaver"),
                        rung("dungeonsdelight:ironwood_cleaver", IRONWOOD_GATE,
                                "dungeonsdelight:steel_cleaver"),
                        rung("dungeonsdelight:steel_cleaver", STEEL_GATE,
                                "dungeonsdelight:steeleaf_cleaver"),
                        rung("dungeonsdelight:steeleaf_cleaver", STEELEAF_GATE,
                                "dungeonsdelight:knightmetal_cleaver"),
                        rung("dungeonsdelight:knightmetal_cleaver", KNIGHTMETAL_GATE,
                                "dungeonsdelight:gravitite_cleaver"),
                        rung("dungeonsdelight:gravitite_cleaver", GRAVITITE_GATE,
                                "dungeonsdelight:diamond_cleaver"),
                        rung("dungeonsdelight:diamond_cleaver", DIAMOND_GATE,
                                "dungeonsdelight:stained_cleaver"),
                        rung("dungeonsdelight:stained_cleaver", STAINED_GATE,
                                "dungeonsdelight:fiery_cleaver"),
                        rung("dungeonsdelight:fiery_cleaver", FIERY_GATE,
                                "dungeonsdelight:netherite_cleaver"),
                        terminal("dungeonsdelight:netherite_cleaver", TOP_GATE))));
    }

    /**
     * One family, its rungs stamped with the mod that hosts them so the
     * table states the host once per family instead of once per rung.
     */
    private static Ladder ladder(String name, String requiredMod, List<Rung> rungs) {
        List<Rung> stamped = new ArrayList<>(rungs.size());
        for (Rung rung : rungs) {
            stamped.add(new Rung(rung.itemId(), rung.minEp(), rung.maxEp(), rung.epGain(),
                    rung.evolution(), requiredMod));
        }
        return new Ladder(name, requiredMod, List.copyOf(stamped));
    }

    private static Rung rung(String itemId, int maxEp, String evolution) {
        return new Rung(itemId, 0, maxEp, EP_GAIN, evolution, null);
    }

    /**
     * A family's top rung: it fills its band to its own material's gate -
     * the sword's netherite band for a netherite- or neptunium-topped
     * family, Dungeon's Delight's fiery band for its knife line - and stops
     * there with no further evolution, the shape tensura-iaf-growth's own
     * terminals (the goblin club, the severer blade) use, minus their
     * unbounded band: a kitchen knife's story ends where its sword tier's
     * does.
     */
    private static Rung terminal(String itemId, int maxEp) {
        return new Rung(itemId, 0, maxEp, EP_GAIN, null, null);
    }
}
