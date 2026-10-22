package studio.virgoland.vsrprobe;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import net.momirealms.craftengine.bukkit.api.PlayerAnimationService;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;

/** Test-only controlled player. Does not certify packet rendering or first-person animation. */
public final class AnimationProbe extends JavaPlugin {
    @Override public void onEnable() {
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
            try {
                var service = Bukkit.getServicesManager().load(PlayerAnimationService.class);
                if (!Bukkit.getPluginManager().isPluginEnabled("BetterModel")) {
                    if (service != null) throw new IllegalStateException("Service registered without BetterModel");
                    getLogger().info("VSR_ANIMATION_OPTIONAL_PASS"); return;
                }
                if (service == null) throw new IllegalStateException("Animation service missing");
                var location = new Location(Bukkit.getWorlds().getFirst(), 8, 125, 8);
                var id = UUID.randomUUID();
                var count = new AtomicInteger();
                var scheduler = (EntityScheduler) Proxy.newProxyInstance(getClassLoader(), new Class[]{EntityScheduler.class}, (proxy, method, args) -> {
                    if (method.getName().equals("execute")) {
                        Bukkit.getRegionScheduler().execute(this, location, (Runnable) args[1]);
                        count.incrementAndGet(); return true;
                    }
                    return null;
                });
                Player player = (Player) Proxy.newProxyInstance(getClassLoader(), new Class[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "getScheduler" -> scheduler;
                    case "isOnline" -> true;
                    case "isDead" -> false;
                    case "getName" -> "AnimationProbe";
                    default -> null;
                });
                var corePlayer = (net.momirealms.craftengine.core.entity.player.Player) Proxy.newProxyInstance(getClassLoader(),
                    new Class[]{net.momirealms.craftengine.core.entity.player.Player.class}, (proxy, method, args) -> {
                        if (method.getName().equals("platformEntity")) return player;
                        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                        return null;
                    });
                Context context = (Context) Proxy.newProxyInstance(getClassLoader(), new Class[]{Context.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getOptionalParameter")) return Optional.of(corePlayer);
                    if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                    return null;
                });
                CommonFunctions.fromConfig(ConfigSection.ofRoot(Map.of("type", "vsrender:play_player_animation", "limb", "missing_probe_limb", "animation", "missing"))).run(context);
                CommonFunctions.fromConfig(ConfigSection.ofRoot(Map.of("type", "vsrender:stop_player_animation"))).run(context);
                try {
                    CommonFunctions.fromConfig(ConfigSection.ofRoot(Map.of("type", "vsrender:play_player_animation", "target", "invalid", "limb", "steve", "animation", "missing")));
                    throw new IllegalStateException("Invalid target accepted");
                } catch (IllegalArgumentException expected) {}
                if (service.play(player, "", "missing", false).toCompletableFuture().join().status() != PlayerAnimationService.Status.INVALID_RESOURCE)
                    throw new IllegalStateException("Empty resource accepted");
                service.play(player, "missing_probe_limb", "missing", false).whenComplete((result, error) -> {
                    if (error != null || result.status() != PlayerAnimationService.Status.INVALID_RESOURCE || count.get() < 3) {
                        getLogger().severe("VSR_ANIMATION_FAIL: " + result + " " + error); return;
                    }
                    service.stop(player).whenComplete((stopped, stopError) -> {
                        if (stopError == null && stopped.status() == PlayerAnimationService.Status.NOT_RUNNING)
                            getLogger().info("VSR_ANIMATION_BRIDGE_PASS");
                        else getLogger().severe("VSR_ANIMATION_FAIL: " + stopped);
                    });
                });
            } catch (Throwable failure) { getLogger().severe("VSR_ANIMATION_FAIL: " + failure); failure.printStackTrace(); }
        }, 100L);
    }
}
