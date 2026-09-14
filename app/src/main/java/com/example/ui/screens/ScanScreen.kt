package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.viewmodel.MythicViewModel

@Composable
fun ScanScreen(
    viewModel: MythicViewModel,
    onBack: () -> Unit = {}
) {
    ARScreen(
        viewModel = viewModel,
        onDismiss = onBack
    )
}
