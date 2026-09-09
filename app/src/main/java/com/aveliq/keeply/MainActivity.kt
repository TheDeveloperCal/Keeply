package com.aveliq.keeply

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

private val Ink = Color(0xFFF5F4F7)
private val Muted = Color(0xFFC6C2CA)
private val Bg = Color(0xFF080A10)
private val CardBg = Color(0xFF121721)
private val Accent = Color(0xFF22C7F5)
private val AccentDark = Color(0xFF162A38)
private val Purple = Color(0xFF8A5CF6)

private data class KeeplyItem(
    val id: Long,
    val name: String,
    val category: String,
    val store: String,
    val price: String,
    val purchaseDate: String,
    val returnDate: String,
    val warranty: String,
    val notes: String,
    val receiptText: String,
    val imagePath: String?,
    val barcode: String?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category)
        put("store", store)
        put("price", price)
        put("purchaseDate", purchaseDate)
        put("returnDate", returnDate)
        put("warranty", warranty)
        put("notes", notes)
        put("receiptText", receiptText)
        put("imagePath", imagePath ?: JSONObject.NULL)
        put("barcode", barcode ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): KeeplyItem = KeeplyItem(
            id = o.optLong("id"),
            name = o.optString("name"),
            category = o.optString("category"),
            store = o.optString("store"),
            price = o.optString("price"),
            purchaseDate = o.optString("purchaseDate"),
            returnDate = o.optString("returnDate"),
            warranty = o.optString("warranty"),
            notes = o.optString("notes"),
            receiptText = o.optString("receiptText"),
            imagePath = if (o.isNull("imagePath")) null else o.optString("imagePath"),
            barcode = if (o.isNull("barcode")) null else o.optString("barcode")
        )
    }
}

private data class ScanResult(
    val likelyReceipt: Boolean,
    val name: String,
    val category: String,
    val store: String,
    val price: String,
    val purchaseDate: String,
    val receiptText: String,
    val barcode: String?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KeeplyApp() }
    }
}

