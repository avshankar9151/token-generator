package com.example.tokengenerator.ui.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
import androidx.compose.material3.SwipeToDismissBoxValue.Settled
import androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tokengenerator.data.Person
import com.example.tokengenerator.viewmodel.PersonViewModel

@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditPersonScreen(viewModel: PersonViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var memberId by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }
    var isMemberIdError by remember { mutableStateOf(false) }
    val persons by viewModel.allPersons.observeAsState(initial = emptyList())
    var editingPersonId by remember { mutableStateOf<Int?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    val padding by animateDpAsState(targetValue = 16.dp, label = "padding")
    val spacing by animateDpAsState(targetValue = 16.dp, label = "spacing")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                isNameError = it.isBlank()
            },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = isNameError,
            supportingText = { if (isNameError) Text("Name is required") }

        )
        OutlinedTextField(
            value = memberId,
            onValueChange = {
                memberId = it
                isMemberIdError = it.isBlank()
            },
            label = { Text("Member Id") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = isMemberIdError,
            supportingText = { if (isMemberIdError) Text("Age is required") })
        Button(
            onClick = {
                if (name.isNotBlank() && memberId.isNotBlank()) {
                    println("saving")
                    if (isEditing) {
                        editingPersonId?.let {
                            viewModel.update(Person(id = it, name = name, memberId = memberId))
                        }
                        editingPersonId = null
                        isEditing = false
                    } else {
                        viewModel.insert(Person(name = name, memberId = memberId))
                    }
                    println("saved")
                    name = ""
                    memberId = ""
                    isNameError = false
                    isMemberIdError = false
                } else {
                    isMemberIdError = memberId.isBlank()
                    isNameError = name.isBlank()
                }
            }, modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }

        Text("Saved Persons:")
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = 3.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between items
        ) {
            items(items = persons, key = { person -> person.id }) { person ->
                PersonCard(
                    person = person, onUpdate = {
                    name = person.name
                    memberId = person.memberId
                    isEditing = true
                    editingPersonId = person.id
                }, onRemove = {
                    name = ""
                    memberId = ""
                    editingPersonId = null
                    isEditing = false
                    viewModel.delete(person)
                }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PersonCard(
    person: Person, onUpdate: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                StartToEnd -> {
                    onUpdate()
                    false // Do not dismiss, just trigger update
                }

                EndToStart -> {
                    showDialog = true
                    false // Do not dismiss immediately, show confirmation dialog
                }

                Settled -> false
            }
        },
        positionalThreshold = { distance: Float -> distance * 0.85f })
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        modifier = modifier.fillMaxSize(),
        backgroundContent = {
            when (swipeToDismissBoxState.dismissDirection) {
                StartToEnd -> {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit item",
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRoundRect(lerp(Color.LightGray, Color.Blue, swipeToDismissBoxState.progress), cornerRadius = CornerRadius(12.dp.toPx()))
                            }
                            .wrapContentSize(Alignment.CenterStart)
                            .padding(12.dp),
                        tint = Color.White
                    )
                }
                EndToStart -> {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        modifier = Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRoundRect(lerp(Color.LightGray, Color.Red, swipeToDismissBoxState.progress), cornerRadius = CornerRadius(12.dp.toPx()))
                            }
                            .wrapContentSize(Alignment.CenterEnd)
                            .padding(12.dp),
                        tint = Color.White
                    )
                }
                Settled -> {}
            }
        },
        enableDismissFromStartToEnd = true, // Enable swipe to update
        enableDismissFromEndToStart = true  // Enable swipe to remove
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = modifier.padding(16.dp)) {
                Text("Ref. Name: ${person.name}", style = MaterialTheme.typography.titleMedium)
                Text("Member Id: ${person.memberId}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${person.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showDialog = false
                    }, colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red, contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }, colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray, contentColor = Color.White
                    )
                ) { Text("Cancel") }
            })
    }
}
