package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import com.makotomiyamoto.locksmithy.lock.RegisteredKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class LockpickAttemptHandler implements Listener {

    private Locksmithy plugin;

    public LockpickAttemptHandler(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {

        ConfigurationSection messages = plugin.getConfig().getConfigurationSection("options.messages");
        assert messages != null;

        EquipmentSlot slot = event.getHand();
        if (slot == null) return;

        if (event.getClickedBlock() == null) return;
        LocationReference reference = LocationReference.findByFile(plugin, event.getClickedBlock().getLocation());
        if (reference == null) return;

        if (reference.isExposed()) return;

        if (slot.equals(EquipmentSlot.HAND)
                && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && event.getPlayer().isSneaking()) {

            Player player = event.getPlayer();
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (RegisteredKey.isLockpick(plugin, mainHand)) {

                if (player.getUniqueId().toString().equals(reference.getOwnerByUuid())) {
                    return;
                }
                if (reference.isAdvancedLock()) {
                    plugin.sendMessage(player, messages.getString("lock-unbreakable"));
                    return;
                }
                if (reference.isJammed()) {
                    plugin.sendMessage(player, messages.getString("lock-jammed"));
                    return;
                }

                int successLine = RegisteredKey
                        .findLoreIndexFromConfig(plugin, "%SUCCESS_RATE%", "options.lockpick");
                int failureLine = RegisteredKey
                        .findLoreIndexFromConfig(plugin, "%CRITICAL_FAILURE_RATE%", "options.lockpick");

                //noinspection ConstantConditions
                double success = Double.parseDouble(
                        mainHand.getItemMeta().getLore().get(successLine)
                                .replaceAll("§[A-z0-9]", "")
                                .replaceAll("[^0-9.]", "")) / 100;
                double failure = Double.parseDouble(
                        mainHand.getItemMeta().getLore().get(failureLine)
                                .replaceAll("§[A-z0-9]", "")
                                .replaceAll("[^0-9.]", "")) / 100;

                if (Math.random() < success) {
                    // TODO open lock
                    reference.setExposed(true);
                    plugin.sendMessage(player, messages.getString("lockpick-success"));
                } else if (Math.random() < failure) {
                    // TODO jam lock
                    reference.setJammed(true);
                    plugin.sendMessage(player, messages.getString("lockpick-failure-critical"));
                } else {
                    // TODO print failure
                    plugin.sendMessage(player, messages.getString("lockpick-failure"));
                }
                reference.save(plugin);

                mainHand.setAmount(mainHand.getAmount() - 1);
                player.getInventory().setItem(player.getInventory().getHeldItemSlot(), mainHand);

                event.setCancelled(true);

            }

        }

    }

}
