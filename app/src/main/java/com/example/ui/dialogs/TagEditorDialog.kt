package com.example.ui.dialogs

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Song
import com.example.ui.components.SquareCoverArt
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun TagEditorDialog(
    song: Song,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()

    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var genre by remember { mutableStateOf(song.genre) }
    var lyrics by remember { mutableStateOf(song.lyrics) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var newCoverBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Photo picker launcher (Google Play Policy compliant: zero-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                newCoverBytes = inputStream?.readBytes()
            } catch (_: Exception) {}
        }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        modifier = Modifier.testTag("dialog_tag_editor"),
        title = {
            Text(
                text = "ID3 Tag Editor",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cover Art picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SquareCoverArt(
                        albumArtUri = selectedImageUri?.toString() ?: song.albumArtUri,
                        contentDescription = "Cover Art",
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .size(72.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Change Art", color = Color.Black, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    saveCoverArtToGallery(context, song, newCoverBytes, selectedImageUri)
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = dynamicAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Save Art", color = dynamicAccent, fontSize = 12.sp)
                            }
                        }
                        Text("Save cover to gallery or choose a new one", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Artist
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Album
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Genre
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Lyrics
                OutlinedTextField(
                    value = lyrics,
                    onValueChange = { lyrics = it },
                    label = { Text("Lyrics (Synced [mm:ss.xx] or Static)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = dynamicAccent,
                        unfocusedBorderColor = DarkSurfaceElevated
                    ),
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveTags(
                        song = song,
                        title = title.trim(),
                        artist = artist.trim(),
                        album = album.trim(),
                        genre = genre.trim(),
                        lyrics = lyrics.trim(),
                        coverArtBytes = newCoverBytes
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
            ) {
                Text("Save to File", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

fun saveCoverArtToGallery(
    context: Context,
    song: Song,
    customBytes: ByteArray?,
    selectedUri: Uri?
) {
    try {
        var bitmap: Bitmap? = null

        // 1. If user selected a new image in this dialog
        if (customBytes != null) {
            bitmap = BitmapFactory.decodeByteArray(customBytes, 0, customBytes.size)
        } else if (selectedUri != null) {
            try {
                bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, selectedUri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, selectedUri)
                }
            } catch (_: Exception) {}
        }

        // 2. If no new selection, extract from song's albumArtUri
        if (bitmap == null && !song.albumArtUri.isNullOrBlank()) {
            try {
                val artUri = Uri.parse(song.albumArtUri)
                context.contentResolver.openInputStream(artUri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }

        // 3. Fallback: Embedded picture via MediaMetadataRetriever
        if (bitmap == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (song.uri.isNotBlank()) {
                    retriever.setDataSource(context, Uri.parse(song.uri))
                } else {
                    retriever.setDataSource(song.path)
                }
                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                }
            } catch (_: Exception) {
                try {
                    retriever.setDataSource(song.path)
                    val artBytes = retriever.embeddedPicture
                    if (artBytes != null) {
                        bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    }
                } catch (_: Exception) {}
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        if (bitmap == null) {
            Toast.makeText(context, "No cover art found to save", Toast.LENGTH_SHORT).show()
            return
        }

        // Save bitmap to MediaStore Images Gallery
        val cleanArtist = song.artist.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(15)
        val cleanTitle = song.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(15)
        val filename = "Cover_${cleanArtist}_${cleanTitle}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PrismMusic")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val insertedUri = context.contentResolver.insert(collection, values)
        if (insertedUri != null) {
            context.contentResolver.openOutputStream(insertedUri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(insertedUri, values, null, null)
            }

            Toast.makeText(context, "Cover art saved to Gallery!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save cover art", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        com.example.util.AppLogger.w("TagEditorDialog", "Failed to save cover to gallery", e)
        Toast.makeText(context, "Error saving cover: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
