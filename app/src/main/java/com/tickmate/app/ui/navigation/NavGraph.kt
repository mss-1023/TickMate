package com.tickmate.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tickmate.app.ui.camera.CameraScreen
import com.tickmate.app.ui.home.HomeScreen
import com.tickmate.app.ui.onboarding.OnboardingScreen
import com.tickmate.app.ui.record.AddEditRecordScreen
import com.tickmate.app.ui.record.RecordDetailScreen
import com.tickmate.app.ui.record.RecordListScreen
import com.tickmate.app.ui.record.SearchScreen
import com.tickmate.app.ui.settings.AboutScreen
import com.tickmate.app.ui.settings.SettingsScreen
import com.tickmate.app.ui.stats.MonthlyReportScreen
import com.tickmate.app.ui.stats.StatsScreen

// 路由定义
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RECORD_LIST = "record_list"
    const val ADD_RECORD = "add_record"
    const val EDIT_RECORD = "edit_record/{recordId}"
    const val RECORD_DETAIL = "record_detail/{recordId}"
    const val SEARCH = "search"
    const val STATS = "stats"
    const val MONTHLY_REPORT = "monthly_report"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val CAMERA = "camera"

    fun editRecord(recordId: Long) = "edit_record/$recordId"
    fun recordDetail(recordId: Long) = "record_detail/$recordId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // 引导页
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // 首页
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToAddRecord = { navController.navigate(Routes.ADD_RECORD) },
                onNavigateToCamera = { navController.navigate(Routes.CAMERA) },
                onNavigateToSearch = { navController.navigate(Routes.SEARCH) },
                onNavigateToMonthlyReport = { navController.navigate(Routes.MONTHLY_REPORT) }
            )
        }

        // 记录列表
        composable(Routes.RECORD_LIST) {
            RecordListScreen(
                onNavigateToAddRecord = { navController.navigate(Routes.ADD_RECORD) },
                onNavigateToCamera = { navController.navigate(Routes.CAMERA) },
                onNavigateToEditRecord = { id -> navController.navigate(Routes.editRecord(id)) },
                onNavigateToDetail = { id -> navController.navigate(Routes.recordDetail(id)) }
            )
        }

        // 手动录入
        composable(Routes.ADD_RECORD) {
            AddEditRecordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 编辑记录
        composable(
            route = Routes.EDIT_RECORD,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            AddEditRecordScreen(
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 记录详情
        composable(
            route = Routes.RECORD_DETAIL,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            RecordDetailScreen(
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id -> navController.navigate(Routes.editRecord(id)) }
            )
        }

        // 搜索
        composable(Routes.SEARCH) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { id -> navController.navigate(Routes.recordDetail(id)) }
            )
        }

        // 统计
        composable(Routes.STATS) {
            StatsScreen()
        }

        // 月度报表
        composable(Routes.MONTHLY_REPORT) {
            MonthlyReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateToAbout = { navController.navigate(Routes.ABOUT) }
            )
        }

        // 关于
        composable(Routes.ABOUT) {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }

        // 拍照识别
        composable(Routes.CAMERA) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
