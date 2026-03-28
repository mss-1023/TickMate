package com.tickmate.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tickmate.app.data.db.dao.BudgetDao
import com.tickmate.app.data.db.dao.CategoryDao
import com.tickmate.app.data.db.dao.RecordDao
import com.tickmate.app.data.db.entity.BudgetEntity
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.db.entity.RecordEntity

@Database(
    entities = [RecordEntity::class, CategoryEntity::class, BudgetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordDao(): RecordDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        // 默认类目
        private val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "餐饮", icon = "restaurant", isDefault = true),
            CategoryEntity(name = "交通", icon = "directions_car", isDefault = true),
            CategoryEntity(name = "购物", icon = "shopping_bag", isDefault = true),
            CategoryEntity(name = "娱乐", icon = "sports_esports", isDefault = true),
            CategoryEntity(name = "医疗", icon = "local_hospital", isDefault = true),
            CategoryEntity(name = "其他", icon = "more_horiz", isDefault = true)
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun create(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tickmate.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // 通过原始 SQL 插入默认类目，避免递归获取数据库实例
                        DEFAULT_CATEGORIES.forEach { category ->
                            db.execSQL(
                                "INSERT OR IGNORE INTO categories (name, icon, isDefault) VALUES (?, ?, ?)",
                                arrayOf(category.name, category.icon, if (category.isDefault) 1 else 0)
                            )
                        }
                    }
                })
                .build()
        }
    }
}
