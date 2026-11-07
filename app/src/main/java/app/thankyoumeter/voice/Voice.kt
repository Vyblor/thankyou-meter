package app.thankyoumeter.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import app.thankyoumeter.Log.log
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** 대답 분류. */
enum class Answer { YES, NO, STOP, UNKNOWN }

object Classifier {
    private val stop = listOf("오늘은 그만", "오늘 그만", "그만 물어", "그만")
    private val no = listOf("아니", "아뇨", "안 받", "못 받", "없어")
    private val yes = listOf("고마워", "고맙", "감사", "땡큐", "thank", "응", "네", "예", "그래", "맞아", "받았")

    /** 소리 원본은 저장하지 않는다. 글자도 분류 뒤 버린다. */
    fun classify(raw: String?, anyYes: Boolean): Answer {
        val t = raw?.trim()?.lowercase()?.replace(Regex("[.,!?]"), "") ?: return Answer.UNKNOWN
        if (t.isEmpty()) return Answer.UNKNOWN
        if (stop.any { t.contains(it) }) return Answer.STOP
        if (no.any { t.contains(it) }) return Answer.NO
        if (yes.any { t.contains(it) }) return Answer.YES
        return if (anyYes) Answer.YES else Answer.UNKNOWN
    }
}

enum class Earcon { ASK, SAVED, CANCEL }

/**
 * Pleos TextToSpeech / SpeechToText 3.3.1(ON_DEVICE)와 알림음.
 * 호출 모양은 contest/probe-app 시험 앱에서 에뮬레이터로 확인한 것만 쓴다.
 * 디버그 빌드는 inject()로 "인식된 글자"를 넣을 수 있다(합성 음성은 STT가 0/10이라 e2e는 이 경로로 시험).
 */
