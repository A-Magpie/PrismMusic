package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SolidCircleThumb
import com.example.ui.components.SleekSliderTrack
import com.example.ui.components.SolidCircleThumb
import com.example.ui.components.SleekSliderTrack
import com.example.ui.components.VerticalEqualizerBar
import com.example.ui.theme.DarkCardGlass
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MusicViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EqualizerDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val dynamicAccent by viewModel.dynamicAccentColor.collectAsState()
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val bassBoost by viewModel.bassBoost.collectAsState()
    val virtualizer by viewModel.virtualizer.collectAsState()
    val reverbPreset by viewModel.reverbPreset.collectAsState()

    val bandFrequencies = listOf(
        "31Hz", "62Hz", "125Hz", "250Hz", "500Hz",
        "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
    )

    val reverbNames = listOf(
        "None", "Small Room", "Medium Room", "Large Room",
        "Medium Hall", "Large Hall", "Plate"
    )
    var reverbMenuOpen by remember { mutableStateOf(false) }

    val verticalScroll = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        modifier = Modifier.testTag("dialog_equalizer"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "10-Band Equalizer & FX",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = eqEnabled,
                    onCheckedChange = { viewModel.setEqEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = dynamicAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 10-Band Sliders (Horizontal scrollable bands)
                Text(
                    text = "10 FREQUENCY BANDS",
                    color = dynamicAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                val bandsScroll = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(bandsScroll)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    bandFrequencies.forEachIndexed { index, freq ->
                        val level = if (index in bandLevels.indices) bandLevels[index] else 0
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(44.dp)
                        ) {
                            Text(
                                text = "${level / 10}dB",
                                color = if (eqEnabled) dynamicAccent else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            VerticalEqualizerBar(
                                value = level,
                                onValueChange = { newLvl ->
                                    if (eqEnabled) viewModel.setBandLevel(index, newLvl)
                                },
                                enabled = eqEnabled,
                                activeColor = dynamicAccent,
                                inactiveColor = Color(0x33FFFFFF),
                                thumbColor = if (eqEnabled) dynamicAccent else TextMuted,
                                width = 36.dp,
                                height = 140.dp
                            )

                            Text(
                                text = freq,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Preset: Reset to Flat
                OutlinedButton(
                    onClick = {
                        for (i in 0 until 10) {
                            viewModel.setBandLevel(i, 0)
                        }
                    },
                    enabled = eqEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Flat", color = if (eqEnabled) dynamicAccent else TextMuted)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // FX Section: Bass Boost
                Text(
                    text = "AUDIO FX",
                    color = dynamicAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bass Boost", color = TextWhite, fontSize = 13.sp)
                        Text("${bassBoost.toInt() / 10}%", color = dynamicAccent, fontSize = 13.sp)
                    }
                    Slider(
                        value = bassBoost.toFloat(),
                        onValueChange = { viewModel.setBassBoost(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = eqEnabled,
                        track = { sliderState ->
                            SleekSliderTrack(
                                sliderState = sliderState,
                                activeTrackColor = dynamicAccent,
                                inactiveTrackColor = Color(0x33FFFFFF),
                                trackHeight = 4.dp
                            )
                        },
                        thumb = {
                            SolidCircleThumb(
                                color = if (eqEnabled) dynamicAccent else TextMuted,
                                size = 16.dp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // FX Section: Virtualizer (3D Audio)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3D Virtualizer", color = TextWhite, fontSize = 13.sp)
                        Text("${virtualizer.toInt() / 10}%", color = dynamicAccent, fontSize = 13.sp)
                    }
                    Slider(
                        value = virtualizer.toFloat(),
                        onValueChange = { viewModel.setVirtualizer(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = eqEnabled,
                        track = { sliderState ->
                            SleekSliderTrack(
                                sliderState = sliderState,
                                activeTrackColor = dynamicAccent,
                                inactiveTrackColor = Color(0x33FFFFFF),
                                trackHeight = 4.dp
                            )
                        },
                        thumb = {
                            SolidCircleThumb(
                                color = if (eqEnabled) dynamicAccent else TextMuted,
                                size = 16.dp
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Reverb Preset selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Reverb Environment", color = TextWhite, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedButton(
                            onClick = { if (eqEnabled) reverbMenuOpen = true },
                            enabled = eqEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val currentName = reverbNames.getOrElse(reverbPreset.toInt()) { "None" }
                            Text(currentName, color = if (eqEnabled) TextWhite else TextMuted)
                        }
                        DropdownMenu(
                            expanded = reverbMenuOpen,
                            onDismissRequest = { reverbMenuOpen = false },
                            modifier = Modifier.background(DarkCardGlass)
                        ) {
                            reverbNames.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name, color = TextWhite) },
                                    onClick = {
                                        viewModel.setReverbPreset(idx.toShort())
                                        reverbMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = dynamicAccent)
            ) {
                Text("Done", color = Color.Black)
            }
        }
    )
}
