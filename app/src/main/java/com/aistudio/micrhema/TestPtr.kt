package com.aistudio.micrhema

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestPtr() {
    PullToRefreshBox(isRefreshing = false, onRefresh = { }) {
        Text("Hello")
    }
}
