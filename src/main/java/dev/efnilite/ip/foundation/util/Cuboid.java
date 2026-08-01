package dev.efnilite.ip.foundation.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Wall;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * Asynchronously gets the {@link Block} instances between the two specified {@link Location} instances.
 * Executes the provided {@link Consumer} with the gathered block list.
 */
public class Cuboid {

    /**
     * The amount of changes per tick.
     */
    public static final int CHANGES_PER_TICK = 2500;

    /**
     * Sets blocks in <code>blocks</code> to their respective {@link BlockData}.
     * Performs <code>onComplete</code> when block setting has finished.
     *
     * @param blocks     The block map.
     * @param onComplete What to do on completion.
     */
    public static void set(@NotNull Map<Block, BlockData> blocks, Plugin plugin, @Nullable Runnable onComplete) {
        Queue<Block> queue = new LinkedList<>(blocks.keySet());

        Task.create(plugin).repeat(1).execute(new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < CHANGES_PER_TICK; i++) {
                    Block block = queue.poll();

                    if (block == null) {
                        // no blocks left, so cancel task
                        cancel();

                        if (onComplete != null) {
                            onComplete.run();
                        }

                        return;
                    }

                    setBlock(block, blocks.get(block));
                }
            }
        }).run();
    }

    private static void setBlock(Block block, BlockData data) {
        block.setBlockData(data, data instanceof Fence || data instanceof Wall);
    }

    /**
     * Returns all blocks between the provided locations.
     *
     * @param pos1 The first location
     * @param pos2 The second location
     */
    public static List<Block> get(@NotNull Location pos1, @NotNull Location pos2, boolean ignoreAir) {
        List<Block> blocks = new ArrayList<>();
        Location max = Locations.max(pos1, pos2);
        Location min = Locations.min(pos1, pos2);
        Location location = max.clone();

        location.setWorld(pos1.getWorld() == null ? pos2.getWorld() : pos1.getWorld());
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    if (ignoreAir) {
                        if (location.getBlock().getType() == Material.AIR) {
                            continue;
                        }
                    }

                    blocks.add(location.getBlock());
                }
            }
        }

        return blocks;
    }

    /**
     * Returns all blocks between the provided locations asynchronously.
     *
     * @param pos1       The first location
     * @param pos2       The second location
     * @param onComplete A {@link Consumer} with the list of gathered blocks.
     */
    public static void getAsync(@NotNull Location pos1, @NotNull Location pos2, boolean ignoreAir,
                                Plugin plugin, @NotNull Consumer<List<Block>> onComplete) {
        Task.create(plugin).async().execute(() -> onComplete.accept(get(pos1, pos2, ignoreAir))).run();
    }
}