package com.tickmate.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 百度云 OCR 在线识别服务
 * 免费额度：通用文字识别标准版每天 50,000 次
 */
class OnlineOCRService(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "ocr_config"
        private const val KEY_API_KEY = "baidu_api_key"
        private const val KEY_SECRET_KEY = "baidu_secret_key"
        private const val KEY_ACCESS_TOKEN = "baidu_access_token"
        private const val KEY_TOKEN_EXPIRE = "baidu_token_expire"

        private const val TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token"
        // 通用文字识别（高精度版）
        private const val OCR_ACCURATE_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic"
        // 通用文字识别（标准版）
        private const val OCR_GENERAL_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic"

        // 默认内置 API Key
        private const val DEFAULT_API_KEY = "zPZLEImUedgajxaiF1dlDaAc"
        private const val DEFAULT_SECRET_KEY = "YBW3Qqgfdp79EpIsgAxerrO34b9r38nY"

        // 图片最大尺寸（百度 API 限制 4MB base64，压缩到 1MB 以内）
        private const val MAX_IMAGE_SIZE = 1024 * 1024
        private const val MAX_IMAGE_DIMENSION = 2000
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = getApiKey().isNotBlank() && getSecretKey().isNotBlank()

    fun getApiKey(): String = prefs.getString(KEY_API_KEY, null) ?: DEFAULT_API_KEY
    fun getSecretKey(): String = prefs.getString(KEY_SECRET_KEY, null) ?: DEFAULT_SECRET_KEY
    fun isCustomConfig(): Boolean = prefs.contains(KEY_API_KEY)

    fun saveConfig(apiKey: String, secretKey: String) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_SECRET_KEY, secretKey)
            .remove(KEY_ACCESS_TOKEN)
            .apply()
    }

    // 获取 access_token（有缓存）
    private fun getAccessToken(): String {
        val cached = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        val expire = prefs.getLong(KEY_TOKEN_EXPIRE, 0)
        if (cached.isNotBlank() && System.currentTimeMillis() < expire) {
            return cached
        }

        val apiKey = getApiKey()
        val secretKey = getSecretKey()

        val url = URL("$TOKEN_URL?grant_type=client_credentials&client_id=$apiKey&client_secret=$secretKey")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        try {
            val code = conn.responseCode
            val stream = if (code == 200) conn.inputStream else conn.errorStream
            val response = stream.bufferedReader().readText()

            val json = JSONObject(response)
            if (json.has("error")) {
                throw Exception("认证失败(${json.optString("error")}): ${json.optString("error_description")}")
            }

            val token = json.getString("access_token")
            val expiresIn = json.getLong("expires_in")
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRE, System.currentTimeMillis() + expiresIn * 1000 - 60000)
                .apply()
            return token
        } finally {
            conn.disconnect()
        }
    }

    // 压缩图片到合理大小
    private fun compressImage(imageUri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("无法读取图片")

        // 先读取尺寸
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        // 计算缩放比例
        var sampleSize = 1
        val maxDim = maxOf(options.outWidth, options.outHeight)
        if (maxDim > MAX_IMAGE_DIMENSION) {
            sampleSize = maxDim / MAX_IMAGE_DIMENSION
        }

        // 解码缩放后的图片
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val stream2 = context.contentResolver.openInputStream(imageUri)
            ?: throw Exception("无法读取图片")
        val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions)
            ?: throw Exception("图片解码失败")
        stream2.close()

        // 压缩为 JPEG
        val baos = ByteArrayOutputStream()
        var quality = 90
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        while (baos.size() > MAX_IMAGE_SIZE && quality > 30) {
            baos.reset()
            quality -= 10
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        }
        bitmap.recycle()

        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    // 识别票据
    suspend fun recognizeReceipt(imageUri: Uri): OCRResult {
        val token = getAccessToken()
        val base64Image = compressImage(imageUri)

        // 先尝试高精度版，失败用标准版
        val rawText = try {
            callOCRApi(OCR_ACCURATE_URL, base64Image, token)
        } catch (e: Exception) {
            callOCRApi(OCR_GENERAL_URL, base64Image, token)
        }

        if (rawText.isBlank()) {
            throw Exception("识别结果为空，请确保图片清晰")
        }

        return parseReceiptText(rawText)
    }

    private fun callOCRApi(apiUrl: String, base64Image: String, token: String): String {
        val url = URL("$apiUrl?access_token=$token")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true
        conn.connectTimeout = 20000
        conn.readTimeout = 20000

        try {
            val body = "image=${URLEncoder.encode(base64Image, "UTF-8")}"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(body)
            writer.flush()
            writer.close()

            val code = conn.responseCode
            val stream = if (code == 200) conn.inputStream else conn.errorStream
            val response = BufferedReader(InputStreamReader(stream)).readText()

            val json = JSONObject(response)
            if (json.has("error_code")) {
                val errCode = json.optInt("error_code")
                val errMsg = json.optString("error_msg")
                throw Exception("百度OCR错误($errCode): $errMsg")
            }

            val wordsResult = json.optJSONArray("words_result")
                ?: throw Exception("返回格式异常: 没有 words_result")

            val lines = mutableListOf<String>()
            for (i in 0 until wordsResult.length()) {
                val word = wordsResult.getJSONObject(i).optString("words", "")
                if (word.isNotBlank()) lines.add(word)
            }
            return lines.joinToString("\n")
        } finally {
            conn.disconnect()
        }
    }

    private fun parseReceiptText(text: String): OCRResult {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        return OCRResult(
            merchant = extractMerchant(lines),
            amount = extractAmount(lines),
            date = extractDate(text),
            rawText = text,
            itemDetails = extractItemDetails(lines)
        )
    }

    /**
     * 提取商户名（销售方）
     *
     * 核心策略：收集所有"名称:"行，用公司后缀区分销售方和个人（购买方）
     * 发票左右两栏被 OCR 读成连续行，两个"名称:"可能紧挨着出现
     */
    private fun extractMerchant(lines: List<String>): String {
        if (lines.isEmpty()) return "未知商户"

        // 公司/商户后缀（有这些后缀的是商户，没有的一般是个人）
        val companySuffixes = listOf(
            "有限公司", "股份公司", "有限责任公司", "集团",
            "超市", "商场", "百货", "便利店", "专卖店",
            "餐厅", "饭店", "酒楼", "咖啡", "奶茶",
            "药店", "药房", "医院", "诊所",
            "酒店", "宾馆", "影城", "加油站",
            "科技", "贸易", "商贸", "实业", "服务"
        )

        fun isCompanyName(name: String): Boolean {
            return companySuffixes.any { name.contains(it) }
        }

        // 从一行中提取"名称"后面的值
        fun extractNameValue(line: String): String? {
            if (!line.contains("名称")) return null
            val idx = line.indexOf("名称")
            val after = line.substring(idx + 2).trimStart(':', '：', ' ')
            val cleaned = after.trim()
            if (cleaned.length < 2 || cleaned == "信息" || cleaned == "往息") return null
            return cleaned
        }

        // 策略1（最核心）：收集所有"名称:"行，优先选含公司后缀的
        val allNames = mutableListOf<String>()
        for (line in lines) {
            val name = extractNameValue(line)
            if (name != null) allNames.add(name)
        }
        // 优先返回含公司后缀的名称（这是商户/销售方）
        val companyName = allNames.find { isCompanyName(it) }
        if (companyName != null) return companyName.take(50)

        // 策略2：查找明确的销售方/开票标签行
        val sellerLabels = listOf(
            "开票公司", "开票单位", "销售方名称", "收款方名称", "收款方", "收款单位"
        )
        for (line in lines) {
            for (label in sellerLabels) {
                if (line.contains(label)) {
                    val after = line.substring(line.indexOf(label) + label.length)
                        .trimStart(':', '：', ' ')
                    if (after.length >= 2 && after != "信息") return after.take(50)
                }
            }
        }

        // 策略3：全文中含公司后缀的行
        for (line in lines) {
            if (isCompanyName(line)) {
                val cleaned = line
                    .replace(Regex("""(名称|:|：)\s*"""), "")
                    .replace(Regex("""\d{7,}"""), "")
                    .trim()
                if (cleaned.length >= 2) return cleaned.take(50)
            }
        }

        // 策略4：知名品牌（兜底）
        val brands = listOf("星巴克", "麦当劳", "肯德基", "瑞幸", "喜茶", "海底捞", "沃尔玛", "永辉", "盒马", "美团", "滴滴")
        val fullText = lines.joinToString(" ")
        for (brand in brands) {
            if (fullText.contains(brand)) return brand
        }

        // 策略5：如果有多个"名称:"但都不含公司后缀，取最长的
        if (allNames.isNotEmpty()) {
            return allNames.maxByOrNull { it.length }?.take(50) ?: "未知商户"
        }

        // 策略6：第一行有意义的文字
        val skipPatterns = listOf(
            Regex("""^\d{1,2}:\d{2}"""), Regex("""^\d{4}[-/]"""), Regex("""^[¥￥$]"""),
            Regex("""^\d+\.?\d*$"""), Regex("""^No\.|^编号|^发票|^查看"""),
            Regex("""^合计|^总计|^金额|^税"""), Regex("""^http|^www"""),
            Regex("""^共\d+页"""), Regex("""^电子发票"""),
            Regex("""^购买方"""), Regex("""^销售方"""),
        )
        for (line in lines.take(10)) {
            if (line.length < 2) continue
            if (skipPatterns.any { it.containsMatchIn(line) }) continue
            return line.take(50)
        }

        return "未知商户"
    }

    private fun extractAmount(lines: List<String>): Double? {
        val moneyPattern = Regex("""[¥￥$]?\s*(\d{1,3}(?:,\d{3})*\.\d{2})""")
        val simpleMoneyPattern = Regex("""(\d+\.\d{2})""")

        fun extractNumbers(line: String): List<Double> {
            val results = mutableListOf<Double>()
            moneyPattern.findAll(line).forEach { m ->
                m.groupValues[1].replace(",", "").toDoubleOrNull()?.let {
                    if (it > 0 && it < 1000000) results.add(it)
                }
            }
            if (results.isEmpty()) {
                simpleMoneyPattern.findAll(line).forEach { m ->
                    m.value.toDoubleOrNull()?.let {
                        if (it > 0 && it < 1000000) results.add(it)
                    }
                }
            }
            return results
        }

        // 在关键词行及其前后各2行搜索金额
        fun searchNearby(lineIndex: Int): Double? {
            val range = maxOf(0, lineIndex - 2)..minOf(lines.size - 1, lineIndex + 2)
            val allNums = range.flatMap { extractNumbers(lines[it]) }
            return allNums.maxOrNull()
        }

        // 第一优先级：价税合计（发票最终金额，包含税）
        for ((idx, line) in lines.withIndex()) {
            if (line.contains("价税合计")) {
                val nums = extractNumbers(line)
                if (nums.isNotEmpty()) return nums.max()
                return searchNearby(idx)
            }
        }

        // 第二优先级：合计/总计/总额（不含"小计"）
        val totalKw = listOf("合计", "总计", "总额", "总价", "实付", "实收")
        for ((idx, line) in lines.withIndex().toList().reversed()) {
            // 排除"小计"和纯"税额合计"
            if (line.contains("小计")) continue
            if (line.contains("税额") && !line.contains("价税")) continue
            if (totalKw.any { line.contains(it) }) {
                val nums = extractNumbers(line)
                if (nums.isNotEmpty()) return nums.max()
                return searchNearby(idx)
            }
        }

        // 第三优先级：¥符号后面的金额（从后往前找最大的）
        val yenAmounts = mutableListOf<Double>()
        for (line in lines) {
            Regex("""[¥￥]\s*(\d{1,3}(?:,\d{3})*\.\d{2})""").findAll(line).forEach { m ->
                m.groupValues[1].replace(",", "").toDoubleOrNull()?.let {
                    if (it > 0 && it < 1000000) yenAmounts.add(it)
                }
            }
        }
        if (yenAmounts.isNotEmpty()) return yenAmounts.max()

        // 兜底：全文最大的 XX.XX 金额
        val all = lines.flatMap { extractNumbers(it) }
        return all.maxOrNull()
    }

    private fun extractDate(text: String): String? {
        Regex("""(20\d{2}[-/]\d{1,2}[-/]\d{1,2})""").find(text)?.let {
            val p = it.value.split(Regex("[-/]"))
            if (p.size == 3) return "${p[0]}-${p[1].padStart(2, '0')}-${p[2].padStart(2, '0')}"
        }
        Regex("""(20\d{2})年(\d{1,2})月(\d{1,2})日""").find(text)?.let {
            val (y, m, d) = it.destructured
            return "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        }
        Regex("""(20\d{2})\.(\d{1,2})\.(\d{1,2})""").find(text)?.let {
            val (y, m, d) = it.destructured
            return "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        }
        return null
    }

    // 商品明细提取
    private fun extractItemDetails(lines: List<String>): List<ItemDetail> {
        val details = mutableListOf<ItemDetail>()
        val patterns = listOf(
            Regex("""(.{2,15})\s+[xX×](\d+)\s+(\d+\.?\d{0,2})"""),
            Regex("""(.{2,15})\s+(\d+)\s+(\d+\.\d{2})"""),
            Regex("""(.{2,15})\s+(\d+\.\d{2})$""")
        )
        for (line in lines) {
            var matched = false
            for ((idx, pattern) in patterns.withIndex()) {
                if (matched) continue
                val match = pattern.find(line) ?: continue
                val name: String
                val qty: Int
                val price: Double
                if (idx == 2) {
                    name = match.groupValues[1].trim()
                    qty = 1
                    price = match.groupValues[2].toDoubleOrNull() ?: continue
                } else {
                    name = match.groupValues[1].trim()
                    qty = match.groupValues[2].toIntOrNull() ?: 1
                    price = match.groupValues[3].toDoubleOrNull() ?: continue
                }
                if (name.length in 2..20 && price > 0 && price < 100000) {
                    details.add(ItemDetail(name = name, price = price, quantity = qty))
                    matched = true
                }
            }
        }
        return details
    }
}
