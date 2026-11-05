package com.dcnm.carapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dcnm.carapp.core.model.SignalSource

/** 합성 신호가 섞여 있으면 반드시 표시한다. 시연 신뢰성의 기본. */
@Composable
fun SimulatedBadge(source: SignalSource, modifier: Modifier = Modifier) {
    if (source != SignalSource.SIMULATED) return
    Text(
        text = "SIMULATED",
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
