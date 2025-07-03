package org.reujdon.itemFrameLock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemFrameLock extends JavaPlugin  implements Listener {

    private NamespacedKey ownerKey;
    private NamespacedKey locked;

    @Override
    public void onEnable() {
        ownerKey = new NamespacedKey(this, "owner");
        locked = new NamespacedKey(this, "locked");

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ItemFrameLock enabled.");
    }

    private void toggleItemFrameLock(Player player, ItemFrame frame) {
        Boolean frameLocked = frame.getPersistentDataContainer().getOrDefault(locked, PersistentDataType.BOOLEAN, false);

//        Lock
        if (!frameLocked) {
            frame.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            frame.getPersistentDataContainer().set(locked, PersistentDataType.BOOLEAN, true);
            player.sendActionBar(Component.text("Item frame locked!", NamedTextColor.GOLD));
            return;
        }

        String ownerUuid = frame.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

//        Unlock
        assert ownerUuid != null;
        if (!ownerUuid.equals(player.getUniqueId().toString()) && !player.isOp()) {
            player.sendActionBar(Component.text("You don't have permission to unlock that item frame!", NamedTextColor.RED));
            return;
        }

        frame.getPersistentDataContainer().remove(ownerKey);
        frame.getPersistentDataContainer().set(locked, PersistentDataType.BOOLEAN, false);

        player.sendActionBar(Component.text("Item frame unlocked!", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame frame)) return;

        Player player = event.getPlayer();
        Block behind = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());


        if (!player.isSneaking() || player.getInventory().getItemInMainHand().getType() != Material.AIR) {

            if (behind.getState() instanceof Container container) {
                player.openInventory(container.getInventory());
                event.setCancelled(true);
            }

            return;
        }

        if (player.isSneaking() && behind.getState() instanceof Container && player.getInventory().getItemInMainHand().getType() != Material.AIR)
            return;

//        Toggle lock
        toggleItemFrameLock(player, frame);
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        Boolean frameLocked = frame.getPersistentDataContainer().getOrDefault(locked, PersistentDataType.BOOLEAN, false);

        if (!frameLocked)
            return; // Allow unowned item frames

        event.setCancelled(true);
        player.sendActionBar(Component.text("Item frame locked!", NamedTextColor.RED));
    }
}
