package com.virgolandstudio.items.api.curve;

import java.util.List;
import java.util.Objects;

/** Immutable authored curve. Values outside the authored X range clamp to the first/last point. */
public record CurveDefinition(CurveInterpolation interpolation, List<CurvePoint> points) {
    public CurveDefinition {
        interpolation = Objects.requireNonNull(interpolation, "interpolation");
        points = List.copyOf(Objects.requireNonNull(points, "points"));
        if (points.size() < 2) throw new IllegalArgumentException("Curve requires at least 2 points");
        if (points.size() > 64) throw new IllegalArgumentException("Curve supports at most 64 points");
        double previousX = Double.NEGATIVE_INFINITY;
        for (CurvePoint point : points) {
            Objects.requireNonNull(point, "curve point");
            if (!(point.x() > previousX)) throw new IllegalArgumentException("Curve point X values must be strictly increasing");
            previousX = point.x();
        }
    }

    public static CurveDefinition linear(List<CurvePoint> points) {
        return new CurveDefinition(CurveInterpolation.LINEAR, points);
    }
}
