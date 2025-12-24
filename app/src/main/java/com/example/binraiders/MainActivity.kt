package com.example.binraiders
import kotlinx.coroutines.delay
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.centerAlignedTopAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BinRaidersApp() }
    }
}

enum class Screen {
    HOME,
    CONTENEDORES,
    NOTIFICACIONES,
    CAMARA,
    ACCEDER,
    LOGIN_CLIENTE,
    LOGIN_ADMIN,
    ADMIN,
    REPORTES,

    CLIENTE_CONTENEDORES

}



data class ContainerItem(
    val id: Int,
    val name: String,
    val address: String,
    val isReal: Boolean // true solo para contenedor 1
)
fun binStateText(distanceCm: Int?): String {
    if (distanceCm == null) return "Sin datos"
    return if (distanceCm <= 10) "Lleno" else "Vacío"   // según tu regla
}




@Composable
fun BinRaidersApp() {

    MaterialTheme {
        var screen by remember { mutableStateOf(Screen.HOME) }
        var selectedContainer by remember { mutableStateOf<ContainerItem?>(null) }
        val notifications = remember { mutableStateListOf<String>() }
        var isAdmin by remember { mutableStateOf(false) }
        var adminUser by remember { mutableStateOf("") }
        var isCliente by remember { mutableStateOf(false) }
        var clienteUser by remember { mutableStateOf("") }
        val eventos = remember { mutableStateListOf<Evento>() }



        when (screen) {

            Screen.HOME -> HomeScreen(
                onGoContainers = { screen = Screen.CONTENEDORES },
                onGoNotifications = { screen = Screen.NOTIFICACIONES },
                onGoLogin = { screen = Screen.ACCEDER }
            )

            Screen.ACCEDER -> AccederScreen(
                onBack = { screen = Screen.HOME },
                onCliente = { screen = Screen.LOGIN_CLIENTE },
                onAdmin = { screen = Screen.LOGIN_ADMIN }
            )

            Screen.LOGIN_CLIENTE -> LoginScreen(
                title = "Login Cliente",
                onBack = { screen = Screen.ACCEDER },
                onSuccess = { u ->
                    screen = Screen.CLIENTE_CONTENEDORES
                }
            )



            Screen.LOGIN_ADMIN -> LoginScreen(
                title = "Login Admin",
                onBack = { screen = Screen.ACCEDER },
                onSuccess = { u ->
                    isAdmin = true
                    adminUser = u
                    screen = Screen.ADMIN   // o REPORTES si quieres saltar directo
                }
            )


            Screen.ADMIN -> AdminScreen(
                adminUser = adminUser,
                onGoReportes = { screen = Screen.REPORTES },
                onLogout = {
                    isAdmin = false
                    adminUser = ""
                    screen = Screen.HOME
                }
            )

            Screen.REPORTES -> ReportesScreen(
                onBack = { screen = Screen.ADMIN }
            )

            Screen.CONTENEDORES -> ContainersScreen(
                onBack = { screen = Screen.HOME },
                onOpenCamera = { c ->
                    selectedContainer = c
                    screen = Screen.CAMARA
                },
                isCliente = isCliente
            )


            Screen.NOTIFICACIONES -> NotificationsScreen(
                onBack = { screen = Screen.HOME }
            )

            Screen.CAMARA -> CameraScreen(
                container = selectedContainer,
                onBack = { screen = Screen.CONTENEDORES }
            )

            Screen.CLIENTE_CONTENEDORES -> ClienteContainersScreen(
                onBack = { screen = Screen.HOME },
                onOpenCamera = { c: ContainerItem ->
                    selectedContainer = c
                    screen = Screen.CAMARA
                }
            )


        }

    }
}

/* ------------------------- UI base ------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
        },
        colors = centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun GradientBackground(content: @Composable ColumnScope.() -> Unit) {
    val bg = Brush.verticalGradient(
        colors = listOf(Color(0xFFEFF6FF), Color(0xFFFFFFFF))
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun PrimaryPillButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(999.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun OutlinePillButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(999.dp)
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/* ------------------------- HOME ------------------------- */

