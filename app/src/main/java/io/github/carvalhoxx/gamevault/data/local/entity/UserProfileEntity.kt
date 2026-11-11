package io.github.carvalhoxx.gamevault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String,
    val globalDailySeconds: Long
)
