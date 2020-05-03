package com.makotomiyamoto.locksmithy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public final class Locksmithy extends JavaPlugin {

    public String PATH, LOCATIONS_DIR;
    private String PREFIX = "[Locksmithy]";

    @Override
    public void onEnable() {

        PATH = this.getDataFolder().getPath();
        LOCATIONS_DIR = PATH + File.separator + "locations";

        File path = new File(PATH);
        if (path.mkdirs()) {
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



    }

    public void print(String message) {
        print(message, false);
    }
    public void print(String message, boolean announce) {

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
        entity.sendMessage(ChatColor.translateAlternateColorCodes('&', PREFIX + " " + message));
    }

}
