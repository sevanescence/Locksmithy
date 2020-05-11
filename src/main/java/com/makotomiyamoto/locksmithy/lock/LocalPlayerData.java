package com.makotomiyamoto.locksmithy.lock;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.makotomiyamoto.locksmithy.Locksmithy;
import org.bukkit.entity.Player;

import java.io.*;
import java.time.ZonedDateTime;

public class LocalPlayerData {

    // TODO set up state data (debug mode)
    private transient String playerByUuid;
    private boolean breakLock, debug;
    private long timeTried;

    public LocalPlayerData(Player player) {
        playerByUuid = player.getUniqueId().toString();
        breakLock = false;
        debug = false;
        timeTried = 0;
    }

    public static LocalPlayerData fetchFromFile(Locksmithy plugin, Player player) {

        LocalPlayerData localPlayerData;

        File file = new File(plugin.USERS_DIR + File.separator + player.getUniqueId().toString() + ".json");
        try {
            JsonReader reader = new JsonReader(new FileReader(file));
            localPlayerData = new Gson().fromJson(reader, LocalPlayerData.class);
            localPlayerData.playerByUuid = player.getUniqueId().toString();
            reader.close();
            return localPlayerData;
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }

    }

    public String getPlayerByUuid() {
        return playerByUuid;
    }
    public boolean canBreakLock() {
        return breakLock;
    }
    public boolean isDebug() {
        return debug;
    }
    public long getTimeTried() {
        return timeTried;
    }

    public void setPlayerByUuid(String playerByUuid) {
        this.playerByUuid = playerByUuid;
    }
    public void setBreakLock(boolean breakLock) {
        this.breakLock = breakLock;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public void updateTimeTried() {
        timeTried = ZonedDateTime.now().toInstant().toEpochMilli();
    }

    public void save(Locksmithy plugin) {

        File file = new File(plugin.USERS_DIR + File.separator + playerByUuid + ".json");
        Gson gson = new Gson();

        try {
            if (file.createNewFile()) {
                plugin.print(file.getPath() + " created.");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(gson.toJson(this));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
