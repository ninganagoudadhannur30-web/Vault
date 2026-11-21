package com.example.vault

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

// ---------------------------------------------------------------
// 1. DATA
// ---------------------------------------------------------------
data class VaultEntry(
    val appName: String,
    val link: String,
    val password: String
)

// Saves / loads the list in a small private file on the phone
class VaultStorage(context: Context) {
    private val prefs = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)

    fun load(): List<VaultEntry> {
        val text = prefs.getString("entries", null) ?: return emptyList()
        val array = JSONArray(text)
        val result = mutableListOf<VaultEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                VaultEntry(
                    appName = obj.getString("appName"),
                    link = obj.getString("link"),
                    password = obj.getString("password")
                )
            )
        }
        return result
    }

    fun save(list: List<VaultEntry>) {
        val array = JSONArray()
        for (entry in list) {
            val obj = JSONObject()
            obj.put("appName", entry.appName)
            obj.put("link", entry.link)
            obj.put("password", entry.password)
            array.put(obj)
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}

// ---------------------------------------------------------------
// 2. HELPER FUNCTIONS
// ---------------------------------------------------------------
private val secureRandom = SecureRandom()

fun generatePassword(length: Int = 16): String {
    val allowedChars =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
    return (1..length)
        .map { allowedChars[secureRandom.nextInt(allowedChars.length)] }
        .joinToString("")
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("password", text)

    // Android 13+: mark as sensitive so previews hide the copied text
    if (Build.VERSION.SDK_INT >= 33) {
        val extras = PersistableBundle()
        extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        clip.description.extras = extras
    }
    clipboard.setPrimaryClip(clip)
}

fun openLink(context: Context, link: String) {
    val url = if (link.startsWith("http://") || link.startsWith("https://")) link else "https://$link"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this link", Toast.LENGTH_SHORT).show()
    }
}

// ---------------------------------------------------------------
// 3. THEME (colors for light and dark mode)
// ---------------------------------------------------------------
private val LightColors = lightColorScheme(
    primary = Color(0xFF3B5BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF0B1F6B),
    secondary = Color(0xFF5A6482),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE3F5),
    onSecondaryContainer = Color(0xFF161C33),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1C24),
    surface = Color.White,
    onSurface = Color(0xFF1A1C24),
    surfaceVariant = Color(0xFFE9ECF6),
    onSurfaceVariant = Color(0xFF454A5C),
    outline = Color(0xFF767B8E),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB6C4FF),
    onPrimary = Color(0xFF0F2380),
    primaryContainer = Color(0xFF263A9E),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFFC1C7E2),
    onSecondary = Color(0xFF2B3149),
    secondaryContainer = Color(0xFF424866),
    onSecondaryContainer = Color(0xFFDFE3F5),
    background = Color(0xFF11131A),
    onBackground = Color(0xFFE3E4EE),
    surface = Color(0xFF191C25),
    onSurface = Color(0xFFE3E4EE),
    surfaceVariant = Color(0xFF2A2E3B),
    onSurfaceVariant = Color(0xFFC5C9DB),
    outline = Color(0xFF8F93A6),
    error = Color(0xFFFFB4AB)
)

@Composable
fun VaultAppTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

// ---------------------------------------------------------------
// 4. MAIN ACTIVITY (single activity)
// ---------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultAppTheme {
                VaultApp()
            }
        }
    }
}

