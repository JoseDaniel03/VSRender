package com.virgolandstudio.items.core.cinematic;

import java.util.List;
import java.util.Objects;

public record CinematicSequence(String id, List<CinematicStep> steps) {
    public CinematicSequence {
        Objects.requireNonNull(id, "id");
        steps = List.copyOf(Objects.requireNonNull(steps, "steps"));
        if (steps.isEmpty()) throw new IllegalArgumentException("sequence must contain at least one step");
    }
}
