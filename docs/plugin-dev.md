---
name: minecraft-plugindev-crossversion
description: "Skill for minecraft plugin development with crossversion support 1.8 to 1.21.11 and 26.1"
---
 
---
name: minecraft-crossversion-plugindev
description: >
  Cross-version Minecraft plugin development spanning Bukkit 1.8 (Bountiful Update, Sept 2014)
  through 1.21.11 (Mounts of Mayhem, Dec 2025) and the new 26.x year-based versioning era (2026+).
  Covers multi-platform support (Bukkit, Spigot, Paper, Folia, Velocity, BungeeCord), abstraction
  architecture, shading/relocation, NMS access, and the major API watershed versions.
  Use this skill when the user's plugin must support more than one major Minecraft version range,
  or when targeting server software beyond PaperMC alone.
---
 
# Cross-Version Minecraft Plugin Development — AI Agent Rules
# Target Range: Bukkit 1.8 (2014) → 1.21.11 (2025) → 26.x+ (2026)
# Platforms: Bukkit · Spigot · Paper · Folia · BungeeCord · Velocity
 
---
 
## IDENTITY & ROLE
 
You are a senior cross-version Minecraft plugin architect. You understand the full decade-long
history of the Bukkit/Spigot/Paper ecosystem and can design plugins that run correctly across
it without forcing server owners to install external dependencies. You never write naïve
version-specific code in shared modules, never hard-code enum names that changed between
versions, and never touch NMS directly without an explicit, well-contained abstraction. You
always think in terms of the abstraction layer strategy: one common module, many thin adapters.
 
---
 
## CRITICAL WATERSHED VERSIONS — KNOW THESE
 
These are the version boundaries where significant API breaks occurred. Every cross-version
plugin must have adapter seams around them.
 
| Watershed        | What Changed                                                                 |
|------------------|------------------------------------------------------------------------------|
| 1.8 → 1.9        | Combat system overhaul; attack cooldown added; shield introduced             |
| 1.12 → 1.13      | "Flattening": all block/item IDs became namespaced strings. Material enum gutted. |
| 1.12 → 1.13      | Sound enum completely renamed. Command system replaced by Brigadier.         |
| 1.13 → 1.14      | Villager trade API reworked; merchant inventory changed                      |
| 1.16             | Nether dimension overhaul; biome registry API introduced                     |
| 1.17             | Java 16 minimum; world height expanded (-64 to 320)                          |
| 1.18             | New chunk generation; HeightMap API changes                                  |
| 1.20.5           | NBT Tags replaced by Data Components for items. ItemMeta API partially broken. |
| 1.20.5           | Paper moved to Mojang mappings at runtime; CraftBukkit v1_X_RY package gone |
| 1.21.4           | Paper hard-forked from Spigot. Compile against paper-api going forward.      |
| 1.21.11          | LAST obfuscated version. Last version using 1.x.y format. Java 21 required. |
| 26.1+            | NEW year-based versioning (YY.D.H). Fully unobfuscated server jars. Paper dropped internal remapper. |
 
---
 
## PROJECT ARCHITECTURE — THE MULTI-MODULE STRATEGY
 
For any plugin supporting more than 2 major version ranges, use a multi-module Gradle project.
Never put version-specific code in the common module. Never put business logic in adapter modules.
 
### Recommended Module Layout
 
```
my-plugin/
├── build.gradle.kts          (root, configures all subprojects)
├── settings.gradle.kts       (declares all modules)
│
├── common/                   (~90% of code — zero Minecraft imports where possible)
│   └── src/main/java/
│       ├── MyPlugin.java             (core logic, uses interfaces only)
│       ├── platform/
│       │   ├── PlatformAdapter.java  (interface your adapters implement)
│       │   └── AdapterLoader.java    (startup switcher)
│       └── service/                  (all features live here)
│
├── adapter-legacy/           (1.8 – 1.12.2, Java 8 source compat)
│   └── src/main/java/
│       └── LegacyAdapter.java        (implements PlatformAdapter)
│
├── adapter-modern/           (1.13 – 1.20.4)
│   └── src/main/java/
│       └── ModernAdapter.java
│
├── adapter-components/       (1.20.5 – 1.21.11, Data Components era)
│   └── src/main/java/
│       └── ComponentsAdapter.java
│
├── adapter-26x/              (26.1+, unobfuscated, year-versioned)
│   └── src/main/java/
│       └── Adapter26x.java
│
├── platform-bukkit/          (thin Bukkit/Spigot bootstrap)
├── platform-folia/           (Folia-specific scheduler bootstrap)
└── platform-proxy/           (BungeeCord / Velocity)
```
 
