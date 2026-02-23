package com.samoba.imagepickerkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.samoba.imagepickerkit.ui.theme.ImagePickerKitTheme
import com.samoba.imagepickkit.ImagePickerResult
import com.samoba.imagepickkit.ImagePickerScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImagePickerKitTheme {
                val context = LocalContext.current
                var showPicker by remember { mutableStateOf(false) }
                var selectedCount by remember { mutableIntStateOf(0) }
                var selectedUris by remember { mutableStateOf(emptyList<String>()) }

                val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val allGranted = permissions.entries.all { it.value }
                    if (allGranted) {
                        showPicker = true
                    } else {
                        Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

                fun checkAndRequestPermissions() {
                    val allGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (allGranted) {
                        showPicker = true
                    } else {
                        permissionLauncher.launch(permissionsToRequest)
                    }
                }

                // Custom gradient for the continue button
                val gradientBrush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF6366F1), // Indigo
                        Color(0xFF8B5CF6)  // Purple
                    )
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showPicker) {
                        ImagePickerScreen(
                            config = com.samoba.imagepickkit.ImagePickerConfig(
                                maxSelection = 10,
                                selectedUris = selectedUris,
                                // Custom TopBar
                                topBar = { state, onBackClick, onSelectAllClick, onClearSelectionClick ->
                                    TopAppBar(
                                        title = {
                                            Column {
                                                Text(
                                                    text = "Select Photos",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (state.selectedCount > 0) {
                                                    Text(
                                                        text = "${state.selectedCount} of 10 selected",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = onBackClick) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close"
                                                )
                                            }
                                        },
                                        actions = {
                                            // Select All checkbox
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Checkbox(
                                                    checked = state.isSelectAllChecked,
                                                    onCheckedChange = { onSelectAllClick() },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color(0xFF6366F1)
                                                    )
                                                )
                                                Text(
                                                    text = "All",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            // Clear selection button
                                            if (state.selectedCount > 0) {
                                                IconButton(onClick = onClearSelectionClick) {
                                                    Icon(
                                                        imageVector = Icons.Default.Done,
                                                        contentDescription = "Clear selection",
                                                        tint = Color(0xFF6366F1)
                                                    )
                                                }
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                },
                                // Custom BottomBar with gradient button
                                bottomBar = { state, onContinueClick ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .navigationBarsPadding()
                                            .padding(16.dp)
                                    ) {
                                        Button(
                                            onClick = onContinueClick,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.Transparent
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(gradientBrush, RoundedCornerShape(16.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Done,
                                                        contentDescription = null,
                                                        tint = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Continue with ${state.selectedCount} photos",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            ),
                            onResult = { result ->
                                when (result) {
                                    is ImagePickerResult.Success -> {
                                        selectedUris = result.images.map { it.uri.toString() }
                                        selectedCount = result.images.size
                                        showPicker = false
                                    }
                                    ImagePickerResult.Cancelled -> {
                                        showPicker = false
                                    }
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Selected Images: $selectedCount")
                            Button(onClick = { checkAndRequestPermissions() }) {
                                Text("Open Image Picker")
                            }
                        }
                    }
                }
            }
        }
    }
}