# VsRender runtime fallback audit — ~95% checkpoint

Date: 2026-10-07

## Native physical path now covered

For BetterModel-backed **entity instances** the native path now owns, when the imported model is representable:

- indexed animation playback, priority/override per bone, damage/jump and terminal death;
- native Item Display rendering, live equipment payload refresh, configured tint layers and damage tint restore;
- entity hitbox AABBs, packet interaction forwarding and projectile interception delegated back to Paper/vanilla;
- TAG / MOB_TAG / PLAYER_TAG nametags and sight-trace visibility;
- SEAT / SUB_SEAT carriers with PASSENGER_ONLY / WALK / FLY policies, rider input, jump/fly, rider-damage policy and lifecycle cleanup;
- entity profile/skin context through BetterModel `RenderSource.completeContext()` without creating an EntityTracker;
- locator/attachment snapshots directly from the native dense pose buffer.

Core render stays provider-independent. BetterModel objects are used only at the compatibility/import/context boundary for this path.

## BetterModel physical fallbacks that intentionally remain

1. **Static instance + external profile**: `RenderInstanceProvider#createInstance` is synchronous while BetterModel profile completion is asynchronous. This route keeps the proven DummyTracker fallback instead of blocking a server thread.
2. **Unsupported/custom physical semantics**: a custom BetterModel mount controller that cannot map to DISABLED/PASSENGER_ONLY/WALK/FLY, unsupported dynamic mapper/visibility payload, or adapter failure still falls back rather than silently changing behavior.
3. **Player-limb safety fallback**: native player payload resolution/host creation can fall back to BetterModel on unsupported/error paths. Supported normal player-limb playback is native-first.
4. **External BetterModel availability**: the compatibility provider is still registered only when the BetterModel plugin/API is present and the module still has a compileOnly BetterModel API dependency. Removing this plugin requirement requires a standalone asset/profile importer or vendored compatible import layer and is not claimed here.

## Native hitbox limitation still open

Native hitboxes cover exact AABB targeting, melee/interact forwarding and projectile interception. They do **not** yet reproduce BetterModel's full server-side living-entity collision body (push/collidable/entityInside/lava/block effects). That work belongs to the final parity tranche and is not counted as complete.

## Build/release limitation

The Gradle wrapper is 9.7.1 and this environment still cannot resolve `services.gradle.org`; the local Java runtime is 21 while the project target is Java 25. Therefore no rebuilt VsRender JAR or CanvasMC standalone smoke is claimed by this checkpoint.