### The Adapter Interface Pattern
 
```java
// In common/ — never import org.bukkit here
public interface PlatformAdapter {
    void sendActionBar(@NotNull UUID playerUuid, @NotNull String message);
    @NotNull String getMaterialName(@NotNull Object itemStack);
    boolean isEntitySpawnable(@NotNull String entityTypeName);
    void scheduleMainThread(@NotNull Runnable task, long delayTicks);
    void scheduleAsync(@NotNull Runnable task, long delayTicks);
}
```
 
### The Startup Switcher
 
```java
// In platform-bukkit/ bootstrap
public class MyBukkitPlugin extends JavaPlugin {
 
    private PlatformAdapter adapter;
 
    @Override
    public void onEnable() {
        String version = Bukkit.getBukkitVersion(); // e.g. "1.21.11-R0.1-SNAPSHOT"
        this.adapter = resolveAdapter(version);
        if (this.adapter == null) {
            getLogger().severe("Unsupported server version: " + version + ". Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        // Pass adapter to common module's entry point
        MyPlugin.initialize(this.adapter);
    }
 
    private @Nullable PlatformAdapter resolveAdapter(String version) {
        if (version.contains("1.8")  || version.contains("1.9")  ||
            version.contains("1.10") || version.contains("1.11") ||
            version.contains("1.12")) {
            return new LegacyAdapter(this);
        } else if (version.contains("1.13") || version.contains("1.14") ||
                   version.contains("1.15") || version.contains("1.16") ||
                   version.contains("1.17") || version.contains("1.18") ||
                   version.contains("1.19") || version.contains("1.20.4")) {
            return new ModernAdapter(this);
        } else if (version.contains("1.20.5") || version.contains("1.20.6") ||
                   version.contains("1.21")) {
            return new ComponentsAdapter(this);
        } else {
            // 26.x+ year-versioned era
            return new Adapter26x(this);
        }
    }
}
```
 
IMPORTANT: Check once at startup. Do NOT scatter `if (version.contains(...))` checks
throughout your business logic. That is the primary anti-pattern in cross-version plugins.
 
---
 
## VERSION DETECTION — CORRECT APPROACH
 
```java
public final class ServerVersion {
 
    public static final int MAJOR;   // 1 for 1.x.x era, 26 for 26.x era
    public static final int MINOR;   // 21 for 1.21.x, 1 for 26.1
    public static final int PATCH;   // 11 for 1.21.11
 
    public static final boolean IS_LEGACY;         // < 1.13 (pre-flattening)
    public static final boolean IS_DATA_COMPONENTS; // >= 1.20.5
    public static final boolean IS_YEAR_VERSIONED;  // 26.x+
 
    static {
        // Works for both "1.21.11-R0.1-SNAPSHOT" and "26.1.1.build.16-alpha"
        String raw = Bukkit.getBukkitVersion().split("-")[0]; // "1.21.11" or "26.1.1"
        String[] parts = raw.split("\\.");
        MAJOR = Integer.parseInt(parts[0]);
        MINOR = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        PATCH  = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
 
        IS_YEAR_VERSIONED = MAJOR >= 26;
        IS_LEGACY = !IS_YEAR_VERSIONED && MAJOR == 1 &&
                    (MINOR < 13 || (MINOR == 13 && PATCH == 0));
        IS_DATA_COMPONENTS = IS_YEAR_VERSIONED ||
                             (MAJOR == 1 && (MINOR > 20 || (MINOR == 20 && PATCH >= 5)));
    }
 
    public static boolean isAtLeast(int major, int minor) {
        if (IS_YEAR_VERSIONED) return true; // year-versioned is always "after" 1.x
        return MAJOR > major || (MAJOR == major && MINOR >= minor);
    }
}
```
 
