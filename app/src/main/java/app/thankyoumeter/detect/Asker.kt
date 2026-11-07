package app.thankyoumeter.detect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import app.thankyoumeter.Log.log
import app.thankyoumeter.TymApp
import app.thankyoumeter.data.Kind
import app.thankyoumeter.vehicle.Gear
import app.thankyoumeter.vehicle.Turn
import app.thankyoumeter.voice.Answer
import app.thankyoumeter.voice.Classifier
import app.thankyoumeter.voice.Earcon

/**
 * 와이어프레임 B3~B5. 깜빡이를 켠 차선 변경이 끝나고 6초 뒤 한 번 묻는다.
 * 띵(150ms) → "양보받았어요?" → 4초 듣기 → 예: "기록했어요" ♪♪ / 아니: 한 음 하강 / 그만: "오늘은 그만 물을게요" / 무응답: 아무 소리 없음.
 * 되묻지 않는다. 간격 180초, 하루 상한(설정), 무응답 3번 연속이면 그날 쉰다.
 */
class Asker(private val app: TymApp) {
    companion object {
        const val DELAY_MS = 6000L
        const val MIN_GAP_MS = 180_000L
        const val LISTEN_MS = 4000L
        const val NO_ANSWER_PAUSE = 3
        const val Q = "양보받았어요?"
    }

    private val main = Handler(Looper.getMainLooper())
    private var pending: Runnable? = null
    private var busy = false

    private fun gap() = if (app.debugMinGapMs > 0) app.debugMinGapMs else MIN_GAP_MS
    private fun micOk() = app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** 묻지 않는 까닭. null이면 물어도 된다. */
    private fun blocked(now: Long): String? {
        val s = app.store.settings.value
        val d = app.store.dayNow(now)
        return when {
            !s.askEnabled -> "off"
            !micOk() -> "mic"
            d.paused != null -> "paused_${d.paused}"
            d.asked >= s.dailyMax -> "daily_max"
            d.lastAskMs > 0 && now - d.lastAskMs < gap() -> "gap"
            busy || pending != null -> "busy"
            else -> null
        }
    }

    fun onLaneChange(e: Event.LaneChange) {
        val why = blocked(System.currentTimeMillis())
        if (why != null) { log("ASK skip $why"); return }
        val side = if (e.side == Turn.LEFT) "왼쪽" else "오른쪽"
        val r = Runnable { pending = null; fire(side) }
        pending = r
        main.postDelayed(r, DELAY_MS)
        log("ASK scheduled in ${DELAY_MS}ms side=$side")
    }

    fun cancel() { pending?.let { main.removeCallbacks(it) }; pending = null; if (busy) { app.voice.cancelListen(); busy = false; app.voice.releaseFocus() } }

    private fun fire(side: String) {
        val now = System.currentTimeMillis()
        val why = blocked(now)
        if (why != null) { log("ASK skip $why"); return }
        if (app.signals.gear.value != Gear.D) { log("ASK skip gear=${app.signals.gear.value}"); return }
        // 다른 앱(내비 안내 등)이 말하는 중이면 포커스를 못 받는다 → 이번엔 묻지 않는다
        if (!app.voice.requestFocus()) { log("ASK skip focus"); return }
        busy = true
        app.store.updateDay { it.copy(asked = it.asked + 1, lastAskMs = now) }
        log("ASK question side=$side")
        val v = app.voice
        v.level = app.store.settings.value.voiceLevel
        v.earcon(Earcon.ASK) {
            v.speak(Q) {
                v.listen(LISTEN_MS) { text -> answer(Classifier.classify(text, app.store.settings.value.anyYes), side) }
            }
        }
    }

    private fun answer(a: Answer, side: String) {
        val v = app.voice
        log("ASK answer $a")
        fun end() { busy = false; v.releaseFocus(); log("ASK done") }
        when (a) {
            Answer.YES -> {
                app.store.add(Kind.RECEIVED, "차선 변경 $side")
                app.store.updateDay { it.copy(noAnswerRun = 0) }
                v.speak("기록했어요") { v.earcon(Earcon.SAVED) { end() } }
            }
            Answer.NO -> {
                app.store.add(Kind.NO, "차선 변경 $side · 아니라고 답함")
                app.store.updateDay { it.copy(noAnswerRun = 0) }
                v.earcon(Earcon.CANCEL) { end() }
            }
            Answer.STOP -> {
                app.store.updateDay { it.copy(paused = "user", noAnswerRun = 0) }
                v.speak("오늘은 그만 물을게요") { end() }
            }
            Answer.UNKNOWN -> {
                app.store.updateDay { d ->
                    val run = d.noAnswerRun + 1
                    if (run >= NO_ANSWER_PAUSE) log("ASK pause noanswer")
                    d.copy(noAnswerRun = run, paused = if (run >= NO_ANSWER_PAUSE) "noanswer" else d.paused)
                }
                end()
            }
        }
    }
}
