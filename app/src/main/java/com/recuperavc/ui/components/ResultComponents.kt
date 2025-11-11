package com.recuperavc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultDialogContainer(
    appliedContrast: Boolean,
    appliedDark: Boolean,
    content: @Composable () -> Unit
) {
    val cardContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val cardBorder = when {
        appliedContrast -> BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 6.dp),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

@Composable
fun ResultTitle(
    text: String,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    accent: Color
) {
    val titleColor = when {
        appliedContrast -> accent
        appliedDark -> Color.White
        else -> Color(0xFF1B1B1B)
    }

    Text(
        text,
        fontSize = 22.sp * appliedScale,
        fontWeight = FontWeight.Bold,
        color = titleColor
    )
}

@Composable
fun ResultMetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    icon: ImageVector,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val container = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color.White.copy(alpha = 0.95f)
    }
    val border = when {
        appliedContrast -> BorderStroke(2.dp, color)
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        else -> null
    }
    val titleColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.8f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> Color.Gray.copy(alpha = 0.8f)
    }
    val valueColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color.White
        else -> color
    }
    val unitColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.7f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> color.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 8.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp * appliedScale, fontWeight = FontWeight.Medium, color = titleColor)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 26.sp * appliedScale, fontWeight = FontWeight.Bold, color = valueColor)
                Text(text = " $unit", fontSize = 14.sp * appliedScale, fontWeight = FontWeight.Medium, color = unitColor)
            }
        }
    }
}

@Composable
fun ResultSectionLabel(
    text: String,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val labelColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF3A3A3A)
    }

    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp * appliedScale, color = labelColor)
}

@Composable
fun ResultItemCard(
    appliedContrast: Boolean,
    appliedDark: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val itemCardContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color(0xFFF5F5F5)
    }
    val itemBorder = when {
        appliedContrast -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        else -> null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = itemCardContainer),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
        border = itemBorder
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun ResultItemText(
    text: String,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    fontWeight: FontWeight = FontWeight.Medium,
    fontSize: Float = 14f
) {
    val itemTextColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF1B1B1B)
    }

    Text(text, fontWeight = fontWeight, fontSize = fontSize.sp * appliedScale, color = itemTextColor)
}