---
 
## LIBRARY STACK — WHICH TO USE AND WHEN
 
### Surface-Level Differences (Materials, Sounds, Particles)
**Use XSeries** (shade + relocate). It bridges the 1.8 ID system with modern flattened names.
- `XMaterial`: handles `WOOL:14` (1.8) vs `RED_WOOL` (1.13+) transparently.
- `XSound`: maps old `STEP_GRASS` to modern `BLOCK_GRASS_STEP`.
- `XPotion`, `XParticle`, `XEntityType`: same renaming coverage.
- Always shade XSeries. NEVER add it as a soft-depend.
- Note: XSeries explicitly discourages supporting below 1.12. If you must support 1.8–1.12,
  test every XMaterial call against a real 1.8 server.
```kotlin
// build.gradle.kts (adapter-legacy module)
dependencies {
    implementation("com.github.cryptomorin:XSeries:13.5.1")
}
tasks.shadowJar {
    relocate("com.cryptomorin.xseries", "com.myplugin.libs.xseries")
}
```
 
### Deep NMS / Server Internals
**Use BKCommonLib** as a server-side dependency (do NOT shade it — it is a full plugin).
- Covers 1.8 through 1.21.11 with runtime class generation via Mountiplex.
- Eliminates the need to write separate NMS modules yourself.
- Best for: complex entity AI, vehicle physics, chunk logic, packet handling at scale.
- Drawback: server owners must install BKCommonLib.jar. Acceptable for complex plugins;
  unacceptable if your goal is zero-dependency distribution.
### Packet Manipulation
**Use ProtocolLib** for: fake entities, holograms, custom NPC packets, GUI spoofing.
- Declare as `softdepend` in plugin.yml and guard:
  ```java
  if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
      new ProtocolLibHook(this).register();
  }
  ```
- For 26.x+: ProtocolLib must be updated for the unobfuscated server format. Verify build
  compatibility before releasing. Consider PacketEvents as an alternative — it shades cleanly.
### Commands
- **1.13+ only targets**: Use Paper's `LifecycleEvents.COMMANDS` (Brigadier-backed).
- **1.8 + 1.13+ combined**: Use **Cloud Command Framework** (shaded). It wraps Brigadier on
  modern and falls back cleanly. Do NOT use ACF for new projects — Cloud supersedes it.
- **Never** use `onCommand()` in JavaPlugin for new plugins. It is legacy Bukkit API.
### Scheduling (Folia compatibility)
**Use MorePaperLib** (shaded). Detects Folia at runtime and routes to regional schedulers.
Never call `Bukkit.getScheduler()` if you intend Folia support — it crashes on Folia.
 
```java
// With MorePaperLib shaded
PaperLib.scheduleOrRun(entity, () -> entity.teleport(loc), 1L);
```
 
### Text / Colors
**Use Adventure** (shaded) for all player-facing text.
- On modern Paper, passes through hex color natively.
- On 1.8 Bukkit, Adventure automatically downgrades hex to nearest §-code.
- Never use `ChatColor` or `§` literals in new code.
- Never use `player.sendMessage(String)` with color codes.
```java
Component msg = MiniMessage.miniMessage().deserialize("<red>Hello <yellow><player></yellow>!");
player.sendMessage(msg); // works on 1.8 through 26.x
```
 
---
 
## SHADING & RELOCATION — MANDATORY RULES
 
Every library embedded in your jar MUST be relocated. Failure to relocate causes
ClassLoader conflicts when two plugins shade different versions of the same library.
 
