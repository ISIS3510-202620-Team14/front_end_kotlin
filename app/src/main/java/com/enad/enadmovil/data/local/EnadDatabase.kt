package com.enad.enadmovil.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GrupoEntity::class, NinoEntity::class, GrupoNinoCrossRef::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class EnadDatabase : RoomDatabase() {
    abstract fun gruposDao(): GruposDao

    companion object {
        @Volatile
        private var instancia: EnadDatabase? = null

        // Groups can now be mirrored in the backend: remote id, sync state and a soft delete until it is synced
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `grupos` ADD COLUMN `remoteId` TEXT")
                db.execSQL("ALTER TABLE `grupos` ADD COLUMN `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE `grupos` ADD COLUMN `eliminado` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun obtener(context: Context): EnadDatabase {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    EnadDatabase::class.java,
                    "enad_movil.db"
                ).addMigrations(MIGRACION_1_2).build().also { instancia = it }
            }
        }
    }
}
