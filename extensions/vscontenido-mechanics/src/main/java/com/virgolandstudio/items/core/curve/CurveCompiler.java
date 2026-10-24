package com.virgolandstudio.items.core.curve;

import com.virgolandstudio.items.api.curve.CurveDefinition;
import com.virgolandstudio.items.api.curve.CurveInterpolation;
import com.virgolandstudio.items.api.curve.CurvePoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CurveCompiler {
    private CurveCompiler() {}

    public static CompiledCurve compile(Object raw) {
        CurveDefinition definition = parse(raw);
        int size = definition.points().size();
        double[] x = new double[size];
        double[] y = new double[size];
        double[] in = new double[size];
        double[] out = new double[size];
        for (int i = 0; i < size; i++) {
            CurvePoint point = definition.points().get(i);
            x[i] = point.x();
            y[i] = point.y();
            in[i] = point.inTangent() == null ? Double.NaN : point.inTangent();
            out[i] = point.outTangent() == null ? Double.NaN : point.outTangent();
        }
        return new CompiledCurve(definition.interpolation(), x, y, in, out);
    }

    @SuppressWarnings("unchecked")
    public static CurveDefinition parse(Object raw) {
        if (raw instanceof CurveDefinition definition) return definition;
        if (!(raw instanceof Map<?, ?> map)) throw new IllegalArgumentException("curve must be a CurveDefinition or map");

        Object interpolationRaw = map.containsKey("interpolation") ? map.get("interpolation") : "LINEAR";
        CurveInterpolation interpolation;
        try {
            interpolation = CurveInterpolation.valueOf(String.valueOf(interpolationRaw).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported curve interpolation: " + interpolationRaw);
        }

        Object pointsRaw = map.get("points");
        if (!(pointsRaw instanceof List<?> list)) throw new IllegalArgumentException("curve.points must be a list");
        var points = new ArrayList<CurvePoint>(list.size());
        for (Object pointRaw : list) {
            if (pointRaw instanceof CurvePoint point) {
                points.add(point);
                continue;
            }
            if (!(pointRaw instanceof Map<?, ?> pointMap)) throw new IllegalArgumentException("curve point must be a map");
            double x = number(pointMap.get("x"), "curve point x");
            double y = number(pointMap.get("y"), "curve point y");
            Double in = optionalNumber(pointMap.get("inTangent"), "curve point inTangent");
            Double out = optionalNumber(pointMap.get("outTangent"), "curve point outTangent");
            points.add(new CurvePoint(x, y, in, out));
        }
        return new CurveDefinition(interpolation, points);
    }

    private static double number(Object raw, String label) {
        if (!(raw instanceof Number number)) throw new IllegalArgumentException(label + " must be numeric");
        return number.doubleValue();
    }

    private static Double optionalNumber(Object raw, String label) {
        if (raw == null) return null;
        return number(raw, label);
    }
}