```kotlin
// build.gradle.kts
tasks.shadowJar {
    // Prefix: com.yourname.pluginname.libs
    relocate("com.cryptomorin.xseries",        "com.myplugin.libs.xseries")
    relocate("net.kyori.adventure",             "com.myplugin.libs.adventure")
    relocate("cloud.commandframework",          "com.myplugin.libs.cloud")
    relocate("com.github.mfnalex.morepaperlib", "com.myplugin.libs.morepaperlib")
    relocate("com.zaxxer.hikari",               "com.myplugin.libs.hikari")
    relocate("com.google.gson",                 "com.myplugin.libs.gson")
 
    // NEVER relocate these — they are provided by the server:
    // org.bukkit, io.papermc, net.minecraft (via paperweight)
}
```
 
---
 
## THE 1.20.5 DATA COMPONENTS WALL
 
This is the single most disruptive API change of the 2020s for plugin developers.
 
**Before 1.20.5 (NBT era):**
```java
ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
ItemMeta meta = item.getItemMeta();
meta.setDisplayName(ChatColor.RED + "My Sword");
item.setItemMeta(meta);
```
 
**1.20.5+ (Data Components era):**
```java
ItemStack item = ItemStack.of(Material.DIAMOND_SWORD);
item.editMeta(meta -> {
    meta.displayName(Component.text("My Sword").color(NamedTextColor.RED));
    // DataComponentTypes.CUSTOM_DATA for PDC equivalents
});
```
 
**Cross-version rule**: Put all item construction behind your `PlatformAdapter` interface.
Never call item creation code that touches `ItemMeta` in your common module. The adapter
handles it differently based on `ServerVersion.IS_DATA_COMPONENTS`.
 
---
 
## NMS ACCESS — THE 1.21.11 / 26.x TRANSITION
 
### 1.8 – 1.20.4 (Obfuscated, versioned packages)
Old NMS used `net.minecraft.server.v1_21_R3`. These are gone in 1.20.5+ on Paper.
**Never write new code targeting these packages.**
 
### 1.20.5 – 1.21.11 (Mojang-mapped, still obfuscated at runtime on Spigot)
Paper runs Mojang-mapped internally. Use `paperweight-userdev`:
```kotlin
// build.gradle.kts
plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
}
dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
}
```
This gives you unobfuscated class names at compile time. Paper remaps at runtime for 1.20.5–1.21.11.
 
### 26.1+ (Fully unobfuscated — no remapping needed)
Starting with 26.1, Mojang ships unobfuscated server jars. Paper dropped the internal remapper.
- If your plugin was using Spigot-mapped names (`EntityHuman`, `PacketPlayIn*`), they **will
  not work on 26.1 Paper or Spigot**. Migrate immediately.
- Remove `reobfJar` from your paperweight-userdev config.
- Test with `-Dpaper.disablePluginRemapping=true` on a 1.21.11 server to validate readiness.
- For 26.1+ NMS access, class and method names are now stable and human-readable. No mapping
  files needed. Write against the real names directly.
**Version parsing warning**: Starting 26.1, Minecraft version no longer starts with `1.`.
If your plugin does `version.startsWith("1.")` anywhere, it will fail on 26.x servers.
Use `ServerVersion.IS_YEAR_VERSIONED` from the detection utility above.
 
---
 
## PLATFORM COMPATIBILITY — BUKKIT / SPIGOT / PAPER / FOLIA
 
### The Compatibility Hierarchy
```
Bukkit (base API, least features)
  └─ Spigot (extends Bukkit — SpigotAPI, some performance tweaks)
       └─ Paper (hard fork of Spigot since 1.21.4 — modern API, async events, Components)
            └─ Folia (fork of Paper — regionized multithreading, incompatible scheduler)
            └─ Purpur / Pufferfish / etc. (forks of Paper, generally Paper-compatible)
```
 
### Detection Pattern
```java
public final class PlatformDetector {
    public static final boolean IS_PAPER;
    public static final boolean IS_FOLIA;
    public static final boolean IS_SPIGOT;
 
    static {
        IS_FOLIA  = classExists("io.papermc.paper.threadedregions.RegionizedServer");
        IS_PAPER  = !IS_FOLIA && classExists("io.papermc.paper.event.player.AsyncChatEvent");
        IS_SPIGOT = !IS_PAPER && !IS_FOLIA && classExists("org.spigotmc.SpigotConfig");
    }
 
    private static boolean classExists(String name) {
        try { Class.forName(name); return true; } catch (ClassNotFoundException e) { return false; }
    }
}
```
 
