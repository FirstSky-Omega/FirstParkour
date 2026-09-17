package dev.efnilite.ip.foundation.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class for easily constructing tasks.
 *
 * @author Efnilite
 */
public class Task {

    private int delay = 0;
    private int repeat = 0;
    private boolean async = false;
    private final Plugin plugin;
    private Runnable defaultRunnable;
    private BukkitTask task;
    private BukkitRunnable bukkitRunnable;

    public Task(Plugin plugin) {
        this.plugin = plugin;
    }

    public static Task create(Plugin plugin) {
        return new Task(plugin);
    }

    public Task execute(Runnable runnable) {
        this.defaultRunnable = runnable;
        return this;
    }

    public Task execute(BukkitRunnable runnable) {
        this.bukkitRunnable = runnable;
        return this;
    }

    public Task async() {
        this.async = true;
        return this;
    }

    public Task delay(int delay) {
        this.delay = delay;
        return this;
    }

    public Task repeat(int repeat) {
        this.repeat = repeat;
        return this;
    }

    public Task cancel() {
        task.cancel();
        return this;
    }

    public void cancelAndRunImmediately() {
        task.cancel();
        if (bukkitRunnable != null) {
            bukkitRunnable.run();
        }
        if (defaultRunnable != null) {
            defaultRunnable.run();
        }
    }

    public BukkitTask run() {
        Runnable runnable = bukkitRunnable != null ? bukkitRunnable : defaultRunnable;
        if (runnable == null) {
            throw new IllegalStateException("Both runnable types are null!");
        }

        AtomicReference<ScheduledTask> scheduledTaskRef = new AtomicReference<>();
        FoliaTask wrapper = new FoliaTask(plugin, scheduledTaskRef, !async);

        if (bukkitRunnable != null) {
            injectTask(bukkitRunnable, wrapper);
        }

        if (async) {
            if (repeat > 0) {
                long initialDelay = delay > 0 ? toMillis(delay) : 1L;
                scheduledTaskRef.set(Bukkit.getServer().getAsyncScheduler()
                        .runAtFixedRate(plugin, t -> runnable.run(), initialDelay, toMillis(repeat), TimeUnit.MILLISECONDS));
            } else if (delay > 0) {
                scheduledTaskRef.set(Bukkit.getServer().getAsyncScheduler()
                        .runDelayed(plugin, t -> runnable.run(), toMillis(delay), TimeUnit.MILLISECONDS));
            } else {
                scheduledTaskRef.set(Bukkit.getServer().getAsyncScheduler()
                        .runNow(plugin, t -> runnable.run()));
            }
        } else {
            if (repeat > 0) {
                long initialDelay = delay > 0 ? delay : 1L;
                scheduledTaskRef.set(Bukkit.getServer().getGlobalRegionScheduler()
                        .runAtFixedRate(plugin, t -> runnable.run(), initialDelay, repeat));
            } else if (delay > 0) {
                scheduledTaskRef.set(Bukkit.getServer().getGlobalRegionScheduler()
                        .runDelayed(plugin, t -> runnable.run(), delay));
            } else {
                scheduledTaskRef.set(Bukkit.getServer().getGlobalRegionScheduler()
                        .run(plugin, t -> runnable.run()));
            }
        }

        task = wrapper;
        return task;
    }

    private static long toMillis(int ticks) {
        return ticks * 50L;
    }

    private static void injectTask(BukkitRunnable runnable, BukkitTask task) {
        try {
            Field field = BukkitRunnable.class.getDeclaredField("task");
            field.setAccessible(true);
            field.set(runnable, task);
        } catch (Exception ignored) {}
    }

    private record FoliaTask(Plugin plugin, AtomicReference<ScheduledTask> ref, boolean sync) implements BukkitTask {

        @Override
        public int getTaskId() {
            return -1;
        }

        @Override
        public Plugin getOwner() {
            return plugin;
        }

        @Override
        public boolean isSync() {
            return sync;
        }

        @Override
        public boolean isCancelled() {
            ScheduledTask t = ref.get();
            return t != null && t.isCancelled();
        }

        @Override
        public void cancel() {
            ScheduledTask t = ref.get();
            if (t != null) t.cancel();
        }
    }
}
