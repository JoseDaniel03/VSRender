package com.virgolandstudio.items.core.cinematic;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Resolves one sequence for one viewer from a reusable set of audience branches. */
public final class CinematicBranchResolver {
    private CinematicBranchResolver() {}

    public static Optional<CinematicSequence> resolve(
            List<CinematicBranch> branches,
            CinematicAudienceContext context
    ) {
        Objects.requireNonNull(branches, "branches");
        Objects.requireNonNull(context, "context");

        // Explicit specificity order avoids ALL_PLAYERS or OTHER_PLAYERS shadowing a more
        // meaningful role when the config happens to list a broad branch first.
        CinematicAudienceRole[] priority = {
                CinematicAudienceRole.SPECTATORS,
                CinematicAudienceRole.TURN_OWNER,
                CinematicAudienceRole.ACTIVE_SIDE,
                CinematicAudienceRole.OTHER_SIDES,
                CinematicAudienceRole.OTHER_PLAYERS,
                CinematicAudienceRole.ALL_PLAYERS
        };
        for (CinematicAudienceRole role : priority) {
            if (!matches(role, context)) continue;
            for (CinematicBranch branch : branches) {
                if (branch.audience() == role) return Optional.of(branch.sequence());
            }
        }
        return Optional.empty();
    }

    public static boolean matches(CinematicAudienceRole role, CinematicAudienceContext context) {
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(context, "context");
        return switch (role) {
            case ALL_PLAYERS -> !context.spectator();
            case TURN_OWNER -> !context.spectator() && context.turnOwner();
            case OTHER_PLAYERS -> !context.spectator() && !context.turnOwner();
            case ACTIVE_SIDE -> !context.spectator() && context.activeSideMember();
            case OTHER_SIDES -> !context.spectator() && !context.activeSideMember();
            case SPECTATORS -> context.spectator();
        };
    }
}
