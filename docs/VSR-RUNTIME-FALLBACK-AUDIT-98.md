# VsRender runtime fallback audit — ~98% source/runtime checkpoint

Date: 2026-10-07

## Scope of this checkpoint

This checkpoint measures **source/runtime parity**, not release readiness. The supported native path now covers the normal representable BetterModel-backed routes for static models, profiled static models, entity models and player limbs without using a BetterModel tracker as the physical renderer.

BetterModel remains at the compatibility boundary as an asset/profile/context source. A full Java 25 build, rebuilt standalone JAR and CanvasMC smoke are separate release gates and are not marked green here.

## Native paths covered

### Static MODEL without external profile
- Compiled native rig/animation runtime.
- Native Item Display payloads.
- Native configured tint layers when the platform tint backend is supported.
- Native sight-trace.
- Native animation/locator capabilities exposed by the native host.

### Static MODEL with external profile/skin
- The synchronous `RenderInstanceProvider#createInstance` no longer forces a `DummyTracker` for the normal representable path.
- `BetterModelNativeDisplayAssetAdapter.adaptStaticProfile` creates a profiled `RenderSource.Dummy` and calls `completeContext()` asynchronously; it never creates a tracker.
- `DeferredRenderInstance` preserves viewer intent while the profile context resolves.
- Final native host construction is rescheduled onto the target chunk/platform scheduler; the caller thread is never blocked waiting for skin/profile completion.
- Close-before-complete permanently retires the deferred host. A late delegate is closed instead of reviving the render instance.
- Scheduler rejection/materialization failure retires the deferred host instead of leaving an unresolved live instance.

### Entity MODEL
- Native displays/animation stack, death/damage/jump, per-bone override/priority.
- Native configured tint + damage-tint restore.
- Native hitbox exact AABB targeting and interaction forwarding.
- Native projectile interception delegated back to Paper/vanilla.
- Native TAG/MOB_TAG/PLAYER_TAG nametags and sight trace.
- Native SEAT/SUB_SEAT PASSENGER_ONLY/WALK/FLY built-in mount runtime.
- Profile/skin context resolves through `RenderSource.completeContext()` without `EntityTracker` creation on the supported path.
- Native locator/attachment snapshots from the dense pose buffer.

### PLAYER limb
- Native-first skin/equipment payload resolution.
- Native animation runtime and payload refresh.
- Legacy BetterModel is retained only as a compatibility/safety fallback when the native host/payload cannot be represented or constructed.

## Remaining BetterModel tracker fallbacks

Tracker references still exist intentionally in the compatibility package, but they are no longer the normal physical route for representable static-profile/entity/player rendering.

1. **Unsupported/custom imported semantics**
   - Custom mount controller that cannot map to the native typed policies.
   - Unsupported dynamic mapper/visibility/payload forms.
   - Platform feature incompatibility such as unavailable display tint support.
   - Adapter failure where changing behavior silently would be worse than retaining the upstream backend.

2. **Exceptional static-profile failure path**
   - If profiled asset resolution produces no representable native asset, the chunk-scheduled materialization may fall back to `DummyTracker`.
   - This is an explicit compatibility fallback, not the normal profiled path.

3. **Exceptional entity/player safety path**
   - Unsupported/error paths may use `EntityTracker`/player tracker fallback.
   - Supported normal entity/profile and player-limb paths remain native-first.

## Physical hitbox parity that remains open

Native hitboxes currently cover the gameplay-critical targeting path:
- exact per-bone world AABB;
- attack/interact forwarding to the real owner;
- projectile segment interception with nearest-hit ordering;
- lifecycle/world migration cleanup.

They do **not** yet claim BetterModel's entire server-side living collision body:
- generic physical push/collidable body behavior;
- `entityInside` block callbacks;
- lava/block environmental effects driven by the hitbox body;
- exact owner/passenger/sibling exclusion inside a hard NMS collider.

The existing CraftEngine FastNMS collision primitive was audited as a possible reuse path. Its generic collision entity exposes an arbitrary AABB and projectile/collidable flags, but its current collision behavior does not encode VsRender owner/passenger/sibling exclusion and its tick path does not reproduce BetterModel's block/lava forwarding. Reusing it directly would therefore be a semantic regression, so this checkpoint does not do that merely to inflate parity.

This is intentionally left in the final parity tranche.

## External BetterModel bootstrap/import dependency

The BetterModel compatibility module still has a BetterModel API dependency and provider registration remains conditional on BetterModel being present. The native runtime no longer needs BetterModel trackers for the supported paths above, but import/profile-context boot still does.

Removing the plugin requirement entirely requires one of:
- a standalone/vendored compatible BetterModel asset + profile importer; or
- migration of the required importer/profile contracts into VsRender-owned code.

That is a final standalone/bootstrap task, not a hidden runtime fallback.

## Build/release gate

Re-attempt on 2026-10-07:

`bash ./gradlew :core:test --no-daemon --max-workers=2`

still stops before compilation while fetching Gradle 9.7.1:

`java.net.UnknownHostException: services.gradle.org`

The local runtime is OpenJDK 21.0.11 while the target toolchain is Java 25. Therefore this checkpoint does not claim a rebuilt VsRender JAR or standalone CanvasMC smoke.

## Result

At source + isolated-harness level, the fusion is reasonably classified as **~98%**. The remaining ~2% is concentrated rather than broad:

- full owner-aware physical hitbox-body parity;
- removal of the external BetterModel import/profile bootstrap requirement;
- mandatory Java 25 full Gradle build and CanvasMC standalone smoke/release proof.
