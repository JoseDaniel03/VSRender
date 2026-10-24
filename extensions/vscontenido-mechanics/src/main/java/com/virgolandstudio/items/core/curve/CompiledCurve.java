package com.virgolandstudio.items.core.curve;

import com.virgolandstudio.items.api.curve.CurveInterpolation;

import java.util.Arrays;

/** Primitive-array hot-path representation of a curve. */
public final class CompiledCurve {
    private final CurveInterpolation interpolation;
    private final double[] x;
    private final double[] y;
    private final double[] inTangents;
    private final double[] outTangents;

    CompiledCurve(CurveInterpolation interpolation, double[] x, double[] y, double[] inTangents, double[] outTangents) {
        this.interpolation = interpolation;
        this.x = x;
        this.y = y;
        this.inTangents = inTangents;
        this.outTangents = outTangents;
    }

    public double evaluate(double input) {
        if (!Double.isFinite(input)) return y[0];
        if (input <= x[0]) return y[0];
        int last = x.length - 1;
        if (input >= x[last]) return y[last];

        int found = Arrays.binarySearch(x, input);
        if (found >= 0) return y[found];
        int right = -found - 1;
        int left = right - 1;
        double span = x[right] - x[left];
        double t = (input - x[left]) / span;

        return switch (interpolation) {
            case STEP -> y[left];
            case LINEAR -> lerp(y[left], y[right], t);
            case SMOOTH -> lerp(y[left], y[right], t * t * (3.0 - 2.0 * t));
            case BEZIER -> cubicHermite(left, right, span, t);
        };
    }

    private double cubicHermite(int left, int right, double span, double t) {
        double secant = (y[right] - y[left]) / span;
        double m0 = Double.isNaN(outTangents[left]) ? secant : outTangents[left];
        double m1 = Double.isNaN(inTangents[right]) ? secant : inTangents[right];
        double t2 = t * t;
        double t3 = t2 * t;
        double h00 = 2.0 * t3 - 3.0 * t2 + 1.0;
        double h10 = t3 - 2.0 * t2 + t;
        double h01 = -2.0 * t3 + 3.0 * t2;
        double h11 = t3 - t2;
        return h00 * y[left] + h10 * span * m0 + h01 * y[right] + h11 * span * m1;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public int pointCount() { return x.length; }
    public CurveInterpolation interpolation() { return interpolation; }
}
