package com.virgolandstudio.items.api.curve;

/** Tangents are dy/dx slopes used by BEZIER interpolation; null falls back to the segment slope. */
public record CurvePoint(double x, double y, Double inTangent, Double outTangent) {
    public CurvePoint(double x, double y) {
        this(x, y, null, null);
    }

    public CurvePoint {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("Curve point coordinates must be finite");
        }
        if (inTangent != null && !Double.isFinite(inTangent)) {
            throw new IllegalArgumentException("Curve point inTangent must be finite when present");
        }
        if (outTangent != null && !Double.isFinite(outTangent)) {
            throw new IllegalArgumentException("Curve point outTangent must be finite when present");
        }
    }
}
