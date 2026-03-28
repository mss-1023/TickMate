package com.tickmate.app.service

import com.tickmate.app.data.db.dao.CategoryDao
import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CategoryMatcherTest {

    private lateinit var matcher: CategoryMatcher

    private val fakeDao = object : CategoryDao {
        private val categories = listOf(
            CategoryEntity(id = 1, name = "餐饮", icon = "restaurant", isDefault = true),
            CategoryEntity(id = 2, name = "交通", icon = "directions_car", isDefault = true),
            CategoryEntity(id = 3, name = "购物", icon = "shopping_bag", isDefault = true),
            CategoryEntity(id = 4, name = "娱乐", icon = "sports_esports", isDefault = true),
            CategoryEntity(id = 5, name = "医疗", icon = "local_hospital", isDefault = true),
            CategoryEntity(id = 6, name = "其他", icon = "more_horiz", isDefault = true)
        )
        override fun getAllCategories() = flowOf(categories)
        override suspend fun getCategoryById(id: Long) = categories.find { it.id == id }
        override suspend fun getCategoryByName(name: String) = categories.find { it.name == name }
        override suspend fun insertCategory(category: CategoryEntity) = 0L
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategory(category: CategoryEntity) {}
        override suspend fun getCategoryCount() = categories.size
    }

    @Before
    fun setup() {
        matcher = CategoryMatcher(CategoryRepository(fakeDao))
    }

    // ====== 商户名匹配（兜底） ======

    @Test
    fun `星巴克应匹配餐饮`() {
        assertEquals("餐饮", matcher.matchCategory("星巴克咖啡"))
    }

    @Test
    fun `麦当劳应匹配餐饮`() {
        assertEquals("餐饮", matcher.matchCategory("麦当劳(朝阳店)"))
    }

    @Test
    fun `滴滴应匹配交通`() {
        assertEquals("交通", matcher.matchCategory("滴滴出行"))
    }

    @Test
    fun `超市应匹配购物`() {
        assertEquals("购物", matcher.matchCategory("永辉超市"))
    }

    @Test
    fun `电影应匹配娱乐`() {
        assertEquals("娱乐", matcher.matchCategory("万达电影"))
    }

    @Test
    fun `医院应匹配医疗`() {
        assertEquals("医疗", matcher.matchCategory("北京协和医院"))
    }

    @Test
    fun `无法匹配的商户应返回其他`() {
        assertEquals("其他", matcher.matchCategory("某某科技公司"))
    }

    @Test
    fun `空字符串应返回其他`() {
        assertEquals("其他", matcher.matchCategory(""))
    }

    // ====== 发票类目字段识别（最高优先级） ======

    @Test
    fun `发票含餐饮服务类目应匹配餐饮`() {
        val rawText = "发票代码: 123456\n类目：餐饮服务\n金额：45.00"
        assertEquals("餐饮", matcher.matchCategory("某某科技公司", rawText))
    }

    @Test
    fun `发票星号标注类目应识别`() {
        // 发票上常见的 *餐饮服务* 格式
        val rawText = "商品名称 *餐饮服务*咖啡\n金额 30.00"
        assertEquals("餐饮", matcher.matchCategory("某某公司", rawText))
    }

    @Test
    fun `发票含交通运输类目应匹配交通`() {
        val rawText = "商品类别: 交通运输\n滴滴出行\n金额 25.00"
        assertEquals("交通", matcher.matchCategory("未知商户", rawText))
    }

    @Test
    fun `发票含医疗卫生类目应匹配医疗`() {
        val rawText = "开票类目：医疗卫生\n门诊费用\n合计 200.00"
        assertEquals("医疗", matcher.matchCategory("某某科技有限公司", rawText))
    }

    // ====== 商品明细推断类目 ======

    @Test
    fun `商品含食物应匹配餐饮`() {
        val items = listOf("拿铁咖啡", "蛋糕")
        assertEquals("餐饮", matcher.matchCategory("某某公司", "", items))
    }

    @Test
    fun `商品含数码产品应匹配购物`() {
        val items = listOf("手机壳", "充电器", "耳机")
        assertEquals("购物", matcher.matchCategory("某某公司", "", items))
    }

    @Test
    fun `商品含药品应匹配医疗`() {
        val items = listOf("感冒药", "维生素C")
        assertEquals("医疗", matcher.matchCategory("某某公司", "", items))
    }

    // ====== 全文内容分析 ======

    @Test
    fun `全文含多个餐饮关键词应匹配餐饮`() {
        val rawText = "牛肉面 15.00\n饮料 5.00\n小吃 10.00\n合计 30.00"
        assertEquals("餐饮", matcher.matchCategory("某某公司", rawText))
    }

    // ====== 优先级测试：发票类目 > 商品 > 商户名 ======

    @Test
    fun `发票类目优先于商户名`() {
        // 商户名像购物，但发票类目明确是餐饮
        val rawText = "类别：餐饮服务\n拿铁 30.00"
        assertEquals("餐饮", matcher.matchCategory("永辉超市", rawText))
    }

    @Test
    fun `商品内容优先于商户名`() {
        // 商户是科技公司（默认其他），但商品是咖啡
        val items = listOf("拿铁咖啡", "蛋糕面包")
        assertEquals("餐饮", matcher.matchCategory("某某科技公司", "", items))
    }

    // ====== Entity 匹配 ======

    @Test
    fun `matchCategoryEntity 应返回正确的 Entity`() = runBlocking {
        val entity = matcher.matchCategoryEntity("星巴克")
        assertNotNull(entity)
        assertEquals("餐饮", entity?.name)
        assertEquals(1L, entity?.id)
    }

    @Test
    fun `matchCategoryEntity 带全文应返回正确 Entity`() = runBlocking {
        val rawText = "类目：医疗卫生\n挂号费 50.00"
        val entity = matcher.matchCategoryEntity("某某科技有限公司", rawText)
        assertNotNull(entity)
        assertEquals("医疗", entity?.name)
    }

    @Test
    fun `matchCategoryEntity 无法匹配时应返回其他`() = runBlocking {
        val entity = matcher.matchCategoryEntity("未知商户")
        assertNotNull(entity)
        assertEquals("其他", entity?.name)
    }
}
