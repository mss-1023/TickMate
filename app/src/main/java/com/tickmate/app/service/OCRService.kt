package com.tickmate.app.service

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import android.content.Context
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// OCR 识别结果
data class OCRResult(
    val merchant: String,
    val amount: Double?,
    val date: String?,
    val rawText: String,
    val itemDetails: List<ItemDetail> = emptyList()
)

// 商品明细行（P3）
data class ItemDetail(
    val name: String,
    val price: Double,
    val quantity: Int = 1
)

class OCRService(private val context: Context) {

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    // 从图片 URI 识别文字并解析票据信息
    suspend fun recognizeReceipt(imageUri: Uri): OCRResult = suspendCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val rawText = visionText.text
                    val result = parseReceiptText(rawText)
                    cont.resume(result)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }

    // 解析识别出的文字，提取商户名、金额、日期
    private fun parseReceiptText(text: String): OCRResult {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

        val merchant = extractMerchant(lines)
        val amount = extractAmount(lines)
        val date = extractDate(text)
        val itemDetails = extractItemDetails(lines)

        return OCRResult(
            merchant = merchant,
            amount = amount,
            date = date,
            rawText = text,
            itemDetails = itemDetails
        )
    }

    // 提取商户名：多种策略综合判断
    private fun extractMerchant(lines: List<String>): String {
        if (lines.isEmpty()) return "未知商户"

        // 购买方关键词（需要排除）
        val buyerKeywords = listOf(
            "购买方名称", "购买方", "购方名称", "购方", "买方", "客户名称", "客户"
        )

        // 销售方关键词（最高优先级）
        val sellerKeywords = listOf(
            "销售方名称", "销售方", "销方名称", "销方", "卖方", "卖方名称",
            "收款方名称", "收款方", "开票公司", "开票单位", "收款单位"
        )

        // 一般商户标签
        val generalLabels = listOf(
            "商户全称", "商户名称", "商户名", "商家名称", "商家",
            "店铺名称", "门店名称", "门店", "公司名称", "店名"
        )

        val allLabels = sellerKeywords + buyerKeywords + generalLabels + listOf("名称")

        fun stripLabel(line: String): String {
            var text = line.trim()
            for (label in allLabels) {
                if (text.contains(label)) {
                    text = text.substring(text.indexOf(label) + label.length)
                        .trimStart(':', '：', '=', ' ', '\t')
                    break
                }
            }
            return text.trim()
        }

        fun isBuyerLine(line: String): Boolean {
            return buyerKeywords.any { line.contains(it) }
        }

        // 策略1（最高优先级）：查找销售方
        for (line in lines) {
            if (sellerKeywords.any { line.contains(it) }) {
                val value = stripLabel(line)
                if (value.isNotBlank() && value.length >= 2) return value.take(30)
            }
        }

        // 策略1.5：一般商户标签（排除购买方）
        for (line in lines) {
            if (isBuyerLine(line)) continue
            val lowerLine = line.lowercase()
            for (keyword in generalLabels + listOf("seller", "merchant")) {
                if (lowerLine.contains(keyword.lowercase())) {
                    val value = stripLabel(line)
                    if (value.isNotBlank() && value.length >= 2) return value.take(30)
                }
            }
        }

        // 策略2：识别含公司/商铺后缀的行，去掉标签前缀
        val merchantSuffixes = listOf(
            "有限公司", "股份有限公司", "有限责任公司", "集团",
            "超市", "商场", "百货", "便利店", "专卖店",
            "餐厅", "餐饮", "饭店", "酒楼", "食堂", "面馆", "烧烤",
            "咖啡", "奶茶", "茶饮", "甜品",
            "药店", "药房", "医院", "诊所",
            "酒店", "宾馆", "旅馆", "民宿",
            "电影院", "影城", "KTV",
            "加油站", "充电站",
            "Store", "Shop", "Mall"
        )
        for (line in lines) {
            if (merchantSuffixes.any { line.contains(it, ignoreCase = true) }) {
                var cleaned = stripLabel(line)
                // 去掉电话号码、地址等干扰
                cleaned = cleaned
                    .replace(Regex("""\d{7,}"""), "")
                    .replace(Regex("""(电话|tel|TEL|手机|传真|地址|addr).*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                if (cleaned.length >= 2) return cleaned.take(30)
            }
        }

        // 策略3：识别知名品牌名
        val knownBrands = listOf(
            "星巴克", "Starbucks", "麦当劳", "McDonald", "肯德基", "KFC",
            "瑞幸", "Luckin", "喜茶", "奈雪", "蜜雪冰城",
            "海底捞", "西贝", "必胜客", "Pizza Hut", "汉堡王", "Burger King",
            "沃尔玛", "Walmart", "家乐福", "Carrefour", "永辉", "盒马",
            "711", "7-Eleven", "全家", "罗森", "便利蜂",
            "滴滴", "美团", "饿了么", "淘宝", "京东", "拼多多",
            "中石化", "中石油", "壳牌",
            "万达", "大润发", "华润万家", "物美",
            "屈臣氏", "丝芙兰", "优衣库", "ZARA", "H&M", "MUJI", "无印良品"
        )
        val fullText = lines.joinToString(" ")
        for (brand in knownBrands) {
            if (fullText.contains(brand, ignoreCase = true)) {
                return brand
            }
        }

        // 策略4：取前8行中第一行有意义的文字（过滤时间/数字/金额等）
        val skipPatterns = listOf(
            Regex("""^\d{1,2}:\d{2}"""),            // 时间 HH:MM
            Regex("""^\d{4}[-/.]"""),                // 日期开头
            Regex("""^[¥￥$]\s*\d"""),              // 金额
            Regex("""^\d+\.?\d*$"""),                // 纯数字
            Regex("""^No\.|^编号|^发票号|^票号"""),  // 编号
            Regex("""^合计|^总计|^金额|^税额"""),    // 汇总行
        )
        for (line in lines.take(8)) {
            val trimmed = line.trim()
            if (trimmed.length < 2) continue
            if (skipPatterns.any { it.containsMatchIn(trimmed) }) continue
            val cleaned = trimmed
                .replace(Regex("""[\d\s\-*#=+.,:;/\\()（）【】\[\]{}]+"""), "")
                .trim()
            if (cleaned.length >= 2) {
                return cleaned.take(30)
            }
        }

        return lines.firstOrNull()?.take(20) ?: "未知商户"
    }

    // 提取金额：优先取"合计/总计/总额"附近的数字，否则取所有金额中的最大值
    private fun extractAmount(lines: List<String>): Double? {
        // 金额数字的正则：支持 ¥123.45 / 123.45 / 1,234.56 等格式
        val moneyPattern = Regex("""[¥￥$]?\s*(\d{1,3}(?:,\d{3})*\.?\d{0,2})""")

        // 从一行文字中提取所有可能的金额数字
        fun extractNumbers(line: String): List<Double> {
            return moneyPattern.findAll(line)
                .map { it.groupValues[1].replace(",", "").toDoubleOrNull() }
                .filterNotNull()
                .filter { it > 0 && it < 1000000 }
                .toList()
        }

        // 第一优先级关键词（最终金额）
        val primaryKeywords = listOf("价税合计", "实付", "实收", "应付", "合计", "总计", "总额", "总价")
        // 第二优先级关键词
        val secondaryKeywords = listOf("金额", "小计", "支付金额", "消费金额", "交易金额", "付款金额", "Total", "TOTAL", "Amount")

        // 从后往前搜索（合计通常在底部），先找第一优先级
        for (line in lines.reversed()) {
            if (primaryKeywords.any { line.contains(it) }) {
                val numbers = extractNumbers(line)
                if (numbers.isNotEmpty()) return numbers.max()

                // 关键词行没有数字，看下一行（有些发票金额在关键词的下一行）
                val idx = lines.indexOf(line)
                if (idx + 1 < lines.size) {
                    val nextNumbers = extractNumbers(lines[idx + 1])
                    if (nextNumbers.isNotEmpty()) return nextNumbers.max()
                }
            }
        }

        // 再找第二优先级
        for (line in lines.reversed()) {
            if (secondaryKeywords.any { line.contains(it, ignoreCase = true) }) {
                val numbers = extractNumbers(line)
                if (numbers.isNotEmpty()) return numbers.max()

                val idx = lines.indexOf(line)
                if (idx + 1 < lines.size) {
                    val nextNumbers = extractNumbers(lines[idx + 1])
                    if (nextNumbers.isNotEmpty()) return nextNumbers.max()
                }
            }
        }

        // 兜底：取所有带两位小数的金额中的最大值
        val allAmounts = lines.flatMap { line ->
            Regex("""(?<!\d)(\d{1,6}\.\d{2})(?!\d)""").findAll(line)
                .map { it.value.toDoubleOrNull() }
                .filterNotNull()
                .filter { it > 0 && it < 1000000 }
                .toList()
        }
        return allAmounts.maxOrNull()
    }

    // 提取日期：匹配常见日期格式
    private fun extractDate(text: String): String? {
        // YYYY-MM-DD 或 YYYY/MM/DD
        val pattern1 = Regex("""(20\d{2}[-/]\d{1,2}[-/]\d{1,2})""")
        pattern1.find(text)?.let { match ->
            return normalizeDate(match.value)
        }

        // YYYY年MM月DD日
        val pattern2 = Regex("""(20\d{2})年(\d{1,2})月(\d{1,2})日""")
        pattern2.find(text)?.let { match ->
            val (year, month, day) = match.destructured
            return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
        }

        // YYYY.MM.DD
        val pattern3 = Regex("""(20\d{2})\.(\d{1,2})\.(\d{1,2})""")
        pattern3.find(text)?.let { match ->
            val (year, month, day) = match.destructured
            return "$year-${month.padStart(2, '0')}-${day.padStart(2, '0')}"
        }

        // YYYYMMDD
        val pattern4 = Regex("""(20\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])""")
        pattern4.find(text)?.let { match ->
            val (year, month, day) = match.destructured
            return "$year-$month-$day"
        }

        return null
    }

    // 标准化日期格式为 YYYY-MM-DD
    private fun normalizeDate(dateStr: String): String {
        val parts = dateStr.split(Regex("[-/]"))
        if (parts.size == 3) {
            val year = parts[0]
            val month = parts[1].padStart(2, '0')
            val day = parts[2].padStart(2, '0')
            return "$year-$month-$day"
        }
        return dateStr
    }

    // 提取商品明细（P3 功能）
    private fun extractItemDetails(lines: List<String>): List<ItemDetail> {
        val details = mutableListOf<ItemDetail>()
        val itemPatterns = listOf(
            // 名称 x数量 单价
            Regex("""(.+?)\s+[xX×](\d+)\s+(\d+\.?\d{0,2})"""),
            // 名称 数量 单价
            Regex("""(.{2,15})\s+(\d+)\s+(\d+\.\d{2})"""),
            // 名称 单价（数量为1）
            Regex("""(.{2,15})\s+(\d+\.\d{2})$""")
        )

        for (line in lines) {
            var matched = false
            for ((idx, pattern) in itemPatterns.withIndex()) {
                if (matched) continue
                val match = pattern.find(line) ?: continue
                val name: String
                val quantity: Int
                val price: Double
                if (idx == 2) {
                    name = match.groupValues[1].trim()
                    quantity = 1
                    price = match.groupValues[2].toDoubleOrNull() ?: continue
                } else {
                    name = match.groupValues[1].trim()
                    quantity = match.groupValues[2].toIntOrNull() ?: 1
                    price = match.groupValues[3].toDoubleOrNull() ?: continue
                }
                if (name.length in 1..20 && price > 0 && price < 100000) {
                    details.add(ItemDetail(name = name, price = price, quantity = quantity))
                    matched = true
                }
            }
        }
        return details
    }
}
