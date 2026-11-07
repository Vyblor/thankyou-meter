package app.thankyoumeter

import android.Manifest
import app.thankyoumeter.detect.DetectService
import app.thankyoumeter.vehicle.PleosSignals
import app.thankyoumeter.ui.screens.RecordsScreen
import app.thankyoumeter.ui.screens.ConfirmScreen
import app.thankyoumeter.ui.screens.MonthScreen
import app.thankyoumeter.ui.screens.QrScreen
import app.thankyoumeter.ui.screens.RegionScreen
import app.thankyoumeter.ui.screens.MannerScreen
import app.thankyoumeter.ui.screens.SettingsScreen
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import app.thankyoumeter.Log.log
import app.thankyoumeter.data.Kind
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.Tab
import app.thankyoumeter.ui.components.TabScaffold
import app.thankyoumeter.ui.screens.HomeScreen
import app.thankyoumeter.ui.screens.HomeState
import app.thankyoumeter.ui.screens.OnboardingScreen
import app.thankyoumeter.ui.theme.DarkColors
import app.thankyoumeter.ui.theme.LightColors
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.TymTheme
import app.thankyoumeter.ui.theme.tc
import app.thankyoumeter.voice.Classifier
import app.thankyoumeter.voice.Earcon
import kotlinx.coroutines.flow.MutableStateFlow

/** 화면 경로. 와이어프레임 v0.2 번호를 따른다. */
sealed interface Route {
    data class Onboard(val step: Int) : Route
    data object Home : Route
    data object Records : Route
    data object Confirm : Route
    data object Month : Route
    data object Qr : Route
    data object Region : Route
    data object Manner : Route
    data object Settings : Route
}

