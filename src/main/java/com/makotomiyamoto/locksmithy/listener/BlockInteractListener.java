package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocalPlayerData;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import com.makotomiyamoto.locksmithy.lock.RegisteredKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class BlockInteractListener implements Listener {

    private Locksmithy plugin;

    public BlockInteractListener(Locksmithy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent event) {

        // this is here because of funny stuff that happened last version
        handleEvent(event);

    }

    public void handleEvent(PlayerInteractEvent event) {

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;
        BlockState state = block.getState();
        Player player = event.getPlayer();

        LocationReference r = LocationReference.findByFile(plugin, block.getLocation());

        LocalPlayerData playerData = LocalPlayerData.fetchFromFile(plugin, player);
        if (playerData == null) {
            playerData = new LocalPlayerData(player);
            playerData.save(plugin);
        } else if (playerData.isDebug() && r != null) {
            if (player.getInventory().getItemInMainHand().getType().equals(Material.STICK)) {
                player.sendMessage("§6Location: §f" + r.asString());
                player.sendMessage("§6Connected Location: §f"
                        + ((r.getConnectedLocationString() == null) ? "none" : r.getConnectedLocationString()));
                player.sendMessage("§6Owner UUID: §f" + r.getOwnerByUuid());
                //noinspection ConstantConditions
                player.sendMessage("§6Owner Name: §f" + Bukkit.getPlayer(UUID.fromString(r.getOwnerByUuid())).getName());
                player.sendMessage("§6Authorized Key: §f" + r.getAuthorizedKeyByUuid());
                player.sendMessage("§6Advanced: §f" + r.isAdvancedLock());
                event.setCancelled(true);
                return;
            }
        }

        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && player.isSneaking()) {

            if (block.getType().isInteractable()) {
                Location loc = block.getLocation();
                World world = loc.getWorld();
                int x = loc.getBlockX();
                int y = loc.getBlockY();
                int z = loc.getBlockZ();
                assert world != null;
                LocationReference reference = new LocationReference(loc);
                LocationReference otherReference = null;

                ItemStack item = event.getItem();
                ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
                assert options != null;
                ConfigurationSection messages = options.getConfigurationSection("messages");
                assert messages != null;
                if (item == null && r != null) {
                    handleOpenAttempt(event);
                    return;
                } else if (item == null) {
                    return;
                }
                if (LocationReference.isAssignedAdvancedKey(plugin, item)) {
                    plugin.sendMessage(player, messages.getString("key-in-use"));
                    return;
                }
                if (LocationReference.locationAlreadyAssigned(plugin, reference)
                        && LocationReference.isAnyAvailableKey(plugin, item)) {
                    plugin.sendMessage(player, messages.getString("lock-already-assigned"));
                    return;
                }
                if (r != null) {
                    handleOpenAttempt(event);
                    return;
                }
                if (!LocationReference.isAnyAvailableKey(plugin, item)) {
                    return;
                }

                if (player.getInventory().firstEmpty() == -1 && item.getAmount() > 1) {
                    plugin.sendMessage(player, messages.getString("inventory-full"));
                    return;
                }

                // TODO rewrite this as LocationReference.getDoorTwin(Location: loc)
                if (LocationReference.isDoor(block)) {
                    Location locTop = new Location(world, x, y+1, z);
                    Location locBottom = new Location(world, x, y-1, z);
                    if (LocationReference.isDoor(world.getBlockAt(locTop))) {
                        otherReference = new LocationReference(locTop);
                    } else if (LocationReference.isDoor(world.getBlockAt(locBottom))) {
                        otherReference = new LocationReference(locBottom);
                    } else {
                        throw new IllegalArgumentException(
                                "Something silly happened. Report this to me at MakotoMiyamoto#0215 on Discord!");
                    }
                } else if (state instanceof org.bukkit.block.Chest) {
                    Chest chestData = (Chest) block.getBlockData();
                    Chest.Type type = chestData.getType();
                    if (!type.equals(Chest.Type.SINGLE)) {
                        BlockFace facing = chestData.getFacing();
                        otherReference = LocationReference.getDoubleChestTwin(type, facing, loc);
                    }
                } else if (!(block.getBlockData() instanceof TrapDoor)) {
                    return;
                }

                int slot = player.getInventory().getHeldItemSlot();
                String id;
                if (LocationReference.isBlankKey(plugin, item)
                        || LocationReference.isAdvancedBlankKey(plugin, item)) {
                    ItemStack itemStack = player.getInventory().getItemInMainHand();
                    itemStack.setAmount(itemStack.getAmount()-1);
                    player.getInventory().setItem(slot, itemStack);
                }
                int i = RegisteredKey.findLoreIndexFromConfig(plugin, "%KEY_ID%", "options.key");
                if (LocationReference.isBlankKey(plugin, item)) {
                    ItemStack key = RegisteredKey.generateKeyByKey(plugin, "options.key", player);
                    //noinspection ConstantConditions
                    id = key.getItemMeta().getLore().get(i);
                    player.getInventory().addItem(key);
                } else if (LocationReference.isKey(plugin, item)) {
                    //noinspection ConstantConditions
                    id = item.getItemMeta().getLore().get(i);
                } else if (LocationReference.isAdvancedBlankKey(plugin, item)) {
                    ItemStack advancedKey = RegisteredKey.generateKeyByKey(plugin, "options.advanced_key", player);
                    i = RegisteredKey.findLoreIndexFromConfig(plugin, "%KEY_ID%", "options.advanced_key");
                    //noinspection ConstantConditions
                    id = advancedKey.getItemMeta().getLore().get(i);
                    player.getInventory().addItem(advancedKey);
                    reference.setAdvancedLock(true);
                    if (otherReference != null) otherReference.setAdvancedLock(true);
                } else {
                    throw new IllegalArgumentException("Something went wrong with the keys. Please report this issue..");
                }

                reference.setAuthorizedKeyByUuid(id);
                reference.setOwnerByUuid(player.getUniqueId().toString());
                if (otherReference != null) {
                    reference.setConnectedLocationString(otherReference.asString());
                    otherReference.setConnectedLocationString(reference.asString());
                    otherReference.setAuthorizedKeyByUuid(reference.getAuthorizedKeyByUuid());
                    otherReference.setOwnerByUuid(reference.getOwnerByUuid());
                    otherReference.save(plugin);
                }
                reference.save(plugin);

                plugin.sendMessage(player, messages.getString("lock-created"));

            }
        } else {
            handleOpenAttempt(event);
        }
    }

    private void handleOpenAttempt(PlayerInteractEvent event) {

        // TODO check if debug mode for player is on, check if player is holding stick
        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;

        Block block = event.getClickedBlock();
        if (block == null) return;
        LocationReference reference = LocationReference.findByFile(plugin, block.getLocation());
        if (reference == null)
            return;

        Player player = event.getPlayer();

        if (reference.isExposed()) {
            if (player.getUniqueId().toString().equals(reference.getOwnerByUuid())) {
                plugin.sendMessage(player, options.getString("messages.was-broken-into"));
                reference.setExposed(false);
                reference.save(plugin);
            }
            return;
        } else if (reference.isJammed()) {
            if (player.getUniqueId().toString().equals(reference.getOwnerByUuid())) {
                plugin.sendMessage(player, options.getString("messages.was-jammed"));
                reference.setJammed(false);
                reference.save(plugin);
                return;
            }
            event.setCancelled(true);
            return;
        }

        Material requiredType = reference.getRequiredType(plugin);
        String requiredNode = (reference.isAdvancedLock()) ? "options.advanced_key" : "options.key";

        ItemStack possibleKey = null;
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();
        if (!mainHandItem.getType().equals(Material.AIR)) {
            if (reference.keyIDMatches(plugin, mainHandItem, requiredNode)) {
                possibleKey = mainHandItem;
            }
        }
        if (possibleKey == null && !offHandItem.getType().equals(Material.AIR)) {
            if (reference.keyIDMatches(plugin, offHandItem, requiredNode)) {
                possibleKey = offHandItem;
            }
        }
        if (possibleKey == null) {
            ItemStack[] inventory = player.getInventory().getContents();
            for (ItemStack itemStack : inventory) {
                if (itemStack == null) continue;
                if (itemStack.getType().equals(requiredType)) {
                    if (reference.keyIDMatches(plugin, itemStack, requiredNode)) {
                        possibleKey = itemStack;
                    }
                }
            }
        }

        ConfigurationSection messages = options.getConfigurationSection("messages");
        assert messages != null;

        if (possibleKey == null) {

            BlockState state = block.getState();
            if (state instanceof org.bukkit.block.Chest) {

                org.bukkit.block.Chest chest = (org.bukkit.block.Chest) state;
                Inventory chestInventory = chest.getInventory();
                ItemStack[] itemStacks = chestInventory.getContents();

                for (ItemStack itemStack : itemStacks) {
                    if (itemStack == null) continue;
                    if (itemStack.getType().equals(requiredType)) {
                        if (reference.keyIDMatches(plugin, itemStack, requiredNode)) {
                            return;
                        }
                    }
                }

            }

            event.setCancelled(true);
            if (!RegisteredKey.isLockpick(plugin, mainHandItem)) {
                //noinspection ConstantConditions
                plugin.sendMessage(player, messages.getString("block-locked")
                        .replaceAll("%block%", String.valueOf(block.getType()).toLowerCase()
                                .replaceAll("_", " ")));
            }
        }

    }

}
