package com.miguel.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val appearanceThemeOrder = listOf(
    CoachTheme.OBSIDIAN,
    CoachTheme.OCEAN,
    CoachTheme.FOREST,
    CoachTheme.GREEN,
    CoachTheme.ORANGE,
    CoachTheme.LIGHT,
    CoachTheme.SAND,
    CoachTheme.ICE
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppearanceScreen(
    selectedTheme: CoachTheme,
    onBack: () -> Unit,
    onThemeSelected: (CoachTheme) -> Unit
) {
    RegisterSystemBackAction(onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apariencia") },
                navigationIcon = {
                    CoachBackButton(onClick = onBack)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(appearanceThemeOrder, key = CoachTheme::id) { theme ->
                ThemeOptionCard(
                    theme = theme,
                    selected = theme == selectedTheme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    theme: CoachTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    theme.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (selected) {
                    Text(
                        "SELECCIONADO",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                theme.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ThemeColorPreview(theme)
        }
    }
}

@Composable
private fun ThemeColorPreview(theme: CoachTheme) {
    val scheme = theme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(scheme.background, scheme.surface, scheme.primary, scheme.secondary).forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(color)
            )
        }
    }
}
