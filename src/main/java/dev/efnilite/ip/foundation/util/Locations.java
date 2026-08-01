package dev.efnilite.ip.foundation.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Utilities for Locations
 */
public class Locations {

    /**
     * Gets the max of the locations
     *
     * @param pos1 The first location
     * @param pos2 The second location
     * @return the max values of the locations
     */
    public static Location max(Location pos1, Location pos2) {
        World world = pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld();
        return new Location(world, Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    /**
     * Gets the min of the locations
     *
     * @param pos1 The first location
     * @param pos2 The second location
     * @return the min values of the locations
     */
    public static Location min(Location pos1, Location pos2) {
        World world = pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld();
        return new Location(world, Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
    }

    /**
     * Creates a string version of a Location.
     *
     * @param location The location
     * @return string version
     */
    public static String toString(Location location, boolean formatted) {
        if (!formatted) {
            return "(%s,%s,%s,%s)".formatted(location.getX(), location.getY(), location.getZ(), location.getWorld().getName());
        } else {
            return "(%d, %d, %d)".formatted(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    /**
     * Get a location from a string
     *
     * @param string The string
     * @return the location from the string
     */
    public static Location parseLocation(String string) {
        String[] values = string.replaceAll("[()]", "")
                .replace(", ", " ")
                .replace(",", " ")
                .split(" ");

        World world = Bukkit.getWorld(values[3]);

        // if world is null, get first world
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }

        return new Location(world, Double.parseDouble(values[0]), Double.parseDouble(values[1]), Double.parseDouble(values[2]));
    }

}
