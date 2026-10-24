package com.virgolandstudio.items.core.world;

public record CompiledWorldInteractionSystem(
        CompiledBlockGrab blockGrab,
        CompiledBlockHold blockHold,
        CompiledBlockThrow blockThrow,
        CompiledEntityPull entityPull,
        CompiledAreaDamage areaDamage,
        CompiledIgnitable ignitable
) {
    public CompiledWorldInteractionSystem(
            CompiledBlockGrab blockGrab,
            CompiledBlockHold blockHold,
            CompiledBlockThrow blockThrow,
            CompiledEntityPull entityPull
    ) {
        this(blockGrab, blockHold, blockThrow, entityPull, null, null);
    }

    public CompiledWorldInteractionSystem(CompiledBlockGrab blockGrab, CompiledBlockHold blockHold, CompiledBlockThrow blockThrow) {
        this(blockGrab, blockHold, blockThrow, null, null, null);
    }
}
