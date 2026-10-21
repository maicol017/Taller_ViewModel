package me.maicolgutierrez.macotavirtual



import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------
// 1) ESTADOS POSIBLES DE LA MASCOTA (cada uno cambia la pantalla)
// ---------------------------------------------------------------
enum class Animo(val carita: String, val texto: String) {
    FELIZ("😸", "¡Estoy feliz!"),
    HAMBRIENTO("😿", "¡Tengo hambre!"),
    TRISTE("😾", "Estoy aburrido, juega conmigo"),
    CANSADO("😴", "Zzz... necesito dormir")
}

// ---------------------------------------------------------------
// 2) ESTADO DE LA PANTALLA: todo lo que la View necesita mostrar
// ---------------------------------------------------------------
data class MascotaUiState(
    val hambre: Int = 80,      // 100 = recién comido
    val energia: Int = 80,
    val felicidad: Int = 80,
    val edadSegundos: Int = 0,
    val animo: Animo = Animo.FELIZ
)

// ---------------------------------------------------------------
// 3) VIEWMODEL ÚNICO: guarda el estado y la lógica del juego.
//    Su corrutina sigue viva aunque rotes la pantalla.
// ---------------------------------------------------------------
class MascotaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MascotaUiState())
    val uiState: StateFlow<MascotaUiState> = _uiState.asStateFlow()

    init {
        // El tiempo pasa: la mascota se va cansando y con hambre
        viewModelScope.launch {
            while (true) {
                delay(2000)
                actualizar {
                    it.copy(
                        hambre = it.hambre - 4,
                        energia = it.energia - 2,
                        felicidad = it.felicidad - 3,
                        edadSegundos = it.edadSegundos + 2
                    )
                }
            }
        }
    }

    // Eventos que envía la View
    fun alimentar() = actualizar { it.copy(hambre = it.hambre + 25, felicidad = it.felicidad + 5) }
    fun jugar() = actualizar { it.copy(felicidad = it.felicidad + 25, energia = it.energia - 15, hambre = it.hambre - 10) }
    fun dormir() = actualizar { it.copy(energia = it.energia + 40, hambre = it.hambre - 5) }

    // Mantiene los valores entre 0 y 100 y recalcula el ánimo
    private fun actualizar(cambio: (MascotaUiState) -> MascotaUiState) {
        _uiState.update { actual ->
            val nuevo = cambio(actual).let {
                it.copy(
                    hambre = it.hambre.coerceIn(0, 100),
                    energia = it.energia.coerceIn(0, 100),
                    felicidad = it.felicidad.coerceIn(0, 100)
                )
            }
            nuevo.copy(animo = calcularAnimo(nuevo))
        }
    }

    private fun calcularAnimo(s: MascotaUiState) = when {
        s.energia <= 20 -> Animo.CANSADO
        s.hambre <= 25 -> Animo.HAMBRIENTO
        s.felicidad <= 25 -> Animo.TRISTE
        else -> Animo.FELIZ
    }
}

// ---------------------------------------------------------------
// 4) VIEW: solo dibuja el estado y envía eventos al ViewModel
// ---------------------------------------------------------------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { PantallaMascota() } }
    }
}

@Composable
fun PantallaMascota(
    modifier: Modifier = Modifier,
    vm: MascotaViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()
    val esHorizontal =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (esHorizontal) {
        // Horizontal: carita a la izquierda, barras y botones a la derecha
        Row(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(estado.animo.carita, fontSize = 80.sp)
                Text(estado.animo.texto, style = MaterialTheme.typography.titleMedium)
                Text("Edad: ${estado.edadSegundos} segundos")
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Barra("🍖 Hambre", estado.hambre)
                Barra("⚡ Energía", estado.energia)
                Barra("💛 Felicidad", estado.felicidad)
                Spacer(Modifier.height(8.dp))
                BotonesMascota(vm)
            }
        }
    } else {
        // Vertical: todo en una columna (con scroll por si la pantalla es pequeña)
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(estado.animo.carita, fontSize = 96.sp)
            Text(estado.animo.texto, style = MaterialTheme.typography.titleLarge)
            Text("Edad: ${estado.edadSegundos} segundos")
            Spacer(Modifier.height(24.dp))
            Barra("🍖 Hambre", estado.hambre)
            Barra("⚡ Energía", estado.energia)
            Barra("💛 Felicidad", estado.felicidad)
            Spacer(Modifier.height(24.dp))
            BotonesMascota(vm)
            Spacer(Modifier.height(16.dp))
            Text("Rota el celular: la mascota no pierde su estado ni su edad.")
        }
    }
}

@Composable
fun BotonesMascota(vm: MascotaViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.alimentar() }) { Text("Alimentar") }
        Button(onClick = { vm.jugar() }) { Text("Jugar") }
        Button(onClick = { vm.dormir() }) { Text("Dormir") }
    }
}

@Composable
fun Barra(titulo: String, valor: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$titulo: $valor")
        LinearProgressIndicator(
            progress = { valor / 100f },
            modifier = Modifier.fillMaxWidth()
        )
    }
}