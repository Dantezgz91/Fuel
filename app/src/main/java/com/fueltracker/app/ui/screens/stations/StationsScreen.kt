package com.fueltracker.app.ui.screens.stations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import com.fueltracker.app.R
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fueltracker.app.domain.model.GasStation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    viewModel: StationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var searchActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            DockedSearchBar(
                query = uiState.municipalityQuery,
                onQueryChange = { query ->
                    viewModel.onMunicipalityQueryChange(query)
                    if (!searchActive) {
                        viewModel.loadMunicipalities()
                        searchActive = true
                    }
                },
                onSearch = { searchActive = false },
                active = searchActive,
                onActiveChange = { active ->
                    searchActive = active
                    if (active) viewModel.loadMunicipalities()
                },
                placeholder = { Text("Buscar municipio...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.municipalityQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onMunicipalityQueryChange("") }) {
                            Icon(Icons.Default.Clear, "Limpiar")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSearchingMunicipalities) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (uiState.filteredMunicipalities.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (uiState.municipalities.isEmpty()) {
                                "No se pudieron cargar los municipios. Comprueba la conexión."
                            } else if (uiState.municipalityQuery.trim().length < 3) {
                                "Escribe al menos 3 letras para buscar municipios"
                            } else {
                                "Ningún municipio coincide con \"${uiState.municipalityQuery}\""
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(uiState.filteredMunicipalities, key = { "municipality-${it.id}" }) { municipality ->
                            ListItem(
                                headlineContent = { Text(municipality.name) },
                                supportingContent = {
                                    Text("${municipality.province} · ${municipality.autonomousCommunity}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        viewModel.selectMunicipality(municipality)
                                        searchActive = false
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val stationQuery = uiState.stationQuery.trim()
            val trackedIds = remember(uiState.trackedStations) {
                uiState.trackedStations.map { it.id }.toSet()
            }
            val visibleStations = remember(uiState.searchResults, stationQuery, trackedIds) {
                val deduped = uiState.searchResults
                    .filter { it.id.isNotBlank() }
                    .distinctBy { it.id }
                val notAlreadyListed = deduped.filter { it.id !in trackedIds }
                if (stationQuery.isBlank()) {
                    notAlreadyListed
                } else {
                    notAlreadyListed.filter { station ->
                        station.name.contains(stationQuery, ignoreCase = true) ||
                            station.address.contains(stationQuery, ignoreCase = true)
                    }
                }
            }

            if (uiState.searchResults.isNotEmpty() && !uiState.isLoadingStations) {
                Text(
                    "Resultados en ${uiState.selectedMunicipality?.name} (${visibleStations.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.stationQuery,
                    onValueChange = viewModel::onStationQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Filtrar por nombre o dirección...") },
                    trailingIcon = {
                        if (uiState.stationQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onStationQueryChange("") }) {
                                Icon(Icons.Default.Clear, "Limpiar filtro")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (uiState.isLoadingStations) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(8.dp))
                                Text("Cargando gasolineras...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    if (uiState.trackedStations.isNotEmpty()) {
                        item {
                            Text(
                                "Siguiendo (${uiState.trackedStations.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        itemsIndexed(
                            uiState.trackedStations.distinctBy { it.id },
                            key = { index, station -> "tracked-${station.id}-$index" }
                        ) { _, station ->
                            TrackedStationCard(
                                station = station,
                                onUntrack = { viewModel.untrackStation(station.id) }
                            )
                        }
                    }

                    if (uiState.searchResults.isNotEmpty()) {
                        itemsIndexed(
                            visibleStations,
                            key = { index, station -> "search-${station.id}-$index" }
                        ) { _, station ->
                            val isTracked = uiState.trackedStations.any { it.id == station.id }
                            StationSearchResultCard(
                                station = station,
                                isTracked = isTracked,
                                onTrack = { viewModel.trackStation(station.id) },
                                onUntrack = { viewModel.untrackStation(station.id) }
                            )
                        }
                    } else if (uiState.trackedStations.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Busca un municipio para ver sus gasolineras",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationSearchResultCard(
    station: GasStation,
    isTracked: Boolean,
    onTrack: () -> Unit,
    onUntrack: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_local_gas_station),
                contentDescription = null,
                tint = if (isTracked) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, fontWeight = FontWeight.SemiBold)
                Text(
                    station.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isTracked) {
                IconButton(onClick = onUntrack) {
                    Icon(Icons.Default.Check, "Siguiendo", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                IconButton(onClick = onTrack) {
                    Icon(Icons.Default.Add, "Seguir", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun TrackedStationCard(station: GasStation, onUntrack: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(R.drawable.ic_local_gas_station), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(station.name, fontWeight = FontWeight.SemiBold)
                Text(
                    "${station.address} · ${station.municipality}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onUntrack) {
                Icon(Icons.Default.Close, "Dejar de seguir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
