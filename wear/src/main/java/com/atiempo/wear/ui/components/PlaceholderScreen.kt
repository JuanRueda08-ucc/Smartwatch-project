package com.atiempo.wear.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.atiempo.wear.ui.theme.AppTheme

/** Enlace de una pantalla vacía hacia otra pantalla. */
data class PlaceholderLink(@StringRes val label: Int, val onClick: () -> Unit)

/**
 * Pantalla provisional de la Fase 2: título, qué mostrará y botones hacia sus pantallas hijas,
 * en una lista que se curva con el borde (TransformingLazyColumn). Cada pantalla real la
 * reemplaza en la Fase 3.
 */
@Composable
fun PlaceholderScreen(
    @StringRes title: Int,
    @StringRes description: Int,
    links: List<PlaceholderLink> = emptyList(),
    extraContent: TransformingLazyColumnScope.() -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(text = stringResource(title), style = AppTheme.type.title, textAlign = TextAlign.Center)
                }
            }
            item {
                Text(
                    text = stringResource(description),
                    style = AppTheme.type.label,
                    color = AppTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }
            links.forEach { link ->
                item {
                    Button(
                        onClick = link.onClick,
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        label = { Text(text = stringResource(link.label), style = AppTheme.type.button) },
                    )
                }
            }
            extraContent()
        }
    }
}
