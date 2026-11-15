package org.bitwisemadness.tensuradelight.appetite;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.bitwisemadness.tensuradelight.TensuraDelight;
import org.bitwisemadness.tensuradelight.race.RaceBridge;

/**
 * Race appetite: the four gates a race can sit behind, and the two questions
 * this mod asks about them.
 *
 * <p>{@link #refusesUse} answers <b>may this race eat it at all</b>. A race in
 * {@code does_not_eat} refuses delight food outright, so the refusal handler
 * cancels the use before anything is consumed: no nutrition, no effects, no
 * grant. {@link #grantAllowed} answers <b>may this race be paid for it</b> -
 * a use that did happen still pays nobody unless the eater's race is allowed
 * through the item's gate, which is {@code monster_eater} for monster meals,
 * {@code drinker} for drinks and {@code eats} for everything else.</p>
 *
 * <p>Both questions split the same way on purpose: the item side of the
 * classification is two shipped item tags ({@link #MONSTER_MEALS} and
 * {@link #DRINKS}) plus the delight namespaces, and the race side is four
 * shipped race tags, so a pack re-classifies either half with a tag file and
 * never a rebuild. Only plain vanilla types live here - every ManasCore race
 * read goes through {@code RaceBridge}, which fails open - which is also what
 * keeps the pure parts ({@link #isDelightNamespace}, {@link #gateFor})
 * testable from headless JUnit.</p>
 */
public final class Appetite {

    /** The race gate for races that refuse delight food before using it. */
    public static final ResourceLocation DOES_NOT_EAT = id("does_not_eat");
    /** The race gate for ordinary meals; empty means every race may be paid. */
    public static final ResourceLocation EATS = id("eats");
    /** The race gate for Dungeon's Delight monster meals; empty means every race may be paid. */
    public static final ResourceLocation MONSTER_EATER = id("monster_eater");
    /** The race gate for drinks; empty means every race may be paid. */
    public static final ResourceLocation DRINKER = id("drinker");

    /** Items only monster eaters are paid for: the Monster Pot's own tag. */
    public static final TagKey<Item> MONSTER_MEALS =
            TagKey.create(Registries.ITEM, id("monster_meals"));
    /** Items only drinkers are paid for: Brewin' And Chewin's drinks plus the delight ones. */
    public static final TagKey<Item> DRINKS = TagKey.create(Registries.ITEM, id("drinks"));

    /**
     * The three namespaces that count as delight food. Farmer's Delight is a
     * required dependency and the other two are optional, but the names are
     * plain strings so this test never loads a registry.
     */
    private static final Set<String> DELIGHT_NAMESPACES =
            Set.of("farmersdelight", "brewinandchewin", "dungeonsdelight");

    private Appetite() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(TensuraDelight.MOD_ID, path);
    }

    /** Whether the item id belongs to one of the three delight food mods. */
    public static boolean isDelightNamespace(String namespace) {
        return DELIGHT_NAMESPACES.contains(namespace);
    }

    /**
     * The race tag that gates grants for this class of item.
     *
     * <p>Monster first, then drink, then plain food: five Dungeon's Delight
     * drinks are also listed in {@code monster_foods}, and a monster meal must
     * stay behind {@code monster_eater} whichever other tag it matches. The
     * order is pinned by {@code AppetiteTest}.</p>
     */
    public static ResourceLocation gateFor(boolean monsterMeal, boolean drink) {
        if (monsterMeal) {
            return MONSTER_EATER;
        }
        if (drink) {
            return DRINKER;
        }
        return EATS;
    }

    /**
     * Whether the eater's race refuses this item before it is even used.
     *
     * <p>Delight namespace plus an eat-or-drink animation is a complete test
     * for "delight food": {@code Item.getUseAnimation} answers EAT for anything
     * carrying the food component and DRINK for Brewin' And Chewin's booze, so
     * a knife, a block or another mod's steak never reaches the race read.</p>
     */
    public static boolean refusesUse(LivingEntity entity, ItemStack stack) {
        if (!isDelightNamespace(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
            return false;
        }
        UseAnim animation = stack.getUseAnimation();
        if (animation != UseAnim.EAT && animation != UseAnim.DRINK) {
            return false;
        }
        return RaceBridge.refusesFood(entity);
    }

    /**
     * Whether an eat that did happen may be paid a grant.
     *
     * <p>The refusal check runs first as belt and braces: a use that started
     * before a datapack reload reclassified the race still pays nothing. The
     * item is then classified by the two shipped item tags and the answer
     * comes from the matching race tag, which fails open when it is empty -
     * so shipping no restriction costs no behaviour.</p>
     */
    public static boolean grantAllowed(LivingEntity eater, ItemStack stack) {
        if (RaceBridge.refusesFood(eater)) {
            return false;
        }
        boolean monsterMeal = stack.is(MONSTER_MEALS);
        boolean drink = stack.is(DRINKS);
        return RaceBridge.allowsAppetite(eater, gateFor(monsterMeal, drink));
    }
}
