package com.example.scrollstop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scrollstop.AppLanguage
import com.example.scrollstop.currentStrings

@Composable
internal fun LanguageSelector(
    selectedLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
) {
    val s = currentStrings
    var expanded by remember { mutableStateOf(false) }
    val activeShortCode = s.language.shortLabel
    val chipLabel = if (selectedLanguage == AppLanguage.SYSTEM) {
        "${selectedLanguage.shortLabel} ($activeShortCode)"
    } else {
        selectedLanguage.shortLabel
    }

    Box(modifier = Modifier.padding(end = 8.dp)) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(chipLabel) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = s.languageMenuTitle,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LanguageMenuItem(
                label = "${s.languageSystem} ($activeShortCode)",
                selected = selectedLanguage == AppLanguage.SYSTEM,
                onClick = {
                    expanded = false
                    onLanguageSelect(AppLanguage.SYSTEM)
                },
            )
            AppLanguage.entries
                .filter { it != AppLanguage.SYSTEM }
                .forEach { lang ->
                    LanguageMenuItem(
                        label = "${lang.nativeName} (${lang.shortLabel})",
                        selected = selectedLanguage == lang,
                        onClick = {
                            expanded = false
                            onLanguageSelect(lang)
                        },
                    )
                }
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
}
