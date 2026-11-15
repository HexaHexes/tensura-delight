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

**Test:** `./gradlew runGameTestServer` - `TensuraDelightGameTests` (6 tests). `expectedTests = 6`
in `build.gradle` must match the `@GameTest` count. The run closure is staged
into `run/mods` by `prepareGametestMods`.

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
