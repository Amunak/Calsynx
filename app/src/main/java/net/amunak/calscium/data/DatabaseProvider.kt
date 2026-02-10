package net.amunak.calscium.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
	@Volatile
	private var instance: AppDatabase? = null

	fun get(context: Context): AppDatabase {
		return instance ?: synchronized(this) {
			instance ?: Room.databaseBuilder(
				context.applicationContext,
				AppDatabase::class.java,
				"calscium.db"
			)
				.fallbackToDestructiveMigration()
				.build()
				.also { instance = it }
		}
	}
}
