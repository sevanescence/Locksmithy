package com.makotomiyamoto.locksmithy;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Extras {

    public static float getVolumeProximity(Location origin, Location target, float degree) {
        return degree/ Float.parseFloat(String.valueOf(target.distance(origin)));
    }

}
