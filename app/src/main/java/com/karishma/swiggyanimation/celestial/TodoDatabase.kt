package com.karishma.swiggyanimation.celestial

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val hour: Int,
    val date: String
)

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE date = :date ORDER BY hour, id")
    fun todosForDate(date: String): Flow<List<TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TodoEntity)

    @Delete
    suspend fun delete(todo: TodoEntity)
}

@Database(entities = [TodoEntity::class], version = 1, exportSchema = false)
abstract class CelestialDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile private var INSTANCE: CelestialDatabase? = null

        fun get(context: Context): CelestialDatabase = INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(context.applicationContext, CelestialDatabase::class.java, "celestial_db")
                .build().also { INSTANCE = it }
        }
    }
}
