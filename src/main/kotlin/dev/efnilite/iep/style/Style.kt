package dev.efnilite.iep.style

import org.bukkit.Material

interface Style {

    /**
     * Returns the next block data.
     */
    fun next(): Material

    /**
     * Returns the name of the style.
     */
    fun name(): String

}