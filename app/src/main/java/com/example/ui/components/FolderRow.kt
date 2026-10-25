package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.FolderItem

@Composable
fun FolderRow(
    folder: FolderItem,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(text = folder.name)
    }
}
