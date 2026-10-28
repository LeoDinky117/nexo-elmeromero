package com.nexo.app.ui.screens


import android.Manifest
import android.content.pm.PackageManager
import android.R.attr.text
import android.os.Build
import android.widget.Toast

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nexo.app.navigation.BottomBarItem
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.LocalNexoProfileIcon
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.icon_agregar
import nexo.composeapp.generated.resources.icon_comida
import nexo.composeapp.generated.resources.icon_gastos
import nexo.composeapp.generated.resources.icon_ocio
import nexo.composeapp.generated.resources.icon_trabajo
import nexo.composeapp.generated.resources.icon_transporte
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs


@Composable
fun MainHome(navController: NavController,
             movimientosVM : MovimientosViewModel
) {
    val paleta = LocalNexoPalette.current
    val iconoPerfilActivo = LocalNexoProfileIcon.current
    val context = LocalContext.current
    var mostrarBorradores by remember { mutableStateOf(false) }
    val permisoNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit){
        movimientosVM.cargarCategorias()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(movimientosVM.borradoresPendientes.size) {
        mostrarBorradores = movimientosVM.borradoresPendientes.isNotEmpty()
    }
    LaunchedEffect(movimientosVM.mensajeBorradores) {
        movimientosVM.mensajeBorradores?.let { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            movimientosVM.ocultarMensajeBorradores()
        }
    }

    val colorFondo = paleta.background
    val degradadoRosa = Brush.horizontalGradient(listOf(paleta.accent, paleta.highlight))


    Scaffold(
        containerColor = colorFondo,
        bottomBar = { MyBottomBar(navController) }, // barra navegable
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .offset(y = 16.dp)
                    .shadow(10.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFFB04A), Color(0xFFFF508E))))
                    .border(2.dp, Color.White.copy(alpha = 0.82f), CircleShape)
                    .clickable { navController.navigate(Routes.ADD_MOVIMIENTO) },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.icon_agregar),
                    contentDescription = "Agregar movimiento",
                    modifier = Modifier.size(31.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        NexoPullToRefresh(
            onRefresh = { movimientosVM.refrescarDatos().join() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Saludo y Perfil
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Buenos días,", color = Color.Gray, fontSize = 14.sp)
                    Text("Bienvenido", color = Color.White, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold)
                }

                if (iconoPerfilActivo == "👤") PerfilGlyph(modifier = Modifier.size(50.dp), color = paleta.accent)
                else Text(iconoPerfilActivo, fontSize = 38.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN RACHA Y NIVEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(paleta.surface, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Racha: ${movimientosVM.racha} días", color = Color(0xFFF0932B), fontWeight = FontWeight.Bold)
                        Text("Nivel ${movimientosVM.puntos / 100 + 1}", color = paleta.accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Barra de XP Rosa
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.DarkGray, CircleShape)) {
                        Box(modifier = Modifier
                            .fillMaxWidth((movimientosVM.puntos % 100) / 100f)
                            .fillMaxHeight()
                            .background(degradadoRosa, CircleShape))
                    }
                    Text("${movimientosVM.puntos % 100} / 100 puntos · ${movimientosVM.puntos} acumulados", color = Color.Gray, fontSize = 12.sp, modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- SALDO TOTAL  ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Saldo", color = Color.LightGray, fontSize = 16.sp)
                Text(
                    "$${movimientosVM.saldoTotal}",
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- SECCIÓN ÚLTIMOS MOVIMIENTOS ---


            Text("Últimos movimientos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
             val ultimosCinco = movimientosVM.listaDeMovimientos.reversed().take(5)

            ultimosCinco.forEach { movimiento ->
                val esGasto = movimiento.monto < 0
                val simbolo = if(esGasto) "-$" else "+$"
                //le agregue la variable para quitarle el signo "-", mostraba -$-200
                val montoSinSigno = abs(movimiento.monto)
                val colorMonto = if (esGasto) Color(0xFFFF508E) else Color(0xFF4CAF50)

                val nombreCategoria = movimientosVM.nombreCategoria(movimiento.idCategoria)

                ItemMovimiento(
                    titulo = movimiento.descripcion ?: "Gasto",
                    sub = nombreCategoria,
                    monto = "${simbolo}${montoSinSigno}",
                    colorMonto = colorMonto,
                    icono = when(movimiento.idCategoria) {
                        1 -> Res.drawable.icon_trabajo
                        2 -> Res.drawable.icon_comida
                        3 -> Res.drawable.icon_ocio
                        4 -> Res.drawable.icon_transporte
                        else -> Res.drawable.icon_gastos
                    },
                    iconoEmoji = emojiCategoria(nombreCategoria)
                )
                Spacer(modifier = Modifier.height(12.dp))

            }

        }
        }
    }

    if (mostrarBorradores && movimientosVM.borradoresPendientes.isNotEmpty()) {
        BorradoresPendientesDialog(
            borradores = movimientosVM.borradoresPendientes,
            categorias = movimientosVM.categorias,
            guardando = movimientosVM.guardandoBorradores,
            mensaje = movimientosVM.mensajeBorradores,
            onQuitar = movimientosVM::quitarBorrador,
            onGuardarUno = movimientosVM::confirmarBorrador,
            onGuardarTodos = movimientosVM::confirmarBorradores,
            onCerrar = { mostrarBorradores = false }
        )
    }
}

@Composable
fun MyBottomBar(navController: NavController) {
    val items = listOf(
        BottomBarItem.Inicio,
        BottomBarItem.Movimientos,
        BottomBarItem.Calendario,
        BottomBarItem.Presupuesto,
        BottomBarItem.Metas,
        BottomBarItem.Perfil
    )

    NavigationBar(
        containerColor = LocalNexoPalette.current.background,
        contentColor = Color.White
    ) {
        // Obtenemos la ruta actual para saber qué icono resaltar
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Esto evita que se amontonen pantallas al picar muchas veces
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                },
                icon = {
                    val iconColor = if (currentRoute == item.route) LocalNexoPalette.current.accent else Color.Gray
                    if (item == BottomBarItem.Perfil) {
                        if (LocalNexoProfileIcon.current == "👤") PerfilGlyph(modifier = Modifier.size(26.dp), color = iconColor)
                        else Text(LocalNexoProfileIcon.current, fontSize = 23.sp)
                    } else {
                        Icon(
                            painter = painterResource(item.icon),
                            contentDescription = item.title,
                            modifier = Modifier.size(26.dp),
                            tint = iconColor
                        )
                    }
                },
                label = {
                    Text(item.title, fontSize = 12.sp, color = if (currentRoute == item.route) Color.White else Color.Gray)
                }
            )
        }
    }
}

