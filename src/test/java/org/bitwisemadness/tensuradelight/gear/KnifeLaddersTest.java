package org.bitwisemadness.tensuradelight.gear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.bitwisemadness.tensuradelight.datagen.KnifeGearContent;
import org.junit.jupiter.api.Test;

/**
 * Headless invariants of the ladder table: owned evolution targets, acyclic
 * chains ordered by their gates, band ascent, unique output paths, exact
 * coverage, the sword ladder's literal gate values, gold running parallel to
 * iron, the Dungeon's Delight interleave, each family's host mod and the
 * datagen round trip. Nothing exercised here loads a Minecraft class, so the
 * suite runs on a bare JVM.
 */
class KnifeLaddersTest {

    private static final int COVERED_ITEMS = 32;

    @Test
    void everyEvolutionTargetIsOurs() {
        Map<String, KnifeLadders.Rung> table = KnifeLadders.byItem();
        table.forEach((itemId, rung) -> {
            String target = rung.evolution();
            if (target != null) {
                assertTrue(table.containsKey(target),
                        itemId + " evolves into unowned target " + target);
            }
        });
    }

    @Test
    void familyTopsTerminalAtTheirOwnGate() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("farmersdelight_knives", 10000);
        expected.put("aquaculture_fillet_knives", 10000);
        expected.put("aquaculturedelight_knives", 10000);
        expected.put("dungeonsdelight_knives", 9000);
        expected.put("dungeonsdelight_cleavers", 10000);
        Map<String, Integer> actual = new LinkedHashMap<>();
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            KnifeLadders.Rung top = ladder.rungs().get(ladder.rungs().size() - 1);
            assertNull(top.evolution(), ladder.name() + " must top out with no further evolution");
            actual.put(ladder.name(), top.maxEp());
        }
        assertEquals(expected, actual,
                "each family must terminal at its own material's gate: netherite and neptunium "
                        + "on the sword's 10000, the Dungeon's Delight knife line on fiery's 9000");
    }

    @Test
    void chainsAreAcyclic() {
        for (String start : KnifeLadders.byItem().keySet()) {
            Set<String> seen = new HashSet<>();
            KnifeLadders.Rung rung = KnifeLadders.byItem().get(start);
            while (rung != null) {
                assertTrue(seen.add(rung.itemId()), "evolution cycle through " + rung.itemId());
                String target = rung.evolution();
                rung = KnifeLadders.byItem().get(target);
            }
        }
    }

    @Test
    void gatesStrictlyAscendAlongEveryEvolutionEdge() {
        KnifeLadders.byItem().forEach((itemId, rung) -> {
            KnifeLadders.Rung next = KnifeLadders.byItem().get(rung.evolution());
            if (next == null) {
                return;
            }
            assertNotNull(rung.maxEp(), itemId + " must declare a band");
            assertNotNull(next.maxEp(), next.itemId() + " must declare a band");
            assertTrue(rung.maxEp() < next.maxEp(),
                    itemId + " band " + rung.maxEp() + " must end before "
                            + next.itemId() + " band " + next.maxEp());
        });
    }

    @Test
    void bandsAscendWithinEachLadder() {
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            Integer previous = null;
            for (KnifeLadders.Rung rung : ladder.rungs()) {
                assertNotNull(rung.maxEp(), rung.itemId() + " must declare a band");
                if (previous != null) {
                    assertTrue(previous <= rung.maxEp(),
                            ladder.name() + " regresses at " + rung.itemId()
                                    + ": " + previous + " then " + rung.maxEp());
                }
                previous = rung.maxEp();
            }
        }
    }

    @Test
    void outputPathIsUniquePerItem() {
        Set<String> paths = new HashSet<>();
        for (String itemId : KnifeLadders.byItem().keySet()) {
            assertTrue(paths.add(KnifeLadders.pathFor(itemId)), "duplicate path for " + itemId);
        }
        assertEquals(COVERED_ITEMS, paths.size());
    }

    @Test
    void everyItemIsCoveredExactlyOnce() {
        int rungs = KnifeLadders.ladders().stream().mapToInt(ladder -> ladder.rungs().size()).sum();
        assertEquals(COVERED_ITEMS, rungs, "the ladders must cover the pack's 32 knives");
        assertEquals(COVERED_ITEMS, KnifeLadders.byItem().size(), "no item may appear twice");
    }

    @Test
    void minEpIsZeroAndEpGainFollowsThePackSpine() {
        KnifeLadders.byItem().forEach((itemId, rung) -> {
            assertEquals(0, rung.minEp(), itemId + " starts with no history");
            assertNotNull(rung.epGain(), itemId + " must state its climb rate");
            double expected = rung.maxEp() < 18000 ? 0.003 : 0.02;
            assertEquals(expected, rung.epGain(), itemId + " climb rate off the pack's ladder");
        });
    }

    /**
     * Every rung gates at the sword ladder's own value for its material -
     * wood/flint 200, stone 500, iron and golden 2600, diamond 7000,
     * netherite 10000, with Dungeon's Delight's materials interleaved
     * between them. One pin per rung, so a drifted gate fails here by name.
     */
    @Test
    void gatesAreTheSwordsOwnValues() {
        Map<String, Integer> gates = new LinkedHashMap<>();
        gates.put("farmersdelight:flint_knife", 200);
        gates.put("aquaculture:wooden_fillet_knife", 200);
        gates.put("dungeonsdelight:flint_cleaver", 200);
        gates.put("aquaculture:stone_fillet_knife", 500);
        gates.put("farmersdelight:iron_knife", 2600);
        gates.put("aquaculture:iron_fillet_knife", 2600);
        gates.put("dungeonsdelight:iron_cleaver", 2600);
        gates.put("farmersdelight:golden_knife", 2600);
        gates.put("aquaculture:gold_fillet_knife", 2600);
        gates.put("dungeonsdelight:golden_cleaver", 2600);
        gates.put("dungeonsdelight:zanite_knife", 3200);
        gates.put("dungeonsdelight:zanite_cleaver", 3200);
        gates.put("dungeonsdelight:ironwood_knife", 3900);
        gates.put("dungeonsdelight:ironwood_cleaver", 3900);
        gates.put("dungeonsdelight:steel_cleaver", 4500);
        gates.put("dungeonsdelight:steeleaf_knife", 5100);
        gates.put("dungeonsdelight:steeleaf_cleaver", 5100);
        gates.put("dungeonsdelight:knightmetal_knife", 5700);
        gates.put("dungeonsdelight:knightmetal_cleaver", 5700);
        gates.put("dungeonsdelight:gravitite_knife", 6400);
        gates.put("dungeonsdelight:gravitite_cleaver", 6400);
        gates.put("farmersdelight:diamond_knife", 7000);
        gates.put("aquaculture:diamond_fillet_knife", 7000);
        gates.put("dungeonsdelight:diamond_cleaver", 7000);
        gates.put("dungeonsdelight:stained_knife", 8000);
        gates.put("dungeonsdelight:stained_cleaver", 8000);
        gates.put("dungeonsdelight:fiery_knife", 9000);
        gates.put("dungeonsdelight:fiery_cleaver", 9000);
        gates.put("farmersdelight:netherite_knife", 10000);
        gates.put("aquaculture:neptunium_fillet_knife", 10000);
        gates.put("aquaculturedelight:neptunium_knife", 10000);
        gates.put("dungeonsdelight:netherite_cleaver", 10000);

        Map<String, KnifeLadders.Rung> table = KnifeLadders.byItem();
        assertEquals(table.keySet(), gates.keySet(), "the pin map must cover every rung");
        gates.forEach((itemId, gate) -> assertEquals(gate, table.get(itemId).maxEp(),
                itemId + " must gate at the sword ladder's value for its material"));
    }

    /**
     * Gold is a parallel sibling of iron, not a hop after it: EP carries
     * across an evolution, so two rungs sharing a band in sequence would
     * promote on the spot. Every golden item sits in iron's band and climbs
     * straight into diamond, like the pack's golden sword.
     */
    @Test
    void goldRunsParallelToIronLikeTheSword() {
        Map<String, KnifeLadders.Rung> table = KnifeLadders.byItem();
        parallelOntoDiamond(table, "farmersdelight:golden_knife",
                "farmersdelight:iron_knife", "farmersdelight:diamond_knife");
        parallelOntoDiamond(table, "aquaculture:gold_fillet_knife",
                "aquaculture:iron_fillet_knife", "aquaculture:diamond_fillet_knife");
        parallelOntoDiamond(table, "dungeonsdelight:golden_cleaver",
                "dungeonsdelight:iron_cleaver", "dungeonsdelight:diamond_cleaver");
    }

    private static void parallelOntoDiamond(Map<String, KnifeLadders.Rung> table,
            String golden, String iron, String diamond) {
        KnifeLadders.Rung gold = table.get(golden);
        KnifeLadders.Rung sibling = table.get(iron);
        KnifeLadders.Rung target = table.get(diamond);
        assertNotNull(gold, golden + " must be a rung");
        assertNotNull(sibling, iron + " must be a rung");
        assertNotNull(target, diamond + " must be a rung");
        assertEquals(2600, gold.maxEp(), golden + " must gate with iron");
        assertEquals(sibling.maxEp(), gold.maxEp(), golden + " must share " + iron + "'s band");
        assertEquals(diamond, gold.evolution(), golden + " must climb straight into " + diamond);
        assertTrue(gold.maxEp() < target.maxEp(),
                golden + " would cross into " + diamond + " with its carried-over band");
    }

    /**
     * Dungeon's Delight's materials thread between the vanilla ones: the
     * chain leaves iron for zanite, runs its own rungs, hands back to
     * diamond, leaves again for stained, and closes on netherite - while
     * the vanilla families only ever climb vanilla targets, since a
     * Farmer's Delight knife has no zanite knife to become.
     */
    @Test
    void dungeonsDelightInterleavesItsOwnMaterialsWithTheSword() {
        Map<String, KnifeLadders.Rung> table = KnifeLadders.byItem();
        assertEvolvesInto(table, "dungeonsdelight:iron_cleaver", "dungeonsdelight:zanite_cleaver");
        assertEvolvesInto(table, "dungeonsdelight:ironwood_cleaver", "dungeonsdelight:steel_cleaver");
        assertEvolvesInto(table, "dungeonsdelight:gravitite_knife", "dungeonsdelight:stained_knife");
        assertEvolvesInto(table, "dungeonsdelight:gravitite_cleaver", "dungeonsdelight:diamond_cleaver");
        assertEvolvesInto(table, "dungeonsdelight:diamond_cleaver", "dungeonsdelight:stained_cleaver");
        assertEvolvesInto(table, "dungeonsdelight:fiery_cleaver", "dungeonsdelight:netherite_cleaver");
        assertEvolvesInto(table, "farmersdelight:iron_knife", "farmersdelight:diamond_knife");
    }

    private static void assertEvolvesInto(Map<String, KnifeLadders.Rung> table,
            String itemId, String target) {
        KnifeLadders.Rung rung = table.get(itemId);
        assertNotNull(rung, itemId + " must be a rung");
        assertEquals(target, rung.evolution(), itemId + " must climb into " + target);
    }

    /**
     * Only the optional hosts declare themselves: Farmer's Delight is
     * required by the pack, so its knives stay unconditional, and every
     * rung of a gated family carries exactly its family's host - the id
     * that becomes the file's neoforge:conditions entry.
     */
    @Test
    void optionalFamiliesDeclareTheirHostMod() {
        assertEquals(5, KnifeLadders.ladders().size(), "the pack ships five knife families");
        assertNull(hostOf("farmersdelight_knives"),
                "Farmer's Delight is required by the pack, its knives must be unconditional");
        assertEquals("aquaculture", hostOf("aquaculture_fillet_knives"));
        assertEquals("aquaculturedelight", hostOf("aquaculturedelight_knives"));
        assertEquals("dungeonsdelight", hostOf("dungeonsdelight_knives"));
        assertEquals("dungeonsdelight", hostOf("dungeonsdelight_cleavers"));
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            for (KnifeLadders.Rung rung : ladder.rungs()) {
                assertEquals(ladder.requiredMod(), rung.requiredMod(),
                        rung.itemId() + " must be gated exactly like its family");
            }
        }
    }

    private static String hostOf(String ladderName) {
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            if (ladderName.equals(ladder.name())) {
                return ladder.requiredMod();
            }
        }
        throw new AssertionError("no such ladder: " + ladderName);
    }

    /** The parser reads conditions back as the host mod, and rejects every other shape. */
    @Test
    void parserAcceptsAndValidatesModConditions() {
        KnifeLadders.ParseResult gated = KnifeLadders.parse(Map.of("gear.json", """
                {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "aquaculture"}],
                 "type": "aquaculture:iron_fillet_knife", "minEP": 0, "maxEP": 2600,
                 "epGain": 0.003, "evolution": "aquaculture:diamond_fillet_knife"}"""));
        assertTrue(gated.problems().isEmpty(), () -> gated.problems().toString());
        assertEquals("aquaculture",
                gated.byItem().get("aquaculture:iron_fillet_knife").requiredMod());

        KnifeLadders.ParseResult bare = KnifeLadders.parse(Map.of("gear.json", """
                {"type": "farmersdelight:iron_knife", "minEP": 0, "maxEP": 2600,
                 "epGain": 0.003, "evolution": "farmersdelight:diamond_knife"}"""));
        assertTrue(bare.problems().isEmpty(), () -> bare.problems().toString());
        assertNull(bare.byItem().get("farmersdelight:iron_knife").requiredMod());

        assertRejected("""
                {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "aquaculture"},
                 {"type": "neoforge:mod_loaded", "modid": "dungeonsdelight"}],
                 "type": "aquaculture:iron_fillet_knife", "minEP": 0}""",
                "exactly one entry");
        assertRejected("""
                {"neoforge:conditions": [{"type": "minecraft:load", "modid": "aquaculture"}],
                 "type": "aquaculture:iron_fillet_knife", "minEP": 0}""",
                "neoforge:mod_loaded");
        assertRejected("""
                {"neoforge:conditions": {"type": "neoforge:mod_loaded", "modid": "aquaculture"},
                 "type": "aquaculture:iron_fillet_knife", "minEP": 0}""",
                "must be an array");
        assertRejected("""
                {"neoforge:conditions": [{"type": "neoforge:mod_loaded"}],
                 "type": "aquaculture:iron_fillet_knife", "minEP": 0}""",
                "modid");
    }

    private static void assertRejected(String text, String expectedFragment) {
        KnifeLadders.ParseResult result = KnifeLadders.parse(Map.of("gear.json", text));
        assertEquals(1, result.problems().size(), () -> result.problems().toString());
        assertTrue(result.problems().get(0).contains(expectedFragment),
                () -> "expected a problem mentioning \"" + expectedFragment + "\", got "
                        + result.problems());
    }

    @Test
    void generatedJsonRoundTripsToTheTable() {
        Map<String, JsonObject> files = KnifeGearContent.build();
        Map<String, String> text = new LinkedHashMap<>();
        files.forEach((path, json) -> {
            if (path.contains("/" + KnifeLadders.DIRECTORY + "/")) {
                text.put(path, json.toString());
            }
        });
        KnifeLadders.ParseResult result = KnifeLadders.parse(text);
        assertTrue(result.problems().isEmpty(), () -> result.problems().toString());
        assertEquals(KnifeLadders.byItem(), result.byItem());
    }
}
