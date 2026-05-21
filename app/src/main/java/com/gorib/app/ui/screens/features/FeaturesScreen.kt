package com.gorib.app.ui.screens.features

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

/**
 * Data class representing an application feature.
 */
data class AppFeature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val emoji: String,
    val accentColor: Color,
    val descriptionPoints: List<String>
)

@Composable
fun FeaturesScreen(
    navController: NavController
) {
    val scrollState = rememberScrollState()

    // Definition of all application features in simple, easy to understand English
    val featuresList = remember {
        listOf(
            AppFeature(
                title = "Home Overview",
                subtitle = "Your central command center for daily finance tracking.",
                icon = Icons.Default.Home,
                emoji = "🏠",
                accentColor = Color(0xFF6200EE), // Premium purple
                descriptionPoints = listOf(
                    "Displays your total monthly budget at a single glance.",
                    "Quickly see how much money you spent and what is remaining.",
                    "Shows beautiful progress bars highlighting the status of each budget category.",
                    "Provides instant access to recent transaction lists and smart shortcut actions."
                )
            ),
            AppFeature(
                title = "Smart Analytics & Charts",
                subtitle = "Beautiful interactive visual insights into your spending.",
                icon = Icons.Default.Star,
                emoji = "📊",
                accentColor = Color(0xFF03DAC6), // Teal
                descriptionPoints = listOf(
                    "Compare this month's spending vs. last month with automatic percentage calculations.",
                    "View a detailed breakdown of expenditures by category.",
                    "Interactive 'Daily Spending' bar chart that lets you tap any day to see details.",
                    "Identify your biggest spending areas with the 'Top Spending' merchant ranking list."
                )
            ),
            AppFeature(
                title = "Smart Groceries & AI OCR Scanner",
                subtitle = "Manage lists and automatically parse receipts with Gemini AI.",
                icon = Icons.Default.ShoppingCart,
                emoji = "🛒",
                accentColor = Color(0xFFFF9800), // Rich orange
                descriptionPoints = listOf(
                    "Create grocery sessions to bundle related shopping list items.",
                    "AI Receipt Scanner: Snap a picture of your physical receipt or upload an image.",
                    "Advanced Gemini AI: Automatically processes the receipt layout in seconds!",
                    "Extracts item names, weights/volumes (e.g. 550G), quantities, and final prices without manual entry.",
                    "Review extracted items in a clear editor and save them directly as categorized budget transactions."
                )
            ),
            AppFeature(
                title = "Custom Budget Categories",
                subtitle = "Complete control over how your money is organized.",
                icon = Icons.Default.List,
                emoji = "🏷️",
                accentColor = Color(0xFFE91E63), // Pink
                descriptionPoints = listOf(
                    "View a detailed analysis of transactions under specific categories.",
                    "Configure monthly budget limits for each category to prevent overspending.",
                    "Custom Keyword Override: Set custom words that automatically sort items into correct folders.",
                    "Maintains history of all items bought under that category for simple auditing."
                )
            ),
            AppFeature(
                title = "Rent & Bills Automation",
                subtitle = "Never forget a rental payment or utility bill again.",
                icon = Icons.Default.DateRange,
                emoji = "📅",
                accentColor = Color(0xFF2196F3), // Elegant Blue
                descriptionPoints = listOf(
                    "Configure your monthly rent amount, due date, and landlord bank details.",
                    "Record rent status dynamically and track utility allocations automatically.",
                    "Organize utilities into bill groups (like Electricity, Water, Internet, Sewerage).",
                    "Add individual line items, track their payment dates, and visualize your utility costs over time."
                )
            ),
            AppFeature(
                title = "Recurring Expenses",
                subtitle = "Keep constant tabs on subscriptions and automated payments.",
                icon = Icons.Default.Refresh,
                emoji = "🔁",
                accentColor = Color(0xFF4CAF50), // Fresh Green
                descriptionPoints = listOf(
                    "Manage ongoing commitments like Netflix, Spotify, gym memberships, or insurance.",
                    "Define custom recurring intervals (weekly, monthly, annually).",
                    "Set due dates, category associations, and bill descriptions.",
                    "Generates automatic database entries to keep logs accurate over time."
                )
            ),
            AppFeature(
                title = "Step-by-Step Onboarding Wizard",
                subtitle = "Guided assistant for first-time application configuration.",
                icon = Icons.Default.CheckCircle,
                emoji = "🧙",
                accentColor = Color(0xFF9C27B0), // Purple-violet
                descriptionPoints = listOf(
                    "Launches automatically on your first app opening to set up your profile.",
                    "Configure your nickname and local currency symbol (e.g. RM, $, €).",
                    "Enter your estimated monthly income and general monthly savings goal.",
                    "Set up initial Rent and active Utilities to populate your dashboard dynamically.",
                    "Calculates your safe initial monthly budget limit based on your targets."
                )
            ),
            AppFeature(
                title = "Dark Theme Support",
                subtitle = "Save battery life and reduce eye strain in low light.",
                icon = Icons.Default.Settings,
                emoji = "🌙",
                accentColor = Color(0xFF607D8B), // Steel blue
                descriptionPoints = listOf(
                    "Full, gorgeous system-wide dark mode interface.",
                    "Adapts theme colors seamlessly between day and night modes.",
                    "Preserves high contrast ratios to ensure maximum readability and accessibility.",
                    "Easily toggled directly from the main Settings dashboard."
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 88.dp) // extra padding to avoid being covered by bottom bar
    ) {
        // --- Premium Header Banner ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GORIB Features",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Learn about the powerful tools built to simplify your finances",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }

        // --- Features List container ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Interactive Feature Guide",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            featuresList.forEach { feature ->
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isExpanded = !isExpanded }
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isExpanded) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isExpanded) feature.accentColor else MaterialTheme.colorScheme.outlineVariant
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isExpanded) 4.dp else 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Emoji / Icon Circle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(feature.accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = feature.emoji,
                                    fontSize = 22.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = feature.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Expanded description list
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                feature.descriptionPoints.forEach { point ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "•",
                                            color = feature.accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Friendly Footer ---
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "💡 Pro Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Did you know? GORIB means 'Poor' in Malay/Bengali. It represents our core mission: helping you stay budget-conscious, save smartly, and build healthy financial habits so you're never left feeling gorib!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
