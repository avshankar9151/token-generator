package com.example.tokengenerator.ui.screen

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tokengenerator.data.Person
import com.example.tokengenerator.data.Token
import com.example.tokengenerator.viewmodel.PersonViewModel
import kotlinx.coroutines.launch
import com.example.tokengenerator.doPhotoPrint
import com.example.tokengenerator.generateBitmapsForPrinting
import com.example.tokengenerator.viewmodel.TokenViewModel
import kotlin.collections.mutableListOf

@Composable
fun GenerateTokenScreen(modifier: Modifier = Modifier, viewModel: PersonViewModel = viewModel()) {
    val persons by viewModel.allPersons.observeAsState(initial = emptyList())
    var isPopUpVisible by remember { mutableStateOf(false) }
    var selectedPerson: Person? by remember { mutableStateOf(null) }
    if (isPopUpVisible) {
        selectedPerson?.let {
            ShowPopUp(it) {
                isPopUpVisible = false
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        contentPadding = PaddingValues(top = 3.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between items
    ) {
        items(items = persons, key = { person -> person.id }) { person ->
            Card(
                modifier = modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = {
                    isPopUpVisible = true
                    selectedPerson = person
                }
            ) {
                Column(modifier = modifier.padding(16.dp)) {
                    Text("Ref. Name: ${person.name}", style = MaterialTheme.typography.titleMedium)
                    Text("Member Id: ${person.memberId}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ShowPopUp(person: Person, onDismiss: () -> Unit) {
    var count by remember { mutableIntStateOf(1) }
    var isGenerateAndPrintClicked by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Generate Token", style = MaterialTheme.typography.titleLarge)
                Text("Ref. Person: ${person.name}", style = MaterialTheme.typography.bodyLarge)
                Text("M. Id: ${person.memberId}", style = MaterialTheme.typography.bodyLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = { if (count > 1) count-- }) {
                        Text("-")
                    }
                    OutlinedTextField(
                        value = count.toString(),
                        onValueChange = { value -> count = value.toIntOrNull() ?: 1 },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Button(onClick = { count++ }) {
                        Text("+")
                    }
                }
                Button(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        isGenerateAndPrintClicked = true
//                        generateTokenAndPrint(context, person = person, count = count, onDismiss = {
//                            onDismiss()
//                        })
                    }
                ) {
                    Text("Generate & Print")
                }
            }
        }
    }
    if (isGenerateAndPrintClicked) {
        GenerateTokens(context, person, count, onDismiss)
    }
}

@Composable
fun GenerateTokens(context: Context, person: Person, count: Int, onDismiss: () -> Unit, tokenViewModel: TokenViewModel = viewModel()) {
    val addedTokens by tokenViewModel.newlyAddedTokens.collectAsState()
    val tokens = remember { mutableListOf<Token>() }
    for (i in 1..count) {
        val token = Token(personId = person.id, noOfPerson = count)
        tokens.add(token)
    }
    LaunchedEffect(Unit) {
        tokenViewModel.insertAll(tokens)
    }

    if (addedTokens.isNotEmpty()) {
        Log.d("GenerateTokens", "tokens: $tokens")
        Log.d("GenerateTokens", "Newly inserted tokens: $addedTokens")
        generateTokenAndPrint(context, person, addedTokens, onDismiss)
        tokenViewModel.clearNewlyAddedTokens()
        tokens.clear()
    }
}

fun generateTokenAndPrint(context: Context, person: Person, tokens: List<Token>, onDismiss: () -> Unit) {
    (context as ComponentActivity).lifecycleScope.launch {
        val bitmaps = generateBitmapsForPrinting(context, person, tokens)
        Log.d("PrintToken", "Printing bitmaps ${bitmaps.count()}")
        doPhotoPrint(context, bitmaps, onDismiss)
    }
}