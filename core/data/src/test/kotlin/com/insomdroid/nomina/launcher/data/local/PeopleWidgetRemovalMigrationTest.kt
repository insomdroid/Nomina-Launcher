package com.insomdroid.nomina.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.insomdroid.nomina.data.di.MIGRATION_8_9
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PeopleWidgetRemovalMigrationTest {

    @Test
    fun version9MigrationRemovesOnlyPeopleRowsAndPreservesOtherHomeItems() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "people-widget-removal-migration.db")
        file.delete()

        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val schemaFile = File(
                "schemas/com.insomdroid.nomina.data.local.LauncherDatabase/8.json",
            )
            val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
            val entities = schema.getJSONArray("entities")

            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                db.execSQL(
                    entity.getString("createSql")
                        .replace("${'$'}{TABLE_NAME}", entity.getString("tableName")),
                )
                if (entity.has("indices")) {
                    val indices = entity.getJSONArray("indices")
                    for (indexInEntity in 0 until indices.length()) {
                        db.execSQL(
                            indices.getJSONObject(indexInEntity).getString("createSql")
                                .replace("${'$'}{TABLE_NAME}", entity.getString("tableName")),
                        )
                    }
                }
            }

            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            val setupQueries = schema.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                db.execSQL(setupQueries.getString(index))
            }

            db.execSQL(
                "INSERT INTO home_items (containerId, folderName, packageName, className, userSerial, " +
                    "shortcutId, page, cellX, cellY, spanX, spanY, builtinType) " +
                    "VALUES (-1, NULL, '', '', 0, NULL, 0, 0, 0, 1, 1, 'people')",
            )
            db.execSQL(
                "INSERT INTO home_items (containerId, folderName, packageName, className, userSerial, " +
                    "shortcutId, page, cellX, cellY, spanX, spanY) " +
                    "VALUES (-1, NULL, 'com.mail', 'com.mail.Main', 0, NULL, 0, 1, 0, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO home_items (containerId, folderName, packageName, className, userSerial, " +
                    "shortcutId, page, cellX, cellY, spanX, spanY, appWidgetId, widgetProvider) " +
                    "VALUES (-1, NULL, '', '', 0, NULL, 0, 2, 0, 2, 2, NULL, 'com.clock/.ClockWidget')",
            )
            db.version = 8
        }

        val database = Room.databaseBuilder(
            context,
            LauncherDatabase::class.java,
            file.absolutePath,
        )
            .addMigrations(MIGRATION_8_9)
            .allowMainThreadQueries()
            .build()

        try {
            val items = runBlocking { database.homeItemDao().getAll() }
            assertThat(items).hasSize(2)
            assertThat(items.map { it.packageName }).containsExactly("", "com.mail")
            assertThat(items.map { it.widgetProvider }.filterNotNull())
                .containsExactly("com.clock/.ClockWidget")
            assertThat(items.map { it.builtinType }.filterNotNull()).isEmpty()
            assertThat(database.openHelper.readableDatabase.version).isEqualTo(9)
        } finally {
            database.close()
        }
    }
}
