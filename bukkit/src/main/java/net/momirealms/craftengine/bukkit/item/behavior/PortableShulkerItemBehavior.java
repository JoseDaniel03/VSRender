package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.core.plugin.context.PlayerOptionalContext;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.bukkit.plugin.gui.ObjectGuiAssets;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviorFactory;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portable interaction for real 27-slot vanilla shulker storage.
 *
 * The item deliberately uses shulker_box as its technical material, even when
 * the resource pack makes it look like a leather bundle/backpack. Minecraft
 * remains responsible for placement, breaking, inventory persistence and hover
 * tooltip. Only opening in-hand is added here.
 *
 * Shift-right-click a block uses vanilla placement; normal right-click opens
 * the exact same contents from the player's hand.
 */
public final class PortableShulkerItemBehavior extends ItemBehavior {
    public static final ItemBehaviorFactory<PortableShulkerItemBehavior> FACTORY = new Factory();
    private static final Set<String> OPEN_IDENTITIES = ConcurrentHashMap.newKeySet();
    private static volatile boolean listenerRegistered;

    private final String title;

    private PortableShulkerItemBehavior(String title) {
        this.title = title;
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            // Sneaking places a real shulker box; no second custom storage backend.
            return InteractionResult.PASS;
        }
        return open(player, context.getHand());
    }

    @Override
    public InteractionResult use(net.momirealms.craftengine.core.world.World world,
                                 Player player, InteractionHand hand) {
        return player == null ? InteractionResult.PASS : open(player, hand);
    }

    private InteractionResult open(Player serverPlayer, InteractionHand hand) {
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) serverPlayer.platformPlayer();
        boolean main = hand == InteractionHand.MAIN_HAND;
        ItemStack held = main ? player.getInventory().getItemInMainHand() : player.getInventory().getItemInOffHand();
        if (!isShulker(held) || !(held.getItemMeta() instanceof BlockStateMeta meta)
                || !(meta.getBlockState() instanceof ShulkerBox box) || held.getAmount() != 1) {
            return InteractionResult.PASS;
        }

        NamespacedKey identityKey = new NamespacedKey(BukkitCraftEngine.instance().javaPlugin(), "portable_storage_id");
        String identity = meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING);
        if (identity == null || identity.isBlank()) {
            identity = UUID.randomUUID().toString();
            meta.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, identity);
            held.setItemMeta(meta);
            setHand(player, main, held);
        }
        if (!OPEN_IDENTITIES.add(identity)) return InteractionResult.FAIL;

        try {
            int slot = main ? player.getInventory().getHeldItemSlot() : 40;
            Session session = new Session(player, slot, identity, identityKey);
            // Snapshot inventory copies the same vanilla shulker data that will
            // be placed in the world; do not store it in an unrelated plugin DB.
            session.inventory.setStorageContents(box.getSnapshotInventory().getStorageContents());
            player.openInventory(session.inventory);
            return InteractionResult.SUCCESS_AND_CANCEL;
        } catch (RuntimeException ex) {
            OPEN_IDENTITIES.remove(identity);
            BukkitCraftEngine.instance().logger().warn("Could not open portable shulker backpack", ex);
            return InteractionResult.FAIL;
        }
    }

    private static void setHand(org.bukkit.entity.Player player, boolean main, ItemStack stack) {
        if (main) player.getInventory().setItemInMainHand(stack);
        else player.getInventory().setItemInOffHand(stack);
    }

    private static boolean isShulker(ItemStack stack) {
        return stack != null && stack.getType().name().endsWith("SHULKER_BOX");
    }

    private final class Session implements InventoryHolder {
        final org.bukkit.entity.Player player;
        final int slot;
        final String identity;
        final NamespacedKey identityKey;
        final Inventory inventory;
        boolean completed;

        Session(org.bukkit.entity.Player player, int slot, String identity, NamespacedKey identityKey) {
            this.player = player;
            this.slot = slot;
            this.identity = identity;
            this.identityKey = identityKey;
            this.inventory = Bukkit.createInventory(this, 27,
                    AdventureHelper.deserialize(title, PlayerOptionalContext.of(BukkitAdaptor.adapt(player))));
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        void finish() {
            if (completed) return;
            completed = true;
            try {
                ItemStack stack = player.getInventory().getItem(slot);
                if (!isShulker(stack) || !(stack.getItemMeta() instanceof BlockStateMeta meta)
                        || !identity.equals(meta.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING))
                        || !(meta.getBlockState() instanceof ShulkerBox box)) {
                    BukkitCraftEngine.instance().logger().warn(
                            "Portable backpack moved while opened; not overwriting a different item");
                    return;
                }
                box.getSnapshotInventory().setStorageContents(inventory.getStorageContents());
                meta.setBlockState(box);
                stack.setItemMeta(meta);
                player.getInventory().setItem(slot, stack);
            } finally {
                OPEN_IDENTITIES.remove(identity);
            }
        }
    }

    private static final class SessionListener implements Listener {
        private static Session session(org.bukkit.inventory.Inventory inventory) {
            return inventory.getHolder() instanceof PortableShulkerItemBehavior.Session session ? session : null;
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onClick(InventoryClickEvent event) {
            Session session = session(event.getView().getTopInventory());
            if (session == null) return;
            if (event.getAction() == InventoryAction.HOTBAR_SWAP
                    || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
                event.setCancelled(true);
                return;
            }
            // Block moving the source item or inserting any shulker/bag into the bag.
            if (event.getClickedInventory() == event.getView().getBottomInventory()
                    && event.getSlot() == session.slot) {
                event.setCancelled(true);
                return;
            }
            if (isShulker(event.getCursor()) || isShulker(event.getCurrentItem())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onDrag(InventoryDragEvent event) {
            Session session = session(event.getView().getTopInventory());
            if (session == null) return;
            if (isShulker(event.getOldCursor())) {
                event.setCancelled(true);
                return;
            }
            int top = event.getView().getTopInventory().getSize();
            for (int raw : event.getRawSlots()) {
                if (raw >= top && event.getView().convertSlot(raw) == session.slot) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            Session session = session(event.getInventory());
            if (session != null) session.finish();
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            Inventory top = event.getPlayer().getOpenInventory().getTopInventory();
            Session session = session(top);
            if (session != null) session.finish();
        }

        @EventHandler(ignoreCancelled = true)
        public void onSwap(PlayerSwapHandItemsEvent event) {
            if (session(event.getPlayer().getOpenInventory().getTopInventory()) != null) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onDrop(PlayerDropItemEvent event) {
            if (session(event.getPlayer().getOpenInventory().getTopInventory()) != null
                    && isShulker(event.getItemDrop().getItemStack())) {
                event.setCancelled(true);
            }
        }
    }

    private static class Factory implements ItemBehaviorFactory<PortableShulkerItemBehavior> {
        @Override
        public PortableShulkerItemBehavior create(Pack pack, Path path, Key id, ConfigSection section) {
            if (!listenerRegistered) {
                synchronized (PortableShulkerItemBehavior.class) {
                    if (!listenerRegistered) {
                        Bukkit.getPluginManager().registerEvents(new SessionListener(),
                                BukkitCraftEngine.instance().javaPlugin());
                        listenerRegistered = true;
                    }
                }
            }
            String title = ObjectGuiAssets.titleFor(BukkitCraftEngine.instance(),
                    section.getString("gui"), section.getString("title", "Mochila basica"));
            return new PortableShulkerItemBehavior(title);
        }
    }
}
