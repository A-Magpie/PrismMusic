package com.example.ui.dialogs

import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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

                    Column {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Change Art", color = Color.Black, fontSize = 12.sp)
                        }
                        Text("Supports PNG / JPEG", color = TextMuted, fontSize = 11.sp)
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
