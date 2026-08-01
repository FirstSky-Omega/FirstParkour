package dev.efnilite.ip.foundation.inventory.item;

import com.google.common.annotations.Beta;
import dev.efnilite.ip.foundation.inventory.Menu;
import dev.efnilite.ip.foundation.inventory.MenuClickEvent;
import dev.efnilite.ip.foundation.util.Colls;
import dev.efnilite.ip.foundation.util.Task;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Class for an item which automatically slides.
 *
 * @author Efnilite
 */
@Beta
public class AutoSliderItem extends MenuItem {

    /**
     * The initial value which will be displayed
     */
    private int current;
    private int cooldown;
    private final int slot;
    private final Plugin plugin;
    private final Menu menu;
    private final Map<Integer, Item> items = new HashMap<>();
    private final Map<Integer, Consumer<MenuClickEvent>> clickFunctions = new HashMap<>();

    /**
     * The constructor.
     *
     * @param menu The menu that this item will be displayed in.
     */
    public AutoSliderItem(int slot, Menu menu, Plugin plugin) {
        this.slot = slot;
        this.menu = menu;
        this.plugin = plugin;
    }

    /**
     * Sets the initial viewing index.
     * If you set up 2 items, with index 0 and 1, you can specify which will be viewed first.
     *
     * @param initial The initial viewing index
     * @return the instance of this class
     */
    public AutoSliderItem initial(int initial) {
        this.current = initial;
        return this;
    }

    /**
     * Sets the cooldown between sliding to the next item.
     *
     * @param ticks The amount of ticks until the transition occurs.
     * @return the instance of this class
     */
    public AutoSliderItem cooldown(int ticks) {
        this.cooldown = ticks;
        return this;
    }

    /**
     * Adds an item to the possible options. This uses a Function.
     * The Function will determine whether the item will update in the inventory.
     * If this returns false, it will not update the item in the menu, but it will execute the code.
     *
     * @param value   The value assigned to this item
     * @param item    The item
     * @param onClick What happens on switch to this item. Returns true if it should update, false if not.
     * @return the instance of this class
     */
    public AutoSliderItem add(int value, Item item, Consumer<MenuClickEvent> onClick) {
        items.put(value, item);
        clickFunctions.put(value, onClick);
        return this;
    }

    @Override
    public void handleClick(Menu menu, InventoryClickEvent event, ClickType clickType) {
        Consumer<MenuClickEvent> function = clickFunctions.get(current);
        if (function == null) {
            return;
        }
        function.accept(new MenuClickEvent(event.getSlot(), menu, this, event));
    }

    @Override
    public ItemStack build() {
        if (items.keySet().isEmpty()) {
            throw new IllegalArgumentException("Items size is <0 or 0!");
        }

        Item init = items.get(current);
        if (init == null) {
            init = items.get(Colls.random(new ArrayList<>(items.keySet())));
        }

        if (items.size() > 1) { // loop through if there is more than 1 player
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    InventoryView view = menu.getPlayer().getOpenInventory();
                    if (view.getTitle().equals(menu.getTitle())) {
                        view.getTopInventory().setItem(slot, getNextItem().build());
                    } else {
                        cancel(); // prevent going on forever
                    }
                }
            };

            Task task = Task.create(plugin).delay(cooldown).repeat(cooldown).execute(runnable);
            task.run();
        }

        return init.build();
    }

    /**
     * Returns the next item. Increases the counter.
     *
     * @return the next item
     */
    public Item getNextItem() {
        current++;
        if (current >= items.size() - 1) {
            current = 0;
        }

        return items.get(current);
    }

    @Override
    public boolean isMovable() {
        return false;
    }
}