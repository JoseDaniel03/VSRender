# VsRender

VsRender is VirgolandStudio's native rendering runtime for CanvasMC/Paper targets.

It owns the rendering side of custom content: Blockbench import, native asset compilation, resource-pack generation, display rigs, animations, hitboxes, mounts, attachments and the runtime bridges used by blocks/furniture/entities.

## Native path

```text
.bbmodel
  -> NativeRenderAsset
  -> versioned SHA-256 cache
  -> NativeResourcePackCompiler
  -> CompiledRenderAsset
  -> vsrender_native provider
  -> positional / entity-owned runtime
```

BetterModel and ModelEngine are **not runtime backends** and are not dependencies of VsRender.

Use `README-VSRENDER.md`, `VSR-NATIVE-CONTENT.md` and `VSR-VALIDATION.md` for implementation and validation details.

## Upstream attribution

VsRender started from the public CraftEngine codebase and remains subject to the upstream GPLv3 license and attribution requirements. Internal vendored namespaces may still use `net.momirealms.craftengine`; they are implementation details, not a second product/runtime.

See `VSR-UPSTREAM.md` and `LICENSE`.
