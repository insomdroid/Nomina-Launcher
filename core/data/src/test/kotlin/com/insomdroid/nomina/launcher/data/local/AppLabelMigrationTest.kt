package com.insomdroid.nomina.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.data.di.MIGRATION_7_8
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppLabelMigrationTest {
    @Test fun version7MigrationAddsLabelCacheAndPreservesHomeItems() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "app-label-migration.db")
        file.delete()

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val schemaFile = File(
                "schemas/com.insomdroid.nomina.data.local.LauncherDatabase/7.json",
            )
            val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
            val home = schema.getJSONArray("entities").getJSONObject(0)
            db.execSQL(
                home.getString("createSql").replace("${'$'}{TABLE_NAME}", home.getString("tableName")),
            )
            db.execSQL(
                home.getJSONArray("indices").getJSONObject(0).getString("createSql")
                    .replace("${'$'}{TABLE_NAME}", home.getString("tableName")),
            )
            val setupQueries = schema.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                db.execSQL(setupQueries.getString(index))
            }
            db.execSQL(
                "INSERT INTO home_items (containerId, folderName, packageName, className, userSerial, " +
                    "shortcutId, page, cellX, cellY, spanX, spanY) " +
                    "VALUES (-1, NULL, 'com.mail', 'com.mail.Main', 0, NULL, 0, 1, 2, 1, 1)",
            )
            db.version = 7
        }

        val database = Room.databaseBuilder(context, LauncherDatabase::class.java, file.absolutePath)
            .addMigrations(MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                assertThat(database.homeItemDao().getAll().map { it.key })
                    .containsExactly("com.mail/com.mail.Main/0")
                database.appLabelDao().replaceAll(
                    listOf(AppLabelEntity("com.mail/com.mail.Main/0", "Mail", "en-US")),
                )
                assertThat(database.appLabelDao().getAll("en-US").map { it.label })
                    .containsExactly("Mail")
            }
            assertThat(database.openHelper.readableDatabase.version).isEqualTo(8)
        } finally {
            database.close()
        }
    }
}
