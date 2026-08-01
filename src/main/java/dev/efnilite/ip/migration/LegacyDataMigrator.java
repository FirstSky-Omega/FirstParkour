package dev.efnilite.ip.migration;

import dev.efnilite.ip.IP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Imports the former IPPlus and IEP data folders without modifying the originals.
 */
public final class LegacyDataMigrator {

    private LegacyDataMigrator() {
    }

    public static void migrate(IP plugin) {
        Path plugins = plugin.getDataFolder().toPath().getParent();
        if (plugins == null) {
            return;
        }

        int copied = copyMissing(plugin, plugins.resolve("IPPlus"), plugin.getDataFolder().toPath().resolve("plus"));
        copied += copyMissing(plugin, plugins.resolve("IEP"), plugin.getDataFolder().toPath().resolve("elytra"));

        if (copied > 0) {
            plugin.getLogger().info("Imported " + copied + " legacy IPPlus/IEP data files; the original folders were left untouched.");
        }
    }

    private static int copyMissing(IP plugin, Path source, Path target) {
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        AtomicInteger copied = new AtomicInteger();
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(path -> !Files.isSymbolicLink(path)).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination);
                    } else if (Files.notExists(destination, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                        copied.incrementAndGet();
                    }
                } catch (IOException exception) {
                    plugin.getLogger().warning("Could not import legacy file " + path + ": " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not inspect legacy data folder " + source + ": " + exception.getMessage());
        }
        return copied.get();
    }
}
