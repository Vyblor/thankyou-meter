package app.thankyoumeter

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import app.thankyoumeter.Log.log
import app.thankyoumeter.data.Store
import app.thankyoumeter.vehicle.FakeSignals
import app.thankyoumeter.vehicle.PleosSignals
import app.thankyoumeter.vehicle.VehicleSignals
import app.thankyoumeter.voice.Voice
import kotlinx.coroutines.flow.MutableStateFlow

class TymApp : Application() {
    lateinit var store: Store; private set
    lateinit var signals: VehicleSignals; private set
    lateinit var voice: Voice; private set
    /** 감지·질문(S2·S3에서 채운다). 서비스가 돌리는 동안만 있다. */
    var brain: app.thankyoumeter.detect.Brain? = null

    /** 디버그 빌드 시험 전용 */
    val debugTheme = MutableStateFlow<String?>(null)
    val debugWidthDp = MutableStateFlow(0)
    var debugOffline = false; set(v) { field = v; publishOnline() }
    /** 디버그: 질문 간격(ms). 0이면 기본 180초 */
    var debugMinGapMs = 0L

    val online = MutableStateFlow(true)
    private var netOnline = true

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        signals = try { PleosSignals(this).also { it.start() } } catch (t: Throwable) { log("VEH pleos unavailable ($t), using fake"); FakeSignals().also { it.start() } }
        voice = Voice(this).also { it.init(); it.level = store.settings.value.voiceLevel }
        watchNetwork()
        if (BuildConfig.DEBUG) registerDebug()
        log("APP start vehicle=${signals.name}")
    }

    private fun publishOnline() {
        val v = netOnline && !debugOffline
        if (online.value != v) log("NET online=$v")
        online.value = v
    }

    private fun watchNetwork() {
        val cm = getSystemService(ConnectivityManager::class.java)
        netOnline = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        publishOnline()
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(n: Network, c: NetworkCapabilities) { netOnline = c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET); publishOnline() }
            override fun onLost(n: Network) { netOnline = false; publishOnline() }
        })
    }

    /** 디버그 빌드 전용 시험 방송: adb shell am broadcast -a app.thankyoumeter.DEBUG -p app.thankyoumeter --es say 고마워 */
    private fun registerDebug() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                i.getStringExtra("say")?.let { voice.inject(it) }
                if (i.hasExtra("offline")) debugOffline = i.getBooleanExtra("offline", false)
                if (i.hasExtra("min_gap_ms")) debugMinGapMs = i.getLongExtra("min_gap_ms", 0)
                if (i.hasExtra("theme")) debugTheme.value = i.getStringExtra("theme")?.ifEmpty { null }
                i.getStringExtra("cmd")?.let { cmd ->
                    log("DEBUG cmd $cmd")
                    when (cmd) {
                        "seed" -> store.seedDemo()
                        "reset_day" -> store.updateDay { it.copy(asked = 0, noAnswerRun = 0, paused = null, lastAskMs = 0) }
                        "dump" -> log("DEBUG dump moments=${store.moments.value.map { it.kind }} candidates=${store.candidates.value.map { "${it.fromKmh}->${it.toKmh}:${it.resolved}" }} day=${store.day.value} manner=${store.manner.value} settings=${store.settings.value}")
                    }
                }
            }
        }
        registerReceiver(r, IntentFilter("app.thankyoumeter.DEBUG"), Context.RECEIVER_EXPORTED)
    }
}

val Context.app get() = applicationContext as TymApp
