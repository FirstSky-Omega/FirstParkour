package dev.efnilite.iep.menu

import dev.efnilite.iep.IEP
import dev.efnilite.iep.config.Locales
import dev.efnilite.iep.generator.ResetReason
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.player.ElytraPlayer
import dev.efnilite.iep.style.RandomStyle
import dev.efnilite.ip.foundation.inventory.Menu

object StylesMenu {

    fun open(player: ElytraPlayer) {
        val menu = Menu(3, Locales.getString(player, "styles.title"))
            .distributeRowEvenly(2)

        val styles = IEP.getStyles()
        val generator = player.getGenerator()

        for ((idx, style) in styles.withIndex()) {
            if (!player.hasPermission("iep.setting.style.${style.name()}")) {
                continue
            }

            // Resolve the localized display name from locales/<lang>.yml under
            // styles.names.<key>; fall back to the raw config.yml key if the entry
            // is missing so we don't render "<bold></bold>" for an unmapped style.
            val key = style.name()
            val localizedName = Locales.getString(player, "styles.names.$key").ifBlank { key }
            val typeKey = if (style is RandomStyle) "random" else "incremental"
            val localizedType = Locales.getString(player, "styles.types.$typeKey").ifBlank { typeKey }

            val item = Locales.getItem(player, "styles.style", localizedName, localizedType)

            // Pick a representative material for the icon; loop until we find a
            // material that's a valid Item (some Materials are block-only and would
            // throw on ItemStack construction).
            item.material(style.next())
            while (!item.material.isItem) {
                item.material(style.next())
            }

            menu.item(idx, item
                    .click({
                        generator.set { settings -> Settings(settings, style = key) }

                        // todo for speed demon
                        if (generator.getScore() == 0.0) {
                            generator.reset(ResetReason.RESET)
                        }

                        player.player.closeInventory()
                    }))
        }

        menu.item(21, Locales.getItem(player, "styles.random").material(styles.random().next())
                .click({
                    generator.set { settings -> Settings(settings, style = styles.random().name()) }
                    player.player.closeInventory()
                }))
            .item(23, Locales.getItem(player, "go back").click({ SettingsMenu.open(player) }))
            .open(player.player)
    }
}
