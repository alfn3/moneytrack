package com.example.cashmanage.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        SavingGoalEntity::class,
        BudgetEntity::class,
        SpreadsheetEntity::class,
        SheetEntity::class,
        CellEntity::class,
        AIHistoryEntity::class,
        OCRHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingGoalDao(): SavingGoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun spreadsheetDao(): SpreadsheetDao
    abstract fun aiHistoryDao(): AIHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cashmanage_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed 6 Accounts
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (1, 'default', 'Rekening Utama', 0.0)")
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (2, 'default', 'Tabungan', 0.0)")
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (3, 'default', 'E-Wallet', 0.0)")
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (4, 'default', 'Tunai', 0.0)")
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (5, 'default', 'Kartu Kredit', 0.0)")
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (6, 'default', 'Lainnya', 0.0)")

                        // Seed 11 Categories
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (1, 'Kebutuhan Pokok', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (2, 'Makanan & Minuman', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (3, 'Transportasi', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (4, 'Tagihan & Utang', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (5, 'Edukasi', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (6, 'Kesehatan', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (7, 'Hiburan', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (8, 'Belanja', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (9, 'Investasi & Tabungan', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (10, 'Pendapatan Tetap', 'INCOME', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (11, 'Pendapatan Tambahan', 'INCOME', null)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
