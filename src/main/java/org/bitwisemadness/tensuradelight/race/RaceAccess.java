package org.bitwisemadness.tensuradelight.race;

import io.github.manasmods.manascore.race.api.ManasRace;
import io.github.manasmods.manascore.race.api.ManasRaceInstance;
import io.github.manasmods.manascore.race.api.RaceAPI;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import org.bitwisemadness.tensuradelight.appetite.Appetite;

/**
 * The only class in this mod that imports ManasCore race types.
 *
 * <p>Never reference this class directly - always go through
 * {@link RaceBridge}, which checks {@code ModList} first. Loading this class
 * without ManasCore installed would throw {@link NoClassDefFoundError}.</p>
 */
final class RaceAccess {

    /**
     * The refusal gate, resolved once. Race tags are static registry keys, so
     * the {@code TagKey} never changes after the class loads.
     */
    private static final TagKey<ManasRace> DOES_NOT_EAT =
            TagKey.create(RaceAPI.getRaceRegistryKey(), Appetite.DOES_NOT_EAT);

    private RaceAccess() {
    }

    /**
     * Whether the entity's race sits in {@code does_not_eat}.
     *
     * <p>No race set answers false: nothing is known to refuse the food, so
     * the use goes ahead rather than being blocked by missing data.</p>
     */
    static boolean refusesFood(LivingEntity entity) {
        Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(entity).getRace();
        return race.map(instance -> instance.is(DOES_NOT_EAT)).orElse(false);
    }

    /**
     * Whether the entity's race passes this appetite gate.
     *
     * <p>Three answers lead to an allowance: a gate with no members (empty
     * or unbound, so no restriction ships), an entity with no race set (the
     * data is missing, not the permission), and a race the gate lists. Only
     * a gate with members that does not list the race withholds a grant.</p>
     */
    static boolean allowsAppetite(LivingEntity entity, ResourceLocation gateId) {
        TagKey<ManasRace> gate = TagKey.create(RaceAPI.getRaceRegistryKey(), gateId);
        if (!tagHasMembers(gate)) {
            return true;
        }
        Optional<ManasRaceInstance> race = RaceAPI.getRaceFrom(entity).getRace();
        if (race.isEmpty()) {
            return true;
        }
        return race.get().is(gate);
    }

    /**
     * Whether the race tag exists and has at least one member.
     *
     * <p>The race registry is registered late by ManasCore rather than being
     * a vanilla one, so membership cannot be assumed: the tag is resolved
     * through the root registry by its own key, and any failure reads as "no
     * members" - which turns into an allowance, never into a refusal. A
     * broken lookup therefore fails open, and the GameTest gate for
     * {@code monster_eater} is what catches it.</p>
     */
    private static boolean tagHasMembers(TagKey<ManasRace> tag) {
        try {
            ResourceLocation registryId = tag.registry().location();
            Registry<?> registry = BuiltInRegistries.REGISTRY.get(registryId);
            return registry != null && membersOf(registry, tag).map(set -> set.size() > 0).orElse(false);
        } catch (Throwable failure) {
            return false;
        }
    }

    /**
     * The tag's bound members, empty when nothing bound it.
     *
     * <p>The root registry only hands out {@code Registry<?>}, so the type
     * argument is asserted here - the tag key itself is typed, and any
     * mismatch between the two is caught by the caller's catch-all.</p>
     */
    @SuppressWarnings("unchecked")
    private static Optional<HolderSet.Named<ManasRace>> membersOf(
            Registry<?> registry, TagKey<ManasRace> tag) {
        return ((Registry<ManasRace>) registry).getTag(tag);
    }
}
