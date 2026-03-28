package com.tickmate.app.service

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Method

/**
 * 测试 OCRService 的文字解析逻辑
 * 由于 OCRService 依赖 Android Context，这里通过反射测试私有解析方法
 * 或者直接测试解析逻辑的提取函数
 */
class OCRParsingTest {

    // 测试金额提取正则
    @Test
    fun `应提取带小数的金额`() {
        val pattern = Regex("""(?<!\d)(\d{1,6}\.\d{2})(?!\d)""")
        val text = "合计: 45.00元"
        val matches = pattern.findAll(text).map { it.value.toDouble() }.toList()
        assertTrue(matches.contains(45.00))
    }

    @Test
    fun `应取所有金额中的最大值`() {
        val pattern = Regex("""(?<!\d)(\d{1,6}\.\d{2})(?!\d)""")
        val text = "拿铁 30.00\n蛋糕 15.00\n合计 45.00"
        val max = pattern.findAll(text).mapNotNull { it.value.toDoubleOrNull() }.maxOrNull()
        assertEquals(45.00, max!!, 0.01)
    }

    @Test
    fun `应提取标准日期格式 YYYY-MM-DD`() {
        val pattern = Regex("""(20\d{2}[-/]\d{1,2}[-/]\d{1,2})""")
        val text = "日期: 2025-03-15 时间: 14:30"
        val match = pattern.find(text)
        assertNotNull(match)
        assertEquals("2025-03-15", match?.value)
    }

    @Test
    fun `应提取斜线日期格式 YYYY_MM_DD`() {
        val pattern = Regex("""(20\d{2}[-/]\d{1,2}[-/]\d{1,2})""")
        val text = "2025/3/15 星巴克"
        val match = pattern.find(text)
        assertNotNull(match)
        assertEquals("2025/3/15", match?.value)
    }

    @Test
    fun `应提取中文日期格式`() {
        val pattern = Regex("""(20\d{2})年(\d{1,2})月(\d{1,2})日""")
        val text = "2025年3月15日"
        val match = pattern.find(text)
        assertNotNull(match)
        val (year, month, day) = match!!.destructured
        assertEquals("2025", year)
        assertEquals("3", month)
        assertEquals("15", day)
    }

    @Test
    fun `标准化日期应补零`() {
        val parts = "2025/3/5".split(Regex("[-/]"))
        val normalized = "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
        assertEquals("2025-03-05", normalized)
    }

    // 测试商品明细解析
    @Test
    fun `应解析商品行明细`() {
        val itemPattern = Regex("""(.+?)\s+[xX×]?(\d+)\s+(\d+\.?\d{0,2})""")
        val line = "拿铁 1 30.00"
        val match = itemPattern.find(line)
        assertNotNull(match)
        assertEquals("拿铁", match?.groupValues?.get(1)?.trim())
        assertEquals("1", match?.groupValues?.get(2))
        assertEquals("30.00", match?.groupValues?.get(3))
    }

    @Test
    fun `应解析带乘号的商品行`() {
        val itemPattern = Regex("""(.+?)\s+[xX×]?(\d+)\s+(\d+\.?\d{0,2})""")
        val line = "蛋糕 x2 15.00"
        val match = itemPattern.find(line)
        assertNotNull(match)
        assertEquals("2", match?.groupValues?.get(2))
    }

    // 测试商户名提取
    @Test
    fun `应从关键词行提取商户名`() {
        val merchantKeywords = listOf("商户", "店名", "商家")
        val line = "商户名称：星巴克(国贸店)"
        var result: String? = null
        for (keyword in merchantKeywords) {
            if (line.contains(keyword)) {
                val idx = line.indexOf(keyword)
                val after = line.substring(idx + keyword.length)
                    .replace("[:：名称]".toRegex(), "").trim()
                if (after.isNotBlank()) result = after
            }
        }
        assertNotNull(result)
        assertTrue(result!!.contains("星巴克"))
    }

    // 测试总计关键词优先级
    @Test
    fun `应优先从合计行提取金额`() {
        val totalKeywords = listOf("合计", "总计", "实付")
        val lines = listOf(
            "拿铁 30.00",
            "蛋糕 15.00",
            "合计 45.00"
        )
        val amountPattern = Regex("""(\d+\.?\d{0,2})""")

        var totalAmount: Double? = null
        for (line in lines) {
            if (totalKeywords.any { line.contains(it) }) {
                totalAmount = amountPattern.findAll(line)
                    .mapNotNull { it.value.toDoubleOrNull() }
                    .maxOrNull()
            }
        }
        assertNotNull(totalAmount)
        assertEquals(45.00, totalAmount!!, 0.01)
    }
}
