package dev.efnilite.ip.foundation.inventory;

import dev.efnilite.ip.foundation.event.EventWatcher;
import dev.efnilite.ip.foundation.inventory.item.Item;
import dev.efnilite.ip.foundation.inventory.item.MenuItem;
import dev.efnilite.ip.foundation.util.Numbers;
import dev.efnilite.ip.foundation.util.Strings;
import dev.efnilite.ip.foundation.util.Task;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Class for Menu handling
 *
 * @author Efnilite
 */
@SuppressWarnings("unused")
public class Menu implements EventWatcher {

    private final static Map<UUID, UUID> openMenus = new HashMap<>();
    private final static List<Menu> disabledMenus = new ArrayList<>();

    private static Plugin plugin;

    public static void init(Plugin pl) {
        Task.create(pl).repeat(5 * 20).execute(() -> {
            if (openMenus.keySet().isEmpty()) {
                for (Menu menu : disabledMenus) {
                    menu.unregisterAll();
                }
                disabledMenus.clear();
            }
        }).run();

        plugin = pl;
    }

    protected final int rows;
    protected final String title;
    protected final Map<Integer, MenuItem> items = new HashMap<>();
    protected final List<Integer> evenlyDistributedRows = new ArrayList<>();
    protected boolean deactivated = false;
    protected UUID inventoryId;
    protected Player player;
    protected Material filler = null;

