package org.bitwisemadness.tensuradelight.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.Map;
import org.bitwisemadness.tensuradelight.datagen.KnifeGearContent;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the per-cut grant parser: the shipped default, the
 * datagen round trip and the skip-don't-fail rules. No Minecraft class loads
 * here either.
 */
class CuttingGrantsTest {

    @Test
    void defaultIsATenthOfACowKill() {
        assertEquals(2.0, CuttingGrants.defaults().epPerCut());
    }

    @Test
    void builtInDefaultStandsInBeforeTheFirstReload() {
        assertFalse(CuttingGrants.hasLoaded(), "no reload has run in this JVM");
        assertEquals(CuttingGrants.defaults(), CuttingGrants.current());
    }

    @Test
    void generatedRateRoundTripsToTheDefault() {
        JsonObject json = KnifeGearContent.build().get("data/tensura_delight/ep_per_cut/cutting.json");
        assertTrue(json != null, "the cut rate file must be part of the shipped content");
        CuttingGrants.ParseResult result = CuttingGrants.parse(Map.of("cutting.json", json.toString()));
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(CuttingGrants.defaults(), result.table());
    }

    @Test
    void unknownKeySkipsTheWholeFileRatherThanHalfApplying() {
        CuttingGrants.ParseResult result = CuttingGrants.parse(
                Map.of("typo.json", "{\"ep_per_cutt\": 9.0}"));
        assertEquals(1, result.problems().size());
        assertEquals(CuttingGrants.defaults().epPerCut(), result.table().epPerCut());
    }

    @Test
    void negativeRateIsRejected() {
        CuttingGrants.ParseResult result = CuttingGrants.parse(
                Map.of("negative.json", "{\"ep_per_cut\": -1.0}"));
        assertEquals(1, result.problems().size());
        assertEquals(CuttingGrants.defaults().epPerCut(), result.table().epPerCut());
    }

    @Test
    void laterFileWinsOnConflictingRates() {
        Map<String, String> files = Map.of(
                "a.json", "{\"ep_per_cut\": 5.0}",
                "b.json", "{\"ep_per_cut\": 7.0}");
        CuttingGrants.ParseResult result = CuttingGrants.parse(files);
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(7.0, result.table().epPerCut());
    }
}
