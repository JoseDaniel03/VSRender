package net.momirealms.craftengine.bukkit.api;

import org.bukkit.entity.Player;
import java.util.concurrent.CompletionStage;

/** Obtain through Bukkit ServicesManager while a player-animation provider is available.
 * BetterModel is currently an adapter; callers depend only on this VsRender-facing service.
 * Visual animation only: game effects remain the caller's responsibility.
 */
public interface PlayerAnimationService {
    enum Status { STARTED, STOPPED, NOT_RUNNING, CANCELLED, UNAVAILABLE, INVALID_RESOURCE, CONFLICT, FAILED }
    record Result(Status status, String detail) {}
    CompletionStage<Result> play(Player player, String limb, String animation, boolean loop);
    CompletionStage<Result> stop(Player player);
}