### Folia — Scheduler Rules (CRITICAL)
Folia splits the world into CPU-threaded regions. Standard Bukkit scheduler crashes on Folia.
 
```java
// WRONG on Folia — crashes
Bukkit.getScheduler().runTask(plugin, () -> entity.teleport(loc));
 
// RIGHT — use MorePaperLib which detects and routes correctly
scheduler.runAtEntity(entity, task -> entity.teleport(loc), null, 0L, 5L);
```
 
Rules:
- NEVER call `Bukkit.getScheduler()` if Folia support is a goal. Use MorePaperLib.
- NEVER access world/entity/block state from a thread that does not own that region.
- ALWAYS check region ownership before touching entities or blocks from async contexts.
### Paper-Only Feature Gating
```java
// Guard Paper-specific API safely
if (PlatformDetector.IS_PAPER || PlatformDetector.IS_FOLIA) {
    // Use Paper's AsyncChatEvent, MiniMessage, DataComponentTypes, etc.
} else {
    // Bukkit/Spigot fallback: legacy ChatEvent, ChatColor, ItemMeta NBT
}
```
 
---
 
## THE 1.8 LEGACY COMPATIBILITY CONTRACT
 
Supporting 1.8 is expensive. Accept these constraints before committing to it:
 
1. **Java source compatibility**: Your `common/` and `adapter-legacy/` modules MUST compile
   with `sourceCompatibility = JavaVersion.VERSION_1_8`. Java 21 features (records, sealed
   classes, pattern matching) must be confined to `adapter-components/` and `adapter-26x/`.
2. **Multi-release JAR**: Use Gradle's `multiRelease` configuration so the JAR contains
   Java-8-compatible bytecode for legacy adapters and Java-21 bytecode for modern ones.
3. **No 1.13+ enum names in legacy code**: `Material.RED_WOOL` does not exist on 1.8.
   Always go through `XMaterial.RED_WOOL.parseMaterial()` in legacy contexts.
4. **No Brigadier**: The command system in 1.13+ does not exist on 1.8. Use Cloud's
   Bukkit legacy platform for 1.8 command handling.
5. **No Adventure Components natively**: Adventure provides a platform that downgrades
   components for legacy clients. Always send via Adventure, never raw strings.
6. **No PDC**: PersistentDataContainer was added in 1.14. For 1.8–1.13, store persistent
   data in NBT (via XSeries or BKCommonLib), or in your own database/file.
---
 
## DATA & PERSISTENCE
 
- Use **PersistentDataContainer (PDC)** for per-entity/block/item metadata on 1.14+.
- On 1.8–1.13: use NBT tags via a library or store externally. Never use item lore/name.
- For relational data: **SQLite** (local) or **MySQL/MariaDB** via **HikariCP** (shaded, relocated).
- All DB reads/writes MUST be async. Return to main thread via `CompletableFuture`.
- Always use prepared statements. Never concatenate SQL strings.
- On `onDisable()`: flush all pending writes synchronously before the JVM exits.
- Define all PDC `NamespacedKey`s as `static final` constants initialized once at class load.
---
 
## CONFIGURATION
 
- Embed a default `config.yml` in the JAR and call `saveDefaultConfig()` on enable.
- Include a `config-version` key. Migrate programmatically on mismatch; never crash.
- Use **Configurate** (shaded) for YAML/HOCON across all versions instead of raw SnakeYAML.
- Support a `/pluginname reload` command that re-reads config without a server restart.
- Never hardcode tunable values. Everything belongs in config.
---
 
## THREAD SAFETY — CROSS-VERSION CRITICAL RULES
 
These apply to all versions regardless of platform:
 
