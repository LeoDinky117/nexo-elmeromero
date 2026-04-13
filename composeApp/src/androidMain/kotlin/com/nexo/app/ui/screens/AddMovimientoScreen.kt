package com.nexo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import nexo.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource


@Composable
fun AddMovimientoScreen(navController: NavController,
                        movimientosVM: MovimientosViewModel
                         ) {


    val colorFondo = Color(0xFF0B1019) // Azul muy oscuro de fondo
    val colorContenedor = Color(0xFF161C28) // Color de los bloques (Selector, Categoría, TextField)
    val degradadoRosa = Brush.horizontalGradient(listOf(Color(0xFFC837AB), Color(0xFFFF508E)))
    val colorMoradoNexo = Color(0xFF8E24AA) // Para bordes y texto seleccionado

    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorFondo)
                .padding(horizontal = 24.dp)
        ) {
            // --- HEADER
            Row(
                modifier = Modifier.padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Image(
                        painter = painterResource(Res.drawable.icon_back),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "Agregar movimiento",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN DEL MONTO  ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MONTO", color = Color.Gray, fontSize = 12.sp, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "$",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold

                    )
                    BasicTextField(
                        value = movimientosVM.montoInput,
                        onValueChange = { movimientosVM.onMontoChange(it) },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number //teclado de num.
                        ),
                        cursorBrush = SolidColor(Color(0xFFBE2EDD)),
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .padding(start = 4.dp),
                        decorationBox = { innerTextField ->
                            if(movimientosVM.montoInput.isEmpty()){
                                Text("0", color = Color.DarkGray,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // SELECTOR TIPO (Ingreso / Gasto con Degradado)

            Text("Tipo", color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(colorContenedor, RoundedCornerShape(18.dp))
                    .padding(4.dp)
            ) {
                // Botón Ingreso (Con Degradado Rosa)
                Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (movimientosVM.esIngresoInput) degradadoRosa
                            else Brush.linearGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { movimientosVM.onEsIngresoChange(true) }, contentAlignment = Alignment.Center) {
                    Text("+ Ingreso", color = if (movimientosVM.esIngresoInput) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold)
                }
                // Botón Gasto (Oscuro)
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(
                        if (!movimientosVM.esIngresoInput) degradadoRosa
                        else Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        ),
                        RoundedCornerShape(14.dp)
                    )
                        .clickable { movimientosVM.onEsIngresoChange(false)},
                    contentAlignment = Alignment.Center
                ) {
                    Text("- Gasto", color = if (!movimientosVM.esIngresoInput) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN DE CATEGORÍAS
            Text("Categoría",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))

            // Definimos la lista de categorías con sus datos
            val categorias = listOf(
                Triple("Trabajo", Res.drawable.icon_trabajo, "Trabajo"), // (Nombre, Icono, Etiqueta)
                Triple("Comida", Res.drawable.icon_comida, "Comida"),
                Triple("Ocio", Res.drawable.icon_ocio, "Ocio"),
                Triple("Trans.", Res.drawable.icon_transporte, "Trans.")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categorias) { cat ->

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(height = 90.dp, width = 75.dp) // Tamaño del cuadrito
                            .background(colorContenedor, RoundedCornerShape(16.dp))
                            // Si está seleccionada, le ponemos el borde morado
                            .border(
                                if (movimientosVM.categoriaSeleccionada == cat.first) 2.dp else 0.dp,
                                if (movimientosVM.categoriaSeleccionada == cat.first) colorMoradoNexo else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { movimientosVM.onCategoriaChange(cat.first) }
                            .padding(top = 16.dp)
                    ) {
                        Image(
                            painter = painterResource(cat.second),
                            contentDescription = cat.first,
                            modifier = Modifier.size(28.dp),
                            // El icono brilla en blanco si se selecciona
                            colorFilter = if (movimientosVM.categoriaSeleccionada == cat.first) null else ColorFilter.tint(Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cat.third,
                            color = if (movimientosVM.categoriaSeleccionada == cat.first) colorMoradoNexo else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

        // --- CAMPO DE FECHA
        Text("Fecha", color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = movimientosVM.fechaInput,
            onValueChange = { movimientosVM.onFechaChange(it)},
            leadingIcon = {
                Image(
                    painter = painterResource(Res.drawable.icon_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF161C28),
                unfocusedContainerColor = Color(0xFF161C28),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMPO DE CONCEPTO (TextField Dark Mode con Icono) ---

            Text("Concepto",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = movimientosVM.descripcionInput,
                onValueChange = { movimientosVM.onDescripcionChanged(it)},
                placeholder = { Text("Ej. Cena...", color = Color.DarkGray) },
                leadingIcon = { Image(painter = painterResource(Res.drawable.icon_concepto),
                    contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorContenedor,
                    unfocusedContainerColor = colorContenedor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            // --- BOTÓN DE GUARDAR (Abajo, con degradado rosa completo) ---

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    movimientosVM.registrarMovimiento {
                        navController.navigate(Routes.SUCCESS){
                            popUpTo(Routes.ADD_MOVIMIENTO){inclusive = true}
                        }

                    }
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(degradadoRosa),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Guardar", color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