    public Menu(int rows, String name) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be above 1 and below 6");
        }
        this.rows = rows;
        this.title = Strings.colour(name);
        this.inventoryId = UUID.randomUUID();
    }

    /**
     * Returns a list of slot numbers that evenly distribute the amount of items in a row.
     *
     * @param amountInRow The amount of items in the row.
     * @return A list of slot numbers that ensure an equal distribution.
     */
    public static List<Integer> getEvenlyDistributedSlots(int amountInRow) {
        return switch (amountInRow) {
            case 0 -> Collections.emptyList();
            case 1 -> Collections.singletonList(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(3, 4, 5);
            case 4 -> List.of(2, 3, 5, 6);
            case 5 -> List.of(2, 3, 4, 5, 6);
            case 6 -> List.of(1, 2, 3, 5, 6, 7);
            case 7 -> List.of(1, 2, 3, 4, 5, 6, 7);
            case 8 -> List.of(0, 1, 2, 3, 5, 6, 7, 8);
            default -> List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
        };
    }

    /**
     * Sets an item to a slot
     *
     * @param slot The slot
     * @param item The item
     * @return the instance of this class
     */
    public Menu item(int slot, MenuItem item) {
        if (slot > rows * 9 || slot < 0) {
            throw new IllegalArgumentException("Slot %d is not in inventory".formatted(slot));
        }

        items.put(slot, item);
        return this;
    }

    /**
     * Sets a specific set of rows to be distributed evenly. The items will be distributed.
     * Starts from 0 and goes up to 5.
     *
     * @param rows The rows
     * @return the instance of this class
     */
    public Menu distributeRowEvenly(int... rows) {
        for (int row : rows) {
            if (row < 0 || row > 5) {
                throw new IllegalArgumentException("Rows must be above 1 and below 6");
            }
            evenlyDistributedRows.add(row);
        }
        return this;
    }

    /**
     * Will distribute all rows evenly.
     *
     * @return the instance of this class
     * @see #distributeRowEvenly(int...)
     */
    public Menu distributeRowsEvenly() {
        evenlyDistributedRows.addAll(Numbers.getFromZero(rows));
        return this;
    }

    /**
     * Fills the background with a specific item
     *
     * @param filler The background filler
     * @return the instance of this class
     */
    public Menu fillBackground(@NotNull Material filler) {
        this.filler = filler;
        return this;
    }

    /**
     * Updates a specific item
     *
     * @param slots The slots which are to be updated
     */
    public void updateItem(int... slots) {
        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory.getSize() % 9 != 0) {
            throw new IllegalArgumentException("Invalid inventory type");
        }

        for (int slot : slots) {
            inventory.setItem(slot, items.get(slot).build());
        }
    }

    /**
     * Updates all items in the inventory
     */
    public void update() {
        Inventory inventory = player.getOpenInventory().getTopInventory();

        if (inventory.getSize() % 9 != 0) {
            throw new IllegalArgumentException("Invalid inventory type");
        }

        inventory.clear();
        items.forEach((slot, item) -> inventory.setItem(slot, item.build()));
    }

    /**
     * Updates all items by calling {@link #update()} in the inventory periodically.
     *
     * @param tickInterval The amount of ticks to wait between calling {@link #update()}
     */
    public void update(int tickInterval) {
        if (tickInterval <= 0) {
            throw new IllegalArgumentException("Tick interval must be above 0");
        }

        Task.create(plugin)
                .repeat(tickInterval)
                .execute(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (deactivated) {
                            cancel();
                            return;
                        }

                        update();
                    }
                })
                .run();
    }

    /**
     * Opens the menu for the player. This distributes items on the same row automatically if these rows are assigned to automatically distribute.
     *
     * @param player The player to open it to
     */
    public void open(Player player) {
        this.player = player;
        Inventory inventory = Bukkit.createInventory(null, rows * 9, title);

        // Evenly distributed rows
        for (int row : evenlyDistributedRows) {
            int min = row * 9; // 0 * 9 = 0
            int max = min + 8; // 0 + 8 = slot 8
            Map<Integer, MenuItem> itemsInRow = new HashMap<>();

            for (int slot : items.keySet()) { // get all items in the specified row
                if (slot >= min && slot <= max) {
                    itemsInRow.put(slot, items.get(slot));
                }
            }

            if (itemsInRow.keySet().isEmpty()) {
                continue;
            }

            List<Integer> sortedSlots = itemsInRow.keySet().stream().sorted().toList(); // sort all slots
            List<Integer> slots = getEvenlyDistributedSlots(sortedSlots.size()); // evenly distribute items
            List<Integer> olds = new ArrayList<>();
            List<Integer> news = new ArrayList<>();

            for (int i = 0; i < slots.size(); i++) {
                int newSlot = slots.get(i) + (9 * row); // gets the new slot
                int oldSlot = sortedSlots.get(i); // the previous slot
                MenuItem item = itemsInRow.get(oldSlot); // the item in the previous slot

                news.add(newSlot);
                olds.add(oldSlot);
                items.put(newSlot, item); // put item in new slot
            }

            for (int oldSlot : olds) {
                if (news.contains(oldSlot)) {
                    continue;
                }
                items.remove(oldSlot); // remove items from previous slot without deleting ones that are to-be moved
            }
        }

        // Filler
        if (filler != null) {
            Item fillerItem = new Item(filler, "<red> "); // fill the background with the same material

            // ignore already-set items
            IntStream.range(0, rows * 9)
                    .filter(slot -> items.get(slot) == null)
                    .forEach(slot -> items.put(slot, fillerItem));
        }


        player.openInventory(inventory);
        openMenus.put(player.getUniqueId(), inventoryId);

        items.forEach((slot, item) -> inventory.setItem(slot, item.build()));

        register(plugin);
    }

    @EventHandler
    public void click(@NotNull InventoryClickEvent event) {
        if (deactivated || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        UUID id = openMenus.get(event.getWhoClicked().getUniqueId());
        if (id != inventoryId) {
            return;
        }

        MenuItem clickedItem = items.get(event.getSlot());
        if (clickedItem == null) {
            return;
        }

        event.setCancelled(!clickedItem.isMovable());

        clickedItem.handleClick(this, event, event.getClick());
    }

    @EventHandler
    public void close(InventoryCloseEvent event) {
        if (deactivated) {
            return;
        }

        UUID viewerId = event.getPlayer().getUniqueId();
        UUID id = openMenus.get(viewerId);
        if (id != inventoryId) {
            return;
        }

        deactivated = true;
        disabledMenus.add(this);
        openMenus.remove(viewerId);
    }

    /**
     * Returns the item in the respective slot.
     *
     * @param slot The slot
     * @return the item in this slot. This may be null.
     */
    public @Nullable MenuItem getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Gets the slots and their respective items
     *
     * @return a Map with the slots and items.
     */
    public Map<Integer, MenuItem> getItems() {
        return items;
    }

    /**
     * Gets the player
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    public String getTitle() {
        return title;
    }
}
