package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun InputField(
    value: String,
    placeholder: String,
    isPassword: Boolean = false,
    onValueChange: (String) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {

        if (value.isEmpty()) {
            Text(
                placeholder,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = if (isPassword)
                PasswordVisualTransformation()
            else
                androidx.compose.ui.text.input.VisualTransformation.None,
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
        )
    }
}