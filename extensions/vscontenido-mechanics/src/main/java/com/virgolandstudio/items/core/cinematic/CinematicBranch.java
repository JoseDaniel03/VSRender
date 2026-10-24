package com.virgolandstudio.items.core.cinematic;

import java.util.Objects;

public record CinematicBranch(CinematicAudienceRole audience, CinematicSequence sequence) {
    public CinematicBranch {
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(sequence, "sequence");
    }
}
