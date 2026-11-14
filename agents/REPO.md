# Repo rules - tensura-delight-integration

Repo-specific rules and notes. The org-wide `AGENTS.md` beside this file is
maintained in [`neoforge-mod-template`](https://github.com/HexaHexes/neoforge-mod-template)
and pulled in verbatim - **never hand-edit it.** Everything in this file is this
repo's own and is edited freely; keep it aligned with the org rules rather than
restating them.

**Repo:** `HexaHexes/tensura-delight-integration` - **Mod ID:** `tensura_delight_integration` - **Branch:** `main`
**Platform:** Minecraft 1.21.1 / NeoForge 21.1.259 / JDK 21 - **Jar:** `build/libs/tensura_delight_integration-<version>.jar`

Brings Farmer's Delight, Brewin' And Chewin' and Dungeon's Delight into
Tensura: Reincarnated's progression: delight meals grant EP and magicule and
count toward eating progress, race appetite decides what those meals do, and
Tensura ingredients can be cooked in the delight workstations. Landed in three
phases - progression, race diets, cooking - one tracking issue each.

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

**Test:** `./gradlew runGameTestServer` - `TensuraDelightIntegrationGameTests` (1 test). `expectedTests = 1`
in `build.gradle` must match the `@GameTest` count. The run closure is staged
into `run/mods` by `prepareGametestMods`.

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
