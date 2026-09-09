package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.ceil

const val ADMIN_PAGE_SIZE = 10

@Composable
fun <T> AdminPagedList(
    items: List<T>,
    modifier: Modifier = Modifier,
    pageSize: Int = ADMIN_PAGE_SIZE,
    key: ((T) -> Any)? = null,
    emptyContent: @Composable () -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    val scope = rememberCoroutineScope()
    val totalPages = remember(items.size, pageSize) {
        if (items.isEmpty()) 1 else ceil(items.size / pageSize.toDouble()).toInt().coerceAtLeast(1)
    }
    val pagerState = rememberPagerState(pageCount = { totalPages })

    LaunchedEffect(totalPages) {
        if (pagerState.currentPage >= totalPages) {
            pagerState.scrollToPage((totalPages - 1).coerceAtLeast(0))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.weight(1f, fill = false)) { emptyContent() }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 0
            ) { page ->
                val from = page * pageSize
                val to = minOf(from + pageSize, items.size)
                val pageItems = if (from in 0 until items.size && from < to) items.subList(from, to) else emptyList()
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    pageItems.forEach { item ->
                        key?.invoke(item)
                        itemContent(item)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    enabled = pagerState.currentPage > 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }
                ) { Text("Anterior") }

                Text(
                    "Página ${pagerState.currentPage + 1} de $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    enabled = pagerState.currentPage < totalPages - 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } }
                ) { Text("Próxima") }
            }

            Text(
                "Deslize para a esquerda ou direita para trocar de página.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