@Composable
private fun PerfilGlyph(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val scale = size.minDimension / 100f
        val line = 5f * scale
        drawCircle(
            color = color,
            radius = 14f * scale,
            center = Offset(size.width / 2f, 29f * scale),
            style = Stroke(width = line)
        )
        val shoulders = Path().apply {
            moveTo(16f * scale, 84f * scale)
            cubicTo(17f * scale, 67f * scale, 27f * scale, 58f * scale, 42f * scale, 55f * scale)
            lineTo(50f * scale, 64f * scale)
            lineTo(58f * scale, 55f * scale)
            cubicTo(73f * scale, 58f * scale, 83f * scale, 67f * scale, 84f * scale, 84f * scale)
            close()
        }
        drawPath(
            path = shoulders,
            color = color,
            style = Stroke(width = line, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun emojiCategoria(nombre: String): String? = when (nombre) {
    "Trabajo" -> "💼"
    "Comida" -> "🍽️"
    "Ocio" -> "🎮"
    "Trans." -> "🚗"
    "Ventas" -> "🛍️"
    "Freelance" -> "🧑‍💻"
    "Inversiones" -> "📈"
    "Regalo" -> "🎁"
    "Otros ingresos" -> "💵"
    "Hogar" -> "🏠"
    "Servicios" -> "💡"
    "Salud" -> "🩺"
    "Educación" -> "📚"
    "Compras" -> "🛒"
    "Suscripciones" -> "📱"
    "Deudas" -> "💳"
    "Viajes" -> "✈️"
    "Mascotas" -> "🐾"
    "Otros" -> "📦"
    else -> null
}

@Composable
fun ItemMovimiento(
    titulo: String,
    sub: String,
    monto: String,
    colorMonto: Color,
    icono: DrawableResource,
    iconoEmoji: String? = null,
    fecha: String? = null
) {
    val paleta = LocalNexoPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(paleta.surface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cuadro del icono
        Box(
            modifier = Modifier.size(45.dp).background(paleta.background.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (iconoEmoji != null) {
                Text(iconoEmoji, fontSize = 22.sp)
            } else {
                Image(painter = painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Textos
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                if (fecha.isNullOrBlank()) sub else "$sub · $fecha",
                color = Color(0xFFB8C2D1), fontSize = 12.sp
            )
        }
        // Monto
        Text(monto, color = colorMonto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

