package org.bitwisemadness.tensuradelight.gear;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The cutting board grant: how much gear EP one cut adds to the held knife.
 *
 * <p>The table in force is an immutable snapshot swapped atomically by
 * {@link #replace}; until the first reload the built-in {@link #defaults()}
 * stand in, which is also what {@code runData} ships under
 * {@code data/<mod id>/ep_per_cut/}. The default is 2.0 EP, a tenth of a cow's
 * existence: {@code data/minecraft/entity_existence/cow.json} gives it aura 10
 * and magicule 10, Tensura's base EP is magicule + aura, so one cow kill is
 * worth 20 EP before a knife's {@code epGain} takes its share.</p>
 *
 * <p>No Minecraft types are touched here beyond plain strings and numbers, so
 * the parsing runs headless under JUnit.</p>
 */
public final class CuttingGrants {

    /** Where the grant files live, inside {@code data/<mod id>/}. */
    public static final String DIRECTORY = "ep_per_cut";

    private static final double DEFAULT_EP_PER_CUT = 2.0;

    private static final Table DEFAULT_TABLE = new Table(DEFAULT_EP_PER_CUT);
    private static volatile State state = new State(DEFAULT_TABLE, false);

    private CuttingGrants() {
    }

    /** One grant: {@code epPerCut} added to the held stack's gear EP per cut. */
    public record Table(double epPerCut) {
    }

    /** The merged table plus one problem line per file that was skipped. */
    public record ParseResult(Table table, List<String> problems) {
    }

    private record State(Table table, boolean loaded) {
    }

    /** The table in force: the built-in default until the first reload. */
    public static Table current() {
        return state.table();
    }

    /** Whether a reload has replaced the built-in default at least once. */
    public static boolean hasLoaded() {
        return state.loaded();
    }

    /** The built-in table: the documented per-cut default. */
    public static Table defaults() {
        return DEFAULT_TABLE;
    }

    /**
     * Swaps in a freshly parsed table; the reload listener's call site.
     *
     * <p>Malformed files never reach here: {@link #parse} skips them and
     * reports the problem, so this always installs a complete table.</p>
     */
    static void replace(Table table) {
        state = new State(table, true);
    }

    /**
     * Merges every {@code ep_per_cut} file into one table.
     *
     * <p>Files apply in path order, each fully or not at all: a file with a
     * JSON syntax error, an unknown key or an invalid value is reported in
     * {@link ParseResult#problems()} and skipped. Tuning starts from the
     * built-in default, so a file may carry only the keys it wants to change,
     * and later files override earlier ones on a conflicting key.</p>
     */
    public static ParseResult parse(Map<String, String> files) {
        Double epPerCut = null;
        List<String> problems = new ArrayList<>();
        for (String name : new TreeMap<>(files).keySet()) {
            try {
                Double value = readFile(files.get(name));
                if (value != null) {
                    epPerCut = value;
                }
            } catch (JsonParseException | IllegalStateException | IllegalArgumentException failure) {
                problems.add(name + ": " + String.valueOf(failure.getMessage()));
            }
        }
        Table table = new Table(epPerCut == null ? DEFAULT_EP_PER_CUT : epPerCut);
        return new ParseResult(table, List.copyOf(problems));
    }

    private static Double readFile(String text) {
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        Double epPerCut = null;
        for (Map.Entry<String, JsonElement> field : json.entrySet()) {
            if (!"ep_per_cut".equals(field.getKey())) {
                throw new IllegalArgumentException("unknown key: " + field.getKey());
            }
            epPerCut = amount(field.getValue());
        }
        return epPerCut;
    }

    private static double amount(JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("ep_per_cut must be a number");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException("ep_per_cut must be finite and at least 0, got " + value);
        }
        return value;
    }
}
