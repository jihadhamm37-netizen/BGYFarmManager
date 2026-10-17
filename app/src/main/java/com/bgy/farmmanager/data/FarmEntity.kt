package com.bgy.farmmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "farms")
data class FarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val farm_name: String,
    val group_id: String,
    val game_id: String = "",
    val is_scid_linked: Boolean = false,
    val scid_email: String = "",
    val enable_sound_effects: Boolean = false,
    val enable_background_music: Boolean = false,
    val skip_google_first_login: Boolean = false,
    val mark_google_disconnected: Boolean = true,
    val diamond_notifications: Boolean = true,
    val prioritize_low_end_performance: Boolean = false
)