@Composable
private fun KeeplyApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("keeply", Context.MODE_PRIVATE) }
    var items by remember { mutableStateOf(loadItems(prefs)) }
    var tab by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<KeeplyItem?>(null) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }

    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colors = if (dark) {
        androidx.compose.material3.darkColorScheme(
            background = Bg,
            surface = CardBg,
            primary = Accent,
            secondary = Purple,
            onBackground = Ink,
            onSurface = Ink,
            onSurfaceVariant = Muted
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF006A8A),
            secondary = Color(0xFF6B3DD2)
        )
    }

    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (screen) {
                "scan" -> SmartScanScreen(
                    onBack = { screen = "home" },
                    onSaved = { item ->
                        items = listOf(item) + items
                        saveItems(prefs, items)
                        screen = "home"
                    }
                )
                "edit" -> selected?.let { item ->
                    ItemEditorScreen(
                        item = item,
                        onBack = { screen = "home" },
                        onSave = { updated ->
                            items = items.map { if (it.id == updated.id) updated else it }
                            saveItems(prefs, items)
                            screen = "home"
                        },
                        onDelete = {
                            items = items.filterNot { it.id == item.id }
                            saveItems(prefs, items)
                            screen = "home"
                        }
                    )
                }
                "about" -> AboutScreen { screen = "settings" }
                else -> Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar(containerColor = if (dark) Color(0xFF11151C) else MaterialTheme.colorScheme.surface) {
                            NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                            NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("Items") })
                            NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (tab) {
                            0 -> HomeScreen(items, { screen = "scan" }) { selected = it; screen = "edit" }
                            1 -> ItemsScreen(items, { screen = "scan" }) { selected = it; screen = "edit" }
                            else -> SettingsScreen(
                                items = items,
                                theme = theme,
                                onTheme = { value ->
                                    theme = value
                                    prefs.edit().putString("theme", value).apply()
                                },
                                onAbout = { screen = "about" },
                                onImport = { uri ->
                                    val imported = importData(context, uri)
                                    items = imported
                                    saveItems(prefs, imported)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    val context = LocalContext.current
    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, com.aveliq.keeply.R.drawable.keeply_logo_foreground).asImageBitmap()
    }
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(bitmap, "Keeply", Modifier.size(54.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Keeply", fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("by Aveliq", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
    }
}

@Composable
private fun HomeScreen(items: List<KeeplyItem>, onAdd: () -> Unit, onOpen: (KeeplyItem) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { BrandHeader() }
        item {
            Text("Your stuff, remembered.", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "Keep receipts, warranties and important details together.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                modifier = Modifier.padding(20.dp, 26.dp, 20.dp, 20.dp).fillMaxWidth()
            ) {
                Row(Modifier.padding(25.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("YOUR COLLECTION", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Accent)
                        Text("${items.size} saved ${if (items.size == 1) "item" else "items"}", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (items.isEmpty()) "Start by scanning something you own." else "Your important details are in one place.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Box(Modifier.size(68.dp).clip(CircleShape).background(AccentDark), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Inventory2, null, tint = Accent, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        item { Text("Quick add", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp)) }
        item {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.padding(20.dp, 16.dp).fillMaxWidth().height(66.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF113A73))
                Spacer(Modifier.width(12.dp))
                Text("Smart Scan", color = Color(0xFF113A73), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { Text("Recently added", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 8.dp)) }
        if (items.isEmpty()) {
            item {
                Text("Your saved items will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp), fontSize = 16.sp)
            }
        }
        lazyItems(items.take(6)) { item -> ItemRow(item, onOpen) }
    }
}

@Composable
private fun ItemRow(item: KeeplyItem, onOpen: (KeeplyItem) -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardBg)
            .clickable { onOpen(item) }
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemImage(item.imagePath, Modifier.size(78.dp).clip(RoundedCornerShape(17.dp)))
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name.ifBlank { "Untitled item" }, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                buildString {
                    append(item.category.ifBlank { "Item" })
                    if (item.store.isNotBlank()) append(" · ${item.store}")
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            if (item.purchaseDate.isNotBlank()) Text(item.purchaseDate, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        if (item.price.isNotBlank()) Text(item.price, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemsScreen(items: List<KeeplyItem>, onAdd: () -> Unit, onOpen: (KeeplyItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = items.filter {
        listOf(it.name, it.category, it.store, it.barcode ?: "").any { value -> value.contains(query, ignoreCase = true) }
    }
    Column(Modifier.fillMaxSize()) {
        Text("Items", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 10.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search your stuff") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        )
        LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp), modifier = Modifier.weight(1f)) {
            lazyItems(filtered) { item -> ItemRow(item, onOpen) }
        }
        FloatingActionButton(onClick = onAdd, containerColor = Accent, contentColor = Color(0xFF113A73), modifier = Modifier.align(Alignment.End).padding(20.dp)) {
            Icon(Icons.Default.Add, null)
        }
    }
}

@Composable
private fun SettingsScreen(
    items: List<KeeplyItem>,
    theme: String,
    onTheme: (String) -> Unit,
    onAbout: () -> Unit,
    onImport: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showTheme by remember { mutableStateOf(false) }
    var reminderWarranty by remember { mutableStateOf(getPrefBoolean(context, "reminder_warranty", true)) }
    var reminderReturn by remember { mutableStateOf(getPrefBoolean(context, "reminder_return", true)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImport) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { out -> out.write(itemsToJson(items).toString(2)) }
            }
        }
    }

    if (showTheme) ThemeDialog(theme, onTheme, { showTheme = false })

    LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
        item { Text("Settings", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 22.dp)) }
        item { SettingsSection("Appearance") }
        item {
            SettingRow(Icons.Default.DarkMode, "Appearance", when (theme) { "dark" -> "Dark"; "light" -> "Light"; else -> "System default" }) { showTheme = true }
        }
        item { SettingsSection("Notifications") }
        item {
            ToggleSetting("Warranty reminders", "Remind me before a warranty ends", reminderWarranty) {
                reminderWarranty = it
                context.getSharedPreferences("keeply", Context.MODE_PRIVATE).edit().putBoolean("reminder_warranty", it).apply()
            }
        }
        item {
            ToggleSetting("Return reminders", "Remind me before a return deadline", reminderReturn) {
                reminderReturn = it
                context.getSharedPreferences("keeply", Context.MODE_PRIVATE).edit().putBoolean("reminder_return", it).apply()
            }
        }
        item { SettingsSection("Privacy & data") }
        item { SettingRow(Icons.Default.FileDownload, "Export my data", "Save a portable Keeply backup") { exporter.launch("keeply-backup.json") } }
        item { SettingRow(Icons.Default.FileUpload, "Import data", "Restore a Keeply backup") { picker.launch(arrayOf("application/json", "text/plain")) } }
        item { SettingsSection("About") }
        item { SettingRow(Icons.Default.Info, "About Keeply", "Purpose, creator and app information", onAbout) }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(title.uppercase(Locale.US), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 18.dp, 20.dp, 4.dp))
}

@Composable
private fun SettingRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(27.dp))
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Muted)
    }
}

@Composable
private fun ToggleSetting(title: String, subtitle: String, value: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Switch(checked = value, onCheckedChange = onChanged)
    }
}

