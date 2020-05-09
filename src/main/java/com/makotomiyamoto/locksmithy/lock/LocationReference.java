package com.makotomiyamoto.locksmithy.lock;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.makotomiyamoto.locksmithy.Locksmithy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.List;

public class LocationReference {

    private transient int x, y, z; // written to file name
    private transient String world; // written to file name
    // x-y-z-world.json

    private String connectedLocationString; // location of block under same lock if possible
    private String ownerByUuid; // whoever made the lock
    private String authorizedKeyByUuid; // the key authorized to the lock
    private boolean advancedLock; // whether or not the lock is advanced
    private boolean exposed; // whether or not the lock was successfully broken into
    private boolean jammed; // whether or not the lock is jammed

    public LocationReference(Location location) {

        x = location.getBlockX();
        y = location.getBlockY();
        z = location.getBlockZ();
        // noinspection ConstantConditions
        world = location.getWorld().getName();

    }
    public String asString() {
        return x + "-" + y + "-" + z + "-" + world;
    }
    @SuppressWarnings("ConstantConditions")
    public static String locationToFileString(Location location) {
        return String.format("%1$s-%2$s-%3$s-%4$s.json", location.getBlockX(), location.getBlockY(), location.getBlockZ(),
                location.getWorld().getName());
    }

    public static LocationReference findByFile(Locksmithy plugin, Location location) {

        File file = new File(plugin.LOCATIONS_DIR + File.separator + locationToFileString(location));
        if (file.exists()) {
            Gson gson = new Gson();
            try {
                JsonReader reader = new JsonReader(new FileReader(file));
                LocationReference reference = gson.fromJson(reader, LocationReference.class);
                reader.close();
                String name = file.getName().replace(".json", "");
                String[] format = name.split("-", 4);
                reference.x = Integer.parseInt(format[0]);
                reference.y = Integer.parseInt(format[1]);
                reference.z = Integer.parseInt(format[2]);
                reference.world = format[3];
                return reference;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    public void setAuthorizedKeyByUuid(String authorizedKeyByUuid) {
        this.authorizedKeyByUuid =
                String.valueOf(
                        Long.parseLong(
                                authorizedKeyByUuid
                                        .replaceAll("§[A-z0-9]", "")
                                        .replaceAll("[^0-9]", "")));
    }
    public String getAuthorizedKeyByUuid() {
        return authorizedKeyByUuid;
    }
    public void setConnectedLocationString(String connectedLocationString) {
        this.connectedLocationString = connectedLocationString;
    }
    public String getConnectedLocationString() {
        return connectedLocationString;
    }

    public void setOwnerByUuid(String ownerByUuid) {
        this.ownerByUuid = ownerByUuid;
    }
    public String getOwnerByUuid() {
        return ownerByUuid;
    }

    public void setAdvancedLock(boolean advancedLock) {
        this.advancedLock = advancedLock;
    }
    public boolean isAdvancedLock() {
        return advancedLock;
    }

    public void setJammed(boolean jammed) {
        this.jammed = jammed;
    }
    public boolean isJammed() {
        return jammed;
    }

    public void setExposed(boolean exposed) {
        this.exposed = exposed;
    }
    public boolean isExposed() {
        return exposed;
    }

    public static LocationReference getDoubleChestTwin(Chest.Type type, BlockFace facing, Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        Location otherChest;
        String ERROR = "Something went.... terribly wrong....";
        switch (facing) {
            case NORTH:
                switch (type) {
                    case RIGHT:
                        otherChest = new Location(world, x-1, y, z);
                        break;
                    case LEFT:
                        otherChest = new Location(world, x+1, y, z);
                        break;
                    default:
                        throw new IllegalArgumentException(ERROR);
                }
                break;
            case EAST:
                switch (type) {
                    case RIGHT:
                        otherChest = new Location(world, x, y, z-1);
                        break;
                    case LEFT:
                        otherChest = new Location(world, x, y, z+1);
                        break;
                    default:
                        throw new IllegalArgumentException(ERROR);
                }
                break;
            case SOUTH:
                switch (type) {
                    case RIGHT:
                        otherChest = new Location(world, x+1, y, z);
                        break;
                    case LEFT:
                        otherChest = new Location(world, x-1, y, z);
                        break;
                    default:
                        throw new IllegalArgumentException(ERROR);
                }
                break;
            case WEST:
                switch (type) {
                    case RIGHT:
                        otherChest = new Location(world, x, y, z+1);
                        break;
                    case LEFT:
                        otherChest = new Location(world, x, y, z-1);
                        break;
                    default:
                        throw new IllegalArgumentException(ERROR);
                }
                break;
            default:
                throw new IllegalArgumentException(ERROR);
        }
        return new LocationReference(otherChest);
    }

    public static boolean isDoor(Block block) {
        switch (block.getType()) {
            case DARK_OAK_DOOR:
            case ACACIA_DOOR:
            case BIRCH_DOOR:
            case IRON_DOOR:
            case JUNGLE_DOOR:
            case OAK_DOOR:
            case SPRUCE_DOOR:
                return true;
            default:
                return false;
        }
    }

    public Material getRequiredType(Locksmithy plugin) {

        ConfigurationSection options = plugin.getConfig().getConfigurationSection("options");
        assert options != null;

        if (advancedLock) {
            //noinspection ConstantConditions
            return Material.valueOf(options.getString("advanced_key.item")
                    .replaceAll(" ", "_").toUpperCase());
        } else {
            //noinspection ConstantConditions
            return Material.valueOf(options.getString("key.item")
                    .replaceAll(" ", "_").toUpperCase());
        }

    }

    public static boolean isAnyAvailableKey(Locksmithy plugin, ItemStack itemStack) {
        return (isKey(plugin, itemStack) || isAdvancedBlankKey(plugin, itemStack) || isBlankKey(plugin, itemStack));
    }

    public static boolean isKey(Locksmithy plugin, ItemStack itemStack) {
        // noinspection ConstantConditions
        return matchLoreRegex(
                plugin.getConfig().getStringList("options.key.match-lore-regex"),
                itemStack.getItemMeta().getLore());
    }

    public boolean keyIDMatches(Locksmithy plugin, ItemStack itemStackThatIsDefinitelyAKey, String node) {
        assert itemStackThatIsDefinitelyAKey != null;
        assert itemStackThatIsDefinitelyAKey.getItemMeta() != null;
        assert itemStackThatIsDefinitelyAKey.getItemMeta().getLore() != null;
        ConfigurationSection key = plugin.getConfig().getConfigurationSection(node);
        assert key != null;
        List<String> loreToMatch = key.getStringList("match-lore-regex");
        try {
            if (itemStackThatIsDefinitelyAKey.getItemMeta().getLore().size() != loreToMatch.size()) {
                return false;
            }
            int keyLine;
            for (keyLine = 0; keyLine < loreToMatch.size(); keyLine++) {
                if (loreToMatch.get(keyLine).contains("%KEY_ID%")) break;
            }
            keyLine--;
            String matchingId = itemStackThatIsDefinitelyAKey.getItemMeta().getLore().get(keyLine);
            matchingId = matchingId.replaceAll("§[A-z0-9]", "").replaceAll("[^0-9]", "");
            return authorizedKeyByUuid.equals(matchingId);
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    public static boolean isBlankKey(Locksmithy plugin, ItemStack itemStack) {
        // noinspection ConstantConditions
        return matchLoreRegex(
                plugin.getConfig().getStringList("options.blank_key.match-lore-regex"),
                itemStack.getItemMeta().getLore());
    }

    public static boolean isAdvancedBlankKey(Locksmithy plugin, ItemStack itemStack) {
        // noinspection ConstantConditions
        return matchLoreRegex(
                plugin.getConfig().getStringList("options.advanced_blank_key.match-lore-regex"),
                itemStack.getItemMeta().getLore());
    }

    public static boolean isAssignedAdvancedKey(Locksmithy plugin, ItemStack itemStack) {
        // noinspection ConstantConditions
        return matchLoreRegex(
                plugin.getConfig().getStringList("options.advanced_key.match-lore-regex"),
                itemStack.getItemMeta().getLore());
    }

    private static boolean matchLoreRegex(List<String> matchLoreRegex, List<String> itemStackLore) {
        if (itemStackLore == null) {
            return false;
        }
        try {
            for (int i = 0; i < matchLoreRegex.size(); i++) {
                if (!itemStackLore.get(i).contains(matchLoreRegex.get(i))) {
                    return false;
                }
            }
        } catch (IndexOutOfBoundsException ignored) {
            return false;
        }
        return true;
    }

    public static boolean locationAlreadyAssigned(Locksmithy plugin, LocationReference reference) {
        return new File(plugin.LOCATIONS_DIR + File.separator + reference.toString() + ".json").exists();
    }

    public String toString() {
        return String.format("%1$s-%2$s-%3$s-%4$s", x, y, z, world);
    }

    public void save(Locksmithy plugin) {
        File file = new File(plugin.LOCATIONS_DIR + File.separator + toString() + ".json");
        try {
            if (file.createNewFile()) {
                plugin.print(file.getPath() + " created.");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(new Gson().toJson(this));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
