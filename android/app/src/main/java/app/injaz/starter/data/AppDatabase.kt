package app.injaz.starter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ProductEntity::class, CustomerEntity::class, SaleEntity::class, PaymentEntity::class, StockEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "raseed.db")
                .addCallback(object : Callback() {
                    /* The three operators are planted once, when the database file is first made; deleting one later is permanent. */
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT INTO products (id, name, sortOrder, archived, createdAt) VALUES ('zain', 'زين', 0, 0, $now)")
                        db.execSQL("INSERT INTO products (id, name, sortOrder, archived, createdAt) VALUES ('sudani', 'سوداني', 1, 0, $now)")
                        db.execSQL("INSERT INTO products (id, name, sortOrder, archived, createdAt) VALUES ('mtn', 'MTN', 2, 0, $now)")
                    }
                })
                .build()
    }
}
