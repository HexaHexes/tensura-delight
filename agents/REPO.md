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
knife and cleaver climbs the sword materials' gear ladder. Landed in
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

**Test:** `./gradlew runGameTestServer` - `TensuraDelightGameTests` (19 tests). `expectedTests = 19`
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
- **`monster_meals` is a grant gate, not a refusal gate.** It unions
  `#dungeonsdelight:monster_foods` and `#tensura:raw_monster_consumables`,
  both `required: false` so a pack may drop either. Membership only picks
  `monster_eater` out of `gateFor`, so a human who eats raw monster meat is
  simply paid nothing. `Appetite.refusesUse` is keyed on the delight
  namespaces (`farmersdelight`, `brewinandchewin`, `dungeonsdelight`) and
  still does not see `tensura:` at all, so a `does_not_eat` race keeps the
  nutrition and effects of Tensura's own food. Widening
  `DELIGHT_NAMESPACES` to `tensura` would refuse all forty-four Tensura
  foods rather than these fourteen, so it is a separate decision and was
  deliberately left out of the raw-meats work.
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
- **Same-path tag files union, they do not shadow.** Both the Farmer's Delight
  and the Dungeon's Delight jar ship `data/farmersdelight/tags/item/drinks.json`
  with different contents (FD: milk_bottle, apple_cider, melon_juice, hot_cocoa;
  DD: six monster drinks) - and that is *not* a collision.
  `TagLoader.load` gathers a path with `listMatchingResourceStacks`, which
  returns every pack's copy of that path as a stack, then loops the whole stack
  and appends each `TagFile`'s entries to one accumulator. It calls
  `List.clear()` only when a file sets `"replace": true`; neither host file
  does, so `farmersdelight:drinks` resolves to all ten. `replace` defaults to
  `false`, which is why the shipped appetite tags carry no `replace` key at all
  (`AppetiteTagsContent.validate()` fails the datagen run if one appears): they
  union with whatever a pack contributes rather than overriding it.
  Consequence for this repo: the shipped `tensura_delight:drinks` simply
  references `#farmersdelight:drinks` beside
  `#brewinandchewin:fermented_drinks`, both with `{"id": ..., "required":
  false}`. An explicit list of the ten items was tried first, on the mistaken
  belief that the two files shadowed each other; it produced the same ten items
  but would have gone stale the moment a host added a drink, so the reference
  replaced it. GameTest `appetite_item_tags_resolve` pins the union: it asserts
  `dungeonsdelight:liveroot_beer`, which only reaches our tag through FD's
  copy of that shared path.
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
- **The table:** `recipe/StationRecipes.java` builds the twenty-four recipes as
  gson JSON (no Minecraft classes, so JUnit runs it headless);
  `datagen/StationRecipesContent.java` round-trips every file through gson
  and hands the map to a `JsonFilesProvider` in `TensuraDelight.gatherData`,
  so the JSON ships only from `./gradlew runData` and table and output
  cannot drift apart. Each recipe mirrors the host's own recipe of the same
  dish, keeping its experience, tab, category, temperature and tool:
  - Cooking pot (3): `cooking/cattledeer_stew` - `tensura:cattledeer_beef` +
    carrot + potato -> `farmersdelight:beef_stew`;
    `cooking/spear_toro_fish_stew` - `tensura:raw_spear_toro_meat` +
    tomato sauce + onion -> `farmersdelight:fish_stew`;
    `cooking/baked_cod_stew` - `tensura:raw_megalodon_meat` + potato + egg +
    tomato -> `farmersdelight:baked_cod_stew`. The host reads
    `c:foods/raw_cod`, which no Tensura item joins, so the first slot swaps
    explicitly.
  - Cutting board (14): one recipe per raw monster consumable of Tensura's
    `#tensura:raw_monster_consumables`, all carrying Farmer's Delight's own
    knife tool shape. The land meats (`cattledeer_beef`,
    `raw_armorsaurus_meat`, `raw_blade_tiger_meat`, `raw_giant_bat_meat`,
    `raw_serpent_meat`) fillet to 2x `farmersdelight:minced_beef`; the four
    sea meats (`raw_charybdis_meat`, `raw_megalodon_meat`,
    `raw_spear_toro_meat`, `raw_sissie_meat`) to 2x
    `farmersdelight:salmon_slice` and the two fins (`spear_toro_fin`,
    `sissie_fin`) to 1x; the chitin legs (`giant_ant_leg`,
    `knight_spider_leg`) crush to `minecraft:bone_meal` and `chilled_slime`
    congeals to `minecraft:slime_ball`. Every output bridges into a chain
    some host already cooks, and none of them is tabled as a meal.
  - Keg (2): `fermenting/kippers_from_spear_toro` - 2x
    `tensura:raw_spear_toro_meat` + dried kelp + the empty fourth slot ->
    2x `brewinandchewin:kippers`;
    `fermenting/jerky_from_blade_tiger` - 3x `tensura:raw_blade_tiger_meat`
    + the empty fourth slot -> 3x `brewinandchewin:jerky`, temperature 4.
    The host's `brewinandchewin:foods/jerky_meat` reaches only vanilla
    meat, so all three slots swap rather than keep a tag this mod's
    ingredients never satisfy - which is also what makes the recipe new.
  - Monster Pot (5): `monster_cooking/spider_bisque_from_knight_spider` -
    2x `tensura:knight_spider_leg` + rotgourd slice + spider extract ->
    `dungeonsdelight:spider_bisque`;
    `monster_cooking/spider_salmagundi_from_knight_spider` - fermented
    spider eye + 2x `tensura:knight_spider_leg` + onion ->
    `dungeonsdelight:spider_salmagundi`;
    `monster_cooking/ghoulash_from_giant_bat` - slime noodles +
    `tensura:raw_giant_bat_meat` + rotten tomato ->
    `dungeonsdelight:ghoulash`, swapping the host's
    `#dungeonsdelight:fleshes` slot;
    `monster_cooking/gyudon_from_armorsaurus` - `tensura:raw_armorsaurus_meat`
    + `#dungeonsdelight:sculk_cheese` + cooked rice + fried egg + onion ->
    `dungeonsdelight:gyudon` at experience 2.0, swapping the host's
    `gritty_flesh`;
    `monster_cooking/salt_soaked_stew_from_serpent` - 2x
    `tensura:raw_serpent_meat` + salmon + beetroot ->
    `dungeonsdelight:salt_soaked_stew`, swapping the host's `brined_flesh`.
