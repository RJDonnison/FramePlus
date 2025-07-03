package org.reujdon.itemFrameLock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class ItemFrameLock extends JavaPlugin  implements Listener {

    private NamespacedKey ownerKey;
    private NamespacedKey lockedKey;
    private NamespacedKey passThroughKey;
    private NamespacedKey rotationKey;

    @Override
    public void onEnable() {
        ownerKey = new NamespacedKey(this, "owner");
        lockedKey = new NamespacedKey(this, "locked");
        passThroughKey = new NamespacedKey(this, "passThrough");
        rotationKey = new NamespacedKey(this, "rotation");

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("ItemFrameLock enabled.");
    }

    private void toggleItemFrameLock(Player player, ItemFrame frame) {
        Boolean frameLocked = frame.getPersistentDataContainer().getOrDefault(lockedKey, PersistentDataType.BOOLEAN, false);

//        Lock
        if (!frameLocked) {
            frame.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
            frame.getPersistentDataContainer().set(lockedKey, PersistentDataType.BOOLEAN, true);

            player.playSound(frame.getLocation(), Sound.BLOCK_CHAIN_BREAK, 1.0f, 1.2f);
            frame.getWorld().spawnParticle(Particle.WAX_ON, frame.getLocation(), 10, 0.3, 0.3, 0);

            return;
        }

        String ownerUuid = frame.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);

//        Wrong player
        assert ownerUuid != null;
        if (!ownerUuid.equals(player.getUniqueId().toString()) && !player.isOp()) {
            player.sendActionBar(Component.text("You don't have permission to unlock that item frame!", NamedTextColor.RED));
            return;
        }

//        Unlock
        frame.getPersistentDataContainer().remove(ownerKey);
        frame.getPersistentDataContainer().set(lockedKey, PersistentDataType.BOOLEAN, false);

        player.playSound(frame.getLocation(), Sound.BLOCK_CHAIN_FALL, 1.0f, 1.2f);
        frame.getWorld().spawnParticle(Particle.WAX_OFF, frame.getLocation(), 10, 0.3, 0.3, 0);
    }

    private void lockRotation(ItemFrame frame, ItemStack item, Player player) {
        if (frame.getPersistentDataContainer().getOrDefault(rotationKey, PersistentDataType.BOOLEAN, false))
            return;

        frame.getPersistentDataContainer().set(rotationKey, PersistentDataType.BOOLEAN, true);
        item.setAmount(item.getAmount() - 1);

        player.playSound(frame.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_PLACE, 1.0f, 1.2f);
        frame.getWorld().spawnParticle(Particle.WAX_OFF, frame.getLocation(), 10, 0.3, 0.3, 0);
    }

    private void invisible(ItemFrame frame, Player player, ItemStack item) {
        if (!frame.isVisible())
            return;

        Damageable damageable = (Damageable) item.getItemMeta();
        int damage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();

        damageable.setDamage(damage + 1);
        item.setItemMeta(damageable);

        if (damage + 1 >= maxDurability) {
            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }

        frame.setInvisible(true);
        player.playSound(frame.getLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1.0f, 1.2f);
    }

    private void passThrough(ItemFrame frame, ItemStack item, Player player) {
        if (frame.getPersistentDataContainer().getOrDefault(passThroughKey, PersistentDataType.BOOLEAN, false))
            return;

        item.setAmount(item.getAmount() - 1);
        frame.getPersistentDataContainer().set(passThroughKey, PersistentDataType.BOOLEAN, true);

        player.playSound(frame.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        frame.getWorld().spawnParticle(Particle.DUST, frame.getLocation(), 10, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(102, 51, 0), 0.5f));
    }

    private void reset(ItemFrame frame, Player player, ItemStack item) {
        boolean passThrough = frame.getPersistentDataContainer().getOrDefault(passThroughKey, PersistentDataType.BOOLEAN, false);
        boolean rotLocked = frame.getPersistentDataContainer().getOrDefault(rotationKey, PersistentDataType.BOOLEAN, false);
        if (!passThrough && frame.isVisible() && !rotLocked)
            return;

        Damageable damageable = (Damageable) item.getItemMeta();
        int damage = damageable.getDamage();
        int maxDurability = item.getType().getMaxDurability();

        damageable.setDamage(damage + 1);
        item.setItemMeta(damageable);

        if (damage + 1 >= maxDurability) {
            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
        }

        frame.setFixed(false);
        frame.setInvisible(false);

        if (rotLocked) {
            ItemStack glassPane = new ItemStack(Material.GLASS_PANE, 1);
            frame.getWorld().dropItemNaturally(frame.getLocation(), glassPane);
        }
        frame.getPersistentDataContainer().set(rotationKey, PersistentDataType.BOOLEAN, false);

        if (passThrough) {
            ItemStack itemFrame = new ItemStack(Material.ITEM_FRAME, 1);
            frame.getWorld().dropItemNaturally(frame.getLocation(), itemFrame);
        }
        frame.getPersistentDataContainer().set(passThroughKey, PersistentDataType.BOOLEAN, false);

        player.playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.2f);
        frame.getWorld().spawnParticle(Particle.WAX_OFF, frame.getLocation(), 10, 0.3, 0.3, 0);
    }

    @EventHandler
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame frame)) return;

        Player player = event.getPlayer();

        Block behind = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
        boolean passThrough = frame.getPersistentDataContainer().getOrDefault(passThroughKey, PersistentDataType.BOOLEAN, false);

        if (behind.getState() instanceof Container container && passThrough && !player.isSneaking()) {
            player.openInventory(container.getInventory());
            event.setCancelled(true);
            return;
        }

        String owner = frame.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !player.getUniqueId().toString().equals(owner)) {
            event.setCancelled(true);

            player.playSound(frame.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.2f);
            frame.getWorld().spawnParticle(Particle.DUST, frame.getLocation(), 10, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(215, 66, 66), 1.0f));
            return;
        }

        boolean rotLocked = frame.getPersistentDataContainer().getOrDefault(rotationKey, PersistentDataType.BOOLEAN, false);
        if (rotLocked && !player.isSneaking() && frame.getItem().getType() != Material.AIR) {
            player.playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.2f);
            event.setCancelled(true);
        }

        if (!player.isSneaking()) return;

        boolean didAction = false;

        ItemStack item = player.getInventory().getItemInMainHand();
        Material type = item.getType();

        if (type == Material.CHAIN) {
            toggleItemFrameLock(player, frame);
            didAction = true;
        }

        if (type == Material.GLASS_PANE) {
            lockRotation(frame, item, player);
            didAction = true;
        }

        if (type == Material.SHEARS){
            invisible(frame, player, item);
            didAction = true;
        }

        if (type == Material.ITEM_FRAME) {
            passThrough(frame, item, player);
            didAction = true;
        }

        if (type == Material.BRUSH) {
            reset(frame, player, item);
            didAction = true;
        }

        if (didAction)
            event.setCancelled(true);

        if (rotLocked && frame.getItem().getType() != Material.AIR) {
            player.playSound(frame.getLocation(), Sound.ENTITY_ITEM_FRAME_ADD_ITEM, 1.0f, 1.2f);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemFrameDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        Boolean frameLocked = frame.getPersistentDataContainer().getOrDefault(lockedKey, PersistentDataType.BOOLEAN, false);

        if (!frameLocked)
            return;

        event.setCancelled(true);

        player.playSound(frame.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.2f);
        frame.getWorld().spawnParticle(Particle.DUST, frame.getLocation(), 20, 0.3, 0.3, 0, new Particle.DustOptions(Color.fromRGB(102, 51, 0), 1.0f));
    }

    @EventHandler
    public void onItemFrameBreak(HangingBreakByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame frame)) return;

        if (frame.getPersistentDataContainer().getOrDefault(rotationKey, PersistentDataType.BOOLEAN, false)) {
            ItemStack glassPane = new ItemStack(Material.GLASS_PANE, 1);
            frame.getWorld().dropItemNaturally(frame.getLocation(), glassPane);
        }

        if (frame.getPersistentDataContainer().getOrDefault(passThroughKey, PersistentDataType.BOOLEAN, false)) {
            ItemStack itemFrame = new ItemStack(Material.ITEM_FRAME, 1);
            frame.getWorld().dropItemNaturally(frame.getLocation(), itemFrame);
        }
    }
}
