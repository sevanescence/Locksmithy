package com.makotomiyamoto.locksmithy;

import com.makotomiyamoto.locksmithy.commands.*;
import com.makotomiyamoto.locksmithy.listener.*;
import com.makotomiyamoto.locksmithy.lock.RegisteredKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public final class Locksmithy extends JavaPlugin {

    // TODO fix armor stand glitch (note: might not be an actual bug)
    // TODO make sure to add sounds and stuff
    // TODO disallow hopper placing beneath locked blocks
    // TODO write version in ignore.yml rather than name of project

    public String PATH, LOCATIONS_DIR, USERS_DIR;

    @Override
    public void onEnable() {

        PATH = getDataFolder().getPath();
        LOCATIONS_DIR = PATH + File.separator + "locations";
        USERS_DIR = PATH + File.separator + "data" + File.separator + "users";

        File path = new File(PATH);
        if (path.mkdirs() || !new File(PATH + File.separator + "config.yml").exists()) {
            InputStream iStream = getResource("config.yml");
            try {
                assert iStream != null;
                byte[] buffer = new byte[iStream.available()];
                // noinspection all
                iStream.read(buffer);
                File config = new File(PATH + File.separator + "config.yml");
                OutputStream oStream = new FileOutputStream(config);
                oStream.write(buffer);
                oStream.close();
                print(config.getPath() + " created.");
            } catch (IOException ignored) {}
        }

        File locationsDir = new File(LOCATIONS_DIR);
        if (locationsDir.mkdirs()) {
            print(locationsDir.getPath() + " created.");
        }

        File usersDir = new File(USERS_DIR);
        if (usersDir.mkdirs()) {
            print(usersDir.getPath() + " created.");
        }

        // event handler registration
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockInteractListener(this), this);
        pm.registerEvents(new BlockBreakListener(this), this);
        pm.registerEvents(new BlockRedstoneListener(this), this);
        pm.registerEvents(new PistonPushListener(this), this);
        pm.registerEvents(new CraftListener(this), this);
        if (getConfig().getBoolean("options.crafting.advanced-key-duplication"))
            pm.registerEvents(new AdvancedCraftListener(this), this);

        PluginCommand lks = getCommand("lks");
        assert lks != null;
        lks.setExecutor(new CommandHandler(this));
        lks.setTabCompleter(new CommandHelper());

        ItemStack blankKey = RegisteredKey.buildThing(this, "options.blank_key");
        ItemStack advancedBlankKey = RegisteredKey.buildThing(this, "options.advanced_blank_key");
        ItemStack lockpick = RegisteredKey.buildThing(this, "options.lockpick");

        NamespacedKey blankKeyName = new NamespacedKey(this, "blank_key");
        ShapedRecipe blankKeyRecipe = new ShapedRecipe(blankKeyName, blankKey);
        blankKeyRecipe.shape("iiB", "nn ");
        blankKeyRecipe.setIngredient('i', Material.IRON_INGOT);
        blankKeyRecipe.setIngredient('B', Material.IRON_BLOCK);
        blankKeyRecipe.setIngredient('n', Material.IRON_NUGGET);

        NamespacedKey advancedBlankKeyName = new NamespacedKey(this, "advanced_blank_key");
        ShapedRecipe advancedBlankKeyRecipe = new ShapedRecipe(advancedBlankKeyName, advancedBlankKey);
        advancedBlankKeyRecipe.shape("ddB", "nn ");
        advancedBlankKeyRecipe.setIngredient('d', Material.DIAMOND);
        advancedBlankKeyRecipe.setIngredient('B', Material.DIAMOND_BLOCK);
        advancedBlankKeyRecipe.setIngredient('n', Material.IRON_NUGGET);

        NamespacedKey lockpickName = new NamespacedKey(this, "lockpick");
        ShapedRecipe lockpickRecipe = new ShapedRecipe(lockpickName, lockpick);
        lockpickRecipe.shape("n  ", " i ", "  i");
        lockpickRecipe.setIngredient('n', Material.IRON_NUGGET);
        lockpickRecipe.setIngredient('i', Material.IRON_INGOT);

        getServer().addRecipe(blankKeyRecipe);
        getServer().addRecipe(advancedBlankKeyRecipe);
        if (getConfig().getBoolean("options.lockpicking.enabled"))
            getServer().addRecipe(lockpickRecipe);

    }

    public void print(String message) {
        print(message, false);
    }
    public void print(String message, boolean announce) {

        String PREFIX = "[Locksmithy]";
        if (announce) {
            // todo tell all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(
                        ChatColor.translateAlternateColorCodes('&',
                                PREFIX + " " + message));
            }
        } else {
            Bukkit.getConsoleSender().sendMessage(PREFIX + " " + message);
        }

    }
    public void sendMessage(Entity entity, String message) {
        entity.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("options.message-prefix") + " " + message));
    }

    public long generateNewKeyId() {

        File file = new File(PATH + File.separator + "data" + File.separator + "dont_touch_this.txt");
        try {
            if (file.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                writer.write("1");
                writer.close();
                return 0L;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String s = reader.readLine();
            long l = Long.parseLong(s);
            reader.close();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(String.valueOf(l+1));
            writer.close();
            return Long.parseLong(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0L;

    }

}
