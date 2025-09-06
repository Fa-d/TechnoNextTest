package dev.sadakat.technonexttest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.technonexttest.presentation.navigation.AppNavigation
import dev.sadakat.technonexttest.presentation.ui.theme.TechnoNextTestTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TechnoNextTestTheme {
                AppNavigation()
            }
        }
    }
}