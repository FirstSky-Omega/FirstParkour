package dev.efnilite.ip.foundation.inventory.item;

import dev.efnilite.ip.foundation.inventory.Menu;
import dev.efnilite.ip.foundation.inventory.MenuClickEvent;
import dev.efnilite.ip.foundation.util.Task;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * A MenuItem which only stays for a certain amount of ticks. This item is meant as a confirm option or to display items for a small amount of time.
 * On click of this item it will automatically go back to the previous item.
 */
public class TimedItem extends MenuItem {

    private int timeStay;
    private Task task;
    private MenuItem revertTo;
    private final Plugin plugin;
    private final Player player;
    private final MenuItem item;
    private final MenuClickEvent event;

    public TimedItem(MenuItem item, MenuClickEvent event, Plugin plugin) {
        this.item = item;
        this.revertTo = event.menu().getItem(event.slot());
        if (revertTo == null) {
            revertTo = new Item(Material.AIR, "<red> ");
        }
        this.player = event.getPlayer();
        this.event = event;
        this.plugin = plugin;
    }

    /**
     * Sets the amount of ticks this item will stay in view for.
     *
     * @param ticks The amount of ticks
     * @return the instance of the class
     */
    public TimedItem stay(int ticks) {
        this.timeStay = ticks;
        return this;
    }

    @Override
    public ItemStack build() {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                Menu menu = event.menu();
                InventoryView view = player.getOpenInventory();
                if (view.getTitle().equals(menu.getTitle())) {
                    menu.item(event.slot(), revertTo);
                    menu.updateItem(event.slot());
                } else {
                    cancel(); // prevent going on forever
                }
            }
        };

        task = Task.create(plugin).delay(timeStay).execute(runnable);
        task.run();

        return item.build();
    }

    @Override
    public void handleClick(Menu menu, InventoryClickEvent event, ClickType clickType) {
        item.handleClick(menu, event, clickType);

        task.cancelAndRunImmediately();
    }

    @Override
    public boolean isMovable() {
        return false;
    }
}