- **Why gyudon and not rubaboo:** Dungeon's Delight's own `rubaboo` carries
  no `recipe_book_tab` - its serializer reads the field as
  `optionalFieldOf(..., MONSTER_MISC)`, so a missing tab is legal and lands
  the dish in Monster Misc - and adds a `cookingtime: 100`.
  `StationRecipesTest.eachStationCarriesTheShapeItsHostReads` demands the tab
  for every Monster Pot recipe, so the two disagree. `gyudon` is the
  tab-bearing dish whose first ingredient is the very `gritty_flesh` a
  rubaboo copy would have swapped, so the intended swap survives intact and
  the test stays as written. Adding the missing tab to a rubaboo copy was
  rejected as a one-key divergence from the host; shipping rubaboo
  byte-for-byte and weakening the test was rejected for loosening a rule
  every future recipe would inherit.
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
  `MealGrants.DEFAULT_MEALS`, and later all fourteen raw monster
  consumables plus the five new station results, bringing the table to 31
  entries. The raw meats scale from their own nutrition - a five nutrition
  cut is 10 EP and 50 magicule, the seven nutrition Blade Tiger 14 and 70, a
  one nutrition fin 2 and 10 - all well under `max_meal_ep`. Listing them
  only says they *may* pay: which race is paid is the `monster_meals` tag's
  business, not this table's. `farmersdelight:minced_beef` and the other
  cutting outputs are deliberately absent: a cutting result is an ingredient
  for further cooking, and Farmer's Delight's own minced beef is not tabled
  either.
- **Gotcha:** Tensura's *land* meats are in no `c:foods/raw_*` tag at all,
  while its six sea meats sit in `c:foods/raw_fish` - which Farmer's
  Delight's `c:foods/safe_raw_fish` already unions, so FD's own `fish_stew`
  and `squid_ink_pasta` accept them with no help from this repo. FD's
  `baked_cod_stew` reads `c:foods/raw_cod` instead, which none of them
  join, which is exactly why `cooking/baked_cod_stew` swaps that slot
  explicitly. The land meats reach the beef chain only by being cut to
  `minced_beef` first, which is what the fourteen cutting recipes are for.
  Never "fix" a Tensura ingredient into a `c:` tag from this repo: those
  files belong to the host mods, and editing one changes every recipe that
  reads it rather than just ours.
- **Driving `matches()` headless:** all four stations are driven for real in
  the GameTests. `RecipeWrapper` takes an `IItemHandler` in this NeoForge, not
  a `SimpleContainer`, so the cooking pot and Monster Pot inputs wrap a
  6-slot `ItemStackHandler` (both `matches` scan exactly six slots); the
  cutting board uses `CuttingBoardRecipeInput(item, knife)`; the keg uses an
  inline `KegRecipeWrapper` over 4 slots returning
  `AbstractedFluidStack.EMPTY`, because a recipe with no fluid ingredient
  demands an empty keg fluid.
- **Recipe-book unlocks:** this repo owns `data/tensura_delight/advancement/recipes/**` -
  one unlock per recipe-book recipe, mirroring the hosts' advancement shape
  (`parent` = `minecraft:recipes/root`, an `inventory_changed` ingredient
  criterion OR `recipe_unlocked`, `rewards.recipes` = the recipe). All three
  stations are `RecipeBookMenu`s, so without these their books list the hosts'
  dishes but never ours. The three cooking unlocks ship unconditionally; the
  two keg unlocks gate on `brewinandchewin` and the five Monster Pot
  unlocks on `dungeonsdelight`, each exactly where its recipe gates - an
  advancement may never outlive its recipe, and `StationAdvancements.build()`
  throws if the two ever disagree.
  **No cutting unlock:** the cutting board has no recipe book and Farmer's
  Delight ships none either. `recipe/StationAdvancements.java` holds the
  table (gson only, headless); `datagen/StationAdvancementsContent.java`
  round-trips it and hands it to the same `JsonFilesProvider` in
  `TensuraDelight.gatherData`.

