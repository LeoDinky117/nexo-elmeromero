package com.nexo.app.ui.screens

import android.R.attr.id
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.nexo.app.ui.viewModel.MovimientosViewModel
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.icon_comida
import nexo.composeapp.generated.resources.icon_gastos
import nexo.composeapp.generated.resources.icon_ocio
import nexo.composeapp.generated.resources.icon_trabajo
import nexo.composeapp.generated.resources.icon_transporte


@Composable
fun GastosScreen(navController: NavController,
                 movimientosVM : MovimientosViewModel) {


    val colorFondo = Color(0xFF0B1019)

    Scaffold (
        bottomBar = { MyBottomBar(navController) },
    ){
        paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
                .fillMaxSize()
                .background(colorFondo)
                .padding(paddingValues)
        ){
            Text("Mis Gatos", color = Color.White)

            val listaDeGastos = movimientosVM.listaDeMovimientos.filter {
                it.monto < 0  || it.idCategoria != 1}

                LazyColumn{

                    items(movimientosVM.listaDeMovimientos) { movimiento ->
                        val nombreCategoria = when(movimiento.idCategoria){
                            1 -> "Trabajo"
                            2 -> "Comida"
                            3 -> "Ocio"
                            4 -> "Trans."
                            else -> "Otros"
                        }
                        val iconoCategory = when(movimiento.idCategoria){
                            1 -> Res.drawable.icon_trabajo
                            2 -> Res.drawable.icon_comida
                            3 -> Res.drawable.icon_ocio
                            4 -> Res.drawable.icon_transporte
                            else -> Res.drawable.icon_gastos
                        }
                        val colorMonto = if (movimiento.monto > 0) Color(0xFF4CAF50) else Color(0xFFFF508E)
                        val esGasto = movimiento.idCategoria !=1
                        val simbolo = if(esGasto) "-$" else "+$"

                        ItemMovimiento(
                            titulo = movimiento.descripcion ?: "Sin nombre",
                            sub = nombreCategoria,
                            monto = "${simbolo}${movimiento.monto}",
                            colorMonto = colorMonto,
                            icono = iconoCategory
                        )

                    }

                }
            }
        }
    }

