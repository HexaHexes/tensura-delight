package org.bitwisemadness.tensuradelight.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bitwisemadness.tensuradelight.datagen.MealGrantContent;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the grant arithmetic and the meal_grant parser: nothing
 * exercised here loads a Minecraft class, so the suite runs on a bare JVM.
 */
class MealGrantsTest {

    @Test
    void scaledGrantFollowsNutrition() {
        MealGrants.Grant grant = MealGrants.defaults()
                .grantFor("farmersdelight:beef_stew", 8).orElseThrow();
        assertEquals(16.0, grant.ep());
        assertEquals(80.0, grant.magicule());
    }

    @Test
    void epIsCappedButMagiculeIsNot() {
        MealGrants.Grant grant = MealGrants.defaults()
                .grantFor("farmersdelight:beef_stew", 500).orElseThrow();
        assertEquals(100.0, grant.ep());
        assertEquals(5000.0, grant.magicule());
    }

    @Test
    void unlistedItemGrantsNothing() {
        assertTrue(MealGrants.defaults().grantFor("minecraft:bread", 5).isEmpty());
    }

    @Test
    void zeroNutritionYieldsTheZeroGrantTheBridgeRefuses() {
        MealGrants.Grant grant = MealGrants.defaults()
                .grantFor("farmersdelight:beef_stew", 0).orElseThrow();
        assertEquals(0.0, grant.ep());
        assertEquals(0.0, grant.magicule());
    }

    @Test
    void fixedOverrideBeatsTheScaledGrant() {
        MealGrants.Table table = new MealGrants.Table(2.0, 10.0, 100.0,
                Set.of("farmersdelight:beef_stew"),
                Map.of("dungeonsdelight:ghoulash", new MealGrants.Grant(9.0, 45.0)));
        MealGrants.Grant fixed = table.grantFor("dungeonsdelight:ghoulash", 8).orElseThrow();
        assertEquals(9.0, fixed.ep());
        assertEquals(45.0, fixed.magicule());
        MealGrants.Grant scaled = table.grantFor("farmersdelight:beef_stew", 8).orElseThrow();
        assertEquals(16.0, scaled.ep());
    }

    @Test
    void parsesTuningMealsAndOverrides() {
        String json = """
                {
                  "ep_per_nutrition": 3.0,
                  "magicule_per_nutrition": 4.0,
                  "max_meal_ep": 40.0,
                  "meals": ["farmersdelight:beef_stew"],
                  "overrides": {"dungeonsdelight:ghoulash": {"ep": 9.0, "magicule": 11.0}}
                }
                """;
        MealGrants.ParseResult result = MealGrants.parse(Map.of("meal_grant/meals.json", json));
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        MealGrants.Table table = result.table();
        assertEquals(3.0, table.epPerNutrition());
        assertEquals(4.0, table.magiculePerNutrition());
        assertEquals(40.0, table.maxMealEp());
        assertEquals(Set.of("farmersdelight:beef_stew"), table.meals());
        assertEquals(9.0, table.grantFor("dungeonsdelight:ghoulash", 8).orElseThrow().ep());
        assertEquals(24.0, table.grantFor("farmersdelight:beef_stew", 8).orElseThrow().ep());
    }

    @Test
    void malformedFileIsSkippedAndReported() {
        MealGrants.ParseResult result = MealGrants.parse(Map.of("meal_grant/broken.json", "{ not json"));
        assertEquals(1, result.problems().size());
        assertTrue(result.problems().get(0).startsWith("meal_grant/broken.json"));
        assertTrue(result.table().meals().isEmpty());
        assertEquals(MealGrants.defaults().epPerNutrition(), result.table().epPerNutrition());
    }

    @Test
    void unknownKeySkipsTheWholeFileRatherThanHalfApplying() {
        String json = """
                {
                  "ep_per_nutrition": 3.0,
                  "ep_per_nutritioon": 9.0,
                  "meals": ["farmersdelight:beef_stew"]
                }
                """;
        MealGrants.ParseResult result = MealGrants.parse(Map.of("meal_grant/typo.json", json));
        assertEquals(1, result.problems().size());
        assertTrue(result.table().meals().isEmpty());
        assertEquals(MealGrants.defaults().epPerNutrition(), result.table().epPerNutrition());
    }

    @Test
    void negativeTuningIsRejected() {
        MealGrants.ParseResult result = MealGrants.parse(
                Map.of("meal_grant/negative.json", "{\"ep_per_nutrition\": -1.0}"));
        assertEquals(1, result.problems().size());
        assertEquals(MealGrants.defaults().epPerNutrition(), result.table().epPerNutrition());
    }

    @Test
    void itemIdWithoutNamespaceIsRejected() {
        MealGrants.ParseResult result = MealGrants.parse(
                Map.of("meal_grant/no_namespace.json", "{\"meals\": [\"beef_stew\"]}"));
        assertEquals(1, result.problems().size());
        assertTrue(result.table().meals().isEmpty());
    }

    @Test
    void laterFileWinsOnConflictingTuning() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put("meal_grant/a.json", "{\"ep_per_nutrition\": 5.0}");
        files.put("meal_grant/b.json", "{\"ep_per_nutrition\": 7.0}");
        MealGrants.ParseResult result = MealGrants.parse(files);
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(7.0, result.table().epPerNutrition());
    }

    @Test
    void partialFileKeepsTheDefaultTuningItDoesNotSet() {
        MealGrants.ParseResult result = MealGrants.parse(
                Map.of("meal_grant/extra.json", "{\"meals\": [\"farmersdelight:beef_stew\"]}"));
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(MealGrants.defaults().epPerNutrition(), result.table().epPerNutrition());
        assertEquals(MealGrants.defaults().magiculePerNutrition(), result.table().magiculePerNutrition());
        assertEquals(Set.of("farmersdelight:beef_stew"), result.table().meals());
    }

    @Test
    void builtInDefaultsStandInBeforeTheFirstReload() {
        assertFalse(MealGrants.hasLoaded(), "no reload has run in this JVM");
        assertEquals(MealGrants.defaults(), MealGrants.current());
        assertTrue(MealGrants.current().meals().contains("farmersdelight:roast_chicken"));
    }

    @Test
    void generatedJsonRoundTripsToTheDefaults() {
        Map<String, JsonObject> files = MealGrantContent.build();
        assertEquals(1, files.size());
        Map<String, String> text = new LinkedHashMap<>();
        files.forEach((path, json) -> text.put(path, json.toString()));
        MealGrants.ParseResult result = MealGrants.parse(text);
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(MealGrants.defaults(), result.table());
    }
}
