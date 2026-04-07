package com.voxn.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voxn.ai.manager.BiometricLockManager
import com.voxn.ai.util.HapticFeedback
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.UserProfileManager
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.theme.VoxnTheme
import com.voxn.ai.ui.dashboard.DashboardScreen
import com.voxn.ai.ui.habits.HabitsScreen
import com.voxn.ai.ui.health.HealthScreen
import com.voxn.ai.ui.notes.NotesScreen
import com.voxn.ai.ui.launch.LaunchScreen
import com.voxn.ai.ui.onboarding.OnboardingScreen
import com.voxn.ai.ui.spending.SpendingScreen

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoxnTheme {
                AppRoot()
            }
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED
        ) {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val profileManager = remember { UserProfileManager(context) }
    val budgetManager = remember { BudgetManager(context) }
    val onboardingComplete by profileManager.onboardingComplete.collectAsState()
    val biometricManager = remember { BiometricLockManager(context) }
    val biometricUnlocked by biometricManager.isUnlocked.collectAsState()
    var launchComplete by remember { mutableStateOf(false) }

    if (!launchComplete) {
        LaunchScreen(onComplete = { launchComplete = true })
    } else if (!onboardingComplete) {
        OnboardingScreen(
            profileManager = profileManager,
            budgetManager = budgetManager,
            onComplete = { /* state updates automatically via StateFlow */ },
        )
    } else if (biometricManager.needsUnlock() && !biometricUnlocked) {
        BiometricLockScreen(biometricManager)
    } else {
        MainScreen()
    }
}

@Composable
private fun BiometricLockScreen(biometricManager: BiometricLockManager) {
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity

    LaunchedEffect(Unit) {
        if (activity != null && BiometricLockManager.isBiometricAvailable(context)) {
            BiometricLockManager.showPrompt(
                activity,
                onSuccess = { biometricManager.setUnlocked() },
                onError = { /* stay on lock screen */ },
            )
        } else {
            biometricManager.setUnlocked() // no biometric hardware, skip
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(VoxnColors.backgroundDark),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Lock, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Voxn AI is locked", style = VoxnFont.sectionTitle, color = VoxnColors.textPrimary)
            Spacer(Modifier.height(8.dp))
            Text("Authenticate to continue", style = VoxnFont.cardBody, color = VoxnColors.textTertiary)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (activity != null) {
                        BiometricLockManager.showPrompt(
                            activity,
                            onSuccess = { biometricManager.setUnlocked() },
                            onError = { },
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
            ) {
                Icon(Icons.Default.Fingerprint, null, tint = VoxnColors.backgroundDark, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("UNLOCK", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.backgroundDark)
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

    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().background(VoxnColors.backgroundDark)) {
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
                            VoxnColors.electricBlue.copy(alpha = 0.1f),
                            VoxnColors.backgroundDark.copy(alpha = 0.95f),
                            VoxnColors.backgroundDark.copy(alpha = 0.95f),
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
                    if (isSelected) VoxnColors.electricBlue else VoxnColors.textTertiary,
                    label = "tab"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { HapticFeedback.tick(context); selectedTab = index }
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
                        style = VoxnFont.mono(9, FontWeight.Medium),
                        color = color,
                    )
                    Spacer(Modifier.height(2.dp))
                    // Active indicator
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(
                                if (isSelected) VoxnColors.electricBlue else Color.Transparent,
                                MaterialTheme.shapes.small,
                            )
                    )
                }
            }
        }
    }
}
