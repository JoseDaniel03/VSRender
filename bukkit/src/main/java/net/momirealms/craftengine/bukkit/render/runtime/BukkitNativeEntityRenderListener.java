package net.momirealms.craftengine.bukkit.render.runtime;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import net.momirealms.craftengine.bukkit.plugin.listener.AbstractListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;

/** Native renderer lifecycle bridge for events that cannot be inferred safely from polling. */
public final class BukkitNativeEntityRenderListener extends AbstractListener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        BukkitNativeRigProjectileInterceptor.track(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getHitBlock() != null) {
            BukkitNativeRigProjectileInterceptor.untrack(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getEntities()) {
            if (entity instanceof Projectile projectile) {
                BukkitNativeRigProjectileInterceptor.track(projectile);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRiderDamage(EntityDamageByEntityEvent event) {
        java.util.UUID damagerUuid = event.getDamager().getUniqueId();
        if (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof org.bukkit.entity.Entity shooter) {
            damagerUuid = shooter.getUniqueId();
        }
        if (!BukkitNativeRigMountRegistry.canRiderDamage(
                event.getEntity().getUniqueId(), damagerUuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player && BukkitNativeRigMountRegistry.shouldCancelSelfDismount(event.getDismounted())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player) {
            BukkitNativeRigMountRegistry.onDismounted(event.getDismounted(), player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        BukkitNativeRigMountRegistry.forceDismount(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        BukkitNativeEntityRenderRegistry.damage(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityJump(EntityJumpEvent event) {
        BukkitNativeEntityRenderRegistry.jump(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJump(PlayerJumpEvent event) {
        BukkitNativeEntityRenderRegistry.jump(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            BukkitNativeRigMountRegistry.forceDismount(player.getUniqueId());
        }
        BukkitNativeEntityRenderRegistry.death(event.getEntity().getUniqueId());
    }
}
