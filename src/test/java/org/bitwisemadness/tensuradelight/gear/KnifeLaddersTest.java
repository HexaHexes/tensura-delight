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
 * coverage and the datagen round trip. Nothing exercised here loads a
 * Minecraft class, so the suite runs on a bare JVM.
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
    void topRungsTerminalAtTheSwordGate() {
        for (KnifeLadders.Ladder ladder : KnifeLadders.ladders()) {
            KnifeLadders.Rung top = ladder.rungs().get(ladder.rungs().size() - 1);
            assertNull(top.evolution(), ladder.name() + " must top out with no further evolution");
            assertEquals(KnifeLadders.TOP_GATE, top.maxEp(),
                    ladder.name() + " must finish at the sword's netherite gate");
        }
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
