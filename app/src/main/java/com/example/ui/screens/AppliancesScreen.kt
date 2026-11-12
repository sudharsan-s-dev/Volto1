package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ApplianceCard
import com.example.ui.theme.VoltoGreenPrimary
import com.example.viewmodel.ApplianceFilter
import com.example.viewmodel.VoltoViewModel

@Composable
fun AppliancesScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val selectedApplianceId by viewModel.selectedApplianceId.collectAsState()
    val appliances by viewModel.appliances.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.filterCategory.collectAsState()
    val controlStates by viewModel.deviceControlStates.collectAsState()

    // If an appliance is selected, show detail screen
    if (selectedApplianceId != null) {
        val selectedAppliance = appliances.find { it.id == selectedApplianceId }
        if (selectedAppliance != null) {
            ApplianceDetailScreen(
                appliance = selectedAppliance,
                viewModel = viewModel,
                modifier = modifier
            )
            return
        }
    }

    // Filtered appliances list
    val filteredList = appliances.filter { app ->
        val matchesSearch = searchQuery.isBlank() ||
            app.name.contains(searchQuery, ignoreCase = true) ||
            app.type.contains(searchQuery, ignoreCase = true) ||
            app.deviceId.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (activeFilter) {
            ApplianceFilter.ALL -> true
            ApplianceFilter.RUNNING -> app.isOn && app.isOnline
            ApplianceFilter.OFF -> !app.isOn || !app.isOnline
            ApplianceFilter.CRITICAL -> app.isCritical
        }

        matchesSearch && matchesFilter
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("appliances_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row: Title & Add Appliance Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Appliances",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${appliances.count { it.isOn && it.isOnline }} of ${appliances.size} running now",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { viewModel.openAddApplianceDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltoGreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("add_appliance_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Appliance",
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Add",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search appliances, rooms, or IDs...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("appliance_search_input"),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ApplianceFilter.values()) { filter ->
                    val isSelected = activeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilterCategory(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = VoltoGreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Appliance Cards List
        if (filteredList.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No matching appliances found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try adjusting your search query or filter chips",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(filteredList, key = { it.id }) { appliance ->
                ApplianceCard(
                    appliance = appliance,
                    controlState = controlStates[appliance.deviceId],
                    onCardClick = { viewModel.openApplianceDetails(appliance.id) },
                    onToggleClick = { viewModel.requestToggleAppliance(appliance) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
