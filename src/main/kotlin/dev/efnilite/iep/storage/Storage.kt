package dev.efnilite.iep.storage

import dev.efnilite.iep.config.Config
import dev.efnilite.iep.generator.Settings
import dev.efnilite.iep.leaderboard.Leaderboard
import java.util.*

object Storage {

    private val sql = Config.CONFIG.getBoolean("mysql.enabled")

    fun init(uuid: UUID) {
        if (!sql) {
            DiskStorage.init(uuid)
        }
    }

    fun init(leaderboard: Leaderboard) {
        if (sql) {
            MySQLStorage.init(leaderboard)
        } else {
            DiskStorage.init(leaderboard)
        }
    }

    fun save(uuid: UUID, settings: Settings) {
        if (sql) {
            MySQLStorage.save(uuid, settings)
        } else {
            DiskStorage.save(uuid, settings)
        }
    }

    fun load(uuid: UUID): Settings? {
        return if (sql) {
            MySQLStorage.load(uuid)
        } else {
            DiskStorage.load(uuid)
        }
    }

    fun save(leaderboard: Leaderboard) {
        if (sql) {
            MySQLStorage.save(leaderboard)
        } else {
            DiskStorage.save(leaderboard)
        }
    }

    fun load(leaderboard: Leaderboard) {
        if (sql) {
            MySQLStorage.load(leaderboard)
        } else {
            DiskStorage.load(leaderboard)
        }
    }
}