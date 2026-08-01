package dev.efnilite.ip.foundation.util;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Empty-world generator for Paper 1.21.11 and newer.
 *
 * <p>This is part of the plugin itself. It has no VoidGen, PaperLib, or version-specific
 * implementation dependency.</p>
 */
public final class VoidGenerator extends ChunkGenerator {

    private static final VoidGenerator INSTANCE = new VoidGenerator();
    private static final BiomeProvider BIOMES = new BiomeProvider() {
        @Override
        public @NotNull Biome getBiome(@NotNull WorldInfo worldInfo, int x, int y, int z) {
            return Biome.PLAINS;
        }

        @Override
        public @NotNull List<Biome> getBiomes(@NotNull WorldInfo worldInfo) {
            return List.of(Biome.PLAINS);
        }
    };

    private VoidGenerator() {
    }

    public static @NotNull VoidGenerator getGenerator() {
        return INSTANCE;
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public @NotNull BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return BIOMES;
    }
}
