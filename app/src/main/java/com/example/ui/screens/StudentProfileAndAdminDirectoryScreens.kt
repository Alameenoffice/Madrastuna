package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AdaptiveSoftBorder
import com.example.ui.theme.AdaptiveSoftSurface
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.RoyalGreen

/**
 * Clean Segmented Selector Card for Gender (ذكر / أنثى) and Age (العمر الزمني).
 * Uses explicit lambda types so Kotlin type inference always succeeds across all screens.
 */
@Composable
fun GenderAndAgeSegmentedSelectorCard(
    selectedGender: String,
    onGenderChange: (String) -> Unit,
    ageYears: Int,
    onAgeYearsChange: (Int) -> Unit,
    title: String = "تحديد الجنس والعمر الزمني"
) {
    val isFemaleSelected = selectedGender.trim() == "أنثى" ||
        selectedGender.trim() == "انثى" ||
        selectedGender.equals("FEMALE", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AdaptiveSoftSurface.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, AdaptiveSoftBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepForestGreen
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val maleSelected = !isFemaleSelected
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onGenderChange("ذكر") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (maleSelected) RoyalGreen else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (maleSelected) RoyalGreen else AdaptiveSoftBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "ذكر",
                            tint = if (maleSelected) Color.White else DeepForestGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ذكر",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (maleSelected) Color.White else DeepForestGreen
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onGenderChange("أنثى") },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFemaleSelected) Color(0xFFBE185D) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFemaleSelected) Color(0xFFBE185D) else AdaptiveSoftBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Face,
                            contentDescription = "أنثى",
                            tint = if (isFemaleSelected) Color.White else Color(0xFFBE185D),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أنثى",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFemaleSelected) Color.White else Color(0xFFBE185D)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (ageYears > 0) "العمر المعتمد: $ageYears سنة" else "العمر الزمني (اختياري):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val next = if (ageYears <= 6) 0 else ageYears - 1
                            onAgeYearsChange(next)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "تقليل العمر",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, AdaptiveSoftBorder)
                    ) {
                        Text(
                            text = if (ageYears > 0) "$ageYears" else "—",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = {
                            val next = if (ageYears <= 0) 12 else (ageYears + 1).coerceAtMost(75)
                            onAgeYearsChange(next)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "زيادة العمر",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
