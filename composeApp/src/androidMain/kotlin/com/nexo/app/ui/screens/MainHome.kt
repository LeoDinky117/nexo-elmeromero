package com.nexo.app.ui.screens


import android.R.attr.text

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key.Companion.R
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.nexo.app.navigation.BottomBarItem
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.icon_agregar
import nexo.composeapp.generated.resources.icon_comida
import nexo.composeapp.generated.resources.icon_gastos
import nexo.composeapp.generated.resources.icon_ocio
import nexo.composeapp.generated.resources.icon_perfil
import nexo.composeapp.generated.resources.icon_trabajo
import nexo.composeapp.generated.resources.icon_transporte
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs


@Composable
fun MainHome(navController: NavController,
             movimientosVM : MovimientosViewModel
) {
    LaunchedEffect(Unit){
        movimientosVM.cargarDatosDelServidor()
    }

    val colorFondo = Color(0xFF0B1019)
    val degradadoRosa = Brush.horizontalGradient(listOf(Color(0xFFC837AB), Color(0xFFFF508E)))


    Scaffold(
        containerColor = colorFondo,
        bottomBar = { MyBottomBar(navController) }, // barra navegable
        floatingActionButton = {
            // Botón "+" flotante
            Image(
                painter = painterResource(Res.drawable.icon_agregar),
                contentDescription = "Agregar",
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = 20.dp)
                    .clickable { navController.navigate(Routes.ADD_MOVIMIENTO) }
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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

                Image(
                    painter = painterResource(Res.drawable.icon_perfil),
                    contentDescription = "Perfil",
                    modifier = Modifier.size(50.dp).clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN RACHA Y NIVEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161C28), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Racha: 3 días", color = Color(0xFFF0932B), fontWeight = FontWeight.Bold)
                        Text("Nivel 2", color = Color(0xFFBE2EDD), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Barra de XP Rosa
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color.DarkGray, CircleShape)) {
                        Box(modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .fillMaxHeight()
                            .background(degradadoRosa, CircleShape))
                    }
                    Text("180 / 200 XP", color = Color.Gray, fontSize = 12.sp, modifier = Modifier
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
                val esGasto = movimiento.idCategoria !=1
                val simbolo = if(esGasto) "-$" else "+$"
                //le agregue la variable para quitarle el signo "-", mostraba -$-200
                val montoSinSigno = abs(movimiento.monto)
                val colorMonto = if (esGasto) Color(0xFFFF508E) else Color(0xFF4CAF50)

                val nombreCategoria = when(movimiento.idCategoria) {
                    1 -> "Trabajo"
                    2 -> "Comida"
                    3 -> "Ocio"
                    4 -> "Trans."
                    else -> "Otros"
                }

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
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

            }


        }
    }
}

@Composable
fun MyBottomBar(navController: NavController) {
    val items = listOf(
        BottomBarItem.Inicio,
        BottomBarItem.Movimientos,
        BottomBarItem.Metas,
        BottomBarItem.Perfil
    )

    NavigationBar(
        containerColor = Color(0xFF0B1019), // El fondo oscuro de Nexo
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
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.title,
                        modifier = Modifier.size(26.dp),
                        // Si está seleccionado se pone morado/rosa, si no, gris
                        tint = if (currentRoute == item.route) Color(0xFFC837AB) else Color.Gray
                    )
                },
                label = {
                    Text(item.title, fontSize = 12.sp, color = if (currentRoute == item.route) Color.White else Color.Gray)
                }
            )
        }
    }
}

@Composable
fun ItemMovimiento(titulo: String, sub: String, monto: String, colorMonto: Color, icono: DrawableResource) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF161C28), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cuadro del icono
        Box(
            modifier = Modifier.size(45.dp).background(Color(0xFF1C2431), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(icono), contentDescription = null, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        // Textos
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sub, color = Color.Gray, fontSize = 12.sp)
        }
        // Monto
        Text(monto, color = colorMonto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

