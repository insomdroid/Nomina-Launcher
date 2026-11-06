package com.insomdroid.nomina.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AppLabelDao {
    @Query("SELECT * FROM app_labels WHERE locale = :locale")
    suspend fun getAll(locale: String): List<AppLabelEntity>

    @Query("DELETE FROM app_labels WHERE locale = :locale")
    suspend fun deleteLocale(locale: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(labels: List<AppLabelEntity>)

    @Transaction
    suspend fun replaceAll(labels: List<AppLabelEntity>, locale: String) {
        deleteLocale(locale)
        if (labels.isNotEmpty()) insertAll(labels)
    }
}
