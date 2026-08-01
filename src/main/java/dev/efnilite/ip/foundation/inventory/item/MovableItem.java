package dev.efnilite.ip.foundation.inventory.item;

import org.bukkit.Material;

/**
 * An item which may be moved by the player
 *
 * @author Efnilite
 */
public class MovableItem extends Item {

    public MovableItem(Material material, String name) {
        super(material, name);
    }

    public MovableItem(Material material, int amount, String name) {
        super(material, amount, name);
    }

    @Override
    public boolean isMovable() {
        return true;
    }
}