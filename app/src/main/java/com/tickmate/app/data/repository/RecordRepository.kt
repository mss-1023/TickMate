package com.tickmate.app.data.repository

import com.tickmate.app.data.db.dao.CategoryTotal
import com.tickmate.app.data.db.dao.MonthlyTotal
import com.tickmate.app.data.db.dao.RecordDao
import com.tickmate.app.data.db.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

class RecordRepository(private val recordDao: RecordDao) {

    fun getAllRecords(): Flow<List<RecordEntity>> = recordDao.getAllRecords()

    fun getRecordsByMonth(yearMonth: String): Flow<List<RecordEntity>> =
        recordDao.getRecordsByMonth(yearMonth)

    fun getRecordsByMonthAndCategory(yearMonth: String, categoryId: Long): Flow<List<RecordEntity>> =
        recordDao.getRecordsByMonthAndCategory(yearMonth, categoryId)

    fun getRecordsByCategory(categoryId: Long): Flow<List<RecordEntity>> =
        recordDao.getRecordsByCategory(categoryId)

    fun getMonthlyTotals(): Flow<List<MonthlyTotal>> = recordDao.getMonthlyTotals()

    fun getCategoryTotals(yearMonth: String): Flow<List<CategoryTotal>> =
        recordDao.getCategoryTotals(yearMonth)

    fun getMonthTotal(yearMonth: String): Flow<Double> = recordDao.getMonthTotal(yearMonth)

    fun getRecentRecords(limit: Int = 5): Flow<List<RecordEntity>> =
        recordDao.getRecentRecords(limit)

    suspend fun getRecordById(id: Long): RecordEntity? = recordDao.getRecordById(id)

    suspend fun insertRecord(record: RecordEntity): Long = recordDao.insertRecord(record)

    suspend fun updateRecord(record: RecordEntity) = recordDao.updateRecord(record)

    suspend fun deleteRecord(record: RecordEntity) = recordDao.deleteRecord(record)

    suspend fun updateRecordsCategory(oldCategoryId: Long, newCategoryId: Long) =
        recordDao.updateRecordsCategory(oldCategoryId, newCategoryId)

    fun searchRecords(query: String): Flow<List<RecordEntity>> = recordDao.searchRecords(query)

    fun getMonthRecordCount(yearMonth: String): Flow<Int> = recordDao.getMonthRecordCount(yearMonth)

    fun getMonthMaxAmount(yearMonth: String): Flow<Double?> = recordDao.getMonthMaxAmount(yearMonth)

    fun getMonthActiveDays(yearMonth: String): Flow<Int> = recordDao.getMonthActiveDays(yearMonth)
}
