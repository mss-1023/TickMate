package com.tickmate.app.model

import org.junit.Assert.*
import org.junit.Test

/**
 * 测试预算计算逻辑
 */
class BudgetCalculationTest {

    // 模拟 HomeViewModel 中的预算百分比计算
    private fun calculateBudgetPercent(monthTotal: Double, monthlyBudget: Double): Int {
        return if (monthlyBudget > 0) {
            (monthTotal / monthlyBudget * 100).toInt()
        } else 0
    }

    private fun shouldShowWarning(percent: Int, threshold: Int): Boolean {
        return percent >= threshold
    }

    @Test
    fun `消费800预算1000应为80%`() {
        assertEquals(80, calculateBudgetPercent(800.0, 1000.0))
    }

    @Test
    fun `消费500预算1000应为50%`() {
        assertEquals(50, calculateBudgetPercent(500.0, 1000.0))
    }

    @Test
    fun `消费1200预算1000应为120%`() {
        assertEquals(120, calculateBudgetPercent(1200.0, 1000.0))
    }

    @Test
    fun `消费0预算1000应为0%`() {
        assertEquals(0, calculateBudgetPercent(0.0, 1000.0))
    }

    @Test
    fun `预算0应返回0%`() {
        assertEquals(0, calculateBudgetPercent(500.0, 0.0))
    }

    @Test
    fun `预算负数应返回0%`() {
        assertEquals(0, calculateBudgetPercent(500.0, -100.0))
    }

    @Test
    fun `80%达到80%阈值应触发警告`() {
        assertTrue(shouldShowWarning(80, 80))
    }

    @Test
    fun `79%未达80%阈值不应触发`() {
        assertFalse(shouldShowWarning(79, 80))
    }

    @Test
    fun `100%应触发任何阈值`() {
        assertTrue(shouldShowWarning(100, 80))
        assertTrue(shouldShowWarning(100, 90))
        assertTrue(shouldShowWarning(100, 100))
    }

    @Test
    fun `超支120%应触发`() {
        assertTrue(shouldShowWarning(120, 80))
    }

    // 测试 HomeState 创建
    @Test
    fun `HomeState 默认值应正确`() {
        val state = HomeState()
        assertTrue(state.isLoading)
        assertEquals(0.0, state.monthTotal, 0.01)
        assertNull(state.budget)
        assertEquals(0, state.budgetPercent)
        assertTrue(state.recentRecords.isEmpty())
        assertFalse(state.showBudgetWarning)
        assertNull(state.error)
    }

    // 测试金额格式化
    @Test
    fun `金额应保留两位小数`() {
        val amount = 1234.5
        assertEquals("1234.50", "%.2f".format(amount))
    }

    @Test
    fun `大金额应正确格式化`() {
        val amount = 99999.99
        assertEquals("99999.99", "%.2f".format(amount))
    }

    @Test
    fun `零金额应显示0_00`() {
        assertEquals("0.00", "%.2f".format(0.0))
    }
}
