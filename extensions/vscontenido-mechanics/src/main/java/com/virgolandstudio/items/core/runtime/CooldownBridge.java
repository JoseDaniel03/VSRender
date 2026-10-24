package com.virgolandstudio.items.core.runtime;

/** Timestamp/tick based cooldown service. No repeating task is required per cooldown. */
public interface CooldownBridge {
    boolean ready(String key);
    long remainingTicks(String key);
    void start(String key, long durationTicks);
}
