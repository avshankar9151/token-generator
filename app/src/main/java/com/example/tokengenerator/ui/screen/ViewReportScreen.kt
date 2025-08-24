package com.example.tokengenerator.ui.screen

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tokengenerator.R
import com.example.tokengenerator.data.Person
import com.example.tokengenerator.data.Token
import com.example.tokengenerator.viewmodel.PersonViewModel
import com.example.tokengenerator.viewmodel.TokenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val CHANNEL_ID = "download_channel"
private const val NOTIFICATION_ID = 1

@Composable
fun ViewReportScreen(modifier: Modifier = Modifier, tokenViewModel: TokenViewModel = viewModel(), personViewModel: PersonViewModel = viewModel()) {
    val tokens by tokenViewModel.allTokens.observeAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (tokens.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No tokens generated yet.", style = MaterialTheme.typography.headlineSmall)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = modifier
                    .padding(8.dp),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tokens) { token ->
                    val person by produceState<Person?>(initialValue = null, token.personId) {
                        value = withContext(Dispatchers.IO) { personViewModel.get(token.personId.toLong()) }
                    }
                    person?.let { TokenReportItem(person = it, token = token) }
                }
            }
            FloatingActionButton(
                onClick = {
                    downloadReport(context, coroutineScope, tokens, personViewModel)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd) // Align to bottom end of the Box
                    .padding(25.dp)
                    .zIndex(1f)
            ) {
                Icon(Icons.Default.Download, "Download Report")
            }
        }
    }
}

@SuppressLint("MissingPermission")
fun downloadReport(context: Context, coroutineScope: CoroutineScope, tokens: List<Token>, personViewModel: PersonViewModel) {
    createNotificationChannel(context)
    val notificationManager = NotificationManagerCompat.from(context)
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Downloading Report")
        .setContentText("Download in progress")
        .setSmallIcon(R.drawable.rp_tm_logo_read) // Replace with your download icon
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setProgress(0, 0, true) // Indeterminate progress

    // Display the initial notification
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Check for POST_NOTIFICATIONS permission before showing notification
        // For simplicity, assuming permission is granted here. In a real app, you'd request it.
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    } else {
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    coroutineScope.launch(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Token Details")
        val totalRows = tokens.size
        var progress = 0

        // Update notification progress (initial)
        builder.setProgress(totalRows, progress, false)
        notificationManager.notify(NOTIFICATION_ID, builder.build())


        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Token ID")
        headerRow.createCell(1).setCellValue("Person Name")
        headerRow.createCell(2).setCellValue("Member ID")
        headerRow.createCell(3).setCellValue("Number of Person")
        headerRow.createCell(4).setCellValue("Generated At")

        // Populate data rows
        tokens.forEachIndexed { index, tokenData ->
            val person = personViewModel.get(tokenData.personId.toLong())
            person.let { personData ->
                val dataRow = sheet.createRow(index + 1)
                dataRow.createCell(0).setCellValue(tokenData.id.toString())
                dataRow.createCell(1).setCellValue(personData.name)
                dataRow.createCell(2).setCellValue(personData.memberId)
                dataRow.createCell(3).setCellValue(tokenData.noOfPerson.toString())
                dataRow.createCell(4).setCellValue(SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(tokenData.issuedOn))
                progress++
                builder.setProgress(totalRows, progress, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }

        val fileName = "TokenDetails_${System.currentTimeMillis()}.xlsx"
        var outputStream: OutputStream? = null
        var fileUri: android.net.Uri? = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    fileUri = it
                    outputStream = resolver.openOutputStream(it)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                outputStream = FileOutputStream(file)
                fileUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }

            outputStream?.use {
                workbook.write(outputStream)
                // Create an Intent to open the file
                val openFileIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, openFileIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.setContentText("Download complete")
                    .setProgress(0, 0, false) // Remove progress bar
                    .setOngoing(false) // Allow dismissal
                    .setContentIntent(pendingIntent) // Set the intent to open the file
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: Exception) {
            // Update notification for download failure
            builder.setContentText("Download failed")
                .setProgress(0, 0, false)
                .setOngoing(false)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            // Log the error or handle it as needed
            e.printStackTrace()
        } finally {
            outputStream?.close()
            workbook.close()
        }
    }
}

private fun createNotificationChannel(context: Context) {
    val name = "Download Channel"
    val descriptionText = "Channel for download notifications"
    val importance = NotificationManager.IMPORTANCE_LOW
    val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
        description = descriptionText
    }
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

@Composable
fun TokenReportItem(person: Person, token: Token, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Token ID: ${token.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Person Name:", style = MaterialTheme.typography.bodyMedium)
                Text(text = person.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Member Id:", style = MaterialTheme.typography.bodyMedium)
                Text(text = person.memberId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Number of Person:", style = MaterialTheme.typography.bodyMedium)
                Text(text = token.noOfPerson.toString(), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Generated At:", style = MaterialTheme.typography.bodyMedium)
                Text(text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(token.issuedOn), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}