package com.fueltracker.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.res.painterResource
import com.fueltracker.app.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fueltracker.app.domain.model.DayOfMonthPattern
import com.fueltracker.app.domain.model.DayOfWeekPattern
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.ui.components.FuelTypeChipRow
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.WeekOfMonthPattern
import com.fueltracker.app.ui.theme.PriceBad
import com.fueltracker.app.ui.theme.PriceGood
import com.fueltracker.app.ui.theme.PriceMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.visibleFuelTypes.isNotEmpty()) {
                item {
                    FuelTypeChipRow(
                        configs = uiState.visibleFuelTypes,
                        selected = uiState.selectedFuelType,
                        onSelect = viewModel::selectFuelType
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }

            val analysis = uiState.analysis
            if (analysis == null || analysis.dayOfWeekPatterns.isEmpty()) {
                item {
                    EmptyAnalyticsCard()
                }
                return@LazyColumn
            }

            item {
                SummaryCard(analysis = analysis)
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    edgePadding = 0.dp
                ) {
                    AnalyticsTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }

            item {
                when (uiState.selectedTab) {
                    AnalyticsTab.DAY_OF_WEEK -> DayOfWeekChart(
                        patterns = analysis.dayOfWeekPatterns,
                        best = analysis.bestDayOfWeek,
                        worst = analysis.worstDayOfWeek
                    )
                    AnalyticsTab.DAY_OF_MONTH -> DayOfMonthChart(
                        patterns = analysis.dayOfMonthPatterns
                    )
                    AnalyticsTab.WEEK_OF_MONTH -> WeekOfMonthChart(
                        patterns = analysis.weekOfMonthPatterns
                    )
                }
            }

            item {
                InsightsCard(analysis = analysis)
            }
        }
    }
}

@Composable
private fun SummaryCard(analysis: PriceAnalysis) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resumen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("Mínimo", "${String.format("%.3f", analysis.overallMin)} €/L", PriceGood)
                SummaryItem("Promedio", "${String.format("%.3f", analysis.overallAvg)} €/L", PriceMedium)
                SummaryItem("Máximo", "${String.format("%.3f", analysis.overallMax)} €/L", PriceBad)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (color, label) = when (analysis.trend) {
                    PriceTrend.RISING -> PriceBad to "Tendencia: Subiendo"
                    PriceTrend.FALLING -> PriceGood to "Tendencia: Bajando"
                    PriceTrend.STABLE -> PriceMedium to "Tendencia: Estable"
                }
                when (analysis.trend) {
                    PriceTrend.RISING -> Icon(Icons.Default.KeyboardArrowUp, null, tint = color, modifier = Modifier.size(16.dp))
                    PriceTrend.FALLING -> Icon(Icons.Default.KeyboardArrowDown, null, tint = color, modifier = Modifier.size(16.dp))
                    PriceTrend.STABLE -> Icon(painter = painterResource(R.drawable.ic_horizontal_rule), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayOfWeekChart(
    patterns: List<DayOfWeekPattern>,
    best: DayOfWeekPattern?,
    worst: DayOfWeekPattern?
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Por día de la semana", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Media por día de la semana (cada punto = un día con precio oficial)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (patterns.isEmpty()) {
                Text("Sin datos suficientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxPrice = patterns.maxOf { it.averagePrice }
                val minPrice = patterns.minOf { it.averagePrice }
                val range = if (maxPrice - minPrice > 0.001) maxPrice - minPrice else 0.01

                BarChart(
                    bars = patterns.map { p ->
                        BarData(
                            label = p.dayName.take(3),
                            value = p.averagePrice,
                            normalizedValue = ((p.averagePrice - minPrice) / range).toFloat(),
                            color = when {
                                p == best -> PriceGood
                                p == worst -> PriceBad
                                else -> PriceMedium
                            },
                            subtitle = String.format("%.3f", p.averagePrice)
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    best?.let {
                        LegendItem("Mejor: ${it.dayName}", PriceGood)
                    }
                    worst?.let {
                        LegendItem("Peor: ${it.dayName}", PriceBad)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfMonthChart(patterns: List<DayOfMonthPattern>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Por día del mes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (patterns.isEmpty()) {
                Text("Sin datos suficientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxPrice = patterns.maxOf { it.averagePrice }
                val minPrice = patterns.minOf { it.averagePrice }
                val range = if (maxPrice - minPrice > 0.001) maxPrice - minPrice else 0.01
                val best = patterns.minByOrNull { it.averagePrice }

                BarChart(
                    bars = patterns.map { p ->
                        BarData(
                            label = "${p.dayOfMonth}",
                            value = p.averagePrice,
                            normalizedValue = ((p.averagePrice - minPrice) / range).toFloat(),
                            color = if (p == best) PriceGood else PriceMedium,
                            subtitle = ""
                        )
                    }
                )
                best?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mejor día: día ${it.dayOfMonth} (${String.format("%.3f", it.averagePrice)} €/L)",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceGood
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekOfMonthChart(patterns: List<WeekOfMonthPattern>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Por semana del mes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (patterns.isEmpty()) {
                Text("Sin datos suficientes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxPrice = patterns.maxOf { it.averagePrice }
                val minPrice = patterns.minOf { it.averagePrice }
                val range = if (maxPrice - minPrice > 0.001) maxPrice - minPrice else 0.01
                val best = patterns.minByOrNull { it.averagePrice }

                BarChart(
                    bars = patterns.map { p ->
                        BarData(
                            label = p.weekLabel,
                            value = p.averagePrice,
                            normalizedValue = ((p.averagePrice - minPrice) / range).toFloat(),
                            color = if (p == best) PriceGood else PriceMedium,
                            subtitle = String.format("%.3f", p.averagePrice)
                        )
                    }
                )
            }
        }
    }
}

data class BarData(
    val label: String,
    val value: Double,
    val normalizedValue: Float,
    val color: Color,
    val subtitle: String
)

@Composable
private fun BarChart(bars: List<BarData>) {
    val barColor = MaterialTheme.colorScheme.primary
    val maxBarHeight = 120.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                if (bar.subtitle.isNotEmpty()) {
                    Text(
                        bar.subtitle,
                        fontSize = 7.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                val height = maxBarHeight * (0.2f + bar.normalizedValue * 0.8f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(24.dp)
                        .height(height)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(bar.color)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bar.label,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun InsightsCard(analysis: PriceAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(R.drawable.ic_analytics), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Insights", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            analysis.bestDayOfWeek?.let { best ->
                InsightRow("✓ Mejor día para repostar:", best.dayName, PriceGood)
            }
            analysis.worstDayOfWeek?.let { worst ->
                InsightRow("✗ Peor día para repostar:", worst.dayName, PriceBad)
            }
            analysis.bestDayOfMonth?.let { best ->
                InsightRow("✓ Mejor día del mes:", "Día ${best.dayOfMonth}", PriceGood)
            }

            val savings = if (analysis.worstDayOfWeek != null && analysis.bestDayOfWeek != null) {
                analysis.worstDayOfWeek.averagePrice - analysis.bestDayOfWeek.averagePrice
            } else 0.0

            if (savings > 0.001) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ahorro potencial eligiendo el día adecuado: ${String.format("%.3f", savings)} €/L",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceGood
                )
                Text(
                    "En un depósito de 50L: ${String.format("%.2f", savings * 50)} €",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceGood,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InsightRow(label: String, value: String, color: Color) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun EmptyAnalyticsCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_analytics),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sin datos para analizar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sigue gasolineras y actualiza los precios regularmente para ver patrones a lo largo del tiempo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
