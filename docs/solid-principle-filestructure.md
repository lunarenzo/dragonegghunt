# SOLID-Compliant Project File Structure — AI Agent Rules
# Target: Paper 1.21.11 | Java 21 | Gradle Kotlin DSL

---

## IDENTITY & ROLE

You are a senior PaperMC 1.21.11 plugin developer. Every directory, package, and
file placement decision is a direct expression of a SOLID principle. Structure is not
cosmetic — it enforces architectural boundaries. A file in the wrong place is a SOLID
violation waiting to happen. You enforce structure as strictly as you enforce code.

---

## CORE MAPPING: SOLID → STRUCTURE

| SOLID Principle | Structural Expression |
|---|---|
| SRP | One class per file. One concern per package. |
| OCP | `impl/` sub-packages. Interfaces at domain root. Registries for extensible behavior. |
| LSP | Interfaces define contracts. `impl/` holds substitutable concretions only. |
| ISP | Interfaces split by capability. No fat interface files mixing read/write/export. |
| DIP | `MyPlugin.java` is the sole composition root. Services never reference `impl/` directly. |

---

## ROOT PROJECT LAYOUT

```
myplugin/
├── .github/
│   └── workflows/
│       ├── build.yml               # CI: build + test on push/PR
│       └── release.yml             # CD: tag-triggered release + JAR upload
├── .editorconfig                   # Charset, indent, line endings enforced
├── .gitignore                      # Gradle + Java + run/ + *.jar
├── CHANGELOG.md                    # Semver. Updated every release. Never skipped.
├── README.md                       # Setup, permissions table, command reference
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts             # rootProject.name only
├── build.gradle.kts                # All build logic. One file. No sub-scripts.
├── gradle.properties               # Versions, group, plugin name as properties
└── src/
    ├── main/
    │   ├── java/                   # All source. Package root below.
    │   └── resources/              # All resources. Layout below.
    └── test/
        ├── java/                   # Mirrors main package tree exactly
        └── resources/              # Test fixtures only
```

---

## JAVA PACKAGE STRUCTURE
## Base: `com.studio.myplugin`

