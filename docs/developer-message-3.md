## Memory Analysis — Will This Cause OOM?

Short answer: **No, if built correctly. But let me show you exactly why, with numbers.**

---

### What This Plugin Actually Stores in RAM

Let me be precise about the memory footprint:

**The entire runtime state of this plugin:**

```
AtomicReference<UUID>     →  16 bytes (UUID = two longs)
ConcurrentHashSet<UUID>   →  ~48 bytes per entry (20 players max)
HolderSnapshot record     →  ~64 bytes, created once per broadcast cycle, GC'd immediately
String message cache      →  ~200 bytes, rebuilt every broadcast
```

**Total steady-state memory: under 5KB.** That is not a typo. This plugin's data structures are essentially zero RAM cost.

---

### Your Actual Memory Budget

| Allocation | RAM Usage |
|---|---|
| JVM base overhead | ~256MB |
| Paper server itself | ~512MB |
| World chunks (20 players active) | ~1.2–1.8GB |
| Other plugins (Essentials, WorldGuard, etc.) | ~200–400MB |
| **This plugin** | **~5KB** |
| Headroom remaining | ~500MB–1GB |

This plugin will **never** be your OOM cause. Not even close.

---

### What Actually Causes OOM on Your Setup

Since you're worried about it, here's what genuinely threatens your 4GB budget — none of which is this plugin:

**1. Chunk Loading — the real memory killer**

20 players exploring in different directions simultaneously forces the server to load and cache hundreds of chunks. Each chunk is roughly 100–200KB. 500 loaded chunks = ~100MB. Players spread far apart on a fresh world is the single biggest RAM threat on your server.

**2. Plugin memory leaks — the silent killer**

The pattern that causes real OOM in plugins is storing `Player` object references instead of `UUID`. A `Player` object holds references to the entity, their inventory, their location, their packet handler — it's a large object graph. If that player logs off and you're still holding their `Player` reference, the JVM **cannot garbage collect any of it**. Over hours of players joining and leaving, this accumulates and causes the classic "server gets slow after 6 hours then crashes" OOM pattern.

This is exactly why the skill mandates: **store UUID, never Player.**

**3. Scheduler task accumulation**

If a plugin registers a repeating task every time an event fires — without cancelling the old one — you get exponential task buildup. Each task holds a closure with references to objects. This is a common junior plugin mistake that causes slow OOM over hours.

---

### Specific Guarantees in the Correct Design

| Design Decision | Memory Consequence |
|---|---|
| Store `UUID` not `Player` | Player objects GC'd normally on logout — zero leak |
| Broadcast snapshot is a `record` | Allocated on heap, GC'd within 1-2 GC cycles, never accumulates |
| Single registered scheduler task | One task, fixed cost, never grows |
| No collections that grow unboundedly | Player set is bounded by `Bukkit.getOnlinePlayers()` size — max 20 |
| Event listeners are stateless | Zero per-event allocation beyond the event object itself |

---

### The One Real Risk — and How We Handle It

The only pattern in **this specific feature** that could contribute to memory pressure is if the broadcast task creates new `Component` objects every cycle and the GC can't keep up under load.

The mitigation is simple: **cache the static parts** of the message (prefix text, formatting) and only rebuild the dynamic parts (player name, coordinates) when state actually changes, not every broadcast tick.

```java
// Bad — allocates full Component graph every 40 ticks
Component msg = Component.text("Dragon Egg Holder: ").append(...)

// Good — pre-build the static wrapper once, swap dynamic part on state change
// Only reallocate when holder UUID changes
```

---

### Bottom Line

Your OOM risk from this plugin is **effectively zero**. Your actual OOM risks are chunk loading from spread-out players, other plugins with Player reference leaks, and JVM heap tuning. If you haven't already, make sure your startup flags include:

```
-Xms2G -Xmx3G -XX:+UseG1GC -XX:+ParallelRefProcEnabled
 -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions
 -XX:+DisableExplicitGC -XX:G1NewSizePercent=30
 -XX:G1MaxNewSizePercent=40 -XX:G1HeapRegionSize=8M
 -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5
 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15
 -XX:G1MixedGCLiveThresholdPercent=90
 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32
 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1
```

These are the Aikar flags — the gold standard for Paper servers. They dramatically reduce GC pause spikes which often get misidentified as plugin lag.

Ready to write the code whenever you say go.