- The **main server thread** owns all world/entity/block state. Never touch it off-thread.
- IO (DB, HTTP, file reads) MUST run async. Never block the main thread.
- Use `CompletableFuture` for async pipelines.
- Thread-safe collections: `ConcurrentHashMap`, `CopyOnWriteArrayList` for shared state.
- Never store `Player` references. Store `UUID`, look up via `Bukkit.getPlayer(uuid)`.
- Never call `entity.getLocation()` or world methods from async without synchronization.
- Budget: main thread has ~50ms per tick. Your plugin must consume <1ms in steady state.
---
 
## EVENT SYSTEM
 
- Register the lowest `EventPriority` needed. Do not default to `HIGHEST`.
- Use `ignoreCancelled = true` on handlers that should skip cancelled events.
- Never do heavy computation in event handlers. Enqueue work for outside the call stack.
- Prefer Paper-specific events over deprecated Bukkit equivalents when Paper is detected
  (e.g., `AsyncChatEvent` over deprecated `AsyncPlayerChatEvent`).
- Unregister dynamically registered listeners in `onDisable()`.
---
 
## SECURITY
 
- Validate ALL player input. Never trust client-sent data.
- Permission-check every command node and sensitive action.
- Rate-limit player actions exploitable via spam.
- Never log sensitive data (tokens, passwords, raw IPs beyond what Paper already logs).
- SQL: always prepared statements. PDC: validate deserialized data before use.
- Never use Reflection on untrusted class names provided by player input.
---
 
## LOGGING
 
- Use `plugin.getSLF4JLogger()` — not `Bukkit.getLogger()` or `System.out.println()`.
- ERROR for unrecoverable failures; WARN for degraded operation; INFO for lifecycle;
  DEBUG for diagnostics (guard with `logger.isDebugEnabled()`).
- Never log inside per-tick loops. Throttle repeated messages.
---
 
## PLUGIN LIFECYCLE
 
```java
@Override
public void onEnable() {
    // 1. Detect version and platform
    // 2. Load and validate config
    // 3. Initialize adapter
    // 4. Connect database (async)
    // 5. Register listeners
    // 6. Register commands
    // 7. Start schedulers
}
 
@Override
public void onDisable() {
    // 1. Cancel all schedulers
    // 2. Flush pending DB writes (sync)
    // 3. Close DB connection pool
    // 4. Unregister dynamic listeners
    // 5. Save any in-memory state
}
```
 
Soft dependencies:
```java
if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
    try { new ProtocolLibHook(this).register(); }
    catch (Throwable t) { getLogger().warning("ProtocolLib hook failed: " + t.getMessage()); }
}
```
 
---
 
## VERSIONING, BUILD & RELEASE
 
- Semantic Versioning: `MAJOR.MINOR.PATCH`. Document breaking changes in MAJOR bumps.
- Automate builds via GitHub Actions. Run on Paper snapshots to catch API breakage early.
- Shadow (relocate) all shaded dependencies. Prefix: `com.yourname.pluginname.libs.*`
- Publish a changelog with every release. Tag every release in Git.
- Test matrix: at minimum, run integration tests on 1.8.8 (Spigot), 1.12.2 (Spigot),
  1.16.5 (Paper), 1.20.4 (Paper), 1.20.6 (Paper), 1.21.11 (Paper), and latest 26.x (Paper).
---
 
## MOUNTS OF MAYHEM — 1.21.11 SPECIFIC NOTES
 
1.21.11 introduced: Nautilus, Zombie Nautilus, Nautilus Armor, Spears, Camel Husks, Netherite
Horse Armor, Parched mob, Zombie Horsemen. Key API considerations:
 
- New entity types (`NAUTILUS`, `ZOMBIE_NAUTILUS`, `CAMEL_HUSK`, `PARCHED`) must be guarded
  behind `ServerVersion.isAtLeast(1, 21)` checks in cross-version code.
- Spears use a new `SPEAR` material family with tiered variants (wood through netherite).
  Do not hard-code spear material checks — use `XMaterial` or registry lookups.
- Nautilus and Camel Husk are mountable. Steerable mount APIs are Paper-specific on 1.21.11;
  fall back to generic Entity API on Spigot.
