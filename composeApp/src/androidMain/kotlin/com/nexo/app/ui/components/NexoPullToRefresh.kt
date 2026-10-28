package com.nexo.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun NexoPullToRefresh(
    onRefresh: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (!isRefreshing) scope.launch {
                isRefreshing = true
                val startedAt = System.nanoTime()
                try {
                    onRefresh()
                } finally {
                    val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000
                    if (elapsedMillis < 550) delay(550 - elapsedMillis)
                    isRefreshing = false
                }
            }
        },
        modifier = modifier,
        state = state,
        indicator = {
            NexoRefreshIndicator(state, isRefreshing, Modifier.align(Alignment.TopCenter))
        },
        content = content
    )
}

@Composable
private fun NexoRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val fraction = state.distanceFraction.coerceIn(0f, 1f)
    val offset by animateDpAsState(
        targetValue = if (isRefreshing) 8.dp else (fraction * 48f - 44f).dp,
        label = "nexoRefreshOffset"
    )
    if (isRefreshing || fraction > 0.02f) {
        Row(
            modifier = modifier
                .offset(y = offset)
                .shadow(10.dp, RoundedCornerShape(24.dp))
                .background(Color(0xFF171F2D), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp), color = Color(0xFFFFA23A),
                    trackColor = Color(0x66C837AB), strokeWidth = 2.dp
                )
            } else {
                CircularProgressIndicator(
                    progress = { fraction }, modifier = Modifier.size(17.dp),
                    color = Color(0xFFFFA23A), trackColor = Color(0x66C837AB), strokeWidth = 2.dp
                )
            }
            Text(
                text = if (isRefreshing) "NEXO · Actualizando" else "Desliza para actualizar",
                modifier = Modifier.padding(start = 9.dp), color = Color.White, fontSize = 12.sp
            )
        }
    }
}
