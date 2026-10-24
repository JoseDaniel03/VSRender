package com.virgolandstudio.items.core.cinematic;

/**
 * Frontend-agnostic viewer context used to resolve a cinematic branch.
 *
 * <p>Specific roles win over broad fallbacks. A turn owner may also be an active-side member,
 * but TURN_OWNER is considered more specific than ACTIVE_SIDE.</p>
 */
public record CinematicAudienceContext(
        boolean turnOwner,
        boolean activeSideMember,
        boolean spectator
) {
    public static CinematicAudienceContext forTurnOwner() {
        return new CinematicAudienceContext(true, true, false);
    }

    public static CinematicAudienceContext forOtherPlayer() {
        return new CinematicAudienceContext(false, false, false);
    }

    public static CinematicAudienceContext forActiveSideMember() {
        return new CinematicAudienceContext(false, true, false);
    }

    public static CinematicAudienceContext forSpectatorViewer() {
        return new CinematicAudienceContext(false, false, true);
    }
}
