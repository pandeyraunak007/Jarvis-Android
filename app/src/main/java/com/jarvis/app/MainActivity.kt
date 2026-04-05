package com.jarvis.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.theme.JarvisTheme
import com.jarvis.app.ui.dashboard.DashboardScreen
import com.jarvis.app.ui.habits.HabitsScreen
import com.jarvis.app.ui.health.HealthScreen
import com.jarvis.app.ui.notes.NotesScreen
import com.jarvis.app.ui.spending.SpendingScreen

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        setContent {
            JarvisTheme {
                MainScreen()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private data class TabItem(
    val icon: ImageVector,
    val label: String,
)

@Composable
private fun MainScreen() {
    val tabs = remember {
        listOf(
            TabItem(Icons.Default.GridView, "HUD"),
            TabItem(Icons.Default.Favorite, "Health"),
            TabItem(Icons.Default.CheckCircle, "Habits"),
            TabItem(Icons.Default.CurrencyRupee, "Spend"),
            TabItem(Icons.Default.Description, "Notes"),
        )
    }
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(JarvisColors.backgroundDark)) {
        // Content
        when (selectedTab) {
            0 -> DashboardScreen()
            1 -> HealthScreen()
            2 -> HabitsScreen()
            3 -> SpendingScreen()
            4 -> NotesScreen()
        }

        // Custom Tab Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            JarvisColors.electricBlue.copy(alpha = 0.1f),
                            JarvisColors.backgroundDark.copy(alpha = 0.95f),
                            JarvisColors.backgroundDark.copy(alpha = 0.95f),
                        )
                    )
                )
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index
                val color by animateColorAsState(
                    if (isSelected) JarvisColors.electricBlue else JarvisColors.textTertiary,
                    label = "tab"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        tab.icon, tab.label,
                        tint = color,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label,
                        style = JarvisFont.mono(9, FontWeight.Medium),
                        color = color,
                    )
                    Spacer(Modifier.height(2.dp))
                    // Active indicator
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(
                                if (isSelected) JarvisColors.electricBlue else Color.Transparent,
                                MaterialTheme.shapes.small,
                            )
                    )
                }
            }
        }
    }
}