class MainActivity : ComponentActivity() {
    private val micGranted = MutableStateFlow(false)
    private var afterPermission: (() -> Unit)? = null
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        log("PERM result $r"); (app.signals as? PleosSignals)?.retrySpeed()
        refreshMic()
        afterPermission?.invoke(); afterPermission = null
    }

    private fun refreshMic() { micGranted.value = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED }

    private fun requestPerms(then: () -> Unit) {
        afterPermission = then
        permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, "android.car.permission.CAR_SPEED"))
    }

    override fun onResume() { super.onResume(); refreshMic(); (app.signals as? PleosSignals)?.retrySpeed(); app.store.pruneCandidates(); if (app.store.settings.value.onboarded) DetectService.start(this) }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); readDebugExtras(intent) }

    private fun readDebugExtras(i: Intent?) {
        if (!BuildConfig.DEBUG || i == null) return
        i.getStringExtra("tym_theme")?.let { app.debugTheme.value = it.ifEmpty { null } }
        if (i.hasExtra("tym_width_dp")) app.debugWidthDp.value = i.getIntExtra("tym_width_dp", 0)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = app
        readDebugExtras(intent)
        refreshMic()
        setContent {
            val dbgTheme by app.debugTheme.collectAsState()
            val dark = when (dbgTheme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
            val dw by app.debugWidthDp.collectAsState()
            BoxWithConstraints(Modifier.fillMaxSize().background(if (dark) DarkColors.bg else LightColors.bg)) {
                BoxWithConstraints((if (dw > 0) Modifier.fillMaxHeight().width(dw.dp) else Modifier.fillMaxSize()).semantics { testTagsAsResourceId = true }) {
                    val layout = if (maxWidth < 1200.dp) Layout.Portrait else Layout.Landscape
                    TymTheme(dark, layout) { Root(layout, dark) }
                }
            }
        }
    }

    @Composable
    private fun Root(layout: Layout, dark: Boolean) {
        val app = app
        val settings by app.store.settings.collectAsState()
        var route by remember { mutableStateOf<Route>(if (settings.onboarded) Route.Home else Route.Onboard(1)) }
        val go: (Route) -> Unit = { r -> log("NAV $r layout=$layout dark=$dark"); route = r }
        val goTab: (Tab) -> Unit = { t -> go(when (t) { Tab.HOME -> Route.Home; Tab.RECORDS -> Route.Records; Tab.REGION -> Route.Region; Tab.MANNER -> Route.Manner; Tab.SETTINGS -> Route.Settings }) }
        BackHandler(enabled = route != Route.Home && route !is Route.Onboard) { go(Route.Home) }
        val moments by app.store.moments.collectAsState()
        val candidates by app.store.candidates.collectAsState()
        val day by app.store.day.collectAsState()
        val manner by app.store.manner.collectAsState()
        val mic by micGranted.collectAsState()
        var seenCount by remember { mutableIntStateOf(-1) }

        when (val r = route) {
            is Route.Onboard -> OnboardingScreen(
                step = r.step, setStep = { go(Route.Onboard(it)) },
                onPreviewAsk = { app.voice.earcon(Earcon.ASK) { app.voice.speak("양보받았어요?") {} } },
                onRequestPermission = { requestPerms { go(Route.Onboard(4)) } },
                listenOnce = { done -> app.voice.listen(4000) { t -> done(Classifier.classify(t, app.store.settings.value.anyYes)) } },
                stopListen = { app.voice.cancelListen() },
                onEnableAnyYes = { app.store.update { it.copy(anyYes = true) } },
                onDone = { app.store.update { it.copy(onboarded = true) }; log("ONBOARD done"); DetectService.start(this@MainActivity); go(Route.Home) },
            )
            Route.Home -> {
                val today = moments.filter { Store.ymd(it.atMs) == Store.ymd(System.currentTimeMillis()) }
                val month = moments.filter { Store.ym(it.atMs) == Store.ym(System.currentTimeMillis()) }
                val countedToday = today.count { it.kind != Kind.NO }
                if (seenCount < 0) seenCount = countedToday
                val s = HomeState(
                    today = today, month = month, pending = candidates.filter { it.resolved == null },
                    signalRate = if (manner.laneChanges > 0) manner.signaled * 100 / manner.laneChanges else null,
                    askEnabled = settings.askEnabled, dailyMax = settings.dailyMax,
                    paused = if (day.ymd == Store.ymd(System.currentTimeMillis())) day.paused else null,
                    micOff = !mic, newSinceSeen = countedToday > seenCount,
                )
                log("HOME recv=${today.count { it.kind == Kind.RECEIVED }} give=${today.count { it.kind == Kind.GIVEN }} pending=${s.pending.size} paused=${s.paused} micOff=${s.micOff}")
                HomeScreen(s, goTab,
                    onConfirm = { go(Route.Confirm) }, onRecords = { go(Route.Records) }, onMonth = { go(Route.Month) },
                    onAskToggle = { v -> app.store.update { it.copy(askEnabled = v) } },
                    onResume = { app.store.updateDay { it.copy(paused = null, noAnswerRun = 0) } },
                    onMicOn = { requestPerms {} })
            }
            Route.Records -> RecordsScreen(moments, goTab, onDelete = { app.store.delete(it) }, onRestore = { app.store.restore(it) })
            Route.Confirm -> ConfirmScreen(candidates, goTab, onBack = { go(Route.Home) }, onResolve = { id, gave -> log("CONFIRM id=$id gave=$gave"); app.store.resolve(id, gave) })
            Route.Month -> MonthScreen(moments, settings.region, goTab, onBack = { go(Route.Home) }, onQr = { go(Route.Qr) })
            Route.Qr -> {
                val token = remember { java.util.UUID.randomUUID().toString().take(8) }
                QrScreen("https://card.thankyou-meter.example/c/$token", goTab, onBack = { go(Route.Month) })
            }
            Route.Region -> {
                val online by app.online.collectAsState()
                val todayN = moments.count { it.kind != Kind.NO && Store.ymd(it.atMs) == Store.ymd(System.currentTimeMillis()) }
                val myRecv = moments.count { it.kind == Kind.RECEIVED && Store.ym(it.atMs) == Store.ym(System.currentTimeMillis()) }
                log("REGION online=$online today=$todayN")
                RegionScreen(settings.region, myRecv, todayN, online, goTab)
            }
            Route.Manner -> { log("MANNER $manner"); MannerScreen(manner, goTab) }
            Route.Settings -> SettingsScreen(settings, moments.size, goTab, update = { f -> app.store.update(f) },
                onPreview = { app.voice.earcon(Earcon.ASK) { app.voice.speak("양보받았어요?") {} } },
                onVoiceTest = { go(Route.Onboard(4)) },
                onClearAll = { app.store.clearAll(); log("SETTINGS cleared") },
                onSeed = { app.store.seedDemo() })
            else -> Placeholder(r, goTab)
        }
    }

    @Composable
    private fun Placeholder(r: Route, goTab: (Tab) -> Unit) {
        val tab = when (r) { Route.Records -> Tab.RECORDS; Route.Region -> Tab.REGION; Route.Manner -> Tab.MANNER; Route.Settings -> Tab.SETTINGS; else -> Tab.HOME }
        TabScaffold(tab, goTab) {
            Column(Modifier.fillMaxSize().testTag("screen_todo")) {
                AppHead("준비 중")
                Text("$r", style = TmType.body, color = tc.text3, modifier = Modifier.padding(32.dp))
            }
        }
    }
}
