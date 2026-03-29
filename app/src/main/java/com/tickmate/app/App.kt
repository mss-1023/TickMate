package com.tickmate.app

import android.app.Application
import com.tickmate.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.io.File

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(appModule)
        }
        // 清理过期的临时文件（拍照缓存、导出文件）
        cleanTempFiles()
    }

    private fun cleanTempFiles() {
        try {
            val maxAge = 24 * 60 * 60 * 1000L // 24小时
            val now = System.currentTimeMillis()
            listOf("photos", "exports").forEach { dir ->
                File(cacheDir, dir).listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAge) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) { }
    }
}
