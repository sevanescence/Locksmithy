package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;

public class PistonPushListener implements Listener {

    private Locksmithy plugin;

    public PistonPushListener(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent event) {

        for (Block b : event.getBlocks()) {
            if (LocationReference.findByFile(plugin, b.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }

    }

}
