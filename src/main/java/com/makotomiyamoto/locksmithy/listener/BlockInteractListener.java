package com.makotomiyamoto.locksmithy.listener;

import com.makotomiyamoto.locksmithy.Locksmithy;
import com.makotomiyamoto.locksmithy.lock.LocalPlayerData;
import com.makotomiyamoto.locksmithy.lock.LocationReference;
import com.makotomiyamoto.locksmithy.lock.RegisteredKey;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.HashMap;
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

        EquipmentSlot eSlot = event.getHand();
        if (eSlot == null || !eSlot.equals(EquipmentSlot.HAND)) return;

        if (event.getClickedBlock() == null) return;

        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;
        ConfigurationSection messages = options.getConfigurationSection("messages");
        assert messages != null;

        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        LocalPlayerData playerData = LocalPlayerData.fetchFromFile(plugin, player);
        if (playerData == null) {
            playerData = new LocalPlayerData(player);
            playerData.save(plugin);
        }

        if (playerData.isDebug()) {
            LocationReference r = LocationReference.findByFile(plugin, block.getLocation());
            if (player.getInventory().getItemInMainHand().getType().equals(Material.STICK)) {
                player.sendMessage("§6Locksmithy v1.0 Debugger");
                if (r == null) {
                    player.sendMessage("§6Error: §fThis block does not have a lock.");
                } else {
                    player.sendMessage("§6Location: §f" + r.asString());
                    player.sendMessage("§6Neighbor: §f" + r.getConnectedLocationString());
                    player.sendMessage("§6Owner: §f" + Bukkit.getPlayer(UUID.fromString(r.getOwnerByUuid())));
                    player.sendMessage("§6Owner by UUID: §f" + r.getOwnerByUuid());
                    player.sendMessage("§6Authorized key: §f" + r.getAuthorizedKeyByUuid());
                    player.sendMessage("§6isAdvanced: §f" + r.isAdvancedLock());
                    player.sendMessage("§6isExposed: §f" + r.isExposed());
                    player.sendMessage("§6isJammed: §f" + r.isJammed());
                    player.sendMessage("§6isPublic: §f" + r.isAccessible());
                }
                event.setCancelled(true);
            } else {
                if (r != null) {
                    plugin.sendMessage(player, "&eOpening lock in debug mode.");
                }
            }
            return;
        }

        LocationReference reference = LocationReference.findByFile(plugin, block.getLocation());
        if (reference == null) {

            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && player.isSneaking()) {

                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand.getItemMeta() == null) return;
                if (player.getInventory().firstEmpty() == -1 && mainHand.getAmount() > 1) {
                    plugin.sendMessage(player, messages.getString("inventory-full"));
                    return;
                }

                if (LocationReference.isAnyAvailableKey(plugin, mainHand)) {

                    reference = new LocationReference(block.getLocation());
                    LocationReference otherReference = null;

                    if (LocationReference.isDoor(block)) {
                        otherReference = new LocationReference(LocationReference.getDoorPart(block));
                    } else if (block.getState() instanceof org.bukkit.block.Chest) {
                        if (LocationReference.isDoubleChest(block)) {
                            Chest.Type type = ((Chest) block.getBlockData()).getType();
                            BlockFace facing = ((Chest) block.getBlockData()).getFacing();
                            otherReference = LocationReference.getDoubleChestTwin(type, facing, block.getLocation());
                        }
                    } else if (!(block.getBlockData() instanceof TrapDoor || block.getState() instanceof Barrel)) {
                        return;
                    }

                    String id;
                    if (LocationReference.isBlankKey(plugin, mainHand)
                            || LocationReference.isAdvancedBlankKey(plugin, mainHand)) {
                        String keyString = (LocationReference.isAdvancedBlankKey(plugin, mainHand)
                                ? "options.advanced_key" : "options.key");
                        HashMap<String, ItemStack> map = RegisteredKey.generateKeyByKey(plugin, keyString, player);
                        id = "0";
                        for (String s : map.keySet()) {
                            id = s;
                        }
                        ItemStack key = map.get(id);
                        if (mainHand.getAmount() == 1) {
                            player.getInventory().setItemInMainHand(key);
                        } else {
                            mainHand.setAmount(mainHand.getAmount() - 1);
                            player.getInventory().addItem(key);
                        }
                    } else {
                        int keyIndex = RegisteredKey
                                .findLoreIndexFromConfig(plugin, "%KEY_ID%", "options.key");
                        //noinspection ConstantConditions;
                        id = mainHand.getItemMeta().getLore().get(keyIndex)
                                .replaceAll("§[A-z0-9]", "")
                                .replaceAll("[^0-9]", "");
                    }

                    reference.setAdvancedLock(LocationReference.isAdvancedBlankKey(plugin, mainHand));
                    reference.setOwnerByUuid(player.getUniqueId().toString());
                    reference.setAuthorizedKeyByUuid(id);
                    if (otherReference != null) {
                        reference.setConnectedLocationString(otherReference.asString());
                        otherReference.setConnectedLocationString(reference.asString());
                        otherReference.setAdvancedLock(reference.isAdvancedLock());
                        otherReference.setOwnerByUuid(reference.getOwnerByUuid());
                        otherReference.setAuthorizedKeyByUuid(reference.getAuthorizedKeyByUuid());
                        otherReference.save(plugin);
                    }
                    reference.save(plugin);

                    plugin.sendMessage(player, messages.getString("lock-created"));

                } else if (LocationReference.isAssignedAdvancedKey(plugin, mainHand)) {
                    plugin.sendMessage(player, messages.getString("key-in-use"));
                }

            }

        } else {

            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                handleOpenAttempt(event);
            }

        }

    }

    // NOTE: This will NEVER run unless LocationReference reference != null. Don't waste power evaluating here.
    private void handleOpenAttempt(PlayerInteractEvent event) {

        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;
        ConfigurationSection messages = options.getConfigurationSection("messages");
        assert messages != null;

        Block block = event.getClickedBlock();
        assert block != null;
        Player player = event.getPlayer();

        LocationReference reference = LocationReference.findByFile(plugin, block.getLocation());
        assert reference != null;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack[] inventory = player.getInventory().getContents();

        Material requiredType = reference.getRequiredType(plugin);
        String node = (reference.isAdvancedLock()) ? "options.advanced_key" : "options.key";

        boolean canOpen = false;
        ItemStack c = null;
        if (mainHand.getType().equals(requiredType)) {
            if (reference.keyIDMatches(plugin, mainHand, node)) {
                canOpen = true;
                c = mainHand;
            }
        } else if (offHand.getType().equals(requiredType)) {
            if (reference.keyIDMatches(plugin, offHand, node)) {
                canOpen = true;
            }
        }
        for (ItemStack itemStack : inventory) {
            if (itemStack == null) continue;
            if (itemStack.getType().equals(requiredType)) {
                if (reference.keyIDMatches(plugin, itemStack, node)) {
                    canOpen = true;
                }
            }
        }
        if (block.getState() instanceof org.bukkit.block.Chest) {
            for (ItemStack itemStack : ((org.bukkit.block.Chest) block.getState()).getInventory()) {
                if (itemStack == null) continue;
                if (itemStack.getType().equals(requiredType)) {
                    if (reference.keyIDMatches(plugin, itemStack, node)) {
                        canOpen = true;
                        break;
                    }
                }
            }
        } else if (block.getState() instanceof Barrel) {
            for (ItemStack itemStack : ((Barrel) block.getState()).getInventory()) {
                if (itemStack == null) continue;
                if (itemStack.getType().equals(requiredType)) {
                    if (reference.keyIDMatches(plugin, itemStack, node)) {
                        canOpen =  true;
                        break;
                    }
                }
            }
        }

        if (canOpen) {

            if (reference.isJammed()) {
                plugin.sendMessage(player, messages.getString("was-jammed"));
                event.setCancelled(true);
            } else if (reference.isExposed()) {
                plugin.sendMessage(player, messages.getString("was-broken-into"));
                event.setCancelled(true);
            }
            if (reference.isJammed() || reference.isExposed()) {
                reference.setJammed(false);
                reference.setExposed(false);
                reference.save(plugin);
                if (reference.getConnectedLocationString() != null) {
                    LocationReference otherReference = LocationReference.loadFromJson(plugin, reference.getConnectedLocationString());
                    assert otherReference != null;
                    otherReference.setJammed(false);
                    otherReference.setExposed(false);
                    otherReference.save(plugin);
                }
            }

            if (player.isSneaking() && c != null && reference.getOwnerByUuid().equals(player.getUniqueId().toString())) {
                 reference.setAccessible(!reference.isAccessible());
                 reference.save(plugin);
                 if (reference.getConnectedLocationString() != null) {
                     LocationReference otherReference = LocationReference.loadFromJson(plugin, reference.getConnectedLocationString());
                     assert otherReference != null;
                     otherReference.setAccessible(!otherReference.isAccessible());
                     otherReference.save(plugin);
                 }
                 plugin.sendMessage(player, (reference.isAccessible() ? messages.getString("lock-public-on") : messages.getString("lock-public-off")));
                 return;
            }

            if (handleIronGateway(player, block)) {
                event.setCancelled(true);
            }

            return;

        } else if (reference.isExposed() || reference.isAccessible()) {
            if (handleIronGateway(player, block)) {
                event.setCancelled(true);
            }
            return;
        }

        if (player.isSneaking()) {

            LocalPlayerData playerData = LocalPlayerData.fetchFromFile(plugin, player);
            assert playerData != null;
            long currentTime = ZonedDateTime.now().toInstant().toEpochMilli();
            long cooldownMilli = options.getLong("lock-break-cooldown-seconds")*1000;
            long diff = currentTime - playerData.getTimeTried();

            if (diff > cooldownMilli) {
                if (RegisteredKey.isLockpick(plugin, mainHand) && options.getBoolean("lockpicking.enabled")) {
                    handleLockpickAttempt(event);
                    playerData.updateTimeTried();
                } else if (RegisteredKey.isSmashItem(plugin, mainHand) && options.getBoolean("lock-smashing.enabled")) {
                    handleLockSmashAttempt(event);
                    playerData.updateTimeTried();
                } else {
                    //noinspection ConstantConditions
                    plugin.sendMessage(player,
                            messages.getString("block-locked")
                                    .replaceAll("%block%", block.getType().toString()
                                            .replaceAll("_", " ").toLowerCase()));
                }
                playerData.save(plugin);
            } else {
                int seconds = (int)(cooldownMilli/1000) - (int)(diff/1000);
                //noinspection ConstantConditions
                String msg = messages.getString("cooldown-on-attempt").replaceAll("%COOLDOWN%", String.valueOf(seconds));
                plugin.sendMessage(player, msg);
            }
            event.setCancelled(true);

        } else {
            event.setCancelled(true);
            //noinspection ConstantConditions
            plugin.sendMessage(player,
                    messages.getString("block-locked")
                            .replaceAll("%block%", block.getType().toString()
                                    .replaceAll("_", " ").toLowerCase()));
        }

    }

    private boolean handleIronGateway(Player player, Block block) {

        if (block.getType().equals(Material.IRON_DOOR)) {
            Door door = (Door) block.getBlockData();
            door.setOpen(!door.isOpen());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), (door.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE), 1f, 1f);
                for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        float vol = 2 / Float.parseFloat(String.valueOf(target.getLocation().distance(player.getLocation())));
                        target.playSound(target.getLocation(), (door.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE), vol, 1f);
                    }
                }
                block.setBlockData(door);
            }, 0);
            return true;
        } else if (block.getType().equals(Material.IRON_TRAPDOOR)) {
            TrapDoor door = (TrapDoor) block.getBlockData();
            door.setOpen(!door.isOpen());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.playSound(player.getLocation(), (door.isOpen() ? Sound.BLOCK_IRON_TRAPDOOR_OPEN : Sound.BLOCK_IRON_TRAPDOOR_CLOSE), 1f, 1f);
                for (Entity entity : player.getNearbyEntities(20, 20, 20)) {
                    if (entity instanceof Player) {
                        Player target = (Player) entity;
                        float vol = 2 / Float.parseFloat(String.valueOf(target.getLocation().distance(player.getLocation())));
                        target.playSound(target.getLocation(), (door.isOpen() ? Sound.BLOCK_IRON_DOOR_OPEN : Sound.BLOCK_IRON_DOOR_CLOSE), vol, 1f);
                    }
                }
                block.setBlockData(door);
            }, 0);
            return true;
        }

        return false;

    }

    private void handleLockpickAttempt(PlayerInteractEvent event) {

        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;
        ConfigurationSection messages = options.getConfigurationSection("messages");
        assert messages != null;

        Block block = event.getClickedBlock();
        assert block != null;
        Player player = event.getPlayer();

        LocationReference reference = LocationReference.findByFile(plugin, block.getLocation());
        assert reference != null;

        if (reference.isAdvancedLock()) {
            plugin.sendMessage(player, messages.getString("lock-unbreakable"));
            event.setCancelled(true);
            return;
        } else if (reference.isJammed()) {
            plugin.sendMessage(player, messages.getString("lock-jammed"));
            event.setCancelled(true);
            return;
        } else if (reference.isExposed()) {
            plugin.sendMessage(player, messages.getString("lock-already-broken"));
            event.setCancelled(true);
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        assert mainHand.getItemMeta() != null && mainHand.getItemMeta().getLore() != null;

        int successLine = RegisteredKey.findLoreIndexFromConfig(plugin, "%SUCCESS_RATE%", "options.lockpick");
        int failureLine = RegisteredKey.findLoreIndexFromConfig(plugin, "%CRITICAL_FAILURE_RATE%", "options.lockpick");

        double success = Double.parseDouble(
                mainHand.getItemMeta().getLore().get(successLine)
                .replaceAll("§[A-z0-9]", "").replaceAll("[^0-9.]", ""))/100;
        double failure = Double.parseDouble(
                mainHand.getItemMeta().getLore().get(failureLine)
                        .replaceAll("§[A-z0-9]", "").replaceAll("[^0-9.]", ""))/100;

        if (Math.random() < success) {
            reference.setExposed(true);
            plugin.sendMessage(player, messages.getString("lockpick-success"));
        } else if (Math.random() < failure) {
            reference.setJammed(true);
            plugin.sendMessage(player, messages.getString("lockpick-failure-critical"));
        } else {
            plugin.sendMessage(player, messages.getString("lockpick-failure"));
        }
        reference.save(plugin);
        if (reference.getConnectedLocationString() != null) {
            LocationReference otherReference = LocationReference.loadFromJson(plugin, reference.getConnectedLocationString());
            assert otherReference != null;
            otherReference.setExposed(reference.isExposed());
            otherReference.setJammed(reference.isJammed());
            otherReference.save(plugin);
        }

        event.setCancelled(true);
        if (mainHand.getAmount() == 1) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> mainHand.setAmount(mainHand.getAmount()-1), 1);
        } else {
            mainHand.setAmount(mainHand.getAmount()-1);
        }

    }

    private void handleLockSmashAttempt(PlayerInteractEvent event) {

        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;
        ConfigurationSection messages = options.getConfigurationSection("messages");
        assert messages != null;

        Block block = event.getClickedBlock();
        assert block != null;
        Player player = event.getPlayer();

        LocationReference reference = LocationReference.findByFile(plugin, block.getLocation());
        assert reference != null;

        if (reference.isAdvancedLock()) {
            plugin.sendMessage(player, messages.getString("lock-unbreakable"));
            event.setCancelled(true);
            return;
        } else if (reference.isJammed()) {
            plugin.sendMessage(player, messages.getString("lock-jammed"));
            event.setCancelled(true);
            return;
        } else if (reference.isExposed()) {
            plugin.sendMessage(player, messages.getString("lock-already-broken"));
            event.setCancelled(true);
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ConfigurationSection item = null;
        //noinspection ConstantConditions
        for (String key : options.getConfigurationSection("lock-smashing.items").getKeys(false)) {
            if (key.toUpperCase().equals(mainHand.getType().toString())) {
                item = options.getConfigurationSection(String.format("lock-smashing.items.%1s", key));
            }
        }
        assert item != null;

        double success = item.getDouble("success-rate")/100d;
        double failure = item.getDouble("critical-failure-rate")/100d;

        if (Math.random() < success) {
            plugin.sendMessage(player, messages.getString("smash-success"));
            reference.setExposed(true);
        } else if (Math.random() < failure) {
            plugin.sendMessage(player, messages.getString("smash-failure-critical"));
            reference.setJammed(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                mainHand.setAmount(0);
                player.playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 10f, 1f);
            }, 5);
        } else {
            plugin.sendMessage(player, messages.getString("smash-failure"));
        }
        reference.save(plugin);
        if (reference.getConnectedLocationString() != null) {
            LocationReference otherReference = LocationReference.loadFromJson(plugin, reference.getConnectedLocationString());
            assert otherReference != null;
            otherReference.setExposed(reference.isExposed());
            otherReference.setJammed(reference.isJammed());
            otherReference.save(plugin);
        }

        event.setCancelled(true);

    }

}
