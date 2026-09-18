package com.enad.enadmovil.ui.screens.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.theme.EnadHeader
import com.enad.enadmovil.ui.theme.EnadHeaderChip
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadPillBg
import com.enad.enadmovil.ui.theme.EnadPillText

data class ReportItem(
    val title: String,
    val estado: String = "Disponible · demo",
    val periodo: String = "Periodo: septiembre · Semana 1"
)

data class FoundationTab(val label: String, val selected: Boolean)

@Composable
fun FoundationReportsScreen(
    miembroNombre: String = "Sofia",
    reportes: List<ReportItem> = listOf(
        ReportItem("Evaluación formativa"),
        ReportItem("Asistencia semanal"),
        ReportItem("Horas de acompañamiento")
    ),
    tabs: List<FoundationTab> = listOf(
        FoundationTab("Resumen", false),
        FoundationTab("Instituciones", false),
        FoundationTab("Reportes", true)
    ),
    onCambiarUsuario: () -> Unit = {},
    onVerCatalogoClick: () -> Unit = {},
    onTabClick: (FoundationTab) -> Unit = {}
) {
    // Estado local solo para mostrar/ocultar el diálogo del prototipo; sin lógica real todavía.
    var reporteSeleccionado by remember { mutableStateOf<ReportItem?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { FoundationTopBar(miembroNombre, onCambiarUsuario) },
        bottomBar = { FoundationBottomBar(tabs, onTabClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
        ) {
            Text(
                text = "Reportes",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vista ilustrativa · Todos los indicadores son datos de ejemplo.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            reportes.forEach { reporte ->
                ReportCard(reporte) { reporteSeleccionado = reporte }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ver catálogo de diseño",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp)
            )
        }
    }

    reporteSeleccionado?.let { reporte ->
        ReportDetailDialog(reporte = reporte, onDismiss = { reporteSeleccionado = null })
    }
}

@Composable
private fun FoundationTopBar(miembroNombre: String, onCambiarUsuario: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadHeader)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "ENAd", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Móvil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(
            text = "Portal\nfundación",
            fontSize = 11.sp,
            color = Color(0xFFCFC7BB),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = "Miembro de fundación",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(end = 12.dp)
        )
        Box(
            modifier = Modifier
                .background(EnadHeaderChip, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = "Cambiar\nusuario", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun ReportCard(item: ReportItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Text(
            text = item.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .background(EnadPillBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = item.estado, fontSize = 12.sp, color = EnadPillText, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ReportDetailDialog(reporte: ReportItem, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = reporte.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        text = {
            Column {
                Text(text = "Reporte de ejemplo", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = reporte.periodo, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Consulta visual sin descarga ni envío real.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun FoundationBottomBar(tabs: List<FoundationTab>, onTabClick: (FoundationTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.selected,
                onClick = { onTabClick(tab) },
                icon = { Box(modifier = Modifier.height(0.dp)) }, // íconos pendientes para otra entrega
                label = { Text(text = tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FoundationReportsScreenPreview() {
    EnadMovilTheme {
        FoundationReportsScreen()
    }
}