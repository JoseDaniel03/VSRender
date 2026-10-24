package com.virgolandstudio.items.core.reload;

import com.virgolandstudio.items.core.instance.ItemInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Central timed runtime. One scheduler may call tick(); no task is created per reload. */
public final class ReloadRuntime {
    public enum EventType { STAGE_READY }
    public record Event(EventType type, ReloadProcess process, ReloadStage stage) {}

    private final ConcurrentHashMap<UUID, ReloadProcess> active = new ConcurrentHashMap<>();

    public synchronized ReloadProcess start(ItemInstance weapon, ReloadSequence sequence, long currentTick) {
        var process = new ReloadProcess(weapon, sequence, currentTick);
        ReloadProcess existing = active.putIfAbsent(weapon.id(), process);
        if (existing != null) throw new IllegalStateException("Reload already active for item instance " + weapon.id());
        return process;
    }

    public synchronized Optional<ReloadProcess> find(UUID weaponInstanceId) {
        return Optional.ofNullable(active.get(weaponInstanceId));
    }

    public synchronized boolean cancel(UUID weaponInstanceId) {
        var process = active.remove(weaponInstanceId);
        if (process == null) return false;
        process.cancel();
        return true;
    }

    /** Marks due stages ready but deliberately does not advance them until the platform resolves conditions/actions. */
    public synchronized List<Event> tick(long currentTick) {
        var events = new ArrayList<Event>();
        for (var process : active.values()) {
            if (process.awaitingResolution()) continue;
            if (currentTick < process.stageEndsAtTick()) continue;
            if (process.markStageReady()) {
                events.add(new Event(EventType.STAGE_READY, process, process.stage()));
            }
        }
        return events;
    }

    /**
     * Resolves the currently-ready stage after its compiled behavior has executed.
     * The runtime itself enforces the stage max-iteration cap.
     */
    public synchronized ReloadProcess.Resolution resolveStage(UUID weaponInstanceId, long currentTick, boolean repeatRequested) {
        ReloadProcess process = active.get(weaponInstanceId);
        if (process == null) throw new IllegalStateException("Reload is not active for item instance " + weaponInstanceId);
        ReloadProcess.Resolution result = process.resolve(currentTick, repeatRequested);
        if (result == ReloadProcess.Resolution.COMPLETED) active.remove(weaponInstanceId, process);
        return result;
    }

    public synchronized int activeCount() { return active.size(); }
}
