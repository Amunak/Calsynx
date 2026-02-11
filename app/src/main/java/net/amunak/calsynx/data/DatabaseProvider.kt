package net.amunak.calsynx.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
	@Volatile
	private var instance: AppDatabase? = null

	fun get(context: Context): AppDatabase {
		return instance ?: synchronized(this) {
			instance ?: Room.databaseBuilder(
				context.applicationContext,
				AppDatabase::class.java,
				"calsynx.db"
			)
				.addMigrations(MIGRATION_5_6)
				.build()
				.also { instance = it }
		}
	}

	private val MIGRATION_5_6 = object : Migration(5, 6) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL(
				"""
				ALTER TABLE sync_jobs
				ADD COLUMN lastSyncUnpairedTargetCount INTEGER NOT NULL DEFAULT 0
				""".trimIndent()
			)
		}
	}
}
