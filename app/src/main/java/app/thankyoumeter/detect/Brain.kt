package app.thankyoumeter.detect

import android.os.Handler
import android.os.Looper
import app.thankyoumeter.Log.log
import app.thankyoumeter.TymApp
import app.thankyoumeter.vehicle.Sig

/**
 * 감지기와 질문 흐름을 묶는다. 서비스가 도는 동안만 있다.
 * S2: 감지 결과를 기록(매너 통계, 양보 후보). S3: 깜빡이 켠 차선 변경 뒤 묻기.
 */
class Brain(private val app: TymApp) {
    private val main = Handler(Looper.getMainLooper())
    private val detector = Detector(::onEvent)
    /** 깜빡이를 켠 차선 변경이 끝났을 때(S3가 연결한다) */
    var onAskable: ((Event.LaneChange) -> Unit)? = null
    private var running = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            detector.tick(System.currentTimeMillis())
            main.postDelayed(this, 250)
        }
    }

    fun start() {
        if (running) return
        running = true
        // 서비스가 늦게 떠도 지금 기어부터 안다
        detector.onSig(Sig.Geared(app.signals.gear.value, System.currentTimeMillis()))
        main.post(ticker)
        log("DET start gear=${app.signals.gear.value}")
    }

    fun stop() { running = false; main.removeCallbacks(ticker); log("DET stop") }

    fun onSig(s: Sig) { if (running) detector.onSig(s) }

    private fun onEvent(e: Event) {
        when (e) {
            is Event.LaneChange -> {
                log("DET lane signaled=${e.signaled} side=${e.side} peak=${"%.0f".format(e.peakDeg)} avg=${"%.0f".format(e.avgKmh)} abrupt=${e.abrupt}")
                app.store.updateManner { m -> m.copy(laneChanges = m.laneChanges + 1, signaled = m.signaled + if (e.signaled) 1 else 0, abrupt = m.abrupt + if (e.abrupt) 1 else 0) }
                if (e.signaled) onAskable?.invoke(e)
            }
            is Event.Rejected -> log("DET reject ${e.reason}")
            is Event.GiveCandidate -> {
                log("DET give ${e.fromKmh}->${e.toKmh}")
                if (app.store.settings.value.collectGive) app.store.addCandidate(e.fromKmh, e.toKmh, e.t)
            }
            is Event.HardBrake -> {
                log("DET hardbrake ${e.fromKmh}->${e.toKmh}")
                app.store.updateManner { m -> m.copy(hardBrakes = m.hardBrakes + 1) }
            }
        }
    }
}
