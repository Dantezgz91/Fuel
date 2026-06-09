@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.fueltracker.app.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.util.Locale
import com.fueltracker.app.R
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.HomeListMode
import com.fueltracker.app.domain.model.PriceTrend
import com.fueltracker.app.domain.model.ScheduleOpenStatus
import com.fueltracker.app.domain.model.StationDisplayItem
import com.fueltracker.app.domain.model.StationRecommendation
import com.fueltracker.app.domain.model.StationSortMode
import com.fueltracker.app.domain.util.GeoUtils
import com.fueltracker.app.ui.components.FuelTypeChipRow
import com.fueltracker.app.ui.theme.PriceBad
import com.fueltracker.app.ui.theme.PriceGood
import com.fueltracker.app.ui.theme.PriceMedium
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    var expandedStationId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(uiState.needsLocationPermission) {
        if (uiState.needsLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.locationMessage) {
        uiState.locationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLocationMessage()
        }
    }

    val recommendationsByStation = remember(uiState.stationRecommendations) {
        uiState.stationRecommendations.associateBy { it.stationId }
    }

    val showSortBar = uiState.listMode == HomeListMode.FAVORITES_NEAR_HOME &&
        uiState.displayStations.isNotEmpty()

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (uiState.isReorderMode) {
            viewModel.moveStation(from.index, to.index)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(uiState.isReorderMode, uiState.listMode) {
        if (uiState.isReorderMode || uiState.listMode == HomeListMode.TRAVEL) {
            expandedStationId = null
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.visibleFuelTypes.isNotEmpty()) {
                    FuelTypeChipRow(
                        configs = uiState.visibleFuelTypes,
                        selected = uiState.selectedFuelType,
                        onSelect = viewModel::selectFuelType
                    )
                }

                HomeListModeBar(
                    listMode = uiState.listMode,
                    isLoadingTravel = uiState.isLoadingTravel,
                    onSelect = viewModel::setListMode,
                    onRefreshTravel = viewModel::refreshTravelStations
                )

                if (uiState.listMode == HomeListMode.FAVORITES_NEAR_HOME &&
                    !uiState.homeConfigured &&
                    uiState.displayStations.isNotEmpty()
                ) {
                    HomeConfigHintBanner()
                }

                if (uiState.listMode == HomeListMode.TRAVEL &&
                    uiState.isLoadingTravel &&
                    uiState.displayStations.isEmpty()
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Buscando gasolineras cercanas…")
                        }
                    }
                }

                if (showSortBar) {
                    if (uiState.isReorderMode) {
                        ReorderModeHint(onDone = viewModel::exitReorderMode)
                    } else {
                        StationSortBar(
                            sortMode = uiState.sortMode,
                            onSelect = viewModel::setSortMode
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.displayStations.isEmpty() && !uiState.isLoadingTravel) {
                    item(key = "empty") {
                        EmptyStateCard(
                            listMode = uiState.listMode,
                            travelRadiusKm = uiState.travelRadiusKm,
                            selectedFuelType = uiState.selectedFuelType,
                            emptyDueToFuelFilter = uiState.emptyDueToFuelFilter
                        )
                    }
                } else {
                    items(uiState.displayStations, key = { it.station.id }) { item ->
                        val station = item.station
                        val isCheapest = station.id == uiState.cheapestStation?.id
                        if (uiState.isReorderMode) {
                            ReorderableItem(reorderableState, key = station.id) { isDragging ->
                                val elevation by animateDpAsState(
                                    if (isDragging) 6.dp else 0.dp,
                                    label = "dragElevation"
                                )
                                Surface(shadowElevation = elevation) {
                                    ExpandableStationCard(
                                        item = item,
                                        fuelType = uiState.selectedFuelType,
                                        recommendation = recommendationsByStation[station.id],
                                        isCheapest = isCheapest,
                                        expanded = false,
                                        isReorderMode = true,
                                        reorderScope = this,
                                        onToggle = {},
                                        onLongPress = {}
                                    )
                                }
                            }
                        } else {
                            ExpandableStationCard(
                                item = item,
                                fuelType = uiState.selectedFuelType,
                                recommendation = recommendationsByStation[station.id],
                                isCheapest = isCheapest,
                                expanded = expandedStationId == station.id,
                                isReorderMode = false,
                                reorderScope = null,
                                onToggle = {
                                    expandedStationId =
                                        if (expandedStationId == station.id) null else station.id
                                },
                                onLongPress = {
                                    if (uiState.listMode == HomeListMode.FAVORITES_NEAR_HOME) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.enterReorderMode()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeListModeBar(
    listMode: HomeListMode,
    isLoadingTravel: Boolean,
    onSelect: (HomeListMode) -> Unit,
    onRefreshTravel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
            SegmentedButton(
                selected = listMode == HomeListMode.FAVORITES_NEAR_HOME,
                onClick = { onSelect(HomeListMode.FAVORITES_NEAR_HOME) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Favoritas") }
            )
            SegmentedButton(
                selected = listMode == HomeListMode.TRAVEL,
                onClick = { onSelect(HomeListMode.TRAVEL) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Cercanas") }
            )
        }
        AnimatedVisibility(visible = listMode == HomeListMode.TRAVEL) {
            FilledTonalIconButton(
                onClick = onRefreshTravel,
                enabled = !isLoadingTravel,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                if (isLoadingTravel) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_sync),
                        contentDescription = "Actualizar cercanas",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeConfigHintBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(
                "Configura tu casa en Ajustes para ver la distancia a tus favoritas.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StationSortBar(
    sortMode: StationSortMode,
    onSelect: (StationSortMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Ordenar por",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        val options = listOf(
            StationSortMode.BY_PRICE to "Precio",
            StationSortMode.CUSTOM to "Personalizado"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = sortMode == mode,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Mantén pulsada una fila para reordenar manualmente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReorderModeHint(onDone: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Arrastra las filas arriba o abajo.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(onClick = onDone) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OpenStatusBadge(status: ScheduleOpenStatus) {
    val (label, color) = when (status) {
        ScheduleOpenStatus.OPEN, ScheduleOpenStatus.ALWAYS_OPEN -> "Abierta" to PriceGood
        ScheduleOpenStatus.CLOSED -> "Cerrada" to PriceBad
        ScheduleOpenStatus.UNKNOWN -> return
    }
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ExpandableStationCard(
    item: StationDisplayItem,
    fuelType: FuelType,
    recommendation: StationRecommendation?,
    isCheapest: Boolean,
    expanded: Boolean,
    isReorderMode: Boolean,
    reorderScope: ReorderableCollectionItemScope?,
    onToggle: () -> Unit,
    onLongPress: () -> Unit
) {
    val station = item.station
    val haptic = LocalHapticFeedback.current
    val price = station.currentPrices[fuelType]
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isReorderMode) {
                    Modifier.combinedClickable(
                        onClick = onToggle,
                        onLongClick = onLongPress
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isReorderMode) {
                    val dragModifier = if (reorderScope != null) {
                        with(reorderScope) {
                            Modifier.draggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Arrastrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = dragModifier
                            .size(40.dp)
                            .padding(8.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    painterResource(R.drawable.ic_local_gas_station),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(station.name, fontWeight = FontWeight.SemiBold)
                        if (isCheapest) {
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Más barata",
                                style = MaterialTheme.typography.labelSmall,
                                color = PriceGood,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        item.distanceKm?.let { distance ->
                            Text(
                                GeoUtils.formatDistanceKm(distance),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        OpenStatusBadge(item.openStatus)
                    }
                    Text(
                        station.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (price != null) {
                    Text(
                        String.format(Locale.US, "%.3f", price),
                        fontWeight = FontWeight.Bold,
                        color = if (isCheapest) PriceGood else MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                } else {
                    Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isReorderMode) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Contraer" else "Expandir"
                    )
                }
            }

            if (!isReorderMode) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    ) {
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        station.schedule?.let { schedule ->
                            Text(
                                "Horario: $schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (recommendation != null) {
                            StationRecommendationContent(recommendation)
                        } else if (station.isTracked) {
                            Text(
                                "Aún no hay suficiente historial para recomendar un día de la semana.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRecommendationContent(recommendation: StationRecommendation) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Recomendación semanal", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TrendIcon(recommendation.trend)
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar_today),
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text("Mejor día: ", style = MaterialTheme.typography.bodySmall)
        Text(
            recommendation.bestDayLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = PriceGood
        )
    }
    if (recommendation.expectedSaving > 0.001) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Ahorro estimado: ${String.format(Locale.US, "%.3f", recommendation.expectedSaving)}",
            style = MaterialTheme.typography.bodySmall,
            color = PriceGood
        )
    }
    Spacer(Modifier.height(6.dp))
    Text(
        recommendation.detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (recommendation.confidence < 0.5f) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(12.dp), tint = PriceMedium)
            Spacer(Modifier.width(4.dp))
            Text(
                "Pocos datos todavía; la recomendación mejorará con el tiempo.",
                style = MaterialTheme.typography.labelSmall,
                color = PriceMedium
            )
        }
    }
}

@Composable
private fun TrendIcon(trend: PriceTrend) {
    when (trend) {
        PriceTrend.RISING -> Icon(Icons.Default.KeyboardArrowUp, contentDescription = trend.name, tint = PriceBad, modifier = Modifier.size(16.dp))
        PriceTrend.FALLING -> Icon(Icons.Default.KeyboardArrowDown, contentDescription = trend.name, tint = PriceGood, modifier = Modifier.size(16.dp))
        PriceTrend.STABLE -> Icon(painter = painterResource(R.drawable.ic_horizontal_rule), contentDescription = trend.name, tint = PriceMedium, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun EmptyStateCard(
    listMode: HomeListMode,
    travelRadiusKm: Int,
    selectedFuelType: FuelType,
    emptyDueToFuelFilter: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painterResource(R.drawable.ic_local_gas_station),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            if (emptyDueToFuelFilter) {
                Text(
                    "Sin ${selectedFuelType.displayName.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (listMode == HomeListMode.TRAVEL) {
                        "Hay gasolineras cercanas, pero ninguna ofrece este combustible."
                    } else {
                        "Tus gasolineras seguidas no ofrecen este combustible."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else when (listMode) {
                HomeListMode.FAVORITES_NEAR_HOME -> {
                    Text(
                        "Sin gasolineras seguidas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ve a Gasolineras para buscar y seguir gasolineras. Configura tu casa en Ajustes para ver la distancia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                HomeListMode.TRAVEL -> {
                    Text(
                        "Sin gasolineras cercanas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "No se encontraron gasolineras en un radio de $travelRadiusKm km.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
