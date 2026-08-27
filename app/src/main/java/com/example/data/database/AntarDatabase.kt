package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.StoreDao
import com.example.data.models.Customer
import com.example.data.models.CustomerTransaction
import com.example.data.models.Product
import com.example.data.models.Sale
import com.example.data.models.User

@Database(
    entities = [Product::class, Sale::class, User::class, Customer::class, CustomerTransaction::class],
    version = 9,
    exportSchema = false
)
abstract class AntarDatabase : RoomDatabase() {
    abstract fun storeDao(): StoreDao

    companion object {
        @Volatile
        private var INSTANCE: AntarDatabase? = null

        fun getInstance(context: Context): AntarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AntarDatabase::class.java,
                    "antar_store_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun destroyInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
