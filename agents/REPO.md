# Repo rules - tensura-delight

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim - **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/tensura-delight` - **Mod ID:** `tensura_delight` - **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 - **Jar:** `build/libs/tensura_delight-<version>.jar`

Brings Farmer's Delight, Brewin' And Chewin' and Dungeon's Delight into
Tensura: Reincarnated's progression: delight meals grant EP and magicule and
count toward eating progress, race appetite decides what those meals do,
Tensura ingredients can be cooked in the delight workstations, and every
knife and cleaver climbs its own gear ladder into Tensura's. Landed in
phases - progression, race diets, cooking, knife gear evolution - one
tracking issue each.

**Dependencies**
- Required: Minecraft, NeoForge, `tensura`, `manascore`, `farmersdelight` -
  `compileOnly` plus declared `required` in the toml.
- Optional: `brewinandchewin` and `dungeonsdelight` - `compileOnly`, declared
  `optional` in the toml, and every call site gated behind `ModList`. No
  optional feature may put a class outside its gate; loading one without its
  mod throws `NoClassDefFoundError`.
- `runiclib` rides along because Dungeon's Delight requires it; it is only on
  the run closure and the compile classpath, never a declared dependency.
- Pins live in `gradle.properties` and were read from the Tensura RPG
  instance's `minecraftinstance.json` (Tensura 2.0.1.3, ManasCore 4.0.0.2,
  Farmer's Delight 1.3.4, Brewin' And Chewin' 4.5.0, Dungeon's Delight 1.5.2,
  RunicLib 5.0.7), not from "latest". Bump the file id and the mod version
  together, after re-checking the API.
- **Dungeon's Delight's metadata lies about its version.** File id 9614702 is
  named `neoforge-dungeonsdelight-1.21.1-1.5.2.jar` but declares `1.5.0` from
  its `neoforge.mods.toml`, and FML checks the declaration. The floor is
  therefore `1.5.0`; re-read the staged jar's metadata after any pin bump
  rather than trusting the file name.

**Test:** `./gradlew runGameTestServer` - `TensuraDelightGameTests` (16 tests). `expectedTests = 16`
in `build.gradle` must match the `@GameTest` count. The run closure is staged
into `run/mods` by `prepareGametestMods`.

**Race appetite (phase 2)**
- **Path ownership:** this repo owns `data/tensura_delight/tags/manascore_race/races/**`
  (the four gates: `does_not_eat`, `monster_eater`, `eats`, `drinker`) and
  `data/tensura_delight/tags/item/{monster_meals,drinks}.json`. All six are
  built by `datagen/AppetiteTagsContent.java` through the `JsonFilesProvider`
  round trip, so `runData` ships them and the file paths are derived from the
  very ids `appetite/Appetite.java` tests at runtime - datagen and code cannot
  name two different tags. `validate()` fails the datagen run if a default the
  code relies on would go missing (an empty `eats`/`drinker` is deliberate, a
  dropped `#tensura:` reference is not).
- **Race-read gotcha:** race identity is read only through ManasCore -
  `RaceAPI.getRaceFrom(entity).getRace()` -> `Optional<ManasRaceInstance>` ->
  `instance.is(TagKey<ManasRace>)`. Never compare race-name strings. Race tags
  are `data/<namespace>/tags/manascore_race/races/<path>.json` (the race
  registry's own directory, not `tags/item/` or `tags/entity_type/`), created
  with `TagKey.create(RaceAPI.getRaceRegistryKey(), id)`. The pattern this
  copies is `capital-tensura-reincarnated`'s `RaceTags.java`.
- **Every ManasCore call sits behind `race/RaceBridge`.** `RaceBridge` holds no
  ManasCore type in its signature and checks `ModList.isLoaded("manascore")`
  before delegating to `RaceAccess` - the exact lazy-resolution trick
  `TensuraBridge` uses, because the JVM only resolves a class's references when
  it is first loaded. Never reference `RaceAccess` directly. Both answers fail
  open on any failure: a race read that cannot happen must not become a silent
  refusal of food, nor a grant the pack meant to withhold.
- **Empty gate = no restriction (fail-open) needs a root-registry lookup.** A
  tag with no members must *allow*, so `RaceAccess.tagHasMembers` resolves the
  race registry through `BuiltInRegistries.REGISTRY` by the tag's own key and
  reads `getTag(tag).size()`; unbound, empty or a failed lookup all read as "no
  members" -> allowance. The GameTest `grant_gate_by_appetite_class` pins this:
  if the lookup broke, `monster_eater` would fail open and a human would be
  paid for a monster meal, failing the assertion the wrong way round.
- **Gate order is monster, then drink, then plain food** (`Appetite.gateFor`).
  Five Dungeon's Delight drinks (`spider_bubble_tea`, `bloody_mary`,
  `taro_milk_tea`, `trial_freakshake`, `skyberry_brew`) are listed in DD's own
  `monster_foods` too, and a monster meal must stay behind `monster_eater`
  whichever other tag it matches. Pinned by `AppetiteTest`.
- **Refusal cancels `LivingEntityUseItemEvent.Start`, never `Finish`.**
  `Finish` is post-hoc and *not* cancellable: it fires after nutrition and
  effects are already applied, so "no effects" is only achievable by cancelling
  the start. Proven from the patched bytecode: a cancelled `Start` makes
  `EventHooks.onItemUseStart` return `-1`, and `LivingEntity.startUsingItem`
  then returns immediately - no consumption, no nutrition, no effects, no
  `Finish`. The refusal test is `EventHooks.onItemUseStart(...) == -1`; there
  is no mixin anywhere. Scope is the three delight namespaces **and** an
  `EAT`/`DRINK` use animation, which `Item.getUseAnimation` answers from the
  food component alone, so a knife or another mod's steak never reaches the
  race read.
- **Dungeon's Delight's effect path is deliberately untouched.** The DD jar
  contains *zero* ManasCore or Tensura references - verified by scanning every
  class file. `UndeadFoodItem.finishUsingItem` picks between
  `DDUtil.MONSTER_EFFECTS` and `DDUtil.NORMAL_EFFECTS` off a `hasEffect`
  predicate and applies the swap to whoever ate, and `BoozeItem` applies Tipsy
  to whoever drank. Per the user's decision those land exactly as they always
  have (Tensura's own rules still apply on top); this mod only gates **who
  receives the EP/magicule grant**. Do not "fix" monsterisation to be
  race-aware - it is out of scope and would fight the host's design.
- **All sixteen Brewin' And Chewin drinks carry `nutrition 0` and
  `alwaysEdible()`** (`BoozeItem.use` calls `player.canEat(canAlwaysEat())`),
  so a scaled grant would compute to zero and be refused as no grant at all.
  They therefore ship as fixed `overrides` in `MealGrants.DEFAULT_OVERRIDES` -
  2 EP and 10 magicule, one nutrition's worth at the shipped scale. Because
  booze drinks at full hunger, what bounds the grant is the brewing that
  produced it, the same class of resource-gated action as phase 4's cutting.
  B&C *foods* all carry real nutrition (creamy_onion_soup 12, cheesy_pasta 14,
  kippers 6) and need no override.
- **Upstream collision: both the Farmer's Delight and the Dungeon's Delight
  jar ship `data/farmersdelight/tags/item/drinks.json` with different
  contents** (FD: milk_bottle, apple_cider, melon_juice, hot_cocoa; DD: six
  monster drinks). One shadows the other at runtime, so the shipped
  `tensura_delight:drinks` never references `#farmersdelight:drinks`: it
  references `#brewinandchewin:fermented_drinks` (only B&C ships that one) and
  lists the ten FD/DD drinks explicitly as entries, the optional ones with
  `{"id": ..., "required": false}`. If a host ever fixes the collision, the
  explicit list is still correct, only redundant.
- **`#capital_tensura_reincarnated:monsters` is inert on purpose.** That mod
  is not in the pack and currently ships its race tags under the
  `tensura_faction_spawn` namespace rather than its own, so the reference
  resolves to nothing today; `required: false` keeps it a self-activating
  extension point the day it ships correctly.
- **GameTests must not use `makeMockServerPlayerInLevel`.** It runs
  `PlayerList.placeNewPlayer`, which fires `playerLogin`, and ManasCore's
  login listener answers by sending `manascore_config:sync_config_to_client`
  to a connection that never negotiated any channel - NeoForge's
  `NetworkRegistry.checkPacket` then throws "Payload ... may not be sent to
  the client!" straight out of the test. Use `makeMockPlayer(GameType)`:
  it is only constructed, never placed, so no login event fires. That works
  here because every appetite decision (`Appetite.refusesUse`,
  `Appetite.grantAllowed`) and `EventHooks.onItemUseStart` take a
  `LivingEntity`, not a `ServerPlayer` - if a future test needs a real logged-in
  player, the config payload problem has to be solved first, not worked around
  by catching the exception.

**Station recipes (phase 3)**
- **Path ownership:** this repo owns `data/tensura_delight/recipe/**` - one
  file per recipe at `data/tensura_delight/recipe/<station>/<name>.json`,
  recipe ids `tensura_delight:<station>/<name>`.
- **The table:** `recipe/StationRecipes.java` builds the five recipes as
  gson JSON (no Minecraft classes, so JUnit runs it headless);
  `datagen/StationRecipesContent.java` round-trips every file through gson
  and hands the map to a `JsonFilesProvider` in `TensuraDelight.gatherData`,
  so the JSON ships only from `./gradlew runData` and table and output
  cannot drift apart. Each recipe mirrors the host's own recipe of the same
  dish, keeping its experience, tab, category, temperature and tool:
  - `cooking/cattledeer_stew` - cooking pot: `tensura:cattledeer_beef` +
    carrot + potato -> `farmersdelight:beef_stew`.
  - `cooking/spear_toro_fish_stew` - cooking pot: `tensura:raw_spear_toro_meat`
    + tomato sauce + onion -> `farmersdelight:fish_stew`.
  - `cutting/cattledeer_beef` - cutting board: `tensura:cattledeer_beef` ->
    2x `farmersdelight:minced_beef`, same knife tool as FD's beef cutting.
  - `fermenting/kippers_from_spear_toro` - keg: 2x `tensura:raw_spear_toro_meat`
    + dried kelp + the empty fourth slot -> 2x `brewinandchewin:kippers`.
  - `monster_cooking/spider_bisque_from_knight_spider` - Monster Pot: 2x
    `tensura:knight_spider_leg` + rotgourd slice + spider extract ->
    `dungeonsdelight:spider_bisque`.
- **Missing-host gating is per-recipe `neoforge:conditions`, not a gated
  provider.** The keg and Monster Pot recipes open with
  `{"neoforge:conditions":[{"type":"neoforge:mod_loaded","modid":...}]}` -
  the exact pattern Farmer's Delight and Dungeon's Delight use for their own
  integration recipes. Conditions are evaluated before the recipe codec
  decodes, so without the host the JSON is never parsed: no reload error, no
  missing-recipe log spam. Gating the *provider* at datagen time was rejected
  deliberately: `runData` output must be byte-identical whichever optional
  mods are installed, or the drift gate breaks - this is a knowing deviation
  from the issue's "provider gated per mod" wording. The two cooking-pot
  recipes and the cutting recipe carry no condition: tensura, farmersdelight
  and minecraft are required deps.
- **Phase 1 table:** `brewinandchewin:kippers` (nutrition 6, so 12 EP and 60
  magicule at the defaults) and `farmersdelight:fish_stew` joined
  `MealGrants.DEFAULT_MEALS`, and `meal_grant/meals.json` was regenerated
  with them. `farmersdelight:minced_beef` is deliberately absent: a cutting
  result is an ingredient for further cooking, and Farmer's Delight's own
  minced beef is not tabled either.
- **Gotcha:** Tensura's fish and meats are not in `c:foods/safe_raw_fish` or
  `c:foods/raw_beef` - that gap is the whole reason these recipes exist.
  Never "fix" an ingredient back into those tags; it would silently exclude
  every Tensura item again.
- **Driving `matches()` headless:** all four stations are driven for real in
  the GameTests. `RecipeWrapper` takes an `IItemHandler` in this NeoForge, not
  a `SimpleContainer`, so the cooking pot and Monster Pot inputs wrap a
  6-slot `ItemStackHandler` (both `matches` scan exactly six slots); the
  cutting board uses `CuttingBoardRecipeInput(item, knife)`; the keg uses an
  inline `KegRecipeWrapper` over 4 slots returning
  `AbstractedFluidStack.EMPTY`, because a recipe with no fluid ingredient
  demands an empty keg fluid.
- **Not shipped:** `advancement/recipes/**` unlock files. Recipe viewers
  (JEI/EMI) show the recipes outright; whether the vanilla recipe book also
  needs unlock advancements mirroring the hosts' is a follow-up decision.

**Knife gear evolution (phase 4)**
- **Path ownership:** this repo owns `data/tensura_delight/gear_existence/**`
  (one file per knife at `data/tensura_delight/gear_existence/<item namespace>/<item path>.json`),
  `data/tensura_delight/tags/item/cutting_tools.json` and
  `data/tensura_delight/ep_per_cut/**`. Never write into `data/tensura/**` or
  `data/gearevolution/**`: `tensura-iaf-growth` and `tensura-iss-growth` own
  paths there, and their registry rules bind this repo's entries too.
- **Ladder table:** `gear/KnifeLadders.java` holds the five ladders with rungs
  in climb order; `datagen/KnifeGearContent.java` emits them through the
  `JsonFilesProvider` round trip, together with the cutting tag and the cut
  rate. Family order was read from the staged jars, so no family needed the
  unordered-terminal exception:
  - Farmer's Delight mirrors the pack's vanilla sword ladder: flint to iron,
    gold parallel onto diamond, diamond to netherite, netherite into
    `tensura:kunai`. Tiers come from FD's `ModMaterials.FLINT` and the vanilla
    `Tiers` its five knives are constructed with.
  - Aquaculture follows its own fillet-knife recipes (planks, stone, iron,
    gold, diamond, neptunium) and its vanilla `Tiers` plus `AquaMats.NEPTUNIUM`;
    gold runs parallel onto diamond like the pack's golden sword.
  - Dungeon's Delight orders its seven knives and thirteen cleavers by the
    tier stats DD itself ships (`DDMaterials$DDCMaterials`, `DDItems`): mining
    level, then speed, then uses. `TFItems` and `AEItems` registration order
    backs the sub-orders (ironwood, steeleaf, knightmetal, fiery and zanite,
    gravitite). Exact stat ties (iron/zanite, diamond/gravitite) go to the
    pack-vanilla material, which keeps the vanilla backbone of the ladder.
  - `aquaculturedelight:neptunium_knife` is a family of one, so it is its own
    top rung.
- **Gates and rates:** the pack's tool spine - 500 on entry rungs (Aquaculture's
  wooden knife takes the pack's 200 wooden-tool band), 3200 iron-class, 10000
  netherite-class, 52000 on every top rung into `tensura:kunai`; Dungeon's
  Delight's long ladders space the rungs between anchors evenly, rounded to
  100. `epGain` is 0.003 below 18000 and 0.02 above, following
  `tensura-iaf-growth`'s `epGainFor`; a datapack file may omit the key to take
  the codec's 0.03 default.
- **`epGain` is a share of the killed mob's EP, not a flat rate.**
  `DeathHandler` pays `victimEP * epGain * (1 + 0.2*VIGOR + 0.2*GROWTH -
  0.2*LETHARGY)` to the killer's equipped item, so a knife accrues only while
  it lands the kill. `victimEP` is the victim's magicule + aura
  (`EnergyHelper.getBaseMaxEP`): a cow is 20 EP, a vanilla zombie 1400.
- **Cutting grant:** `event/CuttingBoardHandler.java` observes
  `PlayerInteractEvent.RightClickBlock` (server, main hand, not sneaking) and
  grants the flat `ep_per_cut` through `TensuraBridge.grantGearEp` when the
  held stack is in `tensura_delight:cutting_tools`, the clicked block's id
  string is `farmersdelight:cutting_board`, and the board's public
  `CuttingBoardBlockEntity.isEmpty()` / `isItemCarvingBoard()` say an
  ingredient sits there. Both tag references ship as `{"id": ..., "required":
  false}` - the shape Dungeon's Delight uses for its own optional references -
  so removing a contributor shrinks the union instead of failing the tag load.
  The event fires before Farmer's Delight's carve
  consumes the stored item, so an empty or display-only board costs nothing.
  Recipe compatibility between the held knife and the stored ingredient is not
  checked: a cut attempt with no matching recipe still earns the grant. The
  default `ep_per_cut` is 2.0, a tenth of a cow's 20 EP, so a stack of sixteen
  cuts is worth a few early kills to the knife; it reloads from
  `data/tensura_delight/ep_per_cut/*.json`.

**Gotchas**
- **Do not redistribute upstream material.** Tensura: Reincarnated and ManasCore
  have no public source; Brewin' And Chewin' and Dungeon's Delight publish
  artifacts under all-rights-reserved-style licences. Interoperate through
  public APIs and datapacks, never copy code or assets.
- Farmed food mods register items by id, so content added through the datapack
  is the default tool: a datapack entry that names an absent item fails the
  reload, which means every entry that mentions an optional mod's items must
  stay in a datapack gated on that mod's presence rather than in a blanket one.
- The optional deps are `ordering = "AFTER"`: registration order matters as
  soon as this mod's registrations name another mod's objects.
- ManasCore's module jars are exploded from `META-INF/jars` for compilation
  (`extractManascoreModules`); if Tensura types stop resolving, that task's
  input pin is the first thing to check.
- **runData boots FML like every other run.** The data run validates the
  toml's mandatory dependencies, so `runData` rides `prepareGametestMods`
  beside the game test runs: a clean checkout (and the CI drift gate) has no
  `run/mods` until that task stages the closure.
- **The meal grant table never looks items up in a registry.** Entries are
  keyed by item id string and matched against the eaten stack at runtime, so
  a file naming an absent optional mod's item is dead weight rather than a
  reload failure - the reload-failure rule above applies to tags and recipes,
  not to this table.
