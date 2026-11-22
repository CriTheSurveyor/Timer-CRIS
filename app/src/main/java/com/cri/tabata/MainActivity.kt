package com.cri.tabata

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

val Ink = Color(0xFF070B16)
val Coral = Color(0xFFFF5A4E)
val Mint = Color(0xFF2FD3A0)
val Amber = Color(0xFFFFB648)
val Sky = Color(0xFF3AA8FF)
val Ice = Color(0xFF7FD8F5)
val Dim = Color.White.copy(alpha = .62f)
val Tnum = TextStyle(fontFeatureSettings = "tnum")

val Kind.color
    get() = when (this) {
        Kind.PREPARE -> Amber
        Kind.WORK -> Coral
        Kind.REST -> Mint
        Kind.SET_REST -> Sky
        Kind.COOLDOWN -> Ice
    }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(this)
        setContent {
            CompositionLocalProvider(LocalContentColor provides Color.White) { App(store) }
        }
    }
}

sealed interface Screen {
    data object Home : Screen
    data class Edit(val workout: Workout, val isNew: Boolean) : Screen
    data class Run(val workout: Workout) : Screen
}

@Composable
fun App(store: Store) {
    var workouts by remember { mutableStateOf(store.load()) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current
    var update by remember { mutableStateOf<Update?>(null) }
    LaunchedEffect(Unit) { update = withContext(Dispatchers.IO) { latestUpdate(BuildConfig.VERSION_CODE) } }
    fun save(list: List<Workout>) {
        workouts = list
        store.save(list)
        screen = Screen.Home
    }

    when (val s = screen) {
        Screen.Home -> Backdrop(Coral) {
            HomeScreen(
                workouts, update,
                onUpdate = { u ->
                    // the browser downloads the APK; tapping the download installs it over this version
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.apkUrl))) }
                },
                onNew = { screen = Screen.Edit(Workout(), true) },
                onEdit = { screen = Screen.Edit(it, false) },
                onRun = { screen = Screen.Run(it) },
            )
        }
        is Screen.Edit -> Backdrop(Sky) {
            EditScreen(
                s.workout, s.isNew,
                onSave = { w -> save(if (s.isNew) workouts + w else workouts.map { if (it.id == w.id) w else it }) },
                onDelete = { save(workouts.filter { it.id != s.workout.id }) },
                onCancel = { screen = Screen.Home },
            )
        }
        is Screen.Run -> RunScreen(s.workout) { screen = Screen.Home }
    }
}

// ---------- Liquid glass kit ----------

@Composable
fun Backdrop(accent: Color, content: @Composable () -> Unit) {
    val a by animateColorAsState(accent, tween(700), label = "accent")
    Box(
        Modifier.fillMaxSize().background(Ink).drawBehind {
            val w = size.width
            val h = size.height
            fun glow(c: Color, x: Float, y: Float, r: Float) = drawCircle(
                Brush.radialGradient(listOf(c, Color.Transparent), Offset(x, y), r), r, Offset(x, y),
            )
            glow(a.copy(alpha = .55f), w * .1f, h * .1f, w * .95f)
            glow(Mint.copy(alpha = .22f), w * 1f, h * .55f, w * .8f)
            glow(Sky.copy(alpha = .22f), w * .2f, h * 1f, w)
        },
    ) { content() }
}

fun Modifier.glass(shape: Shape = RoundedCornerShape(28.dp), tint: Color = Color.White, strength: Float = 1f) =
    clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = .22f * strength), tint.copy(alpha = .07f * strength))))
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = .5f), Color.White.copy(alpha = .06f))), shape)

/** Press feedback: the control shrinks under the finger, like iOS. */
@Composable
fun Modifier.press(onClick: () -> Unit, label: String? = null): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .93f else 1f, label = "press")
    return graphicsLayer { scaleX = s; scaleY = s }
        .clickable(src, null, role = Role.Button, onClick = onClick)
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
}

@Composable
fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = Color.White, icon: (@Composable () -> Unit)? = null) {
    Row(
        modifier.press(onClick).glass(RoundedCornerShape(50), tint).heightIn(min = 54.dp).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { icon(); Spacer(Modifier.width(8.dp)) }
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun RoundButton(label: String, onClick: () -> Unit, size: Dp = 48.dp, tint: Color = Color.White, content: @Composable () -> Unit) {
    Box(Modifier.size(size).press(onClick, label).glass(CircleShape, tint), contentAlignment = Alignment.Center) { content() }
}

@Composable
fun PauseGlyph() {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(2) { Box(Modifier.size(7.dp, 26.dp).clip(RoundedCornerShape(3.dp)).background(Color.White)) }
    }
}

@Composable
fun SectionLabel(text: String) =
    Text(text.uppercase(), Modifier.padding(start = 6.dp, top = 22.dp, bottom = 8.dp), color = Dim, fontSize = 13.sp, letterSpacing = 1.sp)

// ---------- Home ----------

@Composable
fun HomeScreen(list: List<Workout>, update: Update?, onUpdate: (Update) -> Unit, onNew: () -> Unit, onEdit: (Workout) -> Unit, onRun: (Workout) -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Allenamenti", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Text(if (list.isEmpty()) "Crea il primo allenamento" else "${list.size} salvati", color = Dim, fontSize = 15.sp)
        Spacer(Modifier.height(20.dp))
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(list, key = { it.id }) { w -> WorkoutCard(w, { onRun(w) }, { onEdit(w) }) }
        }
        if (update != null) {
            Pill("Aggiorna alla build ${update.build}", { onUpdate(update) }, Modifier.fillMaxWidth().padding(bottom = 12.dp), Mint) {
                Icon(Icons.Default.Refresh, null, Modifier.size(22.dp))
            }
        }
        Pill("Nuovo allenamento", onNew, Modifier.fillMaxWidth().padding(bottom = 12.dp), Coral) {
            Icon(Icons.Default.Add, null, Modifier.size(22.dp))
        }
    }
}

