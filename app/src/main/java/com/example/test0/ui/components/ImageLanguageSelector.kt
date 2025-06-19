package com.example.test0.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.test0.model.ImageLanguage
import com.example.test0.model.getDisplayNameWithDetected

@Composable
fun ImageLanguageSelector(
    selectedLanguage: ImageLanguage,
    languages: List<ImageLanguage>,
    onLanguageSelected: (ImageLanguage) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    enabled: Boolean = true,
    isDetected: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = if (enabled) Modifier
                .clickable { expanded = true }
                .padding(16.dp)
                .height(50.dp)
            else Modifier
                .padding(16.dp)
                .height(50.dp)
        ) {
            if (label.isNotEmpty()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
            
            Text(
                text = selectedLanguage.getDisplayNameWithDetected(isDetected),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = if (label.isNotEmpty()) 16.dp else 0.dp)
                    .align(if (label.isNotEmpty()) Alignment.BottomStart else Alignment.CenterStart)
            )
            
            if (enabled) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select language",
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            
            if (enabled) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    languages.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(text = language.displayName) },
                            onClick = {
                                onLanguageSelected(language)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
} 