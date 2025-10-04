package com.recuperavc.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.recuperavc.ui.theme.BackgroundGreen
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground

@Composable
fun HomeScreen(
    onOpenSentenceTest: () -> Unit = {},
    onOpenAudioTest: () -> Unit,
    onOpenMotionTest: () -> Unit,
    onOpenReports: () -> Unit = {},
    onExit: () -> Unit
) {
    val drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.78f),
                drawerContainerColor = GreenLight,
                drawerContentColor = Color.White
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(0f, h * 0.55f)
                            cubicTo(
                                w * 0.25f, h * 0.20f,
                                w * 0.55f, h * 0.95f,
                                w * 0.80f, h * 0.60f
                            )
                            cubicTo(
                                w * 0.90f, h * 0.40f,
                                w, h * 0.48f,
                                w, h * 0.38f
                            )
                            lineTo(w, 0f)
                            close()
                        }
                        drawPath(path, color = GreenDark)
                    }
                }
                Spacer(Modifier.height(32.dp))
                NavigationDrawerItem(
                    label = { Text("Relatórios") },
                    selected = false,
                    onClick = { onOpenReports(); scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.16f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Preferências do App") },
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.16f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White
                    )
                )
                NavigationDrawerItem(
                    label = { Text("Editar Perfil") },
                    selected = false,
                    onClick = {},
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.White.copy(alpha = 0.16f),
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = Color.White,
                        unselectedTextColor = Color.White,
                        selectedIconColor = Color.White,
                        unselectedIconColor = Color.White
                    )
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, h * 0.55f)
                        cubicTo(
                            w * 0.25f, h * 0.25f,
                            w * 0.45f, h * 0.95f,
                            w * 0.6f, h * 0.65f
                        )
                        cubicTo(
                            w * 0.8f, h * 0.35f,
                            w * 0.9f, h * 0.5f,
                            w, h * 0.4f
                        )
                        lineTo(w, 0f)
                        close()
                    }
                    drawPath(path, color = GreenLight)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = OnBackground)
                    }
                    IconButton(onClick = onExit) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = OnBackground)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 140.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ActionCard(
                    title = "Teste de raciocínio",
                    icon = Icons.Default.Psychology,
                    onClick = onOpenSentenceTest
                )
                Spacer(Modifier.height(16.dp))
                ActionCard(
                    title = "Teste de reconhecimento de voz",
                    icon = Icons.Default.Mic,
                    onClick = onOpenAudioTest
                )
                Spacer(Modifier.height(16.dp))
                ActionCard(
                    title = "Teste de coordenação motora",
                    icon = Icons.Default.Gesture,
                    onClick = onOpenMotionTest
                )
            }

        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                    .background(GreenDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start
            )
        }
    }
}