@Composable
fun WorkoutCard(w: Workout, onRun: () -> Unit, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().press(onRun).glass().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(w.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${w.cycles} × ${fmt(w.work)} / ${fmt(w.rest)}" + if (w.sets > 1) " · ${w.sets} serie" else "",
                color = Color.White.copy(alpha = .8f), fontSize = 15.sp, style = Tnum,
            )
            Text("Totale ${fmt(w.totalSeconds())}", color = Dim, fontSize = 13.sp, style = Tnum)
        }
        RoundButton("Modifica ${w.name}", onEdit, 44.dp) { Icon(Icons.Default.Edit, null, Modifier.size(20.dp)) }
        Spacer(Modifier.width(10.dp))
        RoundButton("Avvia ${w.name}", onRun, 52.dp, Coral) { Icon(Icons.Default.PlayArrow, null) }
    }
}

// ---------- Editor ----------

@Composable
fun EditScreen(initial: Workout, isNew: Boolean, onSave: (Workout) -> Unit, onDelete: () -> Unit, onCancel: () -> Unit) {
    var w by remember(initial) { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    BackHandler(onBack = onCancel)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton("Annulla", onCancel, 44.dp) { Icon(Icons.Default.Close, null) }
            Text(
                if (isNew) "Nuovo allenamento" else "Modifica", Modifier.weight(1f),
                textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            )
            RoundButton("Salva", { onSave(w.copy(name = w.name.trim().ifEmpty { "Allenamento" })) }, 44.dp, Mint) {
                Icon(Icons.Default.Check, null)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionLabel("Nome")
            BasicTextField(
                w.name, { w = w.copy(name = it.take(40)) },
                Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(horizontal = 18.dp, vertical = 16.dp)
                    .semantics { contentDescription = "Nome allenamento" },
                textStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(Coral),
                singleLine = true,
            )
            SectionLabel("Intervalli")
            Column(Modifier.glass()) {
                Stepper("Preparazione", w.prepare, Amber, 0..600, 5, true) { w = w.copy(prepare = it) }
                Stepper("Lavoro", w.work, Coral, 5..3600, 5, true) { w = w.copy(work = it) }
                Stepper("Riposo", w.rest, Mint, 0..3600, 5, true) { w = w.copy(rest = it) }
                Stepper("Cicli", w.cycles, Color.White, 1..99, 1, false) { w = w.copy(cycles = it) }
            }
            SectionLabel("Serie")
            Column(Modifier.glass()) {
                Stepper("Serie", w.sets, Color.White, 1..20, 1, false) { w = w.copy(sets = it) }
                Stepper("Recupero tra serie", w.setRest, Sky, 0..3600, 5, true) { w = w.copy(setRest = it) }
                Stepper("Defaticamento", w.cooldown, Ice, 0..3600, 5, true) { w = w.copy(cooldown = it) }
            }
            Text(
                "Durata totale ${fmt(w.totalSeconds())}", Modifier.fillMaxWidth().padding(top = 20.dp),
                textAlign = TextAlign.Center, color = Dim, fontSize = 15.sp, style = Tnum,
            )
            if (!isNew) {
                Pill(
                    if (confirmDelete) "Tocca di nuovo per eliminare" else "Elimina allenamento",
                    { if (confirmDelete) onDelete() else confirmDelete = true },
                    Modifier.fillMaxWidth().padding(vertical = 20.dp), Coral,
                ) { Icon(Icons.Default.Delete, null, Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun Stepper(label: String, value: Int, dot: Color, range: IntRange, step: Int, time: Boolean, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(12.dp))
        Text(label, Modifier.weight(1f), fontSize = 16.sp)
        RoundButton("Diminuisci $label", { onChange((value - step).coerceIn(range)) }, 44.dp) { Text("−", fontSize = 22.sp) }
        Text(
            if (time) fmt(value) else "$value", Modifier.widthIn(min = 64.dp),
            textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, style = Tnum,
        )
        RoundButton("Aumenta $label", { onChange((value + step).coerceIn(range)) }, 44.dp) { Text("+", fontSize = 22.sp) }
    }
}

// ---------- Timer ----------

@Composable
fun RunScreen(w: Workout, onExit: () -> Unit) {
    val phases = remember(w) { w.phases() }
    var index by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var done by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var endAt by remember { mutableLongStateOf(now + phases[0].seconds * 1000L) }
    var pausedLeft by remember { mutableLongStateOf(phases[0].seconds * 1000L) }
    val sounds = remember { Sounds() }
    val view = LocalView.current
    DisposableEffect(Unit) {
        // shortcut: no foreground service, so the timer only runs reliably with the app on screen; add one if users lock the phone mid-workout
        view.keepScreenOn = true
        sounds.play(sounds.start)
        onDispose { view.keepScreenOn = false; sounds.release() }
    }
    BackHandler(onBack = onExit)

    fun go(i: Int) {
        if (i > phases.lastIndex) {
            done = true; running = false; pausedLeft = 0
            sounds.play(sounds.end)
            return
        }
        index = i.coerceAtLeast(0)
        val ms = phases[index].seconds * 1000L
        now = SystemClock.elapsedRealtime()
        endAt = now + ms
        pausedLeft = ms
        sounds.play(
            when {
                index == 0 -> sounds.start
                phases[index].kind == Kind.WORK -> sounds.work
                else -> sounds.rest
            },
        )
    }

    LaunchedEffect(running) {
        var lastSec = -1
        while (running) {
            now = SystemClock.elapsedRealtime()
            if (now >= endAt) go(index + 1) else {
                val sec = ((endAt - now + 999) / 1000).toInt()
                if (sec != lastSec && lastSec != -1 && sec in 1..3) sounds.play(sounds.countdown)
                lastSec = sec
            }
            delay(50)
        }
    }

    val phase = phases[index]
    val leftMs = if (running) (endAt - now).coerceAtLeast(0) else pausedLeft
    val secLeft = ((leftMs + 999) / 1000).toInt()
    val frac = if (done) 1f else leftMs / (phase.seconds * 1000f)
    val totalLeft = if (done) 0 else secLeft + phases.drop(index + 1).sumOf { it.seconds }
    val accent = if (done) Mint else phase.kind.color
    val ring by animateColorAsState(accent, tween(500), label = "ring")

    Backdrop(accent) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundButton("Chiudi", onExit, 44.dp) { Icon(Icons.Default.Close, null) }
                Text(
                    w.name, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(44.dp))
            }
            Text(
                if (done) "COMPLETATO" else phase.kind.label.uppercase(), Modifier.padding(top = 12.dp),
                color = ring, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
            )
            // the outer box takes the free space; the circle inside fits it without being cut at the sides
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.aspectRatio(1f, matchHeightConstraintsFirst = true), contentAlignment = Alignment.Center) {
                val big = (maxWidth.value * 0.3f).sp
                Box(Modifier.fillMaxSize().glass(CircleShape, accent, .7f))
                Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                    val st = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color.White.copy(alpha = .12f), 0f, 360f, false, style = st)
                    drawArc(ring, -90f, 360f * frac, false, style = st)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fmt(secLeft), fontSize = big, fontWeight = FontWeight.Light, style = Tnum)
                    if (!done && phase.cycle > 0) {
                        Text(
                            "Ciclo ${phase.cycle}/${w.cycles}" + if (w.sets > 1) " · Serie ${phase.set}/${w.sets}" else "",
                            color = Color.White.copy(alpha = .8f), fontSize = 22.sp, fontWeight = FontWeight.Medium, style = Tnum,
                        )
                    }
                }
            }
            }
            val next = phases.getOrNull(index + 1)
            Text(
                when {
                    done -> "Durata ${fmt(w.totalSeconds())}"
                    next == null -> "Ultimo intervallo"
                    else -> "Poi: ${next.kind.label} ${fmt(next.seconds)}"
                },
                color = Color.White.copy(alpha = .8f), fontSize = 22.sp, fontWeight = FontWeight.Medium, style = Tnum,
            )
            Row(
                Modifier.padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Pill("Ricomincia", { done = false; go(0); running = true }, tint = Mint) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(22.dp))
                    }
                } else {
                    RoundButton("Intervallo precedente", { go(index - 1) }, 60.dp) { Icon(Icons.Default.KeyboardArrowLeft, null, Modifier.size(30.dp)) }
                    RoundButton(if (running) "Pausa" else "Riprendi", {
                        if (running) {
                            pausedLeft = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                            running = false
                        } else {
                            now = SystemClock.elapsedRealtime()
                            endAt = now + pausedLeft
                            running = true
                        }
                    }, 84.dp, accent) {
                        if (running) PauseGlyph() else Icon(Icons.Default.PlayArrow, null, Modifier.size(40.dp))
                    }
                    RoundButton("Intervallo successivo", { go(index + 1) }, 60.dp) { Icon(Icons.Default.KeyboardArrowRight, null, Modifier.size(30.dp)) }
                }
            }
            Column(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).glass(RoundedCornerShape(24.dp)).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("TEMPO TOTALE RIMANENTE", color = Dim, fontSize = 13.sp, letterSpacing = 1.sp)
                Text(fmt(totalLeft), fontSize = 44.sp, fontWeight = FontWeight.SemiBold, style = Tnum)
            }
        }
    }
}