@Composable
fun HomeScreen(
    onGoContainers: () -> Unit,
    onGoNotifications: () -> Unit,
    onGoLogin: () -> Unit
) {
    GradientBackground {
        // placeholder logo
        Image(
            painter = painterResource(R.drawable.logo_binraiders),
            contentDescription = "Logo Bin Raiders",
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
        )


        Spacer(Modifier.height(18.dp))

        Text("¡Bienvenidos a\nBin Raiders!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(28.dp))

        PrimaryPillButton("Ver contenedores", onGoContainers)
        Spacer(Modifier.height(12.dp))
        OutlinePillButton("Notificaciones", onGoNotifications)
        Spacer(Modifier.height(12.dp))
        PrimaryPillButton("Acceder", onGoLogin)

        Spacer(Modifier.height(18.dp))
        Text("Powered by Raspberry Pi 4", color = Color(0xFF6B7280), fontSize = 13.sp)
    }
}











@Composable
fun AccederScreen(
    onBack: () -> Unit,
    onCliente: () -> Unit,
    onAdmin: () -> Unit
) {
    Scaffold(topBar = { SimpleTopBar(title = "Acceder", onBack = onBack) }) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(R.drawable.logo_binraiders),
                contentDescription = "Logo Bin Raiders",
                modifier = Modifier.size(250.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text("Elige tipo de usuario", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onCliente,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Cliente") }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onAdmin,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Administrador") }
        }
    }
}

/* ------------------------- CONTENEDORES + LECTURA /estado ------------------------- */

