package com.makotomiyamoto.locksmithy.lock;

import com.makotomiyamoto.locksmithy.Locksmithy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class RegisteredKey {

    public static ItemStack buildThing(Locksmithy plugin, String node) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection blankKey = config.getConfigurationSection(node);
        assert blankKey != null;
        //noinspection ConstantConditions
        ItemStack itemStack = new ItemStack(
                Material.valueOf(blankKey.getString("item").replaceAll(" ", "_").toUpperCase()),
                1);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        //noinspection ConstantConditions
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', blankKey.getString("name")));
        List<String> lore = blankKey.getStringList("lore");
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i) == null) continue;
            String l = lore.get(i);
            if (l.contains("%SUCCESS_RATE%") && blankKey.getString("default-success-rate") != null) {
                //noinspection ConstantConditions
                l = l.replace("%SUCCESS_RATE%", blankKey.getString("default-success-rate"));
            }
            if (l.contains("%CRITICAL_FAILURE_RATE%")) {
                //noinspection ConstantConditions
                l = l.replace("%CRITICAL_FAILURE_RATE%", blankKey.getString("default-critical-failure-rate"));
            }
            lore.set(i, ChatColor.translateAlternateColorCodes('&', l));
        }
        if (lore.size() > 0)
            meta.setLore(lore);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static HashMap<String, ItemStack> generateKeyByKey(Locksmithy plugin, String keyString, Player player) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection key = config.getConfigurationSection(keyString);
        assert key != null;
        @SuppressWarnings("ConstantConditions")
        ItemStack itemStack = new ItemStack(
                Material.valueOf(key.getString("item").replaceAll(" ", "_").toUpperCase()),
                1);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                key.getString("name-color") + key.getString("name")));
        List<String> lore = key.getStringList("lore");
        long id = plugin.generateNewKeyId();
        for (int i = 0; i < lore.size(); i++) {
            String l;
            l = ChatColor.translateAlternateColorCodes('&', lore.get(i));
            l = l.replaceAll("%MODEL_NUMBER%", String.valueOf((int)(Math.random()*5)+1));
            l = l.replaceAll("%KEY_OWNER%", player.getName());
            l = l.replaceAll("%LABEL%", "No Label");
            if (l.contains("%KEY_ID%"))
                l = l.replaceAll("%KEY_ID%", String.valueOf(id));
            lore.set(i, l);
        }
        if (lore.size() > 0)
            meta.setLore(lore);
        itemStack.setItemMeta(meta);
        HashMap<String, ItemStack> map = new HashMap<>();
        map.put(String.valueOf(id), itemStack);
        return map;
    }

    public static int findLoreIndexFromConfig(Locksmithy plugin, String key, String node) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(node);
        assert section != null;
        List<String> lore = section.getStringList("lore");
        if (lore.size() == 0) throw new IllegalArgumentException("Your key or lockpick doesn't have lore. Fix that.");
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(key)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isLockpick(Locksmithy plugin, ItemStack itemStack) {

        if (itemStack == null || itemStack.getItemMeta() == null) return false;

        ConfigurationSection lockpickSection = plugin.getConfig().getConfigurationSection("options.lockpick");
        assert lockpickSection != null;

        List<String> matchLore = lockpickSection.getStringList("match-lore-regex");
        List<String> lore = itemStack.getItemMeta().getLore();
        if (lore == null || lore.size() != matchLore.size()) return false;
        for (int i = 0; i < lore.size(); i++) {
            if (!lore.get(i).contains(matchLore.get(i)))
                return false;
        }

        return true;

    }

    public static boolean isSmashItem(Locksmithy plugin, ItemStack itemStack) {

        if (itemStack == null || itemStack.getItemMeta() == null) return false;

        ConfigurationSection smashingSection = plugin.getConfig().getConfigurationSection("options.lock-smashing");
        assert smashingSection != null;

        //noinspection ConstantConditions
        Set<String> items = smashingSection.getConfigurationSection("items").getKeys(false);
        boolean isItem = false;
        for (String key : items) {
            if (key.toUpperCase().equals(itemStack.getType().toString())) {
                isItem = true;
                break;
            }
        }

        return isItem;

    }

}
