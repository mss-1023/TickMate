package com.tickmate.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.compose.rememberNavController
import com.tickmate.app.ui.navigation.BottomNavBar
import com.tickmate.app.ui.navigation.NavGraph
import com.tickmate.app.ui.navigation.Routes
import com.tickmate.app.ui.theme.DarkBackground
import com.tickmate.app.ui.theme.TickMateTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "tickmate_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

        setContent {
            TickMateTheme {
                val navController = rememberNavController()
                val startDest = if (onboardingDone) Routes.HOME else Routes.ONBOARDING

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    containerColor = DarkBackground,
                    bottomBar = { BottomNavBar(navController) }
                ) { innerPadding ->
                    // 只传 bottom padding（底部导航栏高度），不传 top（避免双重顶部间距）
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                        startDestination = startDest
                    )
                }

                if (!onboardingDone) {
                    LaunchedEffect(navController) {
                        navController.currentBackStackEntryFlow.collect { entry ->
                            if (entry.destination.route == Routes.HOME) {
                                prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
                            }
                        }
                    }
                }
            }
        }
    }
}
