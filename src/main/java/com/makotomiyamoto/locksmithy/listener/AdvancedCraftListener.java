package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

public class AdvancedCraftListener implements Listener {

    private Locksmithy plugin;

    public AdvancedCraftListener(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {

        ItemStack[] matrix = event.getInventory().getMatrix();

        int nni = -1;
        int bki = -1;
        int size = 0;
        for (int i = 0; i < matrix.length; i++) {
            if (matrix[i] != null) {
                size++;
                if (LocationReference.isAssignedAdvancedKey(plugin, matrix[i]))
                    nni = i;
                else if (LocationReference.isAdvancedBlankKey(plugin, matrix[i]))
                    bki = i;
            }
        }

        if (nni > -1 && bki > -1 && size == 2) {
            ItemStack result = matrix[nni].clone();
            result.setAmount(2);
            event.getInventory().setResult(result);
        }

    }

    @EventHandler
    public void onCraftComplete(CraftItemEvent event) {

        if (event.getInventory().getResult() == null) return;

        if (LocationReference.isAssignedAdvancedKey(plugin, event.getInventory().getResult())) {
            for (ItemStack i : event.getInventory().getMatrix()) {
                i.setAmount(i.getAmount()-1);
            }
        }

    }

}
