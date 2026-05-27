package com.beatflow.app.presentation.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beatflow.app.presentation.theme.BeatFlowColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientFormScreen(
    sessionId: Long,
    onNavigateToReport: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PatientFormViewModel = hiltViewModel()
) {
    val nombre by viewModel.nombre.collectAsState()
    val apellidos by viewModel.apellidos.collectAsState()
    val edad by viewModel.edad.collectAsState()
    val sexo by viewModel.sexo.collectAsState()
    val error by viewModel.error.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var sexoExpanded by remember { mutableStateOf(false) }
    val sexoOptions = listOf("Masculino", "Femenino", "Otro")

    LaunchedEffect(isSaved) {
        if (isSaved) {
            onNavigateToReport(sessionId)
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Datos del Paciente") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Atrás")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ingrese los datos del paciente",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Todos los campos son obligatorios",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = nombre,
                onValueChange = viewModel::updateNombre,
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = apellidos,
                onValueChange = viewModel::updateApellidos,
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = edad,
                onValueChange = viewModel::updateEdad,
                label = { Text("Edad") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = sexoExpanded,
                onExpandedChange = { sexoExpanded = !sexoExpanded }
            ) {
                OutlinedTextField(
                    value = sexo,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sexo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexoExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = sexoExpanded,
                    onDismissRequest = { sexoExpanded = false }
                ) {
                    sexoOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.updateSexo(option)
                                sexoExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.savePatient(sessionId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BeatFlowColors.HeartRed
                )
            ) {
                Text(
                    text = "GUARDAR REPORTE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