@Composable
fun ContainersScreen(
    onBack: () -> Unit,
    onOpenCamera: (ContainerItem) -> Unit,
    isCliente: Boolean
) {

    val containers = remember {
        listOf(
            ContainerItem(1, "Contenedor 1", "Dirección: Av. Diego Portales & Las Torres", isReal = true),
            ContainerItem(2, "Contenedor 2", "Dirección: Av. Edmundo Flores 112", isReal = false),
            ContainerItem(3, "Contenedor 3", "Dirección: Av. Santa María 421", isReal = false)
        )
    }

    var distanceCm by remember { mutableStateOf<Int?>(null) }
    var statusText by remember { mutableStateOf("Sin datos") }
    var loading by remember { mutableStateOf(false) }
    var showAvisoOk by remember { mutableStateOf(false) }

    suspend fun fetchEstado() {
        try {
            val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL(ApiConfig.ESTADO_URL).readText()
            }

            val num = Regex(""""distance_cm"\s*:\s*(\d+)""")
                .find(raw)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\b\d+\b""").find(raw)?.value?.toIntOrNull()

            distanceCm = num
            statusText = "OK"
        } catch (e: Exception) {
            distanceCm = null
            statusText = "Sin datos"
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            fetchEstado()
            delay(2000)
        }
    }

    val scope = rememberCoroutineScope()

    if (showAvisoOk) {
        AlertDialog(
            onDismissRequest = { showAvisoOk = false },
            confirmButton = {
                TextButton(onClick = { showAvisoOk = false }) { Text("OK") }
            },
            title = { Text("Aviso enviado ✅") },
            text = { Text("Se envió el reporte al sistema .") }
        )
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "Contenedores", onBack = onBack) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(
                onClick = { scope.launch { fetchEstado() } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (loading) "Actualizando..." else "Actualizar estado (Contenedor 1)")
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(containers) { c ->
                    val dist = if (c.isReal) distanceCm else null
                    val st = if (c.isReal) statusText else "Demo / Sin dispositivo"

                    ContainerCard(
                        item = c,
                        distanceCm = dist,
                        status = st,
                        isCliente = isCliente,               // ✅ AQUI
                        onOpenCamera = { if (c.isReal) onOpenCamera(c) },
                        onAvisar = { showAvisoOk = true }    // ✅ AQUI
                    )
                }
            }
        }
    }
}



@Composable
fun ContainerCard(
    item: ContainerItem,
    distanceCm: Int?,
    status: String,
    isCliente: Boolean,
    onOpenCamera: () -> Unit,
    onAvisar: () -> Unit
) {
    // ✅ Estado calculado solo para el real
    val estadoBin = when {
        !item.isReal -> "(demo)"
        distanceCm == null -> "Sin datos"
        else -> binStateText(distanceCm) // Vacío / Lleno
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(item.address, color = Color(0xFF6B7280), fontSize = 13.sp)

            Spacer(Modifier.height(10.dp))

            // ✅ SOLO ESTADO
            Text(
                text = "Estado: $estadoBin",
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF374151)
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onOpenCamera,
                enabled = item.isReal,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (item.isReal) "Ver cámara" else "Cámara no disponible")
            }
        }
    }
}




/* ------------------------- NOTIFICACIONES ------------------------- */

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var eventos by remember { mutableStateOf<List<Evento>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            loading = true
            eventos = ApiClient.getEventos()
            error = null
        } catch (e: Exception) {
            error = "No se pudo cargar las notificaciones"
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "Notificaciones", onBack = onBack) }
    ) { pad ->
        when {
            loading -> {
                Box(
                    Modifier.padding(pad).fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    Modifier.padding(pad).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error!!, color = Color.Red)
                }
            }

            eventos.isEmpty() -> {
                Column(
                    Modifier.padding(pad).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Aún no hay notificaciones.", color = Color(0xFF6B7280))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Se generarán cuando detectemos eventos (ej: contenedor lleno).",
                        fontSize = 12.sp
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(eventos) { ev ->
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "BR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column {
                                    Text(
                                        ev.mensaje,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        ev.ts,
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


/* ------------------------- LOGIN ------------------------- */

@Composable
fun LoginScreen(
    title: String,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {

    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf<String?>(null) }


    Scaffold(topBar = { SimpleTopBar(title = title, onBack = onBack) }) { pad ->
    Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_binraiders),
                contentDescription = "Logo Bin Raiders",
                modifier = Modifier.size(140.dp)
            )
        }

        Spacer(Modifier.height(24.dp))


        OutlinedTextField(
                value = user,
                onValueChange = { user = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = pass,
                onValueChange = { pass = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPass) androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showPass = !showPass }) { Text(if (showPass) "Ocultar" else "Ver")

                    }
                }
            )

            Spacer(Modifier.height(18.dp))
        if (errorMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMsg!!, color = Color.Red, fontSize = 12.sp)
        }

        Button(
            onClick = {
                val u = user.trim()
                val p = pass.trim()

                val ok = when (title) {
                    "Login Cliente" -> (u == "cliente" && p == "1234")
                    "Login Admin" -> (u == "admin" && p == "1234")
                    else -> false
                }

                if (ok) {
                    errorMsg = null
                    onSuccess(u)
                } else {
                    errorMsg = "Usuario/contraseña incorrectos (demo)"
                }
            }
            ,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Confirmar", fontWeight = FontWeight.SemiBold) }

    }
    }
}

/* ------------------------- CAMARA /stream ------------------------- */

@Composable
fun CameraScreen(
    container: ContainerItem?,
    onBack: () -> Unit
) {
    val streamUrl = ApiConfig.STREAM_URL

    Scaffold(topBar = { SimpleTopBar(title = "Cámara", onBack = onBack) }) { pad ->
        Column(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp)
        ) {
            Text(container?.name ?: "Contenedor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text("Dirección: ${container?.address ?: ""}", color = Color(0xFF6B7280))
            Spacer(Modifier.height(16.dp))

            MjpegWebView(url = streamUrl)

            Spacer(Modifier.height(10.dp))
            Text("Fuente: $streamUrl", color = Color(0xFF6B7280), fontSize = 12.sp)
        }
    }
}

@Composable
fun MjpegWebView(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(420.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp)),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.domStorageEnabled = true
                loadUrl(url)
            }
        },
        update = { webView ->
            if (webView.url != url) webView.loadUrl(url)
        }
    )
}

@Composable
fun AdminScreen(
    adminUser: String,
    onGoReportes: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = { SimpleTopBar(title = "Admin", onBack = onLogout) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.height(8.dp))

            Image(
                painter = painterResource(R.drawable.logo_binraiders),
                contentDescription = "Logo Bin Raiders",
                modifier = Modifier.size(130.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "¡Bienvenidos a\nBin Raiders $adminUser!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onGoReportes,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Generar reportes")
            }
        }
    }
}


data class Reporte(
    val id: Int,
    val titulo: String,
    val direccion: String,
    val estado: String,
    val fecha: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit = {} // pásalo desde AdminScreen si quieres
) {
    var reportes by remember {
        mutableStateOf(
            listOf(
                Reporte(1, "Reporte 1", "Av. Diego Portales & Las Torres", "Vacío", "Hoy 13:20"),
                Reporte(2, "Reporte 2", "Av. Diego Portales & Las Torres", "Lleno", "Hoy 13:05"),
                Reporte(3, "Reporte 3", "Av. Diego Portales & Las Torres", "Vacío", "Ayer 20:10")
            )
        )
    }

    var showCreate by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Reporte?>(null) }

    // ---- Dialog crear ----
    if (showCreate) {
        CreateReporteDialog(
            onDismiss = { showCreate = false },
            onCreate = { nuevo ->
                reportes = listOf(nuevo) + reportes
                showCreate = false
            },
            nextId = (reportes.maxOfOrNull { it.id } ?: 0) + 1
        )
    }

    // ---- Dialog detalle ----
    if (selected != null) {
        ReporteDetailDialog(
            reporte = selected!!,
            onDismiss = { selected = null },
            onDelete = {
                reportes = reportes.filterNot { it.id == selected!!.id }
                selected = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menú de reportes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", fontSize = 20.sp) }
                },
                actions = {
                    IconButton(onClick = { /* aquí después recargas desde Firebase */ }) {
                        Text("↻", fontSize = 18.sp)
                    }
                    // ⎋ logout
                    IconButton(onClick = onLogout) {
                        Text("⎋", fontSize = 18.sp)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Crear reporte
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { showCreate = true },
                shape = RoundedCornerShape(16.dp)
            ) {

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Crear un reporte")
                        Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Lista
            reportes.forEach { r ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { selected = r },
                    shape = RoundedCornerShape(14.dp)
                ) {

                Column(Modifier.padding(14.dp)) {
                        Text(r.titulo, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Dirección: ${r.direccion}", color = Color(0xFF6B7280), fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(onClick = {}, label = { Text(r.estado) })
                            Spacer(Modifier.width(8.dp))
                            Text(r.fecha, fontSize = 12.sp, color = Color(0xFF6B7280))

                            Spacer(Modifier.weight(1f))

                            TextButton(onClick = { selected = r }) { Text("Ver") }
                            TextButton(onClick = {
                                reportes = reportes.filterNot { it.id == r.id }
                            }) { Text("Eliminar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateReporteDialog(
    nextId: Int,
    onDismiss: () -> Unit,
    onCreate: (Reporte) -> Unit
) {
    var contenedor by remember { mutableStateOf("Contenedor 1") }
    var estado by remember { mutableStateOf("Vacío") }
    var obs by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear reporte") },
        text = {
            Column {
                Text("Contenedor")
                Spacer(Modifier.height(6.dp))
                // simple: cambia después a Dropdown
                OutlinedTextField(
                    value = contenedor,
                    onValueChange = { contenedor = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Text("Estado")
                Spacer(Modifier.height(6.dp))
                Row {
                    FilterChip(
                        selected = estado == "Vacío",
                        onClick = { estado = "Vacío" },
                        label = { Text("Vacío") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = estado == "Lleno",
                        onClick = { estado = "Lleno" },
                        label = { Text("Lleno") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    label = { Text("Observación (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCreate(
                    Reporte(
                        id = nextId,
                        titulo = "Reporte $nextId",
                        direccion = "Av. Diego Portales & Las Torres",
                        estado = estado,
                        fecha = "Hoy"
                    )
                )
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ReporteDetailDialog(
    reporte: Reporte,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(reporte.titulo) },
        text = {
            Column {
                Text("Dirección: ${reporte.direccion}")
                Spacer(Modifier.height(6.dp))
                Text("Estado: ${reporte.estado}")
                Spacer(Modifier.height(6.dp))
                Text("Fecha: ${reporte.fecha}")
                Spacer(Modifier.height(14.dp))
                Text("Exportar :")
                Spacer(Modifier.height(6.dp))
                Row {
                    OutlinedButton(onClick = { /* TODO export PDF */ }) { Text("PDF") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { /* TODO export CSV */ }) { Text("CSV") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Eliminar") }
        }
    )
}
@Composable
fun ClienteContainersScreen(
    onBack: () -> Unit,
    onOpenCamera: (ContainerItem) -> Unit
) {
    val containers = remember {
        listOf(
            ContainerItem(1, "Contenedor 1", "Dirección: Av. Diego Portales & Las Torres", isReal = true),
            ContainerItem(2, "Contenedor 2", "Dirección: Av. Edmundo Flores 112", isReal = false),
            ContainerItem(3, "Contenedor 3", "Dirección: Av. Santa María 421", isReal = false)
        )
    }

    var distanceCm by remember { mutableStateOf<Int?>(null) }
    var showAvisoOk by remember { mutableStateOf(false) }

    suspend fun fetchEstado() {
        try {
            val raw = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                java.net.URL(ApiConfig.ESTADO_URL).readText()
            }

            val num = Regex(""""distance_cm"\s*:\s*(\d+)""")
                .find(raw)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""\b\d+\b""").find(raw)?.value?.toIntOrNull()

            distanceCm = num
        } catch (e: Exception) {
            distanceCm = null
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            fetchEstado()
            delay(2000)
        }
    }

    if (showAvisoOk) {
        AlertDialog(
            onDismissRequest = { showAvisoOk = false },
            confirmButton = {
                TextButton(onClick = { showAvisoOk = false }) { Text("OK") }
            },
            title = { Text("Aviso enviado ✅") },
            text = { Text("Tu aviso fue enviado al sistema .") }
        )
    }

    Scaffold(
        topBar = { SimpleTopBar(title = "Contenedores (Cliente)", onBack = onBack) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(containers) { c ->
                val dist = if (c.isReal) distanceCm else null
                val estado = when {
                    !c.isReal -> "(demo)"
                    dist == null -> "Sin datos"
                    else -> binStateText(dist)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(c.address, color = Color(0xFF6B7280), fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Estado: $estado",
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF374151)
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onOpenCamera(c) },
                                enabled = c.isReal,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (c.isReal) "Ver cámara" else "No disponible")
                            }

                            OutlinedButton(
                                onClick = { showAvisoOk = true },
                                enabled = c.isReal && estado == "Lleno",
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Dar aviso")
                            }
                        }

                        if (c.isReal) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Aviso habilitado solo si el contenedor está LLENO",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}
