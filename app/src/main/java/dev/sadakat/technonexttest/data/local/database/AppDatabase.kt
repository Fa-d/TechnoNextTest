package dev.sadakat.technonexttest.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.sadakat.technonexttest.data.local.database.dao.PostDao
import dev.sadakat.technonexttest.data.local.database.dao.UserDao
import dev.sadakat.technonexttest.data.local.database.entities.PostEntity
import dev.sadakat.technonexttest.data.local.database.entities.UserEntity

@Database(
    entities = [PostEntity::class, UserEntity::class], version = 1, exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}