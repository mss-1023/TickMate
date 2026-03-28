package com.tickmate.app.service

import com.tickmate.app.data.db.entity.CategoryEntity
import com.tickmate.app.data.repository.CategoryRepository

/**
 * 智能类目匹配器
 * 优先级：发票明确类目字段 > 商品内容分析 > 商户名匹配 > 默认"其他"
 */
class CategoryMatcher(private val categoryRepository: CategoryRepository) {

    // 关键词→类目名映射（扩充）
    private val keywordMap = mapOf(
        "餐饮" to listOf(
            "餐", "饭", "食", "星巴克", "麦当劳", "肯德基", "火锅", "烧烤",
            "面馆", "奶茶", "咖啡", "蛋糕", "甜品", "外卖", "食堂", "小吃",
            "酒楼", "饮品", "瑞幸", "海底捞", "必胜客", "汉堡", "寿司", "料理",
            "牛肉", "鸡肉", "猪肉", "鱼", "虾", "蔬菜", "水果", "面包",
            "牛奶", "饮料", "啤酒", "白酒", "红酒", "茶叶",
            "零食", "糖果", "巧克力", "方便面", "速食",
            "美团", "饿了么", "大众点评"
        ),
        "交通" to listOf(
            "滴滴", "地铁", "公交", "出租", "加油", "停车", "高铁", "火车",
            "飞机", "机票", "打车", "共享单车", "哈啰", "美团打车", "曹操",
            "汽油", "柴油", "过路费", "高速", "ETC", "车险", "保养", "维修",
            "轮胎", "机油", "中石化", "中石油", "壳牌",
            "航空", "铁路", "客运"
        ),
        "购物" to listOf(
            "超市", "商场", "淘宝", "京东", "拼多多", "天猫", "沃尔玛",
            "家乐福", "便利店", "711", "全家", "百货", "罗森", "便利蜂",
            "服装", "衣服", "裤子", "鞋", "包", "手表", "首饰", "化妆品",
            "洗发水", "沐浴露", "牙膏", "纸巾", "清洁", "日用品",
            "家具", "家电", "电器", "手机", "电脑", "平板", "耳机",
            "数码", "键盘", "鼠标", "显示器", "充电器",
            "优衣库", "ZARA", "H&M", "MUJI", "无印良品",
            "屈臣氏", "丝芙兰", "大润发", "华润万家", "物美", "盒马", "永辉"
        ),
        "娱乐" to listOf(
            "电影", "KTV", "游戏", "网吧", "健身", "游泳", "演出", "门票",
            "旅游", "景点", "酒店", "民宿", "影城", "万达影城",
            "演唱会", "话剧", "展览", "博物馆", "动物园", "游乐园",
            "会员", "视频会员", "音乐", "书", "书店"
        ),
        "医疗" to listOf(
            "医院", "药店", "诊所", "药房", "体检", "门诊", "挂号",
            "药品", "处方", "感冒药", "消炎", "维生素", "口罩",
            "牙科", "眼科", "皮肤科", "中医", "西药", "中药"
        )
    )

    // 发票上的类目/行业字段关键词 → 类目名
    private val invoiceCategoryMap = mapOf(
        // 直接写明类目的
        "餐饮" to "餐饮", "餐饮服务" to "餐饮", "食品" to "餐饮",
        "catering" to "餐饮", "food" to "餐饮",
        "交通运输" to "交通", "交通" to "交通", "运输服务" to "交通",
        "客运" to "交通", "货运" to "交通",
        "transport" to "交通",
        "零售" to "购物", "商品" to "购物", "百货" to "购物",
        "日用品" to "购物", "电子产品" to "购物", "服装" to "购物",
        "retail" to "购物", "shopping" to "购物",
        "住宿" to "娱乐", "旅游" to "娱乐", "文化" to "娱乐",
        "娱乐" to "娱乐", "休闲" to "娱乐",
        "entertainment" to "娱乐",
        "医疗" to "医疗", "医药" to "医疗", "卫生" to "医疗",
        "药品" to "医疗", "保健" to "医疗",
        "medical" to "医疗", "health" to "医疗",
        // 发票上常见的行业分类
        "信息技术" to "购物", "技术服务" to "购物", "软件" to "购物",
        "通信" to "购物", "教育" to "其他", "培训" to "其他"
    )

