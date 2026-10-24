package com.virgolandstudio.items.core.effect;

/** Cheap shared budget for particle/sound emissions. Intended to be reset once per server tick. */
public final class EffectBudget {
    private final int maxParticlesPerTick;
    private final int maxSoundsPerTick;
    private long tick = Long.MIN_VALUE;
    private int particles;
    private int sounds;
    private long droppedParticles;
    private long droppedSounds;

    public EffectBudget(int maxParticlesPerTick, int maxSoundsPerTick) {
        if (maxParticlesPerTick < 0 || maxSoundsPerTick < 0) throw new IllegalArgumentException("Effect limits must be >= 0");
        this.maxParticlesPerTick = maxParticlesPerTick;
        this.maxSoundsPerTick = maxSoundsPerTick;
    }

    public synchronized void beginTick(long currentTick) {
        if (tick == currentTick) return;
        tick = currentTick;
        particles = 0;
        sounds = 0;
    }

    public synchronized boolean tryParticles(int requested) {
        if (requested <= 0) return false;
        if ((long) particles + requested > maxParticlesPerTick) {
            droppedParticles += requested;
            return false;
        }
        particles += requested;
        return true;
    }

    public synchronized boolean trySound() {
        if (sounds >= maxSoundsPerTick) {
            droppedSounds++;
            return false;
        }
        sounds++;
        return true;
    }

    public synchronized int particlesThisTick() { return particles; }
    public synchronized int soundsThisTick() { return sounds; }
    public synchronized long droppedParticles() { return droppedParticles; }
    public synchronized long droppedSounds() { return droppedSounds; }
    public int maxParticlesPerTick() { return maxParticlesPerTick; }
    public int maxSoundsPerTick() { return maxSoundsPerTick; }
}
