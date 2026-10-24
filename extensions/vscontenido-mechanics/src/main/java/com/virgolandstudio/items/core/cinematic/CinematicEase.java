package com.virgolandstudio.items.core.cinematic;

/** Small easing set intentionally kept frontend-agnostic. */
public enum CinematicEase {
    LINEAR {
        @Override public double apply(double t) { return clamp(t); }
    },
    SMOOTHSTEP {
        @Override public double apply(double t) {
            double x = clamp(t);
            return x * x * (3.0 - 2.0 * x);
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override public double apply(double t) {
            double x = clamp(t);
            return x < 0.5 ? 4.0 * x * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 3.0) / 2.0;
        }
    };

    public abstract double apply(double t);

    static double clamp(double t) { return Math.max(0.0, Math.min(1.0, t)); }
}
