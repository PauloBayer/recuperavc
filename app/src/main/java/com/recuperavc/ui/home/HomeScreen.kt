package com.recuperavc.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.R
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.components.RecuperAVCBrandHeader
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.rememberInitialSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HighContrastAccent = Color(0xFFFFD600)

@Composable
fun HomeScreen(
    onOpenSentenceTest: () -> Unit = {},
    onOpenAudioTest: () -> Unit,
    onOpenMotionTest: () -> Unit,
    onOpenReports: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    // Block first frame until settings arrive (prevents flashes)
    val initial: InitialSettings? = rememberInitialSettings(settings)
    if (initial == null) {
        PaintSystemBars(background = Color.Black, lightIcons = false)
        Box(Modifier.fillMaxSize().background(Color.Black)) {}
        return
    }

    // Live prefs
    val appliedDark by settings.darkModeFlow.collectAsState(initial = initial.dark)
    val appliedContrast by settings.contrastFlow.collectAsState(initial = initial.contrast)
    val appliedScale by settings.sizeTextFlow.collectAsState(initial = initial.scale)

    // Base palette
    val background = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF121212)
        else -> Color.White
    }
    val textPrimary = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEAEAEA)
        else -> Color(0xFF1B1B1B)
    }
    val accent = if (appliedContrast) HighContrastAccent else GreenDark
    val drawerContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> GreenLight
    }
    val drawerTextIcon = Color.White
    val drawerSelectedContainer = when {
        appliedContrast || appliedDark -> Color(0xFF222222)
        else -> Color.White.copy(alpha = 0.16f)
    }

    PaintSystemBars(background = background, lightIcons = !(appliedContrast || appliedDark))

    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sfx = rememberSfxController()

    fun navigateWithClick(action: () -> Unit) {
        scope.launch {
            sfx.play(Sfx.CLICK)
            delay(140)
            action()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.78f),
                drawerContainerColor = drawerContainer,
                drawerContentColor = drawerTextIcon
            ) {
                // Drawer header wave — decorative; hide in HC
                Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    if (!appliedContrast) {
                        Image(
                            painter = painterResource(
                                // ✅ light -> wave_light, dark -> wave_green
                                id = if (appliedDark) R.drawable.wave_green else R.drawable.wave_light
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                NavigationDrawerItem(
                    label = { Text("Relatórios", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        navigateWithClick {
                            onOpenReports()
                            scope.launch { drawerState.close() }
                        }
                    },
                    icon = { Icon(Icons.Filled.Summarize, contentDescription = null, tint = drawerTextIcon) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = drawerSelectedContainer,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = drawerTextIcon,
                        unselectedTextColor = drawerTextIcon,
                        selectedIconColor = drawerTextIcon,
                        unselectedIconColor = drawerTextIcon
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Preferências do App", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        navigateWithClick {
                            onOpenSettings()
                            scope.launch { drawerState.close() }
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null, tint = drawerTextIcon) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = drawerSelectedContainer,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = drawerTextIcon,
                        unselectedTextColor = drawerTextIcon,
                        selectedIconColor = drawerTextIcon,
                        unselectedIconColor = drawerTextIcon
                    )
                )

                Spacer(Modifier.weight(1f))
                Divider(color = drawerTextIcon.copy(alpha = 0.2f))
                NavigationDrawerItem(
                    label = { Text("Sair", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        navigateWithClick {
                            onExit()
                            scope.launch { drawerState.close() }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = drawerTextIcon
                        )
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = drawerSelectedContainer.copy(alpha = 0.6f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = drawerTextIcon,
                        unselectedTextColor = drawerTextIcon,
                        selectedIconColor = drawerTextIcon,
                        unselectedIconColor = drawerTextIcon
                    )
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    ) {
        Box(modifier = Modifier
                        .fillMaxSize()
                        .background(background)
                        .systemBarsPadding()
        ) {

            // Top header with wave (decorative; hide in HC)
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (!appliedContrast) {
                    Image(
                        // Keep the GREEN wave in the main header for both light & dark
                        painter = painterResource(id = R.drawable.wave_green),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = {
                        scope.launch {
                            sfx.play(Sfx.CLICK)
                            delay(60)
                            drawerState.open()
                        }
                    }) {
                        // ✅ Always white in light/dark/HC
                        Icon(Icons.Filled.Menu, contentDescription = null, tint = Color.White)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 130.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                RecuperAVCBrandHeader()
                Spacer(Modifier.height(58.dp))

                ActionCard(
                    title = "Teste de raciocínio",
                    icon = Icons.Filled.Psychology,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    accent = accent,
                    textPrimary = textPrimary,
                    background = background,
                    onClick = { navigateWithClick(onOpenSentenceTest) }
                )
                Spacer(Modifier.height(16.dp))
                ActionCard(
                    title = "Teste de reconhecimento de voz",
                    icon = Icons.Filled.Mic,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    accent = accent,
                    textPrimary = textPrimary,
                    background = background,
                    onClick = { navigateWithClick(onOpenAudioTest) }
                )
                Spacer(Modifier.height(16.dp))
                ActionCard(
                    title = "Teste de coordenação motora",
                    icon = Icons.Filled.Gesture,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    accent = accent,
                    textPrimary = textPrimary,
                    background = background,
                    onClick = { navigateWithClick(onOpenMotionTest) }
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    accent: Color,
    textPrimary: Color,
    background: Color,
    onClick: () -> Unit
) {
    val containerColor: Color
    val contentColor: Color
    val chipBg: Color
    val chipIcon: Color
    val borderColor: Color?

    if (appliedContrast) {
        containerColor = Color.Black
        contentColor = Color.White
        chipBg = accent
        chipIcon = Color.Black
        borderColor = accent
    } else if (appliedDark) {
        containerColor = Color(0xFF1E1E1E)
        contentColor = Color.White
        chipBg = GreenDark
        chipIcon = Color.White
        borderColor = null
    } else {
        containerColor = GreenLight
        contentColor = Color.White
        chipBg = GreenDark
        chipIcon = Color.White
        borderColor = null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .then(
                if (borderColor != null)
                    Modifier.border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast) 0.dp else 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(chipBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = chipIcon, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                color = contentColor,
                fontSize = 18.sp * appliedScale,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )
        }
    }
}