```
com.studio.myplugin/
│
├── MyPlugin.java
│     ROLE: JavaPlugin subclass. Composition root only.
│     SRP: Lifecycle wiring. Zero business logic. Zero static getInstance().
│     DIP: The ONLY class that calls `new` on concrete implementations.
│          Wires: config → db → schema → repo → cache → service → hook → command → listener → task
│
├── bootstrap/
│   ├── MyPluginBootstrap.java      # PluginBootstrap. Early-lifecycle registry hooks only.
│   └── MyPluginLoader.java         # PluginLoader. Custom registry entries only. Optional.
│     SRP: Pre-server-start concerns only. Nothing that belongs in onEnable().
│
├── config/
│   ├── PluginConfig.java           # record. Typed, immutable config snapshot. Pure data. No logic.
│   ├── ConfigManager.java          # Loads, validates, migrates, exposes PluginConfig. One class.
│   └── ConfigMigrator.java         # Version migration logic only. Called by ConfigManager.
│     SRP: Data (PluginConfig) and behavior (ConfigManager) are always separate files.
│     OCP: New config version = new migration method in ConfigMigrator. No edits to ConfigManager.
│     DIP: ConfigManager depends on PluginConfig (abstraction/record), not on raw FileConfiguration.
│
├── constant/
│   ├── Permissions.java            # public static final String ADMIN = "myplugin.admin";
│   ├── PDCKeys.java                # ALL NamespacedKey constants. Static final. One class.
│   └── Messages.java               # Lang file key path constants. e.g. "messages.join"
│     SRP: Constants only. No methods. No logic. No instantiation.
│     ISP: Split by domain — permissions are not mixed with PDC keys.
│
├── exception/
│   ├── PluginException.java        # Base unchecked RuntimeException for plugin domain.
│   ├── DataException.java          # Thrown by repositories on data access failure.
│   ├── ConfigException.java        # Thrown by ConfigManager on validation failure.
│   └── ServiceException.java       # Thrown by services on business rule violation.
│     SRP: One exception type per concern domain.
│     LSP: All extend PluginException. Callers catching PluginException get safe behavior.
│
├── data/
│   │   DIP BOUNDARY: Nothing outside `data/` imports from `data/impl/` directly.
│   │   Services depend on interfaces in `data/repository/` only.
│   │
│   ├── model/
│   │   ├── PlayerData.java         # record. Immutable. Pure data carrier. No behavior.
│   │   ├── RegionData.java         # record. Immutable.
│   │   └── RankData.java           # record. Immutable.
│   │     SRP: Data only. No persistence logic, no formatting, no business rules.
│   │     LSP: Records are final. No subclassing data models.
│   │
│   ├── repository/
│   │   │   ISP BOUNDARY: Reader, Writer, Deleter are separate interfaces.
│   │   │   Never one fat interface per entity.
│   │   │
│   │   ├── PlayerReader.java       # interface: Optional<PlayerData> load(UUID id);
│   │   ├── PlayerWriter.java       # interface: void save(PlayerData data);
│   │   ├── PlayerDeleter.java      # interface: void delete(UUID id);
│   │   ├── PlayerRepository.java   # interface extends PlayerReader, PlayerWriter, PlayerDeleter
│   │   ├── RegionReader.java
│   │   ├── RegionWriter.java
│   │   ├── RegionRepository.java   # interface extends RegionReader, RegionWriter, RegionDeleter
│   │   └── impl/
│   │       ├── SqlPlayerRepository.java   # Implements PlayerRepository fully. SQL only.
│   │       └── SqlRegionRepository.java   # Implements RegionRepository fully. SQL only.
│   │     OCP: New storage backend = new impl/ class. Repository interfaces never change.
│   │     LSP: Every impl honors full interface contract. No UnsupportedOperationException ever.
│   │     DIP: Services import PlayerRepository (interface). Never SqlPlayerRepository.
│   │
│   └── database/
│       ├── DatabaseManager.java    # HikariCP pool init, shutdown. Returns DataSource only.
│       ├── SchemaManager.java      # Applies versioned SQL schema files. Idempotent.
│       └── sql/
│           ├── schema_v1.sql       # Versioned. Never modified after release.
│           ├── schema_v2.sql       # New version = new file. Old files are immutable.
│           ├── player_upsert.sql
│           └── region_select.sql
│         SRP: DatabaseManager owns pool lifecycle. SchemaManager owns schema lifecycle.
│         OCP: New schema version = new .sql file. SchemaManager.apply() logic unchanged.
│
├── cache/
│   ├── PlayerCache.java            # interface: get, put, invalidate, invalidateAll
│   ├── RegionCache.java            # interface
│   └── impl/
│       ├── ConcurrentPlayerCache.java  # ConcurrentHashMap impl of PlayerCache
│       └── ConcurrentRegionCache.java
│     ISP: Cache interface per entity. Never one cache interface for all entities.
│     DIP: Services depend on PlayerCache interface. Never on ConcurrentPlayerCache.
│     SRP: Cache holds data. Services decide when to read/write cache.
│
├── service/
│   │   DIP BOUNDARY: Services import interfaces from repository/, cache/, hook/ only.
│   │   Services NEVER import from data/impl/, cache/impl/, hook/impl/.
│   │
│   ├── PlayerService.java          # interface: handleJoin, handleQuit, getPlayerData, etc.
│   ├── RegionService.java          # interface
│   ├── RankService.java            # interface
│   └── impl/
│       ├── DefaultPlayerService.java   # Implements PlayerService. Depends on PlayerRepository,
│       │                               # PlayerCache, PlayerMessenger (all interfaces).
│       ├── DefaultRegionService.java
│       └── DefaultRankService.java
│     SRP: One service per domain. PlayerService never touches region data.
│     OCP: New behavior variant = new impl/. DefaultPlayerService never edited for new features.
│     DIP: DefaultPlayerService constructor: (PlayerRepository, PlayerCache, PlayerMessenger)
│          All interfaces. Never concrete types.
│
├── listener/
│   ├── ListenerRegistry.java       # Registers all listeners. No logic. SRP: wiring only.
│   ├── player/
│   │   ├── PlayerJoinListener.java # @EventHandler → playerService.handleJoin(). Nothing else.
│   │   └── PlayerQuitListener.java
│   ├── entity/
│   │   └── EntityDamageListener.java
│   └── world/
│       └── BlockBreakListener.java
│     SRP: Listeners delegate 100%. Zero logic inside @EventHandler methods.
│     DIP: Listeners depend on service interfaces. Never on impl classes.
│     ISP: One listener class per event group. Never one listener for all events.
│
├── command/
│   ├── CommandRegistry.java        # Registers Brigadier commands via LifecycleEvents. No logic.
│   └── impl/
│       ├── ReloadCommand.java      # Parses args, validates, calls service. Nothing else.
│       ├── RegionCommand.java      # One class per root command. Sub-commands as inner nodes.
│       └── AdminCommand.java
│     SRP: Commands parse and delegate. Zero business logic.
│     DIP: Commands depend on service interfaces. Never impl classes.
│     OCP: New command = new impl/ class + one line in CommandRegistry. Nothing else changes.
│
├── scheduler/
│   ├── TaskRegistry.java           # Registers and cancels all tasks. Called by MyPlugin.
│   └── task/
│       ├── AutoSaveTask.java       # One task, one purpose. Calls service.save() only.
│       └── CacheCleanupTask.java   # Calls cache.invalidateExpired() only.
│     SRP: One task = one timed responsibility. Never combined.
│     DIP: Tasks depend on service/cache interfaces. Never impl classes.
│
├── hook/
│   ├── HookManager.java            # Detects enabled plugins, init hooks, returns interfaces.
│   ├── EconomyProvider.java        # interface: getBalance, deposit, withdraw
│   ├── PlaceholderProvider.java    # interface: resolve(player, placeholder)
│   └── impl/
│       ├── VaultEconomyHook.java       # Implements EconomyProvider via Vault API.
│       ├── NoOpEconomyHook.java        # Null-object impl when Vault absent. Never returns null.
│       ├── PlaceholderAPIHook.java     # Implements PlaceholderProvider.
│       └── NoOpPlaceholderHook.java    # Null-object impl when PAPI absent.
│     OCP: New hook = new impl/. HookManager.init() returns interface. Services unchanged.
│     LSP: NoOp impls are fully substitutable. Callers never null-check hook references.
│     DIP: Services depend on EconomyProvider interface. HookManager wires the correct impl.
│
├── messaging/
│   ├── PlayerMessenger.java        # interface: send(Player, String key), sendRaw(Player, Component)
│   └── impl/
│       └── AdventurePlayerMessenger.java  # MiniMessage + lang file. No legacy ChatColor ever.
│     SRP: Presentation concern isolated from all service logic.
│     ISP: Messenger only sends. It does not format, does not log, does not store.
│     DIP: Services depend on PlayerMessenger interface, never AdventurePlayerMessenger.
│
├── pdc/
│   ├── PDCKeys.java                # (also in constant/ — import from constant/ only)
│   └── PDCUtil.java                # Static helpers: read, write, has. No state.
│     SRP: PDC access helpers only. Never mixed with item logic or entity logic.
│
└── util/
    ├── LocationUtil.java           # Stateless static methods for Location ↔ primitives
    ├── ItemUtil.java               # Stateless static item builders/inspectors
    ├── TextUtil.java               # MiniMessage deserialization helpers
    ├── TimeUtil.java               # Tick ↔ duration conversions
    └── Validate.java               # Precondition guards. throws PluginException on failure.
      SRP: One util class per domain. Never a god Utils.java.
      ISP: No util class has methods from two different domains.
```

