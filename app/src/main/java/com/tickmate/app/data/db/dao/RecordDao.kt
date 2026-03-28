package com.tickmate.app.data.db.dao

import androidx.room.*
import com.tickmate.app.data.db.entity.RecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records ORDER BY date DESC, createdAt DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE date LIKE :yearMonth || '%' ORDER BY date DESC, createdAt DESC")
    fun getRecordsByMonth(yearMonth: String): Flow<List<RecordEntity>>

    @Query(
        "SELECT * FROM records WHERE date LIKE :yearMonth || '%' AND categoryId = :categoryId " +
        "ORDER BY date DESC, createdAt DESC"
    )
    fun getRecordsByMonthAndCategory(yearMonth: String, categoryId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getRecordsByCategory(categoryId: Long): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getRecordById(id: Long): RecordEntity?

    // 月度汇总（折线图）
    @Query(
        "SELECT substr(date, 1, 7) as month, SUM(amount) as total " +
        "FROM records GROUP BY substr(date, 1, 7) ORDER BY month DESC LIMIT 12"
    )
    fun getMonthlyTotals(): Flow<List<MonthlyTotal>>

    // 当月类目汇总（饼图）
    @Query(
        "SELECT categoryId, SUM(amount) as total " +
        "FROM records WHERE date LIKE :yearMonth || '%' GROUP BY categoryId"
    )
    fun getCategoryTotals(yearMonth: String): Flow<List<CategoryTotal>>

    // 当月总支出
    @Query("SELECT COALESCE(SUM(amount), 0) FROM records WHERE date LIKE :yearMonth || '%'")
    fun getMonthTotal(yearMonth: String): Flow<Double>

    // 最近N条记录
    @Query("SELECT * FROM records ORDER BY date DESC, createdAt DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Delete
    suspend fun deleteRecord(record: RecordEntity)

    @Query("UPDATE records SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun updateRecordsCategory(oldCategoryId: Long, newCategoryId: Long)

    // 搜索记录
    @Query(
        "SELECT * FROM records WHERE merchant LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' " +
        "ORDER BY date DESC LIMIT 50"
    )
    fun searchRecords(query: String): Flow<List<RecordEntity>>

    // 当月记录数
    @Query("SELECT COUNT(*) FROM records WHERE date LIKE :yearMonth || '%'")
    fun getMonthRecordCount(yearMonth: String): Flow<Int>

    // 当月最大单笔
    @Query("SELECT MAX(amount) FROM records WHERE date LIKE :yearMonth || '%'")
    fun getMonthMaxAmount(yearMonth: String): Flow<Double?>

    // 当月记录天数
    @Query("SELECT COUNT(DISTINCT date) FROM records WHERE date LIKE :yearMonth || '%'")
    fun getMonthActiveDays(yearMonth: String): Flow<Int>
}

data class MonthlyTotal(
    val month: String,
    val total: Double
)

data class CategoryTotal(
    val categoryId: Long,
    val total: Double
)
