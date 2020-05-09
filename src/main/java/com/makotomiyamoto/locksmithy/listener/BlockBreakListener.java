package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocalPlayerData;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.UUID;

public final class BlockBreakListener implements Listener {

    private Locksmithy plugin;

    public BlockBreakListener(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        ConfigurationSection messages = plugin.getConfig().getConfigurationSection("options.messages");
        assert messages != null;

        LocationReference reference = LocationReference.findByFile(plugin, event.getBlock().getLocation());
        if (reference == null)
            return;

        Player owner = Bukkit.getPlayer(UUID.fromString(reference.getOwnerByUuid()));
        Player player = event.getPlayer();
        LocalPlayerData playerData = LocalPlayerData.fetchFromFile(plugin, player);

        if (!player.equals(owner)) {
            //noinspection ConstantConditions
            plugin.sendMessage(player, messages.getString("block-locked")
                    .replaceAll("%block%", String.valueOf(event.getBlock().getType()).toLowerCase()
                            .replaceAll("_", " ")));
            event.setCancelled(true);
        } else {
            if (playerData == null || !playerData.canBreakLock()) {
                plugin.sendMessage(player, messages.getString("confirm-block-destroy"));
                event.setCancelled(true);
            } else if (playerData.canBreakLock()) {
                plugin.sendMessage(player, messages.getString("lock-broken"));
                // TODO delete location and stuff
            }
        }

        if (playerData == null) {
            playerData = new LocalPlayerData(player);
            playerData.save(plugin);
        }

    }

}
