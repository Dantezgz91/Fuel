package com.fueltracker.app.ui.screens.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.painterResource
import com.fueltracker.app.R
import com.fueltracker.app.domain.model.ThemeMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fueltracker.app.domain.model.FuelTypeConfig
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Apariencia", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Tema", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        val themeOptions = listOf(
                            ThemeMode.SYSTEM to "Sistema",
                            ThemeMode.LIGHT to "Claro",
                            ThemeMode.DARK to "Oscuro"
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeOptions.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = uiState.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index, themeOptions.size),
                                    label = { Text(label) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sistema sigue el modo claro u oscuro del dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_sync),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Sincronización automática",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = uiState.autoSyncEnabled,
                                onCheckedChange = viewModel::setAutoSync
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Actualizar precios automáticamente. Los precios oficiales del MITECO se publican una vez al día.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.autoSyncEnabled) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "La app comprueba precios una vez al día, también al abrirla si hace más de 20 h que no se actualiza.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_home),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Ubicación de casa", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Usada en Inicio para calcular la distancia a tus favoritas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        val homeLocation = uiState.homeLocation
                        if (homeLocation != null) {
                            Text(
                                "Lat: ${String.format(java.util.Locale.US, "%.5f", homeLocation.latitude)}, " +
                                    "Lng: ${String.format(java.util.Locale.US, "%.5f", homeLocation.longitude)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            Text(
                                "Aún no has configurado tu casa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        Text("Radio en modo Cercanas", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        val radiusOptions = listOf(10, 20, 30)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            radiusOptions.forEachIndexed { index, km ->
                                SegmentedButton(
                                    selected = uiState.travelRadiusKm == km,
                                    onClick = { viewModel.setTravelRadiusKm(km) },
                                    shape = SegmentedButtonDefaults.itemShape(index, radiusOptions.size),
                                    label = { Text("${km} km") }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = viewModel::requestHomeFromCurrentLocation,
                            enabled = !uiState.isLoadingLocation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isLoadingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Usar mi ubicación actual")
                            }
                        }
                        if (uiState.homeLocation != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = viewModel::clearHomeLocation,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Eliminar ubicación de casa")
                            }
                        }
                    }
                }
            }

            item {
                FuelTypesSettingsCard(
                    configs = uiState.fuelTypeConfigs,
                    onEnabledChange = viewModel::setFuelTypeEnabled,
                    onLabelChange = viewModel::setFuelTypeLabel
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(R.drawable.ic_delete_sweep), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Historial de datos", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))

                        Text("Retención de datos", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Al elegir un periodo se eliminan automáticamente los registros más antiguos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        val retentionOptions = listOf(30, 90, 180, 365)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            retentionOptions.forEachIndexed { index, days ->
                                SegmentedButton(
                                    selected = uiState.dataRetentionDays == days,
                                    onClick = { viewModel.setDataRetention(days) },
                                    shape = SegmentedButtonDefaults.itemShape(index, retentionOptions.size),
                                    label = { Text(if (days < 365) "${days}d" else "1 año") }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        var showClearAllDialog by remember { mutableStateOf(false) }
                        Button(
                            onClick = { showClearAllDialog = true },
                            enabled = !uiState.isClearingData,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.isClearingData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Vaciar todo el historial de precios")
                            }
                        }
                        if (showClearAllDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearAllDialog = false },
                                title = { Text("Vaciar historial") },
                                text = {
                                    Text(
                                        "Se borrarán todos los precios guardados. " +
                                            "Las gasolineras que sigues no se eliminan."
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showClearAllDialog = false
                                            viewModel.clearAllPriceHistory()
                                        }
                                    ) {
                                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearAllDialog = false }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Acerca de", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("FuelTracker v1.0", style = MaterialTheme.typography.bodyMedium)
                        Text("Datos de precios: MITECO (Ministerio para la Transición Ecológica)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Actualización automática: una vez al día", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelTypesSettingsCard(
    configs: List<FuelTypeConfig>,
    onEnabledChange: (com.fueltracker.app.domain.model.FuelType, Boolean) -> Unit,
    onLabelChange: (com.fueltracker.app.domain.model.FuelType, String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_local_gas_station),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Combustibles", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Todos los combustibles que publica la API MITECO. Puedes ocultarlos o renombrarlos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            configs.forEach { config ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        config.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(100.dp)
                    )
                    OutlinedTextField(
                        value = config.displayName,
                        onValueChange = { onLabelChange(config.type, it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(config.type.displayName) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = config.isEnabled,
                        onCheckedChange = { onEnabledChange(config.type, it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
