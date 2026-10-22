# VsRender — Compiled Runtime v1

## Objetivo
Separar authoring/importación de ejecución. `RenderRig` y `RenderAnimationClip` siguen siendo modelos descriptivos fáciles de inspeccionar y adaptar desde BetterModel/CraftEngine; antes de entrar al hot path se compilan a estructuras densas propias de VsRender.

## Pipeline

```text
BetterModel / CraftEngine / futuro formato VsRender
                    |
                    v
          RenderRig + AnimationClip
                    |
                    v
           CompiledRenderAsset
             /             \
    IndexedRenderRig   IndexedAnimationClip
             \             /
              CompiledRenderState
                    |
              tick(delta)
                    |
          RenderAnimationBuffer
                    |
          IndexedRigPoseResolver
                    |
             RenderPoseBuffer
                    |
               dirty mask
                    |
          futuro packet renderer
```

## Decisiones de rendimiento

### IDs sólo en el borde
Los nombres se conservan para configuración/debug, pero se resuelven a `partIndex` al compilar. `CompiledRenderState.tick()` no realiza búsquedas por nombre.

### Topología parent-before-child
`IndexedRenderRig` recorre las roots en orden determinista y compila una secuencia donde el parent siempre tiene un índice menor que sus children. El resolver puede ejecutar un único loop lineal, sin recursion ni maps.

### Rest pose precomputada
Translation/scale/Euler base se guardan en arrays. El quaternion de rest pose también se precomputa y se reutiliza cuando un track no modifica rotación.

### Clips compactos
Un clip con 8 tracks sobre un rig de 100 bones guarda 8 `IndexedAnimationTrack`, no 100 slots por clip. Cada track usa arrays primitivos para time/translation/scale/rotation/interpolation/globalRotation.

### Buffers reutilizables
Cada instancia posee sólo:
- un `RenderAnimationBuffer`;
- un `RenderPoseBuffer`;
- playback mutable mínimo.

Rig y clips compilados pertenecen al asset compartido y no se duplican por NPC/furniture/entity.

### Dirty mask
`RenderPoseBuffer` compara la pose resuelta actual con la anterior usando epsilon. Quaternion considera equivalentes `q` y `-q`. El renderer físico podrá iterar únicamente `forEachDirty` y no enviar transforms iguales.

## Invariantes
- Core no importa BetterModel/Bukkit.
- El sampler/resolver no crea threads ni packets.
- El hot path no usa `Map<String,...>`.
- Buffers y clips están ligados a la identidad del rig compilado y no pueden mezclarse por accidente.
- La salida indexada debe coincidir con `RenderRigPoseResolver` descriptivo dentro del epsilon numérico.

## Validación local
- Harness de equivalencia ONCE/LOOP/HOLD: PASS.
- Rest pose, parent rotation/scale, globalRotation: PASS.
- Reuso de animation buffer sin tracks stale: PASS.
- Estado por instancia play/tick/stop/rest: PASS.
- Dirty mask 64/8: `8 -> 0 -> 8`: PASS.
- `javac -Xlint:all`: PASS.
- 0 imports `kr.toxicity` en Core.

Micro-harness orientativo, no JMH: 64 bones / 16 tracks, pipeline completo descriptivo (`sampleState + resolve`) vs indexado (`sample + resolve`), tres procesos separados: 2.90x, 2.93x y 3.06x de speedup observados. Esto valida la dirección, no constituye un benchmark definitivo de producción.

## Próximo paso
El backend físico nativo debe consumir directamente `RenderPoseBuffer`:

```text
poses.forEachDirty(partIndex -> renderer.update(
    partIndex,
    poses.translationX(partIndex), ...,
    poses.rotationW(partIndex)
));
```

Así packet batching, culling y LOD pueden operar sobre índices/dirty masks sin conocer BetterModel, Blockbench ni la jerarquía descriptiva.
