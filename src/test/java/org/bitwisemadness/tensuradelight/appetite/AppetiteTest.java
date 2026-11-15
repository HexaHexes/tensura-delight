package org.bitwisemadness.tensuradelight.appetite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the appetite classification: only vanilla value types
 * are touched and nothing looks a race or an item up in a registry, so the
 * suite still runs on a bare JVM. The race side of every decision is proven
 * in the GameTests, where the tags are actually bound.
 */
class AppetiteTest {

    @Test
    void monsterMealsGateBeforeDrinksBeforePlainFood() {
        assertEquals("tensura_delight:monster_eater", Appetite.gateFor(true, true).toString());
        assertEquals("tensura_delight:monster_eater", Appetite.gateFor(true, false).toString());
        assertEquals("tensura_delight:drinker", Appetite.gateFor(false, true).toString());
        assertEquals("tensura_delight:eats", Appetite.gateFor(false, false).toString());
    }

    @Test
    void onlyTheThreeDelightModsCountAsDelightFood() {
        assertTrue(Appetite.isDelightNamespace("farmersdelight"));
        assertTrue(Appetite.isDelightNamespace("brewinandchewin"));
        assertTrue(Appetite.isDelightNamespace("dungeonsdelight"));
        assertFalse(Appetite.isDelightNamespace("minecraft"));
        assertFalse(Appetite.isDelightNamespace("tensura"));
        assertFalse(Appetite.isDelightNamespace(TensuraDelight.MOD_ID));
    }

    @Test
    void theFourRaceGatesShipUnderTheModsOwnNamespace() {
        assertEquals(TensuraDelight.MOD_ID, Appetite.DOES_NOT_EAT.getNamespace());
        assertEquals(TensuraDelight.MOD_ID, Appetite.EATS.getNamespace());
        assertEquals(TensuraDelight.MOD_ID, Appetite.MONSTER_EATER.getNamespace());
        assertEquals(TensuraDelight.MOD_ID, Appetite.DRINKER.getNamespace());
    }
}
