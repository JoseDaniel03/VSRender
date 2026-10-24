package com.virgolandstudio.items.core.reload;

import com.virgolandstudio.items.core.instance.ItemInstance;

import java.util.Objects;

public final class ReloadProcess {
    public enum Resolution { REPEATED, ADVANCED, COMPLETED }

    private final ItemInstance weapon;
    private final ReloadSequence sequence;
    private int stageIndex;
    private int stageIteration = 1;
    private long stageEndsAtTick;
    private boolean awaitingResolution;
    private boolean cancelled;

    ReloadProcess(ItemInstance weapon, ReloadSequence sequence, long currentTick) {
        this.weapon = Objects.requireNonNull(weapon, "weapon");
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.stageEndsAtTick = currentTick + sequence.stages().get(0).durationTicks();
    }

    public ItemInstance weapon() { return weapon; }
    public ReloadStage stage() { return sequence.stages().get(stageIndex); }
    public int stageIndex() { return stageIndex; }
    public int stageIteration() { return stageIteration; }
    public long stageEndsAtTick() { return stageEndsAtTick; }
    public boolean awaitingResolution() { return awaitingResolution; }
    public boolean cancelled() { return cancelled; }

    boolean markStageReady() {
        if (cancelled || awaitingResolution) return false;
        awaitingResolution = true;
        return true;
    }

    Resolution resolve(long currentTick, boolean repeatRequested) {
        if (cancelled) throw new IllegalStateException("Reload process is cancelled");
        if (!awaitingResolution) throw new IllegalStateException("Reload stage is not awaiting resolution");
        awaitingResolution = false;

        ReloadStage current = stage();
        if (repeatRequested && stageIteration < current.maxIterations()) {
            stageIteration++;
            stageEndsAtTick = currentTick + current.durationTicks();
            return Resolution.REPEATED;
        }

        stageIndex++;
        stageIteration = 1;
        if (stageIndex >= sequence.stages().size()) return Resolution.COMPLETED;
        stageEndsAtTick = currentTick + sequence.stages().get(stageIndex).durationTicks();
        return Resolution.ADVANCED;
    }

    void cancel() { cancelled = true; }
}