class Voice(private val ctx: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var stt: SpeechToText? = null
    @Volatile var ttsReady = false; private set
    @Volatile var sttReady = false; private set
    private var onSpoken: (() -> Unit)? = null
    private var onHeard: ((String?) -> Unit)? = null
    private var lastText: String? = null
    @Volatile private var started = false
    private var listenTimeout: Runnable? = null
    private var focus: AudioFocusRequest? = null
    /** 0 작게, 1 보통, 2 크게 — 알림음에만(TTS 음량 API는 문서에 없음) */
    var level = 1
    /** 디버그: 다음 듣기에 넣을 글자 */
    private var injected: String? = null
    private var injectedAt = 0L

    fun init() {
        try {
            tts = TextToSpeech(ctx, ai.pleos.playground.tts.constant.Mode.ON_DEVICE).also { t ->
                t.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                    override fun onSuccess() { ttsReady = true; t.setLocale(Locale.KOREA); log("VOICE tts ready") }
                    override fun onFailure(e: ai.pleos.playground.tts.constant.ErrorType) { log("VOICE tts init failed $e") }
                })
                t.addEventListener(object : EventListener {
                    override fun onStart() { log("VOICE tts start") }
                    override fun onReady() {}
                    override fun onDone() { log("VOICE tts done"); main.post { val f = onSpoken; onSpoken = null; f?.invoke() } }
                    override fun onError(e: ai.pleos.playground.tts.constant.ErrorType) { log("VOICE tts error $e"); main.post { val f = onSpoken; onSpoken = null; f?.invoke() } }
                    override fun onStop() {}
                    override fun onUpdatedRms(r: Double) {}
                })
            }
        } catch (e: Throwable) { log("VOICE tts unavailable $e") }
        try {
            stt = SpeechToText(ctx, ai.pleos.playground.stt.constant.Mode.ON_DEVICE).also { s ->
                s.initialize(object : ai.pleos.playground.stt.listener.OnInitializeListener {
                    override fun onSuccess() { sttReady = true; s.setLocale(Locale.KOREA); log("VOICE stt ready") }
                    override fun onFailure(e: ai.pleos.playground.stt.constant.ErrorType) { log("VOICE stt init failed $e") }
                })
                s.addListener(object : ResultListener {
                    override fun onUpdated(text: String, isFinal: Boolean) {
                        log("VOICE stt heard final=$isFinal len=${text.length}")
                        lastText = text
                        if (isFinal && started) main.post { finishListen(text) }
                    }
                    override fun onUpdatedEpdData(a: Long, b: Long) {}
                    override fun onStartedRecognition() { started = true; log("VOICE stt started") }
                    override fun onEndedRecognition() { log("VOICE stt ended started=$started"); val ok = started; started = false; if (ok) main.post { finishListen(lastText) } }
                    override fun onError(e: ai.pleos.playground.stt.constant.ErrorType) { log("VOICE stt error $e started=$started"); val ok = started; started = false; if (ok) main.post { finishListen(lastText) } }
                    override fun onReady() {}
                    override fun onUpdatedRms(r: Double) {}
                })
            }
        } catch (e: Throwable) { log("VOICE stt unavailable $e") }
    }

    /** 오디오 포커스. 내비 안내 등 다른 앱이 말하고 있으면 거절될 수 있다 → 그때는 묻지 않는다. */
    fun requestFocus(): Boolean {
        val am = ctx.getSystemService(AudioManager::class.java)
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener { f -> log("VOICE focus change $f") }.build()
        val r = am.requestAudioFocus(req)
        focus = req
        log("VOICE focus result=$r")
        return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun releaseFocus() {
        focus?.let { ctx.getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it) }
        focus = null
    }

    fun speak(text: String, done: () -> Unit) {
        log("VOICE say '$text'")
        val t = tts
        if (t == null || !ttsReady) { log("VOICE tts not ready, skip speech"); main.postDelayed(done, 300); return }
        onSpoken = done
        try { t.speak(text, true) } catch (e: Throwable) { log("VOICE speak failed $e"); onSpoken = null; main.post(done) }
        // onDone이 오지 않을 때를 대비한 안전 시간
        main.postDelayed({ if (onSpoken === done) { log("VOICE tts done timeout"); onSpoken = null; done() } }, 6000)
    }

    /** windowMs 동안 듣는다. 끝나면 인식된 글자(없으면 null)로 heard를 부른다. */
    fun listen(windowMs: Long, heard: (String?) -> Unit) {
        lastText = null
        onHeard = heard
        log("VOICE listen ${windowMs}ms")
        // 디버그 주입: 듣기 시작 전후 10초 안에 들어온 글자
        val inj = injected
        if (inj != null && System.currentTimeMillis() - injectedAt < 10_000) {
            injected = null
            main.postDelayed({ log("VOICE injected used"); finishListen(inj) }, 400)
            return
        }
        // 앞 듣기의 늦은 종료 이벤트가 이번 듣기를 끝내지 않게: 이번 요청의 시작 이벤트를 본 뒤에만 종료를 받는다
        started = false
        try { stt?.request() } catch (e: Throwable) { log("VOICE stt request failed $e") }
        val r = Runnable { log("VOICE listen window end"); try { stt?.stop() } catch (_: Throwable) {}; main.postDelayed({ finishListen(lastText) }, 600) }
        listenTimeout = r
        main.postDelayed(r, windowMs)
    }

    private fun finishListen(text: String?) {
        val f = onHeard ?: return
        onHeard = null
        listenTimeout?.let { main.removeCallbacks(it) }; listenTimeout = null
        log("VOICE heard ${if (text.isNullOrBlank()) "nothing" else "text"}")
        f(text)
    }

    /** 디버그 빌드 전용: 지금(또는 곧 시작할) 듣기에 글자를 넣는다. */
    fun inject(text: String) {
        log("VOICE inject")
        if (onHeard != null) { started = false; try { stt?.stop() } catch (_: Throwable) {}; finishListen(text) }
        else { injected = text; injectedAt = System.currentTimeMillis() }
    }

    fun cancelListen() { onHeard = null; started = false; listenTimeout?.let { main.removeCallbacks(it) }; try { stt?.stop() } catch (_: Throwable) {} }

    // ---------- 알림음 ----------
    fun earcon(e: Earcon, done: (() -> Unit)? = null) {
        val tones: List<Pair<Double, Int>> = when (e) {
            Earcon.ASK -> listOf(659.25 to 150)
            Earcon.SAVED -> listOf(523.25 to 120, 783.99 to 120)
            Earcon.CANCEL -> listOf(-392.0 to 120) // 음수 = 아래로 미끄러짐
        }
        log("VOICE earcon $e")
        val rate = 22050
        val pcm = ArrayList<Short>()
        val amp = when (level) { 0 -> 0.18; 2 -> 0.55; else -> 0.35 } * Short.MAX_VALUE
        for ((f, ms) in tones) {
            val n = rate * ms / 1000
            for (i in 0 until n) {
                val freq = if (f < 0) -f * (1.0 - 0.18 * i / n) else f
                val env = minOf(1.0, i / (rate * 0.008), (n - i) / (rate * 0.03))
                pcm += (sin(2 * PI * freq * i / rate) * amp * env).toInt().toShort()
            }
            repeat(rate * 30 / 1000) { pcm += 0 }
        }
        try {
            val buf = pcm.toShortArray()
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(buf.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            track.write(buf, 0, buf.size)
            track.play()
            val dur = buf.size * 1000L / rate
            main.postDelayed({ try { track.release() } catch (_: Throwable) {}; done?.invoke() }, dur + 40)
        } catch (t: Throwable) { log("VOICE earcon failed $t"); done?.invoke() }
    }
}
