package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

public class BlockRedstoneListener implements Listener {

    private Locksmithy plugin;

    public BlockRedstoneListener(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {

        LocationReference reference = LocationReference.findByFile(plugin, event.getBlock().getLocation());

        if (reference != null) {
            event.setNewCurrent(event.getOldCurrent());
        }

    }

}
