package com.fueltracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.FuelTypeConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelTypeChipRow(
    configs: List<FuelTypeConfig>,
    selected: FuelType,
    onSelect: (FuelType) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(configs, key = { it.type.name }) { config ->
            FilterChip(
                selected = selected == config.type,
                onClick = { onSelect(config.type) },
                label = { Text(config.displayName, fontSize = 12.sp) }
            )
        }
    }
}
