package dev.efnilite.ip.foundation.inventory.item;

import dev.efnilite.ip.foundation.inventory.Menu;
import dev.efnilite.ip.foundation.inventory.MenuClickEvent;
import dev.efnilite.ip.foundation.util.Colls;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Class for an item which uses the difference between left/right click to assign values.
 *
 * @author Efnilite
 */
public class SliderItem extends MenuItem {

    /**
     * The initial value which will be displayed
     */
    private int current;
    private final Map<Integer, Item> items = new HashMap<>();
    private final Map<Integer, Predicate<MenuClickEvent>> switchFunctions = new HashMap<>();

    /**
     * Sets the initial viewing index.
     * If you set up 2 items, with index 0 and 1, you can specify which will be viewed first.
     *
     * @param initial The initial viewing index
     * @return the instance of this class
     */
    public SliderItem initial(int initial) {
        this.current = initial;
        return this;
    }

    /**
     * Adds an item to the possible options. This uses a Function.
     * The Function will determine whether the item will update in the inventory.
     * If this returns false, it will not update the item in the menu, but it will execute the code.
     *
     * @param value      The value assigned to this item
     * @param item       The item
     * @param onSwitchTo What happens on switch to this item. Returns true if it should update, false if not.
     * @return the instance of this class
     */
    public SliderItem add(int value, Item item, Predicate<MenuClickEvent> onSwitchTo) {
        items.put(value, item);
        switchFunctions.put(value, onSwitchTo);
        return this;
    }

    @Override
    public void handleClick(Menu menu, InventoryClickEvent event, ClickType clickType) {
        int currentTo = current;
        switch (clickType) {
            case LEFT:
                currentTo++;
                if (currentTo >= items.size()) {
                    currentTo = 0;
                }
                break;
            case SHIFT_LEFT:
                currentTo = items.size() - 1;
                break;
            case RIGHT:
                currentTo--;
                if (currentTo == -1) {
                    currentTo = items.size() - 1;
                }
                break;
            case SHIFT_RIGHT:
                currentTo = 0;
                break;
            default:
                return;
        }

        Predicate<MenuClickEvent> function = switchFunctions.get(currentTo);
        if (function == null) {
            return;
        }
        boolean update = function.test(new MenuClickEvent(event.getSlot(), menu, this, event));

        if (update) {
            event.getInventory().setItem(event.getSlot(), items.get(currentTo).build());

            current = currentTo;

            menu.updateItem(event.getSlot());
        }
    }

    @Override
    public ItemStack build() {
        if (items.keySet().size() == 0) {
            throw new IllegalArgumentException("Items size is 0!");
        }

        Item init = items.get(current);
        if (init == null) {
            init = items.get(Colls.random(new ArrayList<>(items.keySet())));
        }

        return init.build();
    }

    @Override
    public boolean isMovable() {
        return false;
    }
}
