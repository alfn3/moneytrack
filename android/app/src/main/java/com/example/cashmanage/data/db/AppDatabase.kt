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
    version = 2,
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
                        db.execSQL("INSERT INTO accounts (id, userId, name, balance) VALUES (1, 'default', 'Cash', 0.0)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (1, 'Food', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (2, 'Transport', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (3, 'Salary', 'INCOME', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (4, 'Entertainment', 'EXPENSE', null)")
                        db.execSQL("INSERT INTO categories (id, name, type, icon) VALUES (5, 'Others', 'EXPENSE', null)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
