package com.insomdroid.nomina.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A launcher's cached activity label, scoped to the Android profile and locale in [key]. */
@Entity(tableName = "app_labels")
data class AppLabelEntity(
    @PrimaryKey val key: String,
    val label: String,
    val locale: String,
)