---

## RESOURCES LAYOUT

```
src/main/resources/
├── paper-plugin.yml                # Mandatory. Bootstrapper + dependency declarations.
├── config.yml                      # Default config. Fully commented. config-version key present.
├── lang/
│   ├── en_US.yml                   # Default locale. Always present. Fallback for all keys.
│   └── zh_CN.yml                   # Additional locales.
└── data/
    └── sql/
        ├── schema_v1.sql           # Immutable after release. Never edited.
        └── schema_v2.sql           # New version = new file only.
```

---

## TEST LAYOUT — SOLID ENFORCED

```
src/test/java/com.studio.myplugin/
├── AbstractPluginTest.java         # MockBukkit setup/teardown. Base for all test classes.
├── service/
│   ├── PlayerServiceTest.java      # Tests DefaultPlayerService with mocked interfaces only.
│   └── RegionServiceTest.java      # Never mocks concrete SQL classes — mocks interfaces.
├── repository/
│   └── SqlPlayerRepositoryTest.java    # Tests SQL impl against in-memory SQLite.
├── command/
│   └── RegionCommandTest.java          # Tests command parsing + delegation. Mocks service.
├── listener/
│   └── PlayerJoinListenerTest.java     # Verifies delegation to service. Mocks service.
├── config/
│   └── ConfigManagerTest.java          # Tests load, validate, migrate. No server required.
├── cache/
│   └── ConcurrentPlayerCacheTest.java  # Tests cache behavior in isolation.
└── hook/
    └── VaultEconomyHookTest.java       # Tests hook against mocked Vault API.

# LSP TEST RULE:
# For every interface with multiple implementations, create one shared abstract test:
# abstract class PlayerRepositoryContractTest {
#     abstract PlayerRepository createRepository();
#     @Test void testSaveAndLoad() { ... }
# }
# class SqlPlayerRepositoryTest extends PlayerRepositoryContractTest { ... }
# This guarantees LSP compliance across all implementations automatically.
```