    /**
     * 综合匹配类目
     * @param merchantName 商户名
     * @param rawText OCR 识别的全文（用于发票类目字段和商品内容分析）
     * @param itemNames 商品明细名称列表
     */
    fun matchCategory(
        merchantName: String,
        rawText: String = "",
        itemNames: List<String> = emptyList()
    ): String {
        // 优先级1：从发票全文中提取明确的类目字段
        if (rawText.isNotBlank()) {
            val categoryFromInvoice = extractCategoryFromInvoice(rawText)
            if (categoryFromInvoice != null) return categoryFromInvoice
        }

        // 优先级2：分析商品明细名称推断类目
        if (itemNames.isNotEmpty()) {
            val categoryFromItems = matchFromItemNames(itemNames)
            if (categoryFromItems != "其他") return categoryFromItems
        }

        // 优先级3：从全文内容中分析关键词频率
        if (rawText.isNotBlank()) {
            val categoryFromContent = matchFromFullText(rawText)
            if (categoryFromContent != "其他") return categoryFromContent
        }

        // 优先级4：根据商户名匹配
        return matchFromMerchant(merchantName)
    }

    // 从发票中提取明确的类目/行业字段
    private fun extractCategoryFromInvoice(rawText: String): String? {
        val lines = rawText.split("\n").map { it.trim() }

        // 查找含有类目标签的行
        val categoryLabels = listOf(
            "类目", "类别", "行业", "服务类型", "商品类别",
            "消费类型", "业务类型", "项目类别", "开票类目"
        )
        for (line in lines) {
            for (label in categoryLabels) {
                if (line.contains(label)) {
                    // 取标签后面的值
                    val idx = line.indexOf(label) + label.length
                    val value = line.substring(idx)
                        .trimStart(':', '：', ' ', '\t')
                        .trim()
                    if (value.isNotBlank()) {
                        // 用 invoiceCategoryMap 匹配
                        for ((key, category) in invoiceCategoryMap) {
                            if (value.contains(key, ignoreCase = true)) {
                                return category
                            }
                        }
                    }
                }
            }
        }

        // 查找发票上的行业/税率描述（如 "*餐饮服务*" / "*信息技术服务*"）
        val starPattern = Regex("""\*([^*]+)\*""")
        for (line in lines) {
            starPattern.findAll(line).forEach { match ->
                val content = match.groupValues[1]
                for ((key, category) in invoiceCategoryMap) {
                    if (content.contains(key, ignoreCase = true)) {
                        return category
                    }
                }
            }
        }

        return null
    }

    // 从商品明细名称推断类目（统计各类目命中次数，取最多的）
    private fun matchFromItemNames(itemNames: List<String>): String {
        val hits = mutableMapOf<String, Int>()
        val combinedText = itemNames.joinToString(" ")

        for ((category, keywords) in keywordMap) {
            val count = keywords.count { combinedText.contains(it, ignoreCase = true) }
            if (count > 0) hits[category] = count
        }

        return hits.maxByOrNull { it.value }?.key ?: "其他"
    }

    // 从全文内容分析关键词频率
    private fun matchFromFullText(rawText: String): String {
        val hits = mutableMapOf<String, Int>()

        for ((category, keywords) in keywordMap) {
            val count = keywords.count { rawText.contains(it, ignoreCase = true) }
            if (count > 0) hits[category] = count
        }

        // 命中 1 个关键词即可
        return hits.maxByOrNull { it.value }?.key ?: "其他"
    }

    // 仅根据商户名匹配（兜底）
    private fun matchFromMerchant(merchantName: String): String {
        val lowerName = merchantName.lowercase()
        for ((category, keywords) in keywordMap) {
            if (keywords.any { lowerName.contains(it) }) {
                return category
            }
        }
        return "其他"
    }

    /**
     * 综合匹配并返回 CategoryEntity
     */
    suspend fun matchCategoryEntity(
        merchantName: String,
        rawText: String = "",
        itemNames: List<String> = emptyList()
    ): CategoryEntity? {
        val categoryName = matchCategory(merchantName, rawText, itemNames)
        return categoryRepository.getCategoryByName(categoryName)
    }
}
