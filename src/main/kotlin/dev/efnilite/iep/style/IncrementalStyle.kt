package dev.efnilite.iep.style

import org.bukkit.Material

/**
 * Represents a style where each block is selected incrementally.
 */
data class IncrementalStyle(val name: String, val data: List<Material>) : Style {

    private var idx = 0

    override fun next(): Material {
        val next = data[idx]

        idx++
        if (idx >= data.size) {
            idx = 0
        }

        return next
    }

    override fun name() = name
}