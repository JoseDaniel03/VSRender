package com.virgolandstudio.items.core.ranged;

import com.virgolandstudio.items.api.ranged.FireMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central cadence runtime. It tracks only weapons that currently need cadence state.
 * No scheduler is created here; the platform calls {@link #tick(long)} from its shared ticker.
 *
 * <p>The state machine is synchronized because Folia may accept fire input on one owning entity
 * thread while the shared logical clock advances active sequences from the global-region scheduler.</p>
 */
public final class FireControlRuntime {
    private static final long IDLE_RETENTION_TICKS = 1200L;
    private static final long MAINTENANCE_INTERVAL_TICKS = 200L;

    private final Map<UUID, State> states = new HashMap<>();
    private final Set<UUID> active = new LinkedHashSet<>();
    private long nextMaintenanceTick = Long.MIN_VALUE;

    public synchronized RequestResult request(UUID weaponId, CompiledFireControl control, long currentTick) {
        if (weaponId == null) throw new IllegalArgumentException("weaponId cannot be null");
        if (control == null) return new RequestResult(true, false);

        State state = states.computeIfAbsent(weaponId, ignored -> new State());
        if (state.activeMode == FireMode.BURST) return new RequestResult(false, true);
        if (state.activeMode == FireMode.AUTO) {
            if (currentTick <= state.autoLeaseUntil) {
                state.autoLeaseUntil = Math.max(state.autoLeaseUntil, currentTick + control.autoLeaseTicks());
                return new RequestResult(false, true);
            }
            deactivate(weaponId, state);
        }
        if (state.nextAllowedTick > currentTick + 1.0e-9) {
            return new RequestResult(false, active.contains(weaponId));
        }

        state.nextAllowedTick = currentTick + control.intervalTicks();
        switch (control.mode()) {
            case SEMI -> deactivate(weaponId, state);
            case BURST -> {
                if (control.burstSize() > 1) {
                    state.activeMode = FireMode.BURST;
                    state.remainingShots = control.burstSize() - 1;
                    state.activeControl = control;
                    active.add(weaponId);
                } else {
                    deactivate(weaponId, state);
                }
            }
            case AUTO -> {
                state.activeMode = FireMode.AUTO;
                state.activeControl = control;
                state.autoLeaseUntil = currentTick + control.autoLeaseTicks();
                active.add(weaponId);
            }
        }
        return new RequestResult(true, active.contains(weaponId));
    }

    /** Returns weapon instance IDs that should emit one shot this tick. */
    public synchronized List<UUID> tick(long currentTick) {
        if (active.isEmpty()) {
            maintenanceIfDue(currentTick);
            return List.of();
        }

        ArrayList<UUID> due = null;
        Iterator<UUID> iterator = active.iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            State state = states.get(id);
            if (state == null || state.activeMode == null || state.activeControl == null) {
                iterator.remove();
                continue;
            }

            if (state.activeMode == FireMode.AUTO && currentTick > state.autoLeaseUntil) {
                clearActiveFields(state);
                iterator.remove();
                continue;
            }
            if (state.nextAllowedTick > currentTick + 1.0e-9) continue;

            if (due == null) due = new ArrayList<>();
            due.add(id);
            state.nextAllowedTick += state.activeControl.intervalTicks();

            if (state.activeMode == FireMode.BURST) {
                state.remainingShots--;
                if (state.remainingShots <= 0) {
                    clearActiveFields(state);
                    iterator.remove();
                }
            }
        }

        maintenanceIfDue(currentTick);
        return due == null ? List.of() : List.copyOf(due);
    }

    public synchronized int previewPatternShotIndex(UUID weaponId, CompiledFireControl control, long currentTick) {
        if (control == null) return 0;
        State state = states.computeIfAbsent(weaponId, ignored -> new State());
        if (state.lastSuccessfulShotTick == Long.MIN_VALUE
                || currentTick - state.lastSuccessfulShotTick > control.patternResetTicks()) {
            return 0;
        }
        return state.patternShotIndex + 1;
    }

    public synchronized void commitPatternShot(UUID weaponId, int shotIndex, long currentTick) {
        State state = states.computeIfAbsent(weaponId, ignored -> new State());
        state.patternShotIndex = Math.max(0, shotIndex);
        state.lastSuccessfulShotTick = currentTick;
    }

    public synchronized void cancel(UUID weaponId) {
        State state = states.get(weaponId);
        if (state == null) return;
        deactivate(weaponId, state);
    }

    public synchronized boolean isActive(UUID weaponId) {
        return active.contains(weaponId);
    }

    public synchronized int activeCount() { return active.size(); }
    public synchronized int trackedCount() { return states.size(); }

    private void deactivate(UUID weaponId, State state) {
        clearActiveFields(state);
        active.remove(weaponId);
    }

    private static void clearActiveFields(State state) {
        state.activeMode = null;
        state.activeControl = null;
        state.remainingShots = 0;
    }

    private void maintenanceIfDue(long currentTick) {
        if (currentTick < nextMaintenanceTick) return;
        nextMaintenanceTick = currentTick + MAINTENANCE_INTERVAL_TICKS;
        if (states.isEmpty()) return;

        Iterator<Map.Entry<UUID, State>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, State> entry = iterator.next();
            if (active.contains(entry.getKey())) continue;
            State state = entry.getValue();
            double readyAge = currentTick - state.nextAllowedTick;
            long patternAge = state.lastSuccessfulShotTick == Long.MIN_VALUE
                    ? Long.MAX_VALUE
                    : currentTick - state.lastSuccessfulShotTick;
            if (readyAge > IDLE_RETENTION_TICKS && patternAge > IDLE_RETENTION_TICKS) iterator.remove();
        }
    }

    public record RequestResult(boolean fireNow, boolean activeSequence) {}

    private static final class State {
        private double nextAllowedTick = Double.NEGATIVE_INFINITY;
        private FireMode activeMode;
        private CompiledFireControl activeControl;
        private int remainingShots;
        private long autoLeaseUntil;
        private int patternShotIndex;
        private long lastSuccessfulShotTick = Long.MIN_VALUE;
    }
}
