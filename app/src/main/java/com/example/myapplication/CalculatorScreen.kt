package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.objecthunter.exp4j.ExpressionBuilder
import java.util.Locale

data class HistoryEntry(val expression: String, val result: String)

data class UnitInfo(val name: String, val factor: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<HistoryEntry>() }
    
    val showHistory = remember { mutableStateOf(false) }
    val showConverter = remember { mutableStateOf(false) }

    val samsungGreen = Color(0xFF4CAF50)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = if (expression.length > 10) 32.sp else 48.sp
                ),
                maxLines = 3,
                textAlign = TextAlign.End,
                lineHeight = 56.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (result.isNotEmpty() && result != "Error") {
                Text(
                    text = "= $result",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    ),
                    textAlign = TextAlign.End
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { showHistory.value = true }) {
                    Icon(Icons.Default.History, contentDescription = "History")
                }
                IconButton(onClick = { showConverter.value = true }) {
                    Icon(Icons.Default.Straighten, contentDescription = "Unit Converter")
                }
            }
            
            if (expression.isNotEmpty()) {
                IconButton(onClick = {
                    expression = expression.dropLast(1)
                    if (expression.isEmpty()) {
                        result = ""
                    } else {
                        val eval = evaluateExpression(expression)
                        result = if (eval != "Error") eval else ""
                    }
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Backspace, 
                        contentDescription = "Backspace", 
                        tint = samsungGreen
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(bottom = 8.dp), 
            thickness = 0.5.dp, 
            color = MaterialTheme.colorScheme.outlineVariant
        )

        val buttons = listOf(
            listOf("C", "( )", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("+/-", "0", ".", "=")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { label ->
                    CalculatorButton(
                        label = label,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (label) {
                                "C" -> {
                                    expression = ""
                                    result = ""
                                }
                                "( )" -> {
                                    val openCount = expression.count { it == '(' }
                                    val closeCount = expression.count { it == ')' }
                                    expression += if (openCount == closeCount || expression.lastOrNull() in listOf('+', '-', '×', '÷', '(')) "(" else ")"
                                }
                                "=" -> {
                                    if (expression.isNotEmpty()) {
                                        val eval = evaluateExpression(expression)
                                        if (eval != "Error") {
                                            history.add(0, HistoryEntry(expression, eval))
                                            expression = eval
                                            result = ""
                                        } else {
                                            result = "Error"
                                        }
                                    }
                                }
                                "+/-" -> {
                                    if (expression.isNotEmpty()) {
                                        expression = if (expression.startsWith("-")) {
                                            expression.substring(1)
                                        } else {
                                            "-$expression"
                                        }
                                    }
                                }
                                else -> {
                                    expression += label
                                    val eval = evaluateExpression(expression)
                                    result = if (eval != "Error") eval else ""
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showHistory.value) {
        HistoryBottomSheet(
            history = history,
            onDismiss = { showHistory.value = false },
            onItemClick = { entry ->
                expression = entry.expression
                result = ""
                showHistory.value = false
            },
            onClearHistory = { history.clear() }
        )
    }

    if (showConverter.value) {
        ConverterBottomSheet(
            onDismiss = { showConverter.value = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryBottomSheet(
    history: List<HistoryEntry>,
    onDismiss: () -> Unit,
    onItemClick: (HistoryEntry) -> Unit,
    onClearHistory: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Clear history")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No history",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        history.forEach { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemClick(entry) }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = entry.expression,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "= ${entry.result}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConverterBottomSheet(onDismiss: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val converterData = mapOf(
        "Area" to listOf(UnitInfo("m²", 1.0), UnitInfo("cm²", 0.0001), UnitInfo("km²", 1000000.0), UnitInfo("ha", 10000.0)),
        "Length" to listOf(UnitInfo("m", 1.0), UnitInfo("cm", 0.01), UnitInfo("mm", 0.001), UnitInfo("km", 1000.0), UnitInfo("in", 0.0254), UnitInfo("ft", 0.3048)),
        "Volume" to listOf(UnitInfo("L", 1.0), UnitInfo("ml", 0.001), UnitInfo("m³", 1000.0), UnitInfo("gal (US)", 3.78541)),
        "Mass" to listOf(UnitInfo("kg", 1.0), UnitInfo("g", 0.001), UnitInfo("mg", 0.000001), UnitInfo("t", 1000.0), UnitInfo("lb", 0.453592)),
        "Data" to listOf(UnitInfo("MB", 1.0), UnitInfo("KB", 1/1024.0), UnitInfo("GB", 1024.0), UnitInfo("TB", 1024.0 * 1024.0)),
        "Speed" to listOf(UnitInfo("m/s", 1.0), UnitInfo("km/h", 1/3.6), UnitInfo("mph", 0.44704)),
        "Time" to listOf(UnitInfo("h", 1.0), UnitInfo("min", 1/60.0), UnitInfo("s", 1/3600.0), UnitInfo("day", 24.0))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedCategory != null) {
                    IconButton(onClick = { selectedCategory = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = selectedCategory ?: "Unit Converter",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedCategory == null) {
                val categories = converterData.keys.toList()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { name ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clickable { selectedCategory = name },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(name, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val units = converterData[selectedCategory] ?: emptyList()
                UnitConverterUI(units)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun UnitConverterUI(units: List<UnitInfo>) {
    var fromValue by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf(units.first()) }
    var toUnit by remember { mutableStateOf(if (units.size > 1) units[1] else units.first()) }

    val convertedValue = try {
        val input = fromValue.toDoubleOrNull() ?: 0.0
        val result = input * (fromUnit.factor / toUnit.factor)
        if (result == result.toLong().toDouble()) result.toLong().toString()
        else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
    } catch (_: Exception) {
        "0"
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column {
            Text("From", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = fromValue,
                    onValueChange = { fromValue = it },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color(0xFF4CAF50)
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium
                )
                UnitSelector(units, fromUnit) { fromUnit = it }
            }
        }

        Column {
            Text("To", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = convertedValue,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                UnitSelector(units, toUnit) { toUnit = it }
            }
        }
    }
}

@Composable
fun UnitSelector(units: List<UnitInfo>, selectedUnit: UnitInfo, onUnitSelected: (UnitInfo) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(selectedUnit.name, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CalculatorButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val samsungGreen = Color(0xFF4CAF50)
    
    val contentColor = when (label) {
        "C" -> Color.Red
        "÷", "×", "-", "+", "( )", "%" -> samsungGreen
        "=" -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    val backgroundColor = if (label == "=") {
        samsungGreen
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 1) 20.sp else 28.sp,
            fontWeight = FontWeight.Normal,
            color = contentColor
        )
    }
}

fun evaluateExpression(expression: String): String {
    if (expression.isEmpty()) return ""
    return try {
        var cleanExpression = expression
            .replace("×", "*")
            .replace("÷", "/")
        
        if (cleanExpression.endsWith("%")) {
             val numStr = cleanExpression.dropLast(1)
             return (numStr.toDoubleOrNull()?.let { it / 100 } ?: "Error").toString()
        }
        
        val openCount = cleanExpression.count { it == '(' }
        val closeCount = cleanExpression.count { it == ')' }
        repeat(openCount - closeCount) {
            cleanExpression += ")"
        }

        val exp = ExpressionBuilder(cleanExpression).build()
        val res = exp.evaluate()
        
        if (res == res.toLong().toDouble()) {
            res.toLong().toString()
        } else {
            String.format(Locale.US, "%.8f", res).trimEnd('0').trimEnd('.')
        }
    } catch (_: Exception) {
        "Error"
    }
}
