package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocalPlayerData;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        if (reference == null) {
            Location loc = event.getBlock().getLocation();
            // I originally used loc.add() but that function never worked for some reason.
            Location top = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY()+1, loc.getBlockZ());
            if (LocationReference.isDoor(top.getBlock())) {
                LocationReference topReference = LocationReference.findByFile(plugin, top);
                if (topReference != null) {
                    //noinspection ConstantConditions
                    plugin.sendMessage(event.getPlayer(), messages.getString("block-locked")
                            .replaceAll("%block%", event.getBlock().getType().toString()
                            .replaceAll("_", " ").toLowerCase()));
                    event.setCancelled(true);
                }
            }
            return;
        }

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
                reference.delete(plugin);
                if (reference.getConnectedLocationString() != null) {
                    LocationReference otherReference = LocationReference.loadFromJson(plugin, reference.getConnectedLocationString());
                    assert otherReference != null;
                    otherReference.delete(plugin);
                }
            }
        }

        if (playerData == null) {
            playerData = new LocalPlayerData(player);
            playerData.save(plugin);
        }

    }

}
