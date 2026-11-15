package org.bitwisemadness.tensuradelight.food;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * The meal grant table: which items pay out, and how much.
 *
 * <p>The table in force is an immutable snapshot swapped atomically by
 * {@link #replace}, so a reload never tears a lookup. Until the first reload
 * the built-in {@link #defaults()} stand in, which is also the table
 * {@code runData} ships under {@code data/<mod id>/meal_grant/} - edit one
 * side and the datagen round trip in {@code MealGrantContent} fails the build
 * rather than shipping the two apart.</p>
 *
 * <p>No Minecraft types are touched here beyond plain strings and numbers,
 * so the parsing and the arithmetic run headless under JUnit.</p>
 */
public final class MealGrants {

    /** Where the grant files live, inside {@code data/<mod id>/}. */
    public static final String DIRECTORY = "meal_grant";

    private static final double DEFAULT_EP_PER_NUTRITION = 2.0;
    private static final double DEFAULT_MAGICULE_PER_NUTRITION = 10.0;
    private static final double DEFAULT_MAX_MEAL_EP = 100.0;

    /**
     * The starter list: real ids from the three delight mods' registration
     * classes, one dish per mod at least. Entries for the optional mods are
     * plain strings, so they are inert rather than an error when the mod is
     * absent.
     */
    private static final List<String> DEFAULT_MEALS = List.of(
            "brewinandchewin:cheesy_pasta",
            "brewinandchewin:creamy_onion_soup",
            "brewinandchewin:kippers",
            "brewinandchewin:pizza_slice",
            "dungeonsdelight:ghoulash",
            "dungeonsdelight:monster_burger",
            "dungeonsdelight:spider_bisque",
            "farmersdelight:beef_stew",
            "farmersdelight:fish_stew",
            "farmersdelight:fried_rice",
            "farmersdelight:roast_chicken",
            "farmersdelight:vegetable_soup");

    /**
     * The shipped fixed grants: every drink of Brewin' And Chewin's
     * {@code fermented_drinks} tag, at one nutrition's worth of the shipped
     * scale (2 EP and 10 magicule per nutrition).
     *
     * <p>Drinks carry nutrition 0, so a scaled grant would compute to zero
     * and be refused as no grant at all - yet {@code alwaysEdible} booze can
     * be drunk at full hunger, which bounds it by the brewing cost instead,
     * the same kind of resource-gated action as a knife cut. The order below
     * is the shipped file's field order: the datagen round trip and the
     * drift gate read it back, so it must never depend on hash order.</p>
     */
    private static final Map<String, Grant> DEFAULT_OVERRIDES = fixedGrants(
            "brewinandchewin:beer",
            "brewinandchewin:vodka",
            "brewinandchewin:mead",
            "brewinandchewin:rice_wine",
            "brewinandchewin:egg_grog",
            "brewinandchewin:strongroot_ale",
            "brewinandchewin:saccharine_rum",
            "brewinandchewin:pale_jane",
            "brewinandchewin:salty_folly",
            "brewinandchewin:steel_toe_stout",
            "brewinandchewin:glittering_grenadine",
            "brewinandchewin:bloody_mary",
            "brewinandchewin:red_rum",
            "brewinandchewin:withering_dross",
            "brewinandchewin:dread_nog",
            "brewinandchewin:kombucha");

    private static final String ID_NAMESPACE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_.-";
    private static final String ID_PATH_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789_./-";

    private static final Table DEFAULT_TABLE = defaultTable();
    private static volatile State state = new State(DEFAULT_TABLE, false);

    private MealGrants() {
    }

    /**
     * One grant: {@code ep} added to the maximum, {@code magicule} added to
     * the pool.
     *
     * <p>Most entries scale from the eaten stack's nutrition, which is why
     * the table also carries fixed overrides: all sixteen Brewin' And Chewin'
     * drinks are nutrition 0, so a scaled grant would compute to zero and be
     * refused as no grant at all. They are still worth paying for -
     * {@code alwaysEdible} booze can be drunk at full hunger, so what bounds
     * the grant is the brewing that produced it, the same class of
     * resource-gated action as Phase 4's cutting.</p>
     */
    public record Grant(double ep, double magicule) {
    }

    /**
     * An immutable snapshot of the table.
     *
     * <p>An item pays only when the table lists it, either in {@code meals}
     * (scaled from the eaten stack's nutrition) or in {@code overrides} (fixed
     * amounts, which win for that item). The shipped defaults carry one
     * override per drink for the reason on {@link Grant}: nutrition 0 would
     * otherwise scale to nothing at all.</p>
     */
    public record Table(double epPerNutrition, double magiculePerNutrition, double maxMealEp,
            Set<String> meals, Map<String, Grant> overrides) {

        /**
         * The grant for {@code itemId}, empty when the table does not list it.
         *
         * <p>The scaled grant is {@code min(nutrition * epPerNutrition,
         * maxMealEp)} EP plus {@code nutrition * magiculePerNutrition}
         * magicule; a nutrition of zero therefore yields a zero grant, which
         * the bridge refuses as no grant at all.</p>
         */
        public Optional<Grant> grantFor(String itemId, int nutrition) {
            Grant fixed = overrides.get(itemId);
            if (fixed != null) {
                return Optional.of(fixed);
            }
            if (meals.contains(itemId)) {
                return Optional.of(scaled(nutrition));
            }
            return Optional.empty();
        }

        private Grant scaled(int nutrition) {
            return new Grant(Math.min(nutrition * epPerNutrition, maxMealEp),
                    nutrition * magiculePerNutrition);
        }
    }

    /** The merged table plus one problem line per file that was skipped. */
    public record ParseResult(Table table, List<String> problems) {
    }

    private record State(Table table, boolean loaded) {
    }

    /** The table in force: the built-in defaults until the first reload. */
    public static Table current() {
        return state.table();
    }

    /** Whether a reload has replaced the built-in defaults at least once. */
    public static boolean hasLoaded() {
        return state.loaded();
    }

    /** The built-in table: the tuning constants and the starter meal list. */
    public static Table defaults() {
        return DEFAULT_TABLE;
    }

    /** Convenience lookup against the table in force. */
    public static Optional<Grant> grantFor(String itemId, int nutrition) {
        return current().grantFor(itemId, nutrition);
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
     * Merges every {@code meal_grant} file into one table.
     *
     * <p>Files apply in path order, each fully or not at all: a file with a
     * JSON syntax error, an unknown key or an invalid value is reported in
     * {@link ParseResult#problems()} and skipped. Tuning starts from the
     * built-in defaults, so a file may carry only the keys it wants to
     * change, and later files override earlier ones on a conflicting key.</p>
     */
    public static ParseResult parse(Map<String, String> files) {
        double epPerNutrition = DEFAULT_EP_PER_NUTRITION;
        double magiculePerNutrition = DEFAULT_MAGICULE_PER_NUTRITION;
        double maxMealEp = DEFAULT_MAX_MEAL_EP;
        Set<String> meals = new LinkedHashSet<>();
        Map<String, Grant> overrides = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (String name : new TreeMap<>(files).keySet()) {
            try {
                FileContent content = readFile(files.get(name));
                if (content.epPerNutrition() != null) {
                    epPerNutrition = content.epPerNutrition();
                }
                if (content.magiculePerNutrition() != null) {
                    magiculePerNutrition = content.magiculePerNutrition();
                }
                if (content.maxMealEp() != null) {
                    maxMealEp = content.maxMealEp();
                }
                meals.addAll(content.meals());
                overrides.putAll(content.overrides());
            } catch (JsonParseException | IllegalStateException | IllegalArgumentException failure) {
                problems.add(name + ": " + String.valueOf(failure.getMessage()));
            }
        }
        Table table = new Table(epPerNutrition, magiculePerNutrition, maxMealEp,
                Collections.unmodifiableSet(meals), Collections.unmodifiableMap(overrides));
        return new ParseResult(table, List.copyOf(problems));
    }

    private static Table defaultTable() {
        Set<String> meals = Collections.unmodifiableSet(new LinkedHashSet<>(DEFAULT_MEALS));
        Map<String, Grant> overrides = Collections.unmodifiableMap(new LinkedHashMap<>(DEFAULT_OVERRIDES));
        return new Table(DEFAULT_EP_PER_NUTRITION, DEFAULT_MAGICULE_PER_NUTRITION, DEFAULT_MAX_MEAL_EP,
                meals, overrides);
    }

    /** One fixed grant per id, in call order, so the shipped field order is the written order. */
    private static Map<String, Grant> fixedGrants(String... itemIds) {
        Map<String, Grant> grants = new LinkedHashMap<>();
        for (String itemId : itemIds) {
            grants.put(itemId, new Grant(DEFAULT_EP_PER_NUTRITION, DEFAULT_MAGICULE_PER_NUTRITION));
        }
        return grants;
    }

    private record FileContent(Double epPerNutrition, Double magiculePerNutrition, Double maxMealEp,
            Set<String> meals, Map<String, Grant> overrides) {
    }

    private static FileContent readFile(String text) {
        JsonObject json = JsonParser.parseString(text).getAsJsonObject();
        Double epPerNutrition = null;
        Double magiculePerNutrition = null;
        Double maxMealEp = null;
        Set<String> meals = Set.of();
        Map<String, Grant> overrides = Map.of();
        for (Map.Entry<String, JsonElement> field : json.entrySet()) {
            switch (field.getKey()) {
                case "ep_per_nutrition" -> epPerNutrition = amount(field.getValue(), "ep_per_nutrition");
                case "magicule_per_nutrition" ->
                    magiculePerNutrition = amount(field.getValue(), "magicule_per_nutrition");
                case "max_meal_ep" -> maxMealEp = amount(field.getValue(), "max_meal_ep");
                case "meals" -> meals = readMeals(field.getValue());
                case "overrides" -> overrides = readOverrides(field.getValue());
                default -> throw new IllegalArgumentException("unknown key: " + field.getKey());
            }
        }
        return new FileContent(epPerNutrition, magiculePerNutrition, maxMealEp, meals, overrides);
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

    private static Set<String> readMeals(JsonElement element) {
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("meals must be an array of item ids");
        }
        Set<String> meals = new LinkedHashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("meals entries must be item id strings");
            }
            meals.add(itemId(entry.getAsString(), "meals"));
        }
        return meals;
    }

    private static Map<String, Grant> readOverrides(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("overrides must be an object keyed by item id");
        }
        Map<String, Grant> overrides = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            String id = itemId(entry.getKey(), "overrides");
            overrides.put(id, readGrant(entry.getValue(), id));
        }
        return overrides;
    }

    private static Grant readGrant(JsonElement element, String id) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("override " + id + " must be an object with ep and magicule");
        }
        Double ep = null;
        Double magicule = null;
        for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
            switch (field.getKey()) {
                case "ep" -> ep = amount(field.getValue(), "override " + id + ".ep");
                case "magicule" -> magicule = amount(field.getValue(), "override " + id + ".magicule");
                default -> throw new IllegalArgumentException("unknown key in override " + id + ": " + field.getKey());
            }
        }
        if (ep == null || magicule == null) {
            throw new IllegalArgumentException("override " + id + " needs both ep and magicule");
        }
        return new Grant(ep, magicule);
    }

    /** The same character rules {@code ResourceLocation} enforces, without loading it. */
    private static String itemId(String value, String key) {
        int colon = value.indexOf(':');
        if (colon < 1 || !inSegment(value.substring(0, colon), ID_NAMESPACE_CHARS)
                || colon == value.length() - 1 || !inSegment(value.substring(colon + 1), ID_PATH_CHARS)) {
            throw new IllegalArgumentException(key + " entry is not a valid item id: " + value);
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
}
