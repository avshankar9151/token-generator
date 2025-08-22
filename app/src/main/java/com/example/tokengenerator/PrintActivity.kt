package com.example.tokengenerator

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.util.Log
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.LinearLayout
//import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.example.tokengenerator.data.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MultiPageBitmapAdapter(private val bitmaps: List<Bitmap>, private val documentName: String) : PrintDocumentAdapter() {

    private var pageAttributes: PrintAttributes? = null

    // This method is called to lay out the document.
    // It tells the printing framework about the number of pages.
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        // Handle cancellation
        cancellationSignal?.setOnCancelListener {
            callback.onLayoutCancelled()
        }

        // Store new attributes for later use
        pageAttributes = newAttributes

        if (oldAttributes != newAttributes) {
            // Document info tells the framework about the content
            val info = PrintDocumentInfo.Builder(documentName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                .setPageCount(bitmaps.size) // The key: set page count to the number of bitmaps
                .build()

            callback.onLayoutFinished(info, true)
        } else {
            callback.onLayoutFinished(null, false)
        }
    }

    // This method is called to render the document pages to a PDF file.
    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val pdfDocument = PdfDocument()

        // Iterate through all pages that the framework requests to be written.
        // It may not request all pages at once.
        for (i in pages!!) {
            for (pageNumber in i.start..i.end) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    pdfDocument.close()
                    return
                }

                // Create a page for the current bitmap
                val pageInfo = PdfDocument.PageInfo.Builder(
                    pageAttributes!!.mediaSize!!.widthMils,
                    pageAttributes!!.mediaSize!!.heightMils,
                    pageNumber + 1 // Page numbers are 1-based
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val bitmapToPrint = bitmaps[pageNumber] // Get the correct bitmap from the list

                // Scale the bitmap to fit the page
                val scale =
                    (canvas.width.toFloat() / bitmapToPrint.width).coerceAtMost(canvas.height.toFloat() / bitmapToPrint.height)
                canvas.scale(scale, scale)
                canvas.drawBitmap(bitmapToPrint, 0f, 0f, null)

                pdfDocument.finishPage(page)
            }
        }

        try {
            // Write the PDF document to the destination file
            pdfDocument.writeTo(FileOutputStream(destination.fileDescriptor))
            callback.onWriteFinished(pages) // Notify that writing is complete
        } catch (e: IOException) {
            callback.onWriteFailed(e.toString())
        } finally {
            pdfDocument.close()
        }
    }
}

fun doPhotoPrint(context: Context, bitmaps: List<Bitmap>) {
    val jobName = "TokenPrintJob"
    val printManager = ContextCompat.getSystemService(context, PrintManager::class.java) as PrintManager
    val adapter = MultiPageBitmapAdapter(bitmaps, jobName)
    val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
        .setResolution(PrintAttributes.Resolution("res1","100x100", 100, 100))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()
    printManager.print(jobName, adapter, printAttributes)
}

suspend fun renderComposableToBitmap(activity: Activity, width: Int, height: Int, content: @Composable () -> Unit): Bitmap {
    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            // Create a temporary parent view to hold the ComposeView
            val tempView = LinearLayout(activity)
            tempView.layoutParams = ViewGroup.LayoutParams(width, height)

            // Create the ComposeView to render our content
            val composeView = ComposeView(activity).apply {
                setContent(content)
                layoutParams = ViewGroup.LayoutParams(width, height)
            }

            // Crucially, we add the ComposeView to the temporary parent
            tempView.addView(composeView)

            // THE KEY FIX: Add the temporary parent view to the Activity's root view.
            val decorView = activity.window.decorView as ViewGroup
            decorView.addView(tempView)

            // We use a ViewTreeObserver to wait for the view to be laid out and drawn.
            // This is the most reliable way to ensure the composition is complete.
            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    // Check if the view has a size and is laid out
                    if (composeView.isAttachedToWindow && composeView.width > 0 && composeView.height > 0) {
                        try {
                            // Now that the view is attached and laid out, we can draw it to a bitmap.
                            val bitmap = createBitmap(composeView.width, composeView.height)
                            val canvas = android.graphics.Canvas(bitmap)
                            composeView.draw(canvas)
                            continuation.resume(bitmap)
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            // IMPORTANT: Clean up the listener and remove the view from the hierarchy.
                            tempView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            decorView.removeView(tempView)
                        }
                    }
                }
            }
            tempView.viewTreeObserver.addOnGlobalLayoutListener(listener)

            // Handle cancellation
            continuation.invokeOnCancellation {
                tempView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                decorView.removeView(tempView)
            }
        }
    }
}

suspend fun generateBitmapsForPrinting(context: Context, person: Person, count: Int): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()

    // Using withContext(Dispatchers.IO) to perform heavy data processing
    withContext(Dispatchers.IO) {
        var tokenNumber = 1

        for (i in 1..count) {
//            withContext(Dispatchers.Main) {
//                Toast.makeText(context, "Generating bitmap for token $tokenNumber", Toast.LENGTH_SHORT).show()
//            }
            Log.d("PrintToken", "Generating bitmap for token $tokenNumber")
            // Now, switch to the main thread to render the Composable and capture the image
            val bitmap = renderComposableToBitmap(context as Activity, 900, 1300) {
                // The Composable content to be rendered
                TokenUI(person = person, tokenNumber = tokenNumber, count = count)
            }
            tokenNumber++
            bitmaps.add(bitmap)
        }
    }
    return bitmaps
}

@SuppressLint("SimpleDateFormat")
@Composable
fun TokenUI(person: Person, tokenNumber: Int, count: Int, modifier: Modifier = Modifier) {
    val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Date())



    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(R.drawable.rp_tm_logo_read), "Store Logo", modifier = modifier
            .width(120.dp))
        Spacer(modifier = Modifier.padding(6.dp))

        Text("Visitor Pass", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), fontSize = 20.sp)
        Spacer(modifier = Modifier.padding(4.dp))

        Text("--------------------------------", fontSize = 16.sp) // Separator
        Spacer(modifier = Modifier.padding(4.dp))

        Text("Ref. Name: ${person.name}", style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp)
        Text("M. Id: ${person.memberId}", style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp)
        Text("No. of Person: $count", style = MaterialTheme.typography.bodyLarge, fontSize = 18.sp)
        Spacer(modifier = Modifier.padding(8.dp))

        Text("Token No:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), fontSize = 20.sp)
        Text("$tokenNumber", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), fontSize = 36.sp)

        Spacer(modifier = Modifier.padding(6.dp))


        Text("--------------------------------", fontSize = 16.sp) // Separator
        Text("Issued: $currentDateTime", style = MaterialTheme.typography.bodySmall, fontSize = 12.sp)

        Spacer(modifier = Modifier.padding(8.dp))
        Text("Thank You!", style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp)


    }
    Log.d("TokenUI", "Reached TokenUI end")
}

@Preview(showBackground = true, widthDp = 320, heightDp = 600) // Approximate 80mm width
@Composable
fun TokenUIPreview() {
    MaterialTheme {
        TokenUI(
            person = Person(id = 1, name = "John Doe", memberId = "MEI10001"),
            tokenNumber = 101,
            count = 2)
    }
}