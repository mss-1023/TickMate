package com.tickmate.app.data.db.dao

import androidx.room.*
import com.tickmate.app.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget WHERE yearMonth = :yearMonth LIMIT 1")
    fun getBudget(yearMonth: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE yearMonth = :yearMonth LIMIT 1")
    suspend fun getBudgetSync(yearMonth: String): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)
}