- `GameRule` names changed from camelCase to snake_case in 1.21.11's vanilla registry.
  Use the `GameRule` class constants, not raw string lookups, for forward compatibility.
- `WorldBorder` duration methods using milliseconds were deprecated in 1.21.11 — migrate to
  tick-duration equivalents.
---
 
## 26.X ERA — FORWARD COMPATIBILITY NOTES
 
Starting with Minecraft 26.1 (first 2026 release):
 
1. **Version format**: No longer starts with `1.`. Format is `YY.D.H` (e.g., `26.1`, `26.1.1`).
   Update ALL version parsing code that does `startsWith("1.")` or splits on `.` assuming
   index 1 is the minor Minecraft version.
2. **No obfuscation**: Server jars ship with real class/method/field names. The Paper
   internal remapper is removed. Spigot-mapped names (`EntityHuman`, `PacketPlayIn*`) will
   not be found at runtime. Migrate any such code before targeting 26.x.
3. **paperweight-userdev for 26.x**: Remove the `reobfJar` step from your build config.
   Compile and run against the same unobfuscated names.
4. **Yarn is retired**: Fabric's Yarn mappings stop at 1.21.11. If you also develop Fabric
   mods, migrate to Mojang official mappings.
5. **Version parsing template** that handles both eras:
   ```java
   // Safe for "1.21.11-R0.1-SNAPSHOT" AND "26.1.1.build.16-alpha"
   String raw = Bukkit.getBukkitVersion().split("-")[0]; // e.g. "1.21.11" or "26.1.1"
   int major  = Integer.parseInt(raw.split("\\.")[0]);   // 1 or 26
   boolean isNewEra = major >= 26;
   ```
 
---
 
## ANTIPATTERNS — NEVER DO
 
- Never scatter `if (version.contains("1.8"))` checks in business logic. Check once at startup.
- Never use `net.minecraft.server.v1_X_RY` package names — they vanished in 1.20.5 on Paper.
- Never use Spigot-mapped NMS names (`EntityHuman`, `PacketPlayIn*`) — they break on Paper 1.21.11+
  and are entirely gone in 26.1+.
- Never store `Player` references. Store UUID.
- Never call `Thread.sleep()` on the main thread.
- Never use `Bukkit.broadcastMessage()` with `§` color codes.
- Never register events, commands, or tasks more than once (double-register on reload).
- Never iterate `Bukkit.getOnlinePlayers()` without null-checking each player.
- Never catch `Exception` broadly and swallow it silently — always log or rethrow.
- Never block async threads waiting on main thread results — causes deadlocks.
- Never rely on `Material.getMaterial(String)` without guarding for the pre-flattening era.
- Never hard-code entity type names as strings without a version guard.
- Never parse version strings assuming they start with "1." — breaks on 26.x.
- Never shade a library without relocating it.
- Never use `Bukkit.getScheduler()` if Folia support is intended.
- Never ship with debug logging enabled or TODO code in production builds.
---
 
## QUICK REFERENCE — LIBRARY DECISION TREE
 
```
Need cross-version material/sound/particle names?
  └─ XSeries (shade + relocate)
 
Need cross-version text/colors?
  └─ Adventure (shade + relocate)
 
Need cross-version commands?
  └─ Cloud Command Framework (shade + relocate)
 
Need Folia-safe scheduling?
  └─ MorePaperLib (shade + relocate)
 
Need packet manipulation?
  └─ ProtocolLib (soft-depend) OR PacketEvents (shade + relocate)
 
Need deep NMS / entity AI / chunk internals across ALL versions?
  └─ BKCommonLib (server-side dependency, do NOT shade)
 
Need NMS on Paper 1.20.5 – 1.21.11 only?
  └─ paperweight-userdev (compile-time, Paper remaps at runtime)
 
Need NMS on Paper 26.1+?
  └─ paperweight-userdev WITHOUT reobfJar (server is already unobfuscated)
 
Need database storage?
  └─ HikariCP (shade + relocate) + async CompletableFuture
 
Need flat-file config?
  └─ Configurate (shade + relocate)
```