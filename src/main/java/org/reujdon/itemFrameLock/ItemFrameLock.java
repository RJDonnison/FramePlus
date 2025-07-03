package org.reujdon.itemFrameLock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemFrameLock extends JavaPlugin  implements Listener {

    private NamespacedKey ownerKey;

    @Override
    public void onEnable() {
        ownerKey = new NamespacedKey(this, "owner");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ItemFrameLock enabled.");
    }

    @EventHandler
    public void onItemFramePlace(HangingPlaceEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        frame.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());
    }

    @EventHandler
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame frame)) return;

        String ownerUuid = frame.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

        Player player = event.getPlayer();

        if (ownerUuid == null)
            return; // Allow unowned item frames

        if (ownerUuid.equals(player.getUniqueId().toString()))
            return; // The owner - allow


        if (player.isOp())
            return; // OP - allow

        event.setCancelled(true);
        player.sendActionBar(Component.text("You don't have permission to interact with this item frame.", NamedTextColor.RED));
    }

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        String ownerUuid = frame.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

        if (ownerUuid == null)
            return; // Allow unowned item frames

        if (ownerUuid.equals(player.getUniqueId().toString()))
            return; // The owner - allow


        if (player.isOp())
            return; // OP - allow


        event.setCancelled(true);
        player.sendActionBar(Component.text("You don't have permission to interact with this item frame.", NamedTextColor.RED));
    }
}
