package com.example.todo_list.ui.screens.retrospective

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.todo_list.manager.AnalyticsEngine
import com.example.todo_list.manager.UserProfileManager
import com.example.todo_list.model.*
import com.example.todo_list.ui.theme.*
import com.example.todo_list.utils.HapticManager

/**
 * Apple iOS HIG Analytics, Reporting & Retrospectives Screen.
 *
 * Implements:
 * 1. Structured Category-wise Weekly & Monthly Reviews.
 * 2. Total Work Output: Total hours logged, task completion rates, active focus sessions.
 * 3. Domain Breakdown: Visual distribution across Study, Professional, Fitness, and Hobbies.
 * 4. Month-over-Month (MoM) Growth: Trend analysis showing productivity surge or drops.
 * 5. Adaptive Feedback:
 *    - Growth Detected: In-app badges, progression milestones, reward points.
 *    - Dip Detected: Supportive gratitude prompts & motivational encouragement without punitive messaging.
 */
@Composable
fun AnalyticsRetrospectiveScreen(
    taskList: List<TaskItem>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(ReviewPeriod.WEEKLY) }
    var hasClaimedReward by remember { mutableStateOf(false) }

    val report = remember(taskList, selectedPeriod) {
        AnalyticsEngine.generateReport(
            tasks = taskList,
            period = selectedPeriod
        )
    }

    Scaffold(
        topBar = {
            RetrospectiveTopBar(
                title = "Analytics & Reviews",
                onBackClick = onNavigateBack,
                onShareClick = {
                    HapticManager.performClick(context)
                    val shareText = buildString {
                        append("📊 TaskFlow ${report.period.title}\n")
                        append("⏱️ Total Hours Logged: ${report.totalHoursLogged} hrs\n")
                        append("✅ Task Completion: ${report.completedTasksCount}/${report.totalTasksCount} (${report.completionRatePercentage}%)\n")
                        append("🎯 Active Focus Sessions: ${report.activeFocusSessions} completed\n")
                        append("📈 MoM Trend: ${if (report.growthPercentage >= 0) "+" else ""}${report.growthPercentage}%\n\n")
                        append("Domain Breakdown:\n")
                        report.domainMetrics.forEach { metric ->
                            append("• ${metric.domain.emoji} ${metric.domain.title}: ${metric.completedTasks}/${metric.totalTasks} tasks (${metric.percentageOfTotal.toInt()}%)\n")
                        }
                        append("\n💡 ${report.adaptiveFeedbackTitle}\n${report.adaptiveFeedbackMessage}")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Retrospective Report"))
                }
            )
        },
        containerColor = TFTheme.colors.canvas
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Period Segmented Switcher (Weekly vs Monthly)
            item(key = "period_selector") {
                RetrospectivePeriodSwitcher(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { newPeriod ->
                        HapticManager.performClick(context)
                        selectedPeriod = newPeriod
                        hasClaimedReward = false
                    }
                )
            }

            // Total Work Output Bento Hero
            item(key = "work_output_hero") {
                TotalWorkOutputSection(report = report)
            }

            // Adaptive Feedback & Growth/Support Banner
            item(key = "adaptive_feedback_banner") {
                AdaptiveFeedbackCard(
                    report = report,
                    hasClaimed = hasClaimedReward,
                    onClaimReward = {
                        if (!hasClaimedReward && report.rewardPointsBonus > 0) {
                            HapticManager.performSuccess(context)
                            UserProfileManager.addXp(report.rewardPointsBonus)
                            hasClaimedReward = true
                            Toast.makeText(
                                context,
                                "Claimed +${report.rewardPointsBonus} XP bonus points!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            // Month-over-Month Trend Growth Analysis Card
            item(key = "mom_growth_card") {
                MonthOverMonthTrendCard(report = report)
            }

            // Life Domain Breakdown Distribution
            item(key = "domain_breakdown_section") {
                LifeDomainBreakdownCard(report = report)
            }

            // Bottom space
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RetrospectiveTopBar(
    title: String,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        color = TFTheme.colors.card.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onBackClick()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SystemBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = title,
                    style = TFTheme.typography.headline,
                    fontWeight = FontWeight.Bold,
                    color = TFTheme.colors.labelPrimary
                )

                IconButton(
                    onClick = {
                        HapticManager.performClick(context)
                        onShareClick()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = SystemBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RetrospectivePeriodSwitcher(
    selectedPeriod: ReviewPeriod,
    onPeriodSelected: (ReviewPeriod) -> Unit
) {
    val isDark = TFTheme.colors.isDark
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E5EA),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReviewPeriod.entries.forEach { period ->
                val isSelected = selectedPeriod == period
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected) {
                                if (isDark) Color(0xFF2C2C2E) else Color.White
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onPeriodSelected(period)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period.title,
                        style = TFTheme.typography.subheadline,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TFTheme.colors.labelPrimary else TFTheme.colors.labelSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalWorkOutputSection(report: RetrospectiveReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "TOTAL WORK OUTPUT",
            style = TFTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = TFTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hours Logged
            WorkOutputBentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                iconTint = SystemBlue,
                value = "${report.totalHoursLogged}h",
                label = "Hours Logged",
                subtext = "Focused output"
            )

            // Completion Rate
            WorkOutputBentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
                iconTint = AppleHealth,
                value = "${report.completionRatePercentage}%",
                label = "Task Completion",
                subtext = "${report.completedTasksCount}/${report.totalTasksCount} completed"
            )

            // Active Focus Sessions
            WorkOutputBentoCell(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.LocalFireDepartment,
                iconTint = AppleStudy,
                value = "${report.activeFocusSessions}",
                label = "Focus Sessions",
                subtext = "Pomodoro intervals"
            )
        }
    }
}

@Composable
private fun WorkOutputBentoCell(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    subtext: String
) {
    val isDark = TFTheme.colors.isDark
    Surface(
        shape = TFShape.card,
        color = TFTheme.colors.card,
        border = if (isDark) androidx.compose.foundation.BorderStroke(hairline(), TFTheme.colors.cardStroke) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TFTheme.colors.labelPrimary
            )

            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TFTheme.colors.labelPrimary,
                maxLines = 1
            )

            Text(
                text = subtext,
                fontSize = 10.sp,
                color = TFTheme.colors.labelSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MonthOverMonthTrendCard(report: RetrospectiveReport) {
    val isDark = TFTheme.colors.isDark
    val trendColor = when (report.trend) {
        GrowthTrend.GROWTH -> AppleHealth
        GrowthTrend.DIP -> AppleStudy
        GrowthTrend.CONSISTENT -> SystemBlue
    }

    val trendIcon = when (report.trend) {
        GrowthTrend.GROWTH -> Icons.AutoMirrored.Outlined.TrendingUp
        GrowthTrend.DIP -> Icons.AutoMirrored.Outlined.TrendingDown
        GrowthTrend.CONSISTENT -> Icons.AutoMirrored.Outlined.TrendingFlat
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "PERIOD GROWTH & MOMENTUM",
            style = TFTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = TFTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            shape = TFShape.card,
            color = TFTheme.colors.card,
            border = if (isDark) androidx.compose.foundation.BorderStroke(hairline(), TFTheme.colors.cardStroke) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${report.period.comparisonLabel} Comparison",
                            style = TFTheme.typography.subheadline,
                            fontWeight = FontWeight.Bold,
                            color = TFTheme.colors.labelPrimary
                        )
                        Text(
                            text = "Normalized rate vs previous period baseline",
                            style = TFTheme.typography.caption,
                            color = TFTheme.colors.labelSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = trendColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${if (report.growthPercentage >= 0) "+" else ""}${report.growthPercentage}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                }

                // Progress Bar comparison
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = (report.completionRatePercentage / 100f).coerceIn(0f, 1f),
                        label = "TrendProgress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(trendColor.copy(alpha = 0.7f), trendColor)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Prior Baseline: ~70%",
                            fontSize = 11.sp,
                            color = TFTheme.colors.labelSecondary
                        )
                        Text(
                            text = "Current: ${report.completionRatePercentage}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = trendColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveFeedbackCard(
    report: RetrospectiveReport,
    hasClaimed: Boolean,
    onClaimReward: () -> Unit
) {
    val isDark = TFTheme.colors.isDark
    val isGrowth = report.trend == GrowthTrend.GROWTH
    val isDip = report.trend == GrowthTrend.DIP

    val bannerColor = when {
        isGrowth -> AppleHealth
        isDip -> AppleStudy
        else -> SystemBlue
    }

    Surface(
        shape = TFShape.card,
        color = bannerColor.copy(alpha = if (isDark) 0.12f else 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(bannerColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGrowth) "🏆" else if (isDip) "🌿" else "⚡",
                        fontSize = 18.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.adaptiveFeedbackTitle,
                        style = TFTheme.typography.subheadline,
                        fontWeight = FontWeight.Bold,
                        color = TFTheme.colors.labelPrimary
                    )
                    report.badgeUnlockedTitle?.let { badgeTitle ->
                        Text(
                            text = "Badge Unlocked: $badgeTitle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = bannerColor
                        )
                    }
                }
            }

            Text(
                text = report.adaptiveFeedbackMessage,
                style = TFTheme.typography.footnote,
                color = TFTheme.colors.labelSecondary,
                lineHeight = 18.sp
            )

            // Supportive Gratitude Prompt if Dip is detected (Non-punitive)
            if (isDip && report.adaptiveGratitudePrompt != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TFTheme.colors.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🤍", fontSize = 16.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Gratitude Reflection",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TFTheme.colors.labelPrimary
                            )
                            Text(
                                text = report.adaptiveGratitudePrompt,
                                fontSize = 11.sp,
                                color = TFTheme.colors.labelSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Claim Reward Points Button
            if (report.rewardPointsBonus > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onClaimReward,
                        enabled = !hasClaimed,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = bannerColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (hasClaimed) "✓ Bonus Claimed" else "Claim +${report.rewardPointsBonus} XP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LifeDomainBreakdownCard(report: RetrospectiveReport) {
    val isDark = TFTheme.colors.isDark

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "DOMAIN BREAKDOWN",
            style = TFTheme.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = TFTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Surface(
            shape = TFShape.card,
            color = TFTheme.colors.card,
            border = if (isDark) androidx.compose.foundation.BorderStroke(hairline(), TFTheme.colors.cardStroke) else null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Stacked Horizontal Distribution Bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Life Balance Distribution",
                        style = TFTheme.typography.subheadline,
                        fontWeight = FontWeight.Bold,
                        color = TFTheme.colors.labelPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
                    ) {
                        val totalPct = report.domainMetrics.sumOf { it.percentageOfTotal.toDouble() }.toFloat()
                        if (totalPct > 0f) {
                            report.domainMetrics.forEach { metric ->
                                if (metric.percentageOfTotal > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(metric.percentageOfTotal)
                                            .background(metric.domain.getColor())
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(SystemGray4)
                            )
                        }
                    }
                }

                // Domain rows list with hairlines
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    report.domainMetrics.forEachIndexed { index, metric ->
                        DomainMetricRow(metric = metric)
                        if (index < report.domainMetrics.size - 1) {
                            HorizontalDivider(
                                color = TFTheme.colors.separator,
                                thickness = hairline(),
                                modifier = Modifier.padding(start = 42.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainMetricRow(metric: DomainMetric) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(metric.domain.getColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = metric.domain.emoji, fontSize = 16.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.domain.title,
                style = TFTheme.typography.subheadline,
                fontWeight = FontWeight.SemiBold,
                color = TFTheme.colors.labelPrimary
            )
            Text(
                text = "${metric.completedTasks} of ${metric.totalTasks} tasks done • ${metric.estimatedHours}h logged",
                style = TFTheme.typography.caption,
                color = TFTheme.colors.labelSecondary
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = metric.domain.getColor().copy(alpha = 0.12f)
        ) {
            Text(
                text = "${metric.percentageOfTotal.toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = metric.domain.getColor(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}