@Composable
private fun ThemeDialog(theme: String, onTheme: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                listOf("system" to "System default", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onTheme(value); onDismiss() }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = value == theme, onClick = { onTheme(value); onDismiss() })
                        Spacer(Modifier.width(8.dp))
                        Text(label, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember {
        BitmapFactory.decodeResource(context.resources, com.aveliq.keeply.R.drawable.keeply_logo_foreground).asImageBitmap()
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("About Keeply", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = bitmap,
                contentDescription = "Keeply logo",
                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(Bg),
                contentScale = ContentScale.Crop
            )
            Text("Keeply", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            Text("Your stuff, remembered.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
            Text(
                "Keeply helps you save receipts, product details, warranties, return deadlines and notes together, so the things you own are easier to find when you need them.",
                fontSize = 17.sp,
                lineHeight = 25.sp,
                modifier = Modifier.padding(top = 26.dp)
            )
            Spacer(Modifier.height(26.dp))
            Text("Developed by Mikey Calhoun", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Founder of Aveliq", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
            Spacer(Modifier.height(18.dp))
            Text("Development build", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun SmartScanScreen(onBack: () -> Unit, onSaved: (KeeplyItem) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var cameraAllowed by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraAllowed = it }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var liveStatus by remember { mutableStateOf("Point at a product or receipt") }
    var liveDetail by remember { mutableStateOf("Smart Scan is analyzing the camera view") }
    var detectedKind by remember { mutableStateOf("Looking") }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            val path = withContext(Dispatchers.IO) { copyUriToInternal(context, uri) }
            imagePath = path
            result = scanFile(context, path)
            busy = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    fun capture() {
        if (!cameraAllowed) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        val capture = imageCapture ?: return
        val file = File(context.filesDir, "keeply_${System.currentTimeMillis()}.jpg")
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        busy = true
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    imagePath = file.absolutePath
                    scope.launch {
                        result = scanFile(context, file.absolutePath)
                        busy = false
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    liveStatus = "Couldn't capture image"
                    liveDetail = exception.message ?: "Try again"
                    busy = false
                }
            }
        )
    }

    if (result != null) {
        ScanReview(
            result = result!!,
            imagePath = imagePath,
            onRetake = { result = null; imagePath = null; busy = false },
            onSave = { scan ->
                onSaved(
                    KeeplyItem(
                        id = System.currentTimeMillis(),
                        name = scan.name.ifBlank { "Untitled item" },
                        category = scan.category,
                        store = scan.store,
                        price = scan.price,
                        purchaseDate = scan.purchaseDate,
                        returnDate = "",
                        warranty = "",
                        notes = "",
                        receiptText = scan.receiptText,
                        imagePath = imagePath,
                        barcode = scan.barcode
                    )
                )
            }
        )
        return
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize()) {
            if (cameraAllowed) {
                LiveCameraPreview(
                    lifecycleOwner = lifecycleOwner,
                    executor = cameraExecutor,
                    onImageCaptureReady = { imageCapture = it },
                    onDetection = { kind, status, detail ->
                        detectedKind = kind
                        liveStatus = status
                        liveDetail = detail
                    }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                        Icon(Icons.Default.CameraAlt, null, tint = Accent, modifier = Modifier.size(64.dp))
                        Text("Camera access needed", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                        Text("Keeply uses the live camera to recognize products, receipts, text, and barcodes.", color = Color.White.copy(alpha = .72f), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(top = 10.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.padding(top = 20.dp)) {
                            Text("Allow camera", color = Color(0xFF113A73), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Top bar
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .42f)).padding(top = 8.dp, start = 8.dp, end = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("Smart Scan", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Live product + receipt recognition", color = Color.White.copy(alpha = .74f), fontSize = 12.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = .46f)).padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Accent))
                    Spacer(Modifier.width(7.dp))
                    Text("SMART", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Lens-style scan frame
            Box(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 135.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxWidth().height(310.dp)) {
                    ScanCorner(Alignment.TopStart)
                    ScanCorner(Alignment.TopEnd)
                    ScanCorner(Alignment.BottomStart)
                    ScanCorner(Alignment.BottomEnd)
                    if (!busy) {
                        Box(
                            Modifier.align(Alignment.Center).fillMaxWidth(0.6f).height(2.dp).background(Accent.copy(alpha = .82f))
                        )
                    }
                }
            }

            // Bottom scanner chrome: always stays above the Android navigation area.
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .72f)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 15.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(liveStatus, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                Text(detectedKind.uppercase(Locale.US), color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                liveDetail,
                                color = Color.White.copy(alpha = .72f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 2
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth().height(88.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        RoundCameraAction(Icons.Default.PhotoLibrary, "Gallery") { galleryPicker.launch("image/*") }
                        Box(
                            Modifier.size(78.dp).clip(CircleShape).background(Color.White).padding(7.dp).clickable(enabled = !busy) { capture() },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                                if (busy) androidx.compose.material3.CircularProgressIndicator(color = Color(0xFF113A73), strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                                else Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF113A73), modifier = Modifier.size(30.dp))
                            }
                        }
                        RoundCameraAction(Icons.Default.Search, "Product") { capture() }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundCameraAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = .5f))) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(23.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ScanCorner(alignment: Alignment) {
    Box(Modifier.fillMaxSize()) {
        val horizontalAlignment = when (alignment) {
            Alignment.TopStart, Alignment.BottomStart -> Alignment.TopStart
            Alignment.TopEnd, Alignment.BottomEnd -> Alignment.TopEnd
            else -> Alignment.TopStart
        }
        val verticalAlignment = when (alignment) {
            Alignment.TopStart, Alignment.TopEnd -> Alignment.TopStart
            Alignment.BottomStart, Alignment.BottomEnd -> Alignment.BottomStart
            else -> Alignment.TopStart
        }
        Box(Modifier.width(58.dp).height(4.dp).background(Accent).align(horizontalAlignment))
        Box(Modifier.width(4.dp).height(58.dp).background(Accent).align(verticalAlignment))
    }
}

@Composable
private fun LiveCameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    executor: java.util.concurrent.Executor,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onDetection: (kind: String, status: String, detail: String) -> Unit
) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setJpegQuality(90).build()
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val barcodeScanner = BarcodeScanning.getClient()
                val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val busyFrame = AtomicBoolean(false)
                var lastRun = 0L

                analysis.setAnalyzer(executor) { imageProxy ->
                    val now = System.currentTimeMillis()
                    if (now - lastRun < 650L || !busyFrame.compareAndSet(false, true)) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastRun = now
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        busyFrame.set(false)
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    val barcodeTask = barcodeScanner.process(input)
                    val textTask = textRecognizer.process(input)

                    Tasks.whenAllSuccess<Any>(barcodeTask, textTask)
                        .addOnSuccessListener { values ->
                            val barcodes = values.getOrNull(0) as? List<*> ?: emptyList<Any>()
                            val recognizedText = (values.getOrNull(1) as? com.google.mlkit.vision.text.Text)?.text.orEmpty()
                            val lower = recognizedText.lowercase(Locale.US)
                            val barcodeValue = (barcodes.firstOrNull() as? com.google.mlkit.vision.barcode.common.Barcode)?.rawValue.orEmpty()
                            val receiptWords = listOf("subtotal", "total", "tax", "receipt", "amount due", "change")
                            val receiptHits = receiptWords.count { lower.contains(it) }
                            when {
                                receiptHits >= 2 -> onDetection("Receipt", "Receipt detected", "Text is being read live — tap the shutter to capture it")
                                barcodeValue.isNotBlank() -> onDetection("Barcode", "Barcode detected", "Keep the product centered, then capture when ready")
                                lower.contains("powerfort") -> onDetection("Product", "PowerFort detected", "Keeply found product text in the live camera")
                                lower.contains("samsung") -> onDetection("Product", "Samsung detected", "Keeply found product text in the live camera")
                                lower.contains("apple") -> onDetection("Product", "Apple detected", "Keeply found product text in the live camera")
                                else -> onDetection("Looking", "Scanning…", "Move closer and keep the item steady")
                            }
                        }
                        .addOnCompleteListener {
                            busyFrame.set(false)
                            imageProxy.close()
                        }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                    analysis
                )
                onImageCaptureReady(capture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanReview(result: ScanResult, imagePath: String?, onRetake: () -> Unit, onSave: (ScanResult) -> Unit) {
    var current by remember(result) { mutableStateOf(result) }
    LazyColumn(contentPadding = PaddingValues(bottom = 35.dp)) {
        item {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRetake) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Review scan", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { ItemImage(imagePath, Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(25.dp)), true) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = AccentDark), modifier = Modifier.padding(20.dp, 16.dp).fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Accent)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (current.likelyReceipt) "Receipt detected" else "Product detected", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Review and edit before saving.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
        item { EditorField("Name", current.name) { current = current.copy(name = it) } }
        item { EditorField("Category", current.category) { current = current.copy(category = it) } }
        item { EditorField("Store", current.store) { current = current.copy(store = it) } }
        item { EditorField("Price / total", current.price, KeyboardType.Decimal) { current = current.copy(price = it) } }
        item { EditorField("Purchase date", current.purchaseDate) { current = current.copy(purchaseDate = it) } }
        item { EditorField("Receipt text", current.receiptText, multiline = true) { current = current.copy(receiptText = it) } }
        if (!current.barcode.isNullOrBlank()) {
            item {
                Text("Barcode: ${current.barcode}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 13.sp)
            }
        }
        item {
            Button(
                onClick = { onSave(current) },
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.padding(20.dp).fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Save to Keeply", color = Color(0xFF113A73), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

@Composable
private fun ItemEditorScreen(item: KeeplyItem, onBack: () -> Unit, onSave: (KeeplyItem) -> Unit, onDelete: () -> Unit) {
    var current by remember(item) { mutableStateOf(item) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) current = current.copy(imagePath = copyUriToInternal(context, uri))
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("Edit item", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { ItemImage(current.imagePath, Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(210.dp).clip(RoundedCornerShape(25.dp)), true) }
        item { OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.padding(20.dp, 10.dp).fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Change photo") } }
        item { EditorField("Name", current.name) { current = current.copy(name = it) } }
        item { EditorField("Category", current.category) { current = current.copy(category = it) } }
        item { EditorField("Store", current.store) { current = current.copy(store = it) } }
        item { EditorField("Price", current.price, KeyboardType.Decimal) { current = current.copy(price = it) } }
        item { EditorField("Purchase date", current.purchaseDate) { current = current.copy(purchaseDate = it) } }
        item { EditorField("Return deadline", current.returnDate) { current = current.copy(returnDate = it) } }
        item { EditorField("Warranty", current.warranty) { current = current.copy(warranty = it) } }
        item { EditorField("Notes", current.notes, multiline = true) { current = current.copy(notes = it) } }
        item { EditorField("Receipt text", current.receiptText, multiline = true) { current = current.copy(receiptText = it) } }
        if (!current.barcode.isNullOrBlank()) item { Text("Barcode: ${current.barcode}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
        item {
            Button(onClick = { onSave(current) }, colors = ButtonDefaults.buttonColors(containerColor = Accent), modifier = Modifier.padding(20.dp).fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Save changes", color = Color(0xFF113A73), fontWeight = FontWeight.Bold)
            }
        }
        item { OutlinedButton(onClick = onDelete, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete item") } }
    }
}

@Composable
private fun EditorField(label: String, value: String, keyboard: KeyboardType = KeyboardType.Text, multiline: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp).fillMaxWidth(),
        minLines = if (multiline) 3 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ItemImage(path: String?, modifier: Modifier, large: Boolean = false) {
    val bitmap = remember(path) { path?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() } }
    if (bitmap != null) {
        Image(bitmap.asImageBitmap(), null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(Color(0xFF232A34)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Inventory2, null, tint = Muted, modifier = Modifier.size(if (large) 54.dp else 30.dp))
        }
    }
}

private suspend fun scanFile(context: Context, path: String): ScanResult = withContext(Dispatchers.IO) {
    val image = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val text = try { Tasks.await(recognizer.process(image)).text } catch (_: Exception) { "" } finally { recognizer.close() }
    val scanner = BarcodeScanning.getClient()
    val barcode = try { Tasks.await(scanner.process(image)).firstOrNull()?.rawValue } catch (_: Exception) { null } finally { scanner.close() }
    parseScan(text, barcode)
}

private fun parseScan(text: String, barcode: String?): ScanResult {
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
    val lower = text.lowercase(Locale.US)
    val receiptWords = listOf("subtotal", "tax", "total", "receipt", "amount due", "change")
    val likelyReceipt = lines.size >= 5 || receiptWords.count { lower.contains(it) } >= 2

    val totalPattern = Pattern.compile("(?i)(grand\\s+total|total|amount\\s+due)\\s*[:#-]?\\s*\\$?\\s*([0-9]+[.,][0-9]{2})")
    val matcher = totalPattern.matcher(text)
    var price = ""
    while (matcher.find()) price = matcher.group(2).orEmpty()

    val datePattern = Pattern.compile("\\b(20\\d{2}[-/]\\d{1,2}[-/]\\d{1,2}|\\d{1,2}[-/]\\d{1,2}[-/]20\\d{2})\\b")
    val dateMatcher = datePattern.matcher(text)
    val date = if (dateMatcher.find()) dateMatcher.group(1).orEmpty() else ""

    val store = if (likelyReceipt) lines.firstOrNull { line -> line.length in 3..42 && line.count { c -> c.isDigit() } < 4 }.orEmpty() else ""
    val candidate = lines.maxByOrNull { it.length }.orEmpty()

    val category = when {
        lower.contains("powerfort") || lower.contains("power station") || lower.contains("power generator") -> "POWER"
        lower.contains("laptop") || lower.contains("computer") -> "COMPUTER"
        lower.contains("phone") || lower.contains("galaxy") || lower.contains("iphone") -> "PHONE"
        lower.contains("battery") || lower.contains("charger") -> "ELECTRONICS"
        lower.contains("drill") || lower.contains("tool") -> "TOOLS"
        likelyReceipt -> "PURCHASE"
        else -> "ITEM"
    }

    val name = if (likelyReceipt) {
        lines.firstOrNull { it.length in 4..60 && it.any(Char::isLetter) && !receiptWords.any { word -> it.lowercase(Locale.US).contains(word) } } ?: candidate
    } else candidate

    return ScanResult(likelyReceipt, name, category, store, price, date, text, barcode)
}

private fun copyUriToInternal(context: Context, uri: Uri): String {
    val file = File(context.filesDir, "keeply_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(uri)?.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    return file.absolutePath
}

private fun loadItems(prefs: android.content.SharedPreferences): List<KeeplyItem> = runCatching {
    val array = JSONArray(prefs.getString("items", "[]"))
    val list = mutableListOf<KeeplyItem>()
    for (i in 0 until array.length()) list += KeeplyItem.fromJson(array.getJSONObject(i))
    list
}.getOrDefault(emptyList())

private fun itemsToJson(items: List<KeeplyItem>): JSONArray {
    val array = JSONArray()
    items.forEach { array.put(it.toJson()) }
    return array
}

private fun saveItems(prefs: android.content.SharedPreferences, items: List<KeeplyItem>) {
    prefs.edit().putString("items", itemsToJson(items).toString()).apply()
}

private fun importData(context: Context, uri: Uri): List<KeeplyItem> = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
        val array = JSONArray(reader.readText())
        val list = mutableListOf<KeeplyItem>()
        for (i in 0 until array.length()) list += KeeplyItem.fromJson(array.getJSONObject(i))
        list
    } ?: emptyList()
}.getOrDefault(emptyList())

private fun getPrefBoolean(context: Context, key: String, defaultValue: Boolean): Boolean =
    context.getSharedPreferences("keeply", Context.MODE_PRIVATE).getBoolean(key, defaultValue)
