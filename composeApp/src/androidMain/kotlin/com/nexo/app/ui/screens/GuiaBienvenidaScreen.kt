package com.nexo.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.LocalNexoPalette

data class GuiaPasoFoco(
    val ruta: String,
    val objetivo: String?,
    val titulo: String,
    val descripcion: String
)

val pasosGuiaFoco = listOf(
    GuiaPasoFoco(Routes.MAIN_HOME, "home_add", "¡Bienvenido a Nexo!", "Aquí puedes revisar tu panorama financiero y empezar a registrar tus movimientos."),
    GuiaPasoFoco(Routes.PRESUPUESTO, "budget_title", "Configura tu presupuesto", "Agrega tus ingresos, gastos esenciales, deudas y otros gastos. Indica los montos y fechas para conocer tu saldo disponible y tus próximos compromisos."),
    GuiaPasoFoco(Routes.MOVIMIENTOS, "movements_title", "Registra tus movimientos", "Anota ingresos, gastos y pagos. Aquí puedes consultar el historial ordenado por fecha y concepto."),
    GuiaPasoFoco(Routes.CALENDARIO, "calendar_grid", "Consulta el calendario", "Revisa en qué fechas se esperan tus ingresos y pagos. Los conceptos mantienen sus colores e iconos para identificarlos rápidamente."),
    GuiaPasoFoco(Routes.METAS, "goals_title", "Da seguimiento a tus metas", "Crea objetivos de ahorro, consulta el avance y registra abonos para acercarte a cada meta."),
    GuiaPasoFoco(Routes.PERFIL, "profile_title", "Personaliza tu perfil", "Actualiza tu nombre o correo y explora los temas e iconos que puedes desbloquear con tus puntos."),
    GuiaPasoFoco(Routes.MAIN_HOME, null, "Usa los widgets", "Desde la pantalla de inicio del teléfono, mantén presionado un espacio vacío, abre Widgets y busca Nexo. Te recomendamos agregar Movimientos para registrar rápido y Calendario para consultar próximos compromisos.")
)

class GuiaFocoState {
    var objetivos by mutableStateOf<Map<String, Rect>>(emptyMap())
        private set

    fun registrar(id: String, bounds: Rect) {
        if (objetivos[id] != bounds) objetivos = objetivos + (id to bounds)
    }

    fun limpiar() { objetivos = emptyMap() }
}

val LocalGuiaFocoState = compositionLocalOf<GuiaFocoState?> { null }

fun Modifier.objetivoGuia(id: String): Modifier = composed {
    val estado = LocalGuiaFocoState.current
    onGloballyPositioned { coordenadas ->
        estado?.registrar(id, coordenadas.boundsInRoot())
    }
}

@Composable
fun GuiaBienvenidaOverlay(
    pasoActual: Int,
    estadoFoco: GuiaFocoState,
    onCambiarPaso: (Int) -> Unit,
    onConfigurarPresupuesto: () -> Unit,
    onTerminar: () -> Unit
) {
    val paleta = LocalNexoPalette.current
    val paso = pasosGuiaFoco[pasoActual.coerceIn(pasosGuiaFoco.indices)]
    val rectObjetivo = paso.objetivo?.let { estadoFoco.objetivos[it] }
    val ultimo = pasoActual == pasosGuiaFoco.lastIndex
    val densidad = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val altoPx = with(densidad) { maxHeight.toPx() }
        val ponerTarjetaArriba = rectObjetivo != null && rectObjetivo.center.y > altoPx * 0.55f
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val objetivo = rectObjetivo?.let {
                Rect(
                    left = (it.left - 10.dp.toPx()).coerceAtLeast(0f),
                    top = (it.top - 10.dp.toPx()).coerceAtLeast(0f),
                    right = (it.right + 10.dp.toPx()).coerceAtMost(size.width),
                    bottom = (it.bottom + 10.dp.toPx()).coerceAtMost(size.height)
                )
            }
            val mascara = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                objetivo?.let { addRoundRect(RoundRect(it, CornerRadius(22.dp.toPx()))) }
            }
            drawPath(mascara, Color.Black.copy(alpha = 0.78f))
            objetivo?.let {
                drawRoundRect(
                    color = paleta.highlight,
                    topLeft = androidx.compose.ui.geometry.Offset(it.left, it.top),
                    size = androidx.compose.ui.geometry.Size(it.width, it.height),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        val alineacion = when {
            rectObjetivo == null -> Alignment.Center
            ponerTarjetaArriba -> Alignment.TopCenter
            else -> Alignment.BottomCenter
        }
        Column(
            modifier = Modifier
                .align(alineacion)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(top = if (alineacion == Alignment.TopCenter) 28.dp else 12.dp)
                .padding(bottom = if (alineacion == Alignment.BottomCenter) 88.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("GUÍA DE INICIO", color = paleta.accentTextOnBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text("${pasoActual + 1} / ${pasosGuiaFoco.size}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pasosGuiaFoco.indices.forEach { indice ->
                    Box(
                        Modifier.size(if (indice == pasoActual) 20.dp else 7.dp, 7.dp)
                            .background(if (indice == pasoActual) paleta.highlight else Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = paleta.surface)
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(paso.titulo, color = paleta.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                    Text(paso.descripcion, color = paleta.secondaryOnSurface, fontSize = 14.sp, lineHeight = 20.sp)
                    if (ultimo) {
                        Text("Cuando termines, podrás volver a esta información desde la ayuda de Nexo.", color = paleta.accentTextOnSurface, fontSize = 12.sp)
                    }
                    if (ultimo) {
                        Button(
                            onClick = onConfigurarPresupuesto,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = paleta.accent, contentColor = paleta.onAccent)
                        ) { Text("Configurar presupuesto", fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = onTerminar, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(15.dp)) {
                            Text("Terminar guía", color = paleta.onSurface)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (pasoActual > 0) {
                                OutlinedButton(
                                    onClick = { onCambiarPaso(pasoActual - 1) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ) { Text("Atrás", color = paleta.onSurface) }
                            }
                            Button(
                                onClick = { onCambiarPaso(pasoActual + 1) },
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = paleta.accent, contentColor = paleta.onAccent)
                            ) { Text("Siguiente", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}
