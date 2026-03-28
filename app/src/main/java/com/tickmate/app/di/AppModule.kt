package com.tickmate.app.di

import com.tickmate.app.data.db.AppDatabase
import com.tickmate.app.data.repository.BudgetRepository
import com.tickmate.app.data.repository.CategoryRepository
import com.tickmate.app.data.repository.RecordRepository
import com.tickmate.app.service.CategoryMatcher
import com.tickmate.app.service.ExportService
import com.tickmate.app.service.NotificationService
import com.tickmate.app.service.OCRService
import com.tickmate.app.service.OnlineOCRService
import com.tickmate.app.ui.camera.CameraViewModel
import com.tickmate.app.ui.home.HomeViewModel
import com.tickmate.app.ui.record.RecordListViewModel
import com.tickmate.app.ui.settings.SettingsViewModel
import com.tickmate.app.ui.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // 数据库
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().recordDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().budgetDao() }

    // Repository
    single { RecordRepository(get()) }
    single { CategoryRepository(get()) }
    single { BudgetRepository(get()) }

    // Service
    single { OCRService(androidContext()) }
    single { OnlineOCRService(androidContext()) }
    single { NotificationService(androidContext()) }
    single { CategoryMatcher(get()) }
    single { ExportService(androidContext()) }

    // ViewModel
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { RecordListViewModel(get(), get()) }
    viewModel { StatsViewModel(get(), get()) }
    viewModel { CameraViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get()) }
}
