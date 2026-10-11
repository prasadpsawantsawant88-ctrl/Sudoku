package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyActivityEntity
import com.example.ui.theme.LocalSudokuColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class DayActivityData(
    val dateString: String,      // "yyyy-MM-dd"
    val displayDay: String,      // "11"
    val isToday: Boolean,
    val gamesPlayed: Int,
    val gamesWon: Int
)

@Composable
fun StatsActivityChart(
    dailyActivities: List<DailyActivityEntity>,
    currentStreak: Int,
    maxStreak: Int,
    dailyStreak: com.example.data.DailyStreakEntity = com.example.data.DailyStreakEntity(),
    modifier: Modifier = Modifier
) {
    val colors = LocalSudokuColors.current

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = remember { dateFormat.format(Date()) }
    val isCompletedToday = (dailyStreak.lastCompletedDate == todayStr)

    // Prepare continuous 30-day timeline (from 29 days ago up to today)
    val dayDataList = remember(dailyActivities) {
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())
        val activityMap = dailyActivities.associateBy { it.dateString }

        val list = mutableListOf<DayActivityData>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -29)

        for (i in 0 until 30) {
            val dateStr = dateFormat.format(cal.time)
            val dayStr = dayFormat.format(cal.time)
            val act = activityMap[dateStr]
            list.add(
                DayActivityData(
                    dateString = dateStr,
                    displayDay = dayStr,
                    isToday = (dateStr == todayStr),
                    gamesPlayed = act?.gamesPlayed ?: 0,
                    gamesWon = act?.gamesWon ?: 0
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val total30dPlayed = dayDataList.sumOf { it.gamesPlayed }
    val total30dWon = dayDataList.sumOf { it.gamesWon }
    val winRate30d = if (total30dPlayed > 0) ((total30dWon.toDouble() / total30dPlayed) * 100).toInt() else 0

    var selectedDay by remember { mutableStateOf<DayActivityData?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_activity_chart")
    ) {
        // Daily Challenge Streak Special Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.12f))
                .border(1.dp, colors.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("daily_streak_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (dailyStreak.currentStreak > 0) colors.hint else colors.secondaryText,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${dailyStreak.currentStreak} Day Daily Streak",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                        }
                        Text(
                            text = "Best: ${dailyStreak.maxStreak} days • ${dailyStreak.totalCompleted} solved",
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompletedToday) colors.accent else colors.hint.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompletedToday) "Completed ✓" else "Today Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompletedToday) colors.boardSurface else colors.hint
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "30-DAY ACTIVITY & STREAKS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryText,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "$winRate30d% Win Rate",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Streaks Card Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Current Streak
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.background)
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (currentStreak > 0) colors.hint else colors.secondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "$currentStreak days",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Current Streak",
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            // Longest Streak
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.background)
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "$maxStreak days",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Best Streak",
                            fontSize = 11.sp,
                            color = colors.secondaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 30-Day Activity Bar Chart
        Text(
            text = "Daily Games & Wins",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Canvas Recharts-style Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            DailyBarChartCanvas(
                dayDataList = dayDataList,
                accentColor = colors.accent,
                barBgColor = colors.gridBorder.copy(alpha = 0.6f),
                highlightColor = colors.hint,
                onSelectDay = { selectedDay = it }
            )
        }

        // Selected Day Details / Tooltip
        val currentSelected = selectedDay ?: dayDataList.lastOrNull()
        if (currentSelected != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentSelected.isToday) "Today (${currentSelected.dateString})" else currentSelected.dateString,
                    fontSize = 12.sp,
                    color = colors.secondaryText
                )
                Text(
                    text = "${currentSelected.gamesWon} won / ${currentSelected.gamesPlayed} played",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (currentSelected.gamesWon > 0) colors.accent else colors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 30-Day Heatmap Grid (3 rows x 10 cols)
        Text(
            text = "Activity Grid (Past 30 Days)",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (row in 0 until 3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 10) {
                        val idx = row * 10 + col
                        val dayItem = dayDataList.getOrNull(idx)

                        if (dayItem != null) {
                            val tileColor = when {
                                dayItem.gamesWon >= 3 -> colors.accent
                                dayItem.gamesWon == 2 -> colors.accent.copy(alpha = 0.7f)
                                dayItem.gamesWon == 1 -> colors.accent.copy(alpha = 0.4f)
                                dayItem.gamesPlayed > 0 -> colors.hint.copy(alpha = 0.4f)
                                else -> colors.boardSurface
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tileColor)
                                    .then(
                                        if (dayItem.isToday) Modifier.border(1.5.dp, colors.accent, RoundedCornerShape(4.dp))
                                        else Modifier
                                    )
                                    .clickable { selectedDay = dayItem },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayItem.displayDay,
                                    fontSize = 10.sp,
                                    fontWeight = if (dayItem.isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (dayItem.gamesWon >= 2) colors.boardSurface else colors.secondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 10.sp, color = colors.secondaryText)
                Spacer(modifier = Modifier.width(4.dp))
                LegendBox(colors.boardSurface)
                LegendBox(colors.accent.copy(alpha = 0.4f))
                LegendBox(colors.accent.copy(alpha = 0.7f))
                LegendBox(colors.accent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("More", fontSize = 10.sp, color = colors.secondaryText)
            }
        }
    }
}

@Composable
private fun LegendBox(color: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

@Composable
private fun DailyBarChartCanvas(
    dayDataList: List<DayActivityData>,
    accentColor: Color,
    barBgColor: Color,
    highlightColor: Color,
    onSelectDay: (DayActivityData) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
        val totalDays = dayDataList.size
        if (totalDays == 0) return@Canvas

        val maxGames = max(1, dayDataList.maxOf { it.gamesPlayed })
        val w = size.width
        val h = size.height
        val barSpacing = w / totalDays.toFloat()
        val barWidth = (barSpacing * 0.65f).coerceAtLeast(3f)

        // Draw baseline
        drawLine(
            color = barBgColor,
            start = Offset(0f, h - 2f),
            end = Offset(w, h - 2f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw trend line path
        val trendPath = Path()
        var pathStarted = false

        dayDataList.forEachIndexed { i, day ->
            val cx = i * barSpacing + barSpacing / 2f

            // Full height bar background (placeholder height)
            val totalH = if (maxGames > 0) ((day.gamesPlayed.toFloat() / maxGames.toFloat()) * (h - 10f)).coerceAtLeast(4f) else 4f
            val wonH = if (maxGames > 0) ((day.gamesWon.toFloat() / maxGames.toFloat()) * (h - 10f)) else 0f

            val topY = h - totalH
            val wonTopY = h - wonH

            // Bar for total played (light neutral tint)
            if (day.gamesPlayed > 0) {
                drawRoundRect(
                    color = barBgColor,
                    topLeft = Offset(cx - barWidth / 2f, topY),
                    size = Size(barWidth, totalH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            } else {
                // Subtle empty bar placeholder
                drawRoundRect(
                    color = barBgColor.copy(alpha = 0.3f),
                    topLeft = Offset(cx - barWidth / 2f, h - 4f),
                    size = Size(barWidth, 4f),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }

            // Foreground bar for games won (Muted Sage Accent)
            if (wonH > 0f) {
                drawRoundRect(
                    color = if (day.isToday) highlightColor else accentColor,
                    topLeft = Offset(cx - barWidth / 2f, wonTopY),
                    size = Size(barWidth, wonH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Plot trend point
            val trendY = h - (wonH.coerceAtLeast(4f))
            if (!pathStarted) {
                trendPath.moveTo(cx, trendY)
                pathStarted = true
            } else {
                trendPath.lineTo(cx, trendY)
            }
        }

        // Draw subtle smoothed trend line over bars
        if (pathStarted) {
            drawPath(
                path = trendPath,
                color = accentColor.copy(alpha = 0.5f),
                style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
