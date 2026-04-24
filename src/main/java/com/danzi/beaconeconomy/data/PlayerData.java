package com.danzi.beaconeconomy.data;

import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;

public class PlayerData {
    public boolean introComplete = false;
    public long balance = 0L;
    public int rankIndex = 0;
    public int prestige = 0;
    public final Map<Integer, Location> homes = new HashMap<>();
}