**Knife gear evolution (phase 4)**
- **Path ownership:** this repo owns `data/tensura_delight/gear_existence/**`
  (one file per knife at `data/tensura_delight/gear_existence/<item namespace>/<item path>.json`),
  `data/tensura_delight/tags/item/cutting_tools.json` and
  `data/tensura_delight/ep_per_cut/**`. Never write into `data/tensura/**` or
  `data/gearevolution/**`: `tensura-iaf-growth` and `tensura-iss-growth` own
  paths there, and their registry rules bind this repo's entries too.
- **Ladder table:** `gear/KnifeLadders.java` holds the five ladders with rungs
  in climb order, each ladder declaring the host mod its files are gated on
  (`Ladder.requiredMod`, stamped onto every rung); `datagen/KnifeGearContent.java`
  emits them through the `JsonFilesProvider` round trip, together with the
  cutting tag and the cut rate. Family order was read from the staged jars,
  so no family needed the unordered-terminal exception:
  - Farmer's Delight mirrors the pack's vanilla sword ladder: flint below
    stone, to iron, gold parallel onto diamond, diamond to netherite. Tiers
    come from FD's `ModMaterials.FLINT` and the vanilla `Tiers` its five
    knives are constructed with.
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
- **Gates and rates: the sword ladder's own values, verbatim.** Every family
  climbs the pack's sword material ladder with gates read straight off the
  chain the swords climb - gearevolution's bundled ladder as
  tensura-iaf-growth overrides it: wood/flint 200, stone 500, iron and
  golden 2600, diamond 7000, netherite 10000 (the bundled 13500 replaced
  outright). Gates are global per material, so an iron knife, an iron fillet
  knife and an iron-tier cleaver all evolve at the same 2600. Dungeon's
  Delight's own materials interleave between iron and diamond (zanite 3200,
  ironwood 3900, steel 4500, steeleaf 5100, knightmetal 5700, gravitite
  6400) and between diamond and netherite (stained 8000, fiery 9000), in
  DD's own material order, while the vanilla families only ever climb
  vanilla targets - a Farmer's Delight knife has no zanite knife to become.
  **EP is cumulative and carries across an evolution** (`DeathHandler.gearGetEP`
  rebuilds the new stack with the old component patch and never re-stamps
  EP), so two rungs sharing a band in sequence would promote on the spot:
  golden sits parallel to iron at 2600 and both climb into diamond, exactly
  like the pack's golden sword, never after iron. Each family's final rung
  **terminals at its own material's gate**: netherite or neptunium at
  10000, Dungeon's Delight's knife line at fiery's 9000 - `maxEP` there,
  no `evolution` (Tensura's codec keeps evolution `Optional`; the
  `top_rungs_terminal_at_their_family_gate` GameTest proves the real
  registry took it). All shipped gates sit below 18000, so every `epGain`
  is 0.003; a datapack file may omit the key to take the codec's 0.03
  default. `KnifeLaddersTest.gatesAreTheSwordsOwnValues` pins every one of
  the 32 rungs' gates by literal, so a drifted value fails by name.
- **Optional families are condition-gated in their data, not carved out of
  the table.** Each ladder declares its host mod, and `KnifeGearContent`
  writes it into every rung file as
  `"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": ...}]`
  (the same shape `StationRecipes` uses). NeoForge patches
  `RegistryDataLoader` with `ConditionalOps.createConditionalCodec`, so an
  unmet condition skips the whole registry entry (debug-logged as "Skipping
  loading registry entry ... conditions were not met") instead of decoding
  it; the `gear_existence_covers_every_knife` GameTest asserts present-if
  and only-if the host is on the run closure - its proof of the skip is the
  gametest closure, which stages no Aquaculture. **Aquaculture 2 and
  Aquaculture Delight were dropped from the pack on 2026-11-15**: their
  fillet ladders still ship and simply skip at load, re-activating if the
  mods return; the mod ids (`aquaculture`, `aquaculturedelight`) were read
  from the jars, and no compile dependency exists - the ladders are data
  only. Farmer's Delight is required by the pack, so its knives stay
  unconditional. The parser validates the condition shape (exactly one
  `neoforge:mod_loaded` entry, namespace-only `modid`, no other condition
  type), so a malformed file fails datagen instead of shipping.
- **The kunai is deliberately not a rung.** No mod in the pack tags
  `tensura:kunai` as a knife - verified by scanning every jar's tag files -
  so the old netherite-into-kunai hand-off is gone: a kitchen knife's story
  ends at its family's top material gate. If a future pack wants the
  hand-off back, the fix is a tag file putting `tensura:kunai` into
  `c:tools/knife` plus a ladder entry, not a special case in code.
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
