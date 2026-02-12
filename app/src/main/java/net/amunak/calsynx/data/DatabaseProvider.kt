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
				.addMigrations(
					MIGRATION_5_6,
					MIGRATION_6_7,
					MIGRATION_7_8,
					MIGRATION_8_9,
					MIGRATION_9_10,
					MIGRATION_10_11
				)
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

	private val MIGRATION_6_7 = object : Migration(6, 7) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN availabilityMode INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN copyEventColor INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN copyPrivacy INTEGER NOT NULL DEFAULT 1")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN copyAttendees INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN copyOrganizer INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderMode INTEGER NOT NULL DEFAULT 0")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderAllDayMinutes INTEGER NOT NULL DEFAULT 1440")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderTimedMinutes INTEGER NOT NULL DEFAULT 60")
		}
	}

	private val MIGRATION_7_8 = object : Migration(7, 8) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderAllDayEnabled INTEGER NOT NULL DEFAULT 1")
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderTimedEnabled INTEGER NOT NULL DEFAULT 1")
		}
	}

	private val MIGRATION_8_9 = object : Migration(8, 9) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL("ALTER TABLE sync_jobs ADD COLUMN reminderResyncEnabled INTEGER NOT NULL DEFAULT 1")
		}
	}

	private val MIGRATION_9_10 = object : Migration(9, 10) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL(
				"""
				ALTER TABLE sync_jobs
				ADD COLUMN pairExistingOnFirstSync INTEGER NOT NULL DEFAULT 0
				""".trimIndent()
			)
		}
	}

	private val MIGRATION_10_11 = object : Migration(10, 11) {
		override fun migrate(db: SupportSQLiteDatabase) {
			db.execSQL(
				"""
				ALTER TABLE sync_jobs
				ADD COLUMN deleteUnmappedTargets INTEGER NOT NULL DEFAULT 0
				""".trimIndent()
			)
		}
	}
}
