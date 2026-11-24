package com.nexo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.contrastingTextColor

@Composable
fun InputField(
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingText: String? = null,
    formatDate: Boolean = false,
    containerColor: Color? = null,
    onValueChange: (String) -> Unit
) {
    val colorDeContenedor = containerColor ?: LocalNexoPalette.current.surface
    val colorTexto = contrastingTextColor(colorDeContenedor)
    val colorPlaceholder = colorTexto.copy(alpha = 0.62f)
    // Conserva el texto y la selección del cursor entre recomposiciones.
    var inputValue by remember {
        mutableStateOf(TextFieldValue(text = value))
    }

    // Sincroniza cambios externos, como limpiar el formulario tras guardar.
    LaunchedEffect(value) {
        if (value != inputValue.text) {
            inputValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colorDeContenedor,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Muestra un prefijo opcional, como el signo de pesos.
            if (leadingText != null) {
                Text(
                    text = leadingText,
                    color = colorTexto,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (inputValue.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = colorPlaceholder
                    )
                }

                BasicTextField(
                    value = inputValue,
                    onValueChange = { nuevoValor ->
                        if (formatDate) {
                            // Cuenta los dígitos antes del cursor para reubicarlo
                            // después de agregar automáticamente los guiones.
                            val digitosAntesDelCursor = nuevoValor.text
                                .take(nuevoValor.selection.start)
                                .count { it.isDigit() }
                                .coerceAtMost(8)

                            val digitos = nuevoValor.text
                                .filter { it.isDigit() }
                                .take(8)

                            val fechaFormateada = when {
                                digitos.length <= 4 -> digitos
                                digitos.length <= 6 ->
                                    "${digitos.substring(0, 4)}-${digitos.substring(4)}"
                                else ->
                                    "${digitos.substring(0, 4)}-" +
                                            "${digitos.substring(4, 6)}-" +
                                            digitos.substring(6)
                            }

                            // Cada guion agrega una posición al cursor.
                            val guionesAntesDelCursor =
                                (if (digitosAntesDelCursor > 4) 1 else 0) +
                                        (if (digitosAntesDelCursor > 6) 1 else 0)

                            val nuevaPosicion = (
                                    digitosAntesDelCursor + guionesAntesDelCursor
                                    ).coerceAtMost(fechaFormateada.length)

                            inputValue = TextFieldValue(
                                text = fechaFormateada,
                                selection = TextRange(nuevaPosicion)
                            )
                            onValueChange(fechaFormateada)
                        } else {
                            inputValue = nuevoValor
                            onValueChange(nuevoValor.text)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = TextStyle(
                        color = colorTexto,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(LocalNexoPalette.current.accent)
                )
            }
        }
    }
}
