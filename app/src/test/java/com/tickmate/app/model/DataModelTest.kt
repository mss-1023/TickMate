package com.tickmate.app.model

import com.tickmate.app.data.db.entity.BudgetEntity
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.db.entity.RecordEntity
import org.junit.Assert.*
import org.junit.Test

/**
 * 测试数据模型的创建和属性
 */
class DataModelTest {

    @Test
    fun `RecordEntity 默认值应正确`() {
        val record = RecordEntity(
            merchant = "测试商户",
            amount = 100.0,
            date = "2025-03-15",
            categoryId = 1
        )
        assertEquals(0L, record.id)
        assertEquals("测试商户", record.merchant)
        assertEquals(100.0, record.amount, 0.01)
        assertEquals("2025-03-15", record.date)
        assertEquals(1L, record.categoryId)
        assertNull(record.note)
        assertNull(record.imageUri)
        assertTrue(record.createdAt > 0)
    }

    @Test
    fun `CategoryEntity 默认值应正确`() {
        val category = CategoryEntity(name = "餐饮")
        assertEquals(0L, category.id)
        assertEquals("餐饮", category.name)
        assertNull(category.icon)
        assertFalse(category.isDefault)
    }

    @Test
    fun `CategoryEntity 内置类目应标记 isDefault`() {
        val category = CategoryEntity(name = "餐饮", isDefault = true)
        assertTrue(category.isDefault)
    }

    @Test
    fun `BudgetEntity 固定ID为1`() {
        val budget = BudgetEntity(monthlyBudget = 5000.0, thresholdPercent = 80, yearMonth = "2025-03")
        assertEquals(1L, budget.id)
        assertEquals(5000.0, budget.monthlyBudget, 0.01)
        assertEquals(80, budget.thresholdPercent)
        assertEquals("2025-03", budget.yearMonth)
    }

    @Test
    fun `BudgetEntity 默认阈值为80%`() {
        val budget = BudgetEntity(yearMonth = "2025-03")
        assertEquals(80, budget.thresholdPercent)
        assertEquals(0.0, budget.monthlyBudget, 0.01)
    }

    // MVI Contract 测试
    @Test
    fun `RecordListState 默认值应正确`() {
        val state = RecordListState()
        assertTrue(state.isLoading)
        assertTrue(state.records.isEmpty())
        assertTrue(state.categories.isEmpty())
        assertNull(state.selectedMonth)
        assertNull(state.selectedCategoryId)
        assertNull(state.error)
    }

    @Test
    fun `StatsState 默认值应正确`() {
        val state = StatsState()
        assertTrue(state.isLoading)
        assertTrue(state.monthlyData.isEmpty())
        assertTrue(state.categoryData.isEmpty())
        assertEquals("", state.currentMonth)
    }

    @Test
    fun `MonthAmount 数据应正确`() {
        val data = MonthAmount(month = "2025-03", amount = 1500.0)
        assertEquals("2025-03", data.month)
        assertEquals(1500.0, data.amount, 0.01)
    }

    @Test
    fun `CategoryAmount 百分比应正确`() {
        val category = CategoryEntity(id = 1, name = "餐饮")
        val data = CategoryAmount(category = category, amount = 300.0, percent = 30.0f)
        assertEquals("餐饮", data.category.name)
        assertEquals(300.0, data.amount, 0.01)
        assertEquals(30.0f, data.percent, 0.01f)
    }

    // 日期格式验证
    @Test
    fun `日期格式应为 YYYY-MM-DD`() {
        val datePattern = Regex("""\d{4}-\d{2}-\d{2}""")
        assertTrue(datePattern.matches("2025-03-15"))
        assertFalse(datePattern.matches("2025/03/15"))
        assertFalse(datePattern.matches("20250315"))
    }

    @Test
    fun `yearMonth 格式应为 YYYY-MM`() {
        val pattern = Regex("""\d{4}-\d{2}""")
        assertTrue(pattern.matches("2025-03"))
        assertFalse(pattern.matches("2025-3"))
    }
}
