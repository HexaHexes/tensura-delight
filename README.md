# Tensura Delight

Farmer's Delight, Brewin' And Chewin' and Dungeon's Delight are built around
sitting down to a good meal. Tensura: Reincarnated is built around getting
stronger. This mod is for playing both at once.

## What it does

- Eating the right meal from the three food mods pays toward Tensura
  progression. A serving raises the EP you can hold and adds magicule, scaled
  by how nutritious the dish is, so a plated feast beats a snack. Brewin' And
  Chewin's drinks carry no nutrition, so they pay a small fixed amount instead
  - one nutrition's worth of the same scale. The mod ships a starter list of
  meals from all three mods, and a datapack can retune the amounts or add
  meals to it.
- Knives climb their own gear ladder now. Every knife and cleaver from
  Farmer's Delight, Aquaculture, Aquaculture Delight and Dungeon's Delight
  earns gear EP in combat and while cutting on a cutting board, fills its
  band, and evolves into the next material of its family. The ladders use
  the pack's own sword gates verbatim - wood/flint 200, stone 500, iron and
  golden 2600, diamond 7000, netherite 10000 - with Dungeon's Delight's
  materials interleaved between them, golden running parallel onto diamond
  like the golden sword, and each family topping out at its own material's
  gate. Families hosted by an optional mod load only while that mod does,
  so dropping Aquaculture simply parks its fillet knives. The per-cut
  reward and every rung can be retuned from a datapack.
- Race appetite decides what delight food does. A race that does not eat has
  delight food and drinks refused before use, monster races are the ones built
  for what Dungeon's Delight cooks in a Monster Pot, and the remaining classes
  decide who receives a grant. The shipped appetite classes:

  | Appetite class | Tag | Shipped default | Effect |
  |---|---|---|---|
  | does not eat | `tensura_delight:does_not_eat` | `#tensura:unable_to_heal_with_food` (ghoul, vampire, vampire_overcomer, vampire_lord, divine_vampire) | Delight food and drinks are refused before use: nothing consumed, no nutrition, no effects, no grant |
  | monster eater | `tensura_delight:monster_eater` | `#tensura:undead`, `#tensura:necromancer`, `#tensura:daemon`, `#tensura:slime`, `#capital_tensura_reincarnated:monsters` (optional) | only these races receive EP/magicule from Dungeon's Delight monster meals |
  | eats | `tensura_delight:eats` | empty = no restriction | when non-empty, only listed races receive grants from ordinary meals |
  | drinker | `tensura_delight:drinker` | empty = no restriction | when non-empty, only listed races receive grants from drinks |

  Monster Effects and Tipsy stay Dungeon's Delight's and Brewin' And Chewin's
  own behaviour: this mod only gates who receives the EP and magicule grant.
  Every appetite class is a datapack race tag, so a pack re-classifies a race
  with a tag file, no rebuild.
- Tensura ingredients go back the other way. Tensura beef, fish and spider
  legs work in the cutting board, the cooking pot, the keg and the Monster
  Pot, so the delight mods' recipes can be cooked from a Tensura hunt. The
  pot, keg and Monster Pot recipe books learn the new dishes the way they
  learn the hosts' own: hold one of the ingredients.

## Dependencies

| | Mod | Why |
|---|---|---|
| Required | Minecraft | — |
| Required | NeoForge | — |
| Required | Tensura: Reincarnated | — |
| Required | ManasCore | Ships with Tensura: Reincarnated |
| Required | Farmer's Delight | The base the other two food mods extend |
| Optional | Brewin' And Chewin' | The keg's drinks and fermented food; missing it costs those recipes and the drink grants |
| Optional | Dungeon's Delight | The Monster Pot and its monster food; missing it costs those recipes and the monster-meal grants |

## Credits

- [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) by vectorwing.
- [Brewin' And Chewin'](https://www.curseforge.com/minecraft/mc-mods/brewin-and-chewin) by lumpazl.
- [Dungeon's Delight](https://www.curseforge.com/minecraft/mc-mods/dungeons-delight) by Yirmiri.
- [Tensura: Reincarnated](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated) by ManasMods.
- Built on [NeoForge](https://neoforged.net/).

## License

GNU Lesser General Public License v2.1 — see [`LICENSE`](LICENSE).
Copyright (c) 2026 Bitwise Madness.

## AI disclosure

Parts of this project were written with the assistance of AI coding tools.
AI-assisted code, data and documentation ship only after human review by the
maintainers.