---

## COMPOSITION ROOT — `MyPlugin#onEnable()` WIRE ORDER

```
DIP RULE: The ONLY place `new ConcreteClass()` appears in the entire codebase.
Wire strictly top-to-bottom. Each line depends only on what was declared above it.

1.  configManager    = new ConfigManager(getDataFolder(), getSLF4JLogger());
2.  config           = configManager.load();           // returns PluginConfig record
3.  databaseManager  = new DatabaseManager(config.database());
4.  schemaManager    = new SchemaManager(databaseManager.getPool());
5.  schemaManager.applyLatest();
6.  playerRepository = new SqlPlayerRepository(databaseManager.getPool());
7.  regionRepository = new SqlRegionRepository(databaseManager.getPool());
8.  playerCache      = new ConcurrentPlayerCache();
9.  economyProvider  = hookManager.loadEconomy();      // returns EconomyProvider interface
10. playerService    = new DefaultPlayerService(playerRepository, playerCache, economyProvider);
11. regionService    = new DefaultRegionService(regionRepository, playerService);
12. messenger        = new AdventurePlayerMessenger(getDataFolder());
13. hookManager      = new HookManager(this, playerService);
14. listenerRegistry = new ListenerRegistry(this, playerService, regionService);
15. listenerRegistry.registerAll();
16. commandRegistry  = new CommandRegistry(this, playerService, regionService);
17. commandRegistry.registerAll();
18. taskRegistry     = new TaskRegistry(this, playerService, playerCache);
19. taskRegistry.startAll();
```

---

## NAMING RULES — STRUCTURE ENFORCED

| Type | Pattern | SOLID Reason |
|---|---|---|
| Interface (contract) | `PlayerRepository`, `EconomyProvider` | ISP/DIP — abstract name, no impl detail |
| Implementation | `SqlPlayerRepository`, `VaultEconomyHook` | OCP — impl detail in name, lives in `impl/` |
| Null-object impl | `NoOpEconomyHook` | LSP — fully substitutable, honest name |
| Data model | `PlayerData`, `RegionData` (record) | SRP — pure data, no behavior hinted by name |
| Registry/wiring | `ListenerRegistry`, `CommandRegistry` | SRP — wiring role explicit in name |
| Task | `AutoSaveTask`, `CacheCleanupTask` | SRP — single timed purpose in name |
| Exception | `DataException`, `ConfigException` | SRP — domain scoped |

---

## ANTIPATTERNS — NEVER DO

- Never import `com.studio.myplugin.data.impl.*` from a service class. Import interfaces only.
- Never place business logic in `MyPlugin.java`. It is a wiring file, not a service.
- Never create a `util/Utils.java` god file spanning multiple domains.
- Never merge `PlayerReader` and `PlayerWriter` into one interface unless the consumer always needs both.
- Never skip `impl/` sub-packages and place concretions beside their interfaces.
- Never place `NamespacedKey` declarations inside Listener or Service classes — use `PDCKeys.java`.
- Never allow two domain packages to have circular imports. Use a mediator or event bus.
- Never place SQL strings inline in services. All SQL lives in `data/database/sql/*.sql` files.
- Never write a test that imports a concrete `impl/` class to test a service — mock the interface.
- Never exceed 300 lines per file. That is a structural split signal, not a guideline.
- Never place NoOp hook implementations outside `hook/impl/`. They are implementations.
- Never use inheritance between domain model records. Composition only.
- Never let a Listener hold mutable state. State lives in services or caches only.

---