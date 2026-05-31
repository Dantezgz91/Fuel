package com.fueltracker.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.Recommendation
import com.fueltracker.app.ui.theme.PriceBad
import com.fueltracker.app.ui.theme.PriceGood
import com.fueltracker.app.ui.theme.PriceMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) { viewModel.refresh() }
    }
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) pullRefreshState.endRefresh()
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("FuelTracker", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FuelTypeSelector(
                        selected = uiState.selectedFuelType,
                        onSelect = viewModel::selectFuelType
                    )
                }

                item {
                    uiState.recommendation?.let { rec ->
                        RecommendationCard(
                            recommendation = rec,
                            trend = uiState.analysis?.trend
                        )
                    } ?: run {
                        if (uiState.trackedStations.isEmpty()) {
                            EmptyStateCard()
                        }
                    }
                }

                if (uiState.cheapestStation != null) {
                    item {
                        CheapestStationCard(
                            station = uiState.cheapestStation!!,
                            price = uiState.cheapestPrice!!,
                            fuelType = uiState.selectedFuelType
                        )
                    }
                }

                if (uiState.trackedStations.isNotEmpty()) {
                    item {
                        Text(
                            "Mis gasolineras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(uiState.trackedStations) { station ->
                        StationPriceCard(station = station, fuelType = uiState.selectedFuelType)
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FuelTypeSelector(selected: FuelType, onSelect: (FuelType) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(FuelType.entries) { fuelType ->
            FilterChip(
                selected = selected == fuelType,
                onClick = { onSelect(fuelType) },
                label = { Text(fuelType.displayName, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: Recommendation, trend: PriceTrend?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Recomendación",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                trend?.let { TrendIcon(it) }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "Mejor día: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    recommendation.bestDayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PriceGood
                )
            }
            if (recommendation.expectedSaving > 0.001) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ahorro estimado: ${String.format("%.3f", recommendation.expectedSaving)} €/L",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceGood
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                recommendation.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (recommendation.confidence < 0.5f) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(12.dp), tint = PriceMedium)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Datos insuficientes. La recomendación mejorará con el tiempo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PriceMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendIcon(trend: PriceTrend) {
    val (icon, color) = when (trend) {
        PriceTrend.RISING -> Icons.Default.ArrowUpward to PriceBad
        PriceTrend.FALLING -> Icons.Default.ArrowDownward to PriceGood
        PriceTrend.STABLE -> Icons.Default.HorizontalRule to PriceMedium
    }
    Icon(icon, contentDescription = trend.name, tint = color, modifier = Modifier.size(16.dp))
}

@Composable
private fun CheapestStationCard(station: GasStation, price: Double, fuelType: FuelType) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PriceGood.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Más barata ahora",
                style = MaterialTheme.typography.labelMedium,
                color = PriceGood,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalGasStation, null, tint = PriceGood)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(station.name, fontWeight = FontWeight.Bold)
                    Text(station.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${String.format("%.3f", price)} €/L",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PriceGood
                )
            }
        }
    }
}

@Composable
private fun StationPriceCard(station: GasStation, fuelType: FuelType) {
    val price = station.currentPrices[fuelType]
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalGasStation, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, fontWeight = FontWeight.SemiBold)
                Text(
                    station.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (price != null) {
                Text(
                    "${String.format("%.3f", price)} €/L",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            } else {
                Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.LocalGasStation,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sin gasolineras seguidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ve a la pestaña Gasolineras para buscar y seguir gasolineras de tu municipio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