// ---------------------------------------------------------------
// 5. MASTER CONTROLLER (holds the data, decides which screen shows)
// ---------------------------------------------------------------
@Composable
fun VaultApp() {
    val context = LocalContext.current
    val storage = remember { VaultStorage(context) }
    val vaultList = remember {
        mutableStateListOf<VaultEntry>().apply { addAll(storage.load()) }
    }
    var currentScreen by rememberSaveable { mutableStateOf("Vault") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // System back button on the Add screen returns to the vault
    BackHandler(enabled = currentScreen == "Add") {
        currentScreen = "Vault"
    }

    if (currentScreen == "Add") {
        AddPasswordScreen(
            onSave = { entry ->
                vaultList.add(0, entry) // newest first
                storage.save(vaultList)
                currentScreen = "Vault"
                showMessage("Password saved")
            },
            onBackClick = { currentScreen = "Vault" }
        )
    } else {
        YourVaultScreen(
            vaultList = vaultList,
            snackbarHostState = snackbarHostState,
            onAddClick = { currentScreen = "Add" },
            onCopy = { entry ->
                copyToClipboard(context, entry.password)
                showMessage("Password copied")
            },
            onDelete = { entry ->
                vaultList.remove(entry)
                storage.save(vaultList)
                showMessage("Deleted")
            }
        )
    }
}

// ---------------------------------------------------------------
// 6. SCREEN 1: THE VAULT (home)
// ---------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YourVaultScreen(
    vaultList: List<VaultEntry>,
    snackbarHostState: SnackbarHostState,
    onAddClick: () -> Unit,
    onCopy: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Vault", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add password") }
            )
        }
    ) { innerPadding ->
        if (vaultList.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                // extra bottom space so the button never hides the last card
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vaultList) { entry ->
                    VaultCard(
                        entry = entry,
                        onCopy = { onCopy(entry) },
                        onDelete = { onDelete(entry) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No passwords yet", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap \"Add password\" to save your first one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VaultCard(entry: VaultEntry, onCopy: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current

    // remember(entry): if this card shows a different entry later, it resets to hidden
    var showPassword by remember(entry) { mutableStateOf(false) }
    var showDeleteDialog by remember(entry) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top row: letter avatar, name + link, delete button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.link.isNotBlank()) {
                        Text(
                            text = entry.link,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { openLink(context, entry.link) }
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${entry.appName}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Password row: hidden by default, eye button shows it, copy button copies it
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showPassword) entry.password else "••••••••••",
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy password",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Ask before deleting (standard practice for destructive actions)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete password?") },
            text = { Text("This will permanently remove the saved password for ${entry.appName}.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------------------------------------------------
// 7. SCREEN 2: ADD PASSWORD (form)
// ---------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(onSave: (VaultEntry) -> Unit, onBackClick: () -> Unit) {
    // rememberSaveable keeps what you typed if the phone is rotated
    var appName by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            val nameError = submitted && appName.isBlank()
            val passwordError = submitted && password.isBlank()

            OutlinedTextField(
                value = appName,
                onValueChange = { appName = it },
                label = { Text("App name") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("App name is required") }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("Website link (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                isError = passwordError,
                supportingText = if (passwordError) {
                    { Text("Password is required") }
                } else null,
                visualTransformation = if (showPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    password = generatePassword()
                    showPassword = true // show it so the user can see what was generated
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate strong password")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    submitted = true
                    if (appName.isNotBlank() && password.isNotBlank()) {
                        onSave(VaultEntry(appName.trim(), link.trim(), password))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Save password", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------
// 8. ANDROID STUDIO PREVIEWS
// ---------------------------------------------------------------
private val previewList = listOf(
    VaultEntry("Instagram", "instagram.com", "MySecurePass123!"),
    VaultEntry("Call of Duty", "", "GamerZone99*")
)

@Preview(showBackground = true)
@Composable
fun PreviewAddPasswordScreen() {
    VaultAppTheme {
        AddPasswordScreen(onSave = {}, onBackClick = {})
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewYourVaultScreen() {
    VaultAppTheme {
        YourVaultScreen(
            vaultList = previewList,
            snackbarHostState = remember { SnackbarHostState() },
            onAddClick = {},
            onCopy = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewYourVaultScreenDark() {
    VaultAppTheme(darkTheme = true) {
        YourVaultScreen(
            vaultList = previewList,
            snackbarHostState = remember { SnackbarHostState() },
            onAddClick = {},
            onCopy = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEmptyVault() {
    VaultAppTheme {
        YourVaultScreen(
            vaultList = emptyList(),
            snackbarHostState = remember { SnackbarHostState() },
            onAddClick = {},
            onCopy = {},
            onDelete = {}
        )
    }
}