package com.tickmate.app.data.repository

import com.tickmate.app.data.db.dao.BudgetDao
import com.tickmate.app.data.db.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val budgetDao: BudgetDao) {

    fun getBudget(yearMonth: String): Flow<BudgetEntity?> = budgetDao.getBudget(yearMonth)

    suspend fun getBudgetSync(yearMonth: String): BudgetEntity? = budgetDao.getBudgetSync(yearMonth)

    suspend fun saveBudget(budget: BudgetEntity) = budgetDao.insertOrUpdateBudget(budget)
}
