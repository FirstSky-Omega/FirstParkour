package dev.efnilite.ip.foundation.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

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

    /**
     * Returns a new Task instance
     *
     * @param plugin The plugin which to register this Task with
     * @return the created Task instance
     */
    public static Task create(Plugin plugin) {
        return new Task(plugin);
    }

    /**
     * Specifies which Java Runnable should be executed. This supports lambdas.
     *
     * @param runnable The Java Runnable that is going to be run
     * @return the instance of the class
     */
    public Task execute(Runnable runnable) {
        this.defaultRunnable = runnable;
        return this;
    }

    /**
     * Specifies which Bukkit Runnable should be executed
     *
     * @param runnable The Bukkit Runnable that is going to be run
     * @return the instance of the class
     */
    public Task execute(BukkitRunnable runnable) {
        this.bukkitRunnable = runnable;
        return this;
    }

    /**
     * Whether this task should run async
     *
     * @return the instance of this class
     */
    public Task async() {
        this.async = true;
        return this;
    }

    /**
     * The delay this task will run with
     *
     * @param delay The delay in ticks
     * @return the instance of this class
     */
    public Task delay(int delay) {
        this.delay = delay;
        return this;
    }

    /**
     * The repeating interval this task will run with
     *
     * @param repeat The interval in ticks
     * @return the instance of this class
     */
    public Task repeat(int repeat) {
        this.repeat = repeat;
        return this;
    }

    /**
     * Cancels this task
     *
     * @return the instance of this class
     */
    public Task cancel() {
        task.cancel();
        return this;
    }

    /**
     * Cancels the active/waiting task and runs the task immediately
     */
    public void cancelAndRunImmediately() {
        task.cancel();

        if (bukkitRunnable != null) {
            bukkitRunnable.run();
        }
        if (defaultRunnable != null) {
            defaultRunnable.run();
        }
    }

    /**
     * Runs this task
     *
     * @return the BukkitTask instance returned from running this task
     */
    public BukkitTask run() {
        if (bukkitRunnable != null) {
            if (async) { // async
                if (repeat > 0) {
                    task = bukkitRunnable.runTaskTimerAsynchronously(plugin, delay, repeat);
                } else if (delay > 0) {
                    task = bukkitRunnable.runTaskLaterAsynchronously(plugin, delay);
                } else {
                    task = bukkitRunnable.runTaskAsynchronously(plugin);
                }
            } else {
                if (repeat > 0) {
                    task = bukkitRunnable.runTaskTimer(plugin, delay, repeat);
                } else if (delay > 0) {
                    task = bukkitRunnable.runTaskLater(plugin, delay);
                } else {
                    task = bukkitRunnable.runTask(plugin);
                }
            }
        } else if (defaultRunnable != null) {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            if (async) { // async
                if (repeat > 0) {
                    task = scheduler.runTaskTimerAsynchronously(plugin, defaultRunnable, delay, repeat);
                } else if (delay > 0) {
                    task = scheduler.runTaskLaterAsynchronously(plugin, defaultRunnable, delay);
                } else {
                    task = scheduler.runTaskAsynchronously(plugin, defaultRunnable);
                }
            } else {
                if (repeat > 0) {
                    task = scheduler.runTaskTimer(plugin, defaultRunnable, delay, repeat);
                } else if (delay > 0) {
                    task = scheduler.runTaskLater(plugin, defaultRunnable, delay);
                } else {
                    task = scheduler.runTask(plugin, defaultRunnable);
                }
            }
        } else {
            throw new IllegalStateException("Both runnable types are null!");
        }
        return task;
    }
}