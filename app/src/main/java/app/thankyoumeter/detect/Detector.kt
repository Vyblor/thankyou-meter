package app.thankyoumeter.detect

import app.thankyoumeter.vehicle.Gear
import app.thankyoumeter.vehicle.Sig
import app.thankyoumeter.vehicle.Turn
import kotlin.math.abs
import kotlin.math.sign

/** 감지 결과. */
sealed interface Event {
    val t: Long
    /** 차선 변경 완료. signaled = 깜빡이를 켜고 했는지(켜지 않은 것은 매너 통계에만). */
    data class LaneChange(val side: Turn, val signaled: Boolean, val abrupt: Boolean, val peakDeg: Float, val avgKmh: Float, override val t: Long) : Event
    /** 묻지 않는 경우(로그·시험용). reason: turn, slow, signal_only, timeout, not_drive */
    data class Rejected(val reason: String, override val t: Long) : Event
    /** 양보 후보: 직진 중 감속 후 다시 가속 */
    data class GiveCandidate(val fromKmh: Int, val toKmh: Int, override val t: Long) : Event
    data class HardBrake(val fromKmh: Int, val toKmh: Int, override val t: Long) : Event
}

/**
 * 감지 설계 v0.2(docs/detection-design.md)의 규칙. 값은 모두 INFERRED이며 시나리오 시험으로 조정한다.
 * 좌우는 방향지시등으로만 판단한다. 조향은 절댓값과 "처음과 반대 부호인지"만 본다.
 * 순수 로직이라 시각(t)을 신호와 tick으로만 받는다.
 */
class Detector(private val emit: (Event) -> Unit) {
    companion object {
        const val STEER_START = 5f        // 조향 시작 기준
        const val STEER_COUNTER = 3f      // 반대로 되돌림 기준
        const val STEER_SETTLE = 3f       // 직진 복귀
        const val SETTLE_MS = 3000L
        const val TURN_DEG = 90f          // 이보다 크면 회전
        const val MIN_AVG_KMH = 30f
        const val SIGNAL_GRACE_MS = 10_000L   // 깜빡이 꺼진 뒤에도 같은 사건으로 보는 시간
        const val SIGNAL_ONLY_MS = 30_000L    // 깜빡이만 켜고 조향 없음 → 취소
        const val ABRUPT_DELTA = 40f      // 0.5초 안에 이만큼 바뀌면 급한 차선 변경
        const val ABRUPT_WINDOW_MS = 500L

        const val GIVE_MIN_FROM = 30f
        const val GIVE_DROP = 10f
        const val GIVE_DROP_MS = 5000L
        const val GIVE_FLOOR = 10f
        const val GIVE_STEER_MAX = 10f
        const val GIVE_AFTER_LC_MS = 30_000L
        const val GIVE_RECOVER = 5f
        const val GIVE_RECOVER_MS = 20_000L
        const val HARD_BRAKE_DROP = 15f   // 1초 안에 이만큼 줄면 급제동(약 0.42g)
        const val HARD_BRAKE_MS = 1000L
    }

    var gear = Gear.UNKNOWN; private set
    private var turn = Turn.OFF
    private var steer = 0f
    private var steerAt = 0L
    private var speed = 0f
    private var lastLcAt = -1_000_000L

    // ---------- 조향 사건(깜빡이 유무와 관계없이) ----------
    private class Maneuver(val start: Long) {
        var side: Turn = Turn.OFF
        var signaled = false
        var firstSign = 0f
        var countered = false
        var peak = 0f
        var abrupt = false
        var settleSince = -1L
        val speeds = ArrayList<Float>()
    }
    private var man: Maneuver? = null
    private var signalOnAt = -1L
    private var signalOffAt = -1L
    private var signalSide = Turn.OFF
    private var signalSawSteer = false
    private var signalPeak = 0f
    private var signalSpeeds = ArrayList<Float>()
    private val steerHist = ArrayDeque<Pair<Long, Float>>()

    // ---------- 감속 ----------
    private val speedHist = ArrayDeque<Pair<Long, Float>>()
    private class Decel(val peak: Float, val peakAt: Long) { var min = peak; var minAt = peakAt; var bad = false }
    private var decel: Decel? = null
    private var lastHardAt = -1_000_000L
    private var lastSampleAt = 0L

    private fun pushSpeed(t: Long, v: Float) {
        lastSampleAt = t
        speedHist.addLast(t to v)
        while (speedHist.size > 0 && t - speedHist.first().first > GIVE_DROP_MS + 1000) speedHist.removeFirst()
    }

    fun onSig(s: Sig) {
        when (s) {
            is Sig.Geared -> { gear = s.g; if (s.g != Gear.D) { resetAll() } }
            is Sig.Signal -> onSignal(s.v, s.t)
            is Sig.Steer -> onSteer(s.deg, s.t)
            is Sig.Speed -> onSpeed(s.kmh, s.t)
        }
        tick(s.t)
    }

    private fun resetAll() { man = null; signalOnAt = -1; signalOffAt = -1; decel = null; steerHist.clear(); speedHist.clear() }

    private fun onSignal(v: Turn, t: Long) {
        val prev = turn; turn = v
        if (v != Turn.OFF && prev == Turn.OFF) {
            if (signalOnAt < 0) { signalOnAt = t; signalSide = v; signalSawSteer = false; signalPeak = 0f; signalSpeeds = arrayListOf(speed) }
            signalOffAt = -1
            // 오래 묵은 조향 사건(긴 커브, 앞서 돌려 둔 핸들)이 이번 차선 변경을 막지 않게: 10초 넘은 사건은 버리고 새로 본다
            val old = man
            if (old != null && t - old.start > SIGNAL_GRACE_MS) { man = null; emit(Event.Rejected("stale_reset", t)) }
            man?.let { if (!it.signaled) { it.signaled = true; it.side = v } }
            decel?.bad = true
        } else if (v == Turn.OFF && prev != Turn.OFF) {
            signalOffAt = t
        }
    }

    private fun onSteer(d: Float, t: Long) {
        steer = d; steerAt = t
        steerHist.addLast(t to d)
        while (steerHist.size > 0 && t - steerHist.first().first > ABRUPT_WINDOW_MS) steerHist.removeFirst()
        val a = abs(d)
        if (signalOnAt >= 0) { if (a > STEER_START) signalSawSteer = true; if (a > signalPeak) signalPeak = a }
        if (a > GIVE_STEER_MAX) decel?.bad = true
        val m = man
        if (m == null) {
            if (a > STEER_START && gear == Gear.D) {
                val nm = Maneuver(t)
                nm.firstSign = sign(d); nm.peak = a; nm.speeds += speed
                if (signalActive(t)) { nm.signaled = true; nm.side = activeSide() }
                man = nm
            }
            return
        }
        if (a > m.peak) m.peak = a
        if (!m.countered && sign(d) == -m.firstSign && a > STEER_COUNTER) m.countered = true
        val oldest = steerHist.firstOrNull()?.second ?: d
        if (abs(d - oldest) >= ABRUPT_DELTA) m.abrupt = true
        if (a < STEER_SETTLE) { if (m.settleSince < 0) m.settleSince = t } else m.settleSince = -1
        m.speeds += speed
    }

    /** 지금 켜져 있거나, 꺼진 지 10초 안이다. 원터치(Flash)는 에뮬레이터에서 꺼짐이 오지 않아 켜진 채로 남는다(2026-11-07 실측). */
    private fun signalActive(t: Long) = turn != Turn.OFF || (signalOnAt >= 0 && signalOffAt >= 0 && t - signalOffAt <= SIGNAL_GRACE_MS)
    private fun activeSide() = if (turn != Turn.OFF) turn else signalSide

    private fun onSpeed(v: Float, t: Long) {
        speed = v
        man?.speeds?.add(v)
        if (signalOnAt >= 0) signalSpeeds.add(v)
        pushSpeed(t, v)
        if (gear != Gear.D) return
        // 급제동: 1초 안에 15 km/h 이상
        val oneSecAgo = speedHist.firstOrNull { t - it.first <= HARD_BRAKE_MS }
        if (oneSecAgo != null && oneSecAgo.second - v >= HARD_BRAKE_DROP && t - lastHardAt > 5000) {
            lastHardAt = t
            emit(Event.HardBrake(oneSecAgo.second.toInt(), v.toInt(), t))
            decel?.bad = true
        }
        val d = decel
        if (d == null) {
            // 감속 시작: 최근 5초 최대값에서 10 km/h 이상 줄었다
            // 같은 최대값이 여러 개면 가장 최근 것(속도를 유지하다 줄인 시점)
            val top = speedHist.maxOfOrNull { it.second } ?: return
            val peak = speedHist.last { it.second == top }
            if (peak.second >= GIVE_MIN_FROM && peak.second - v >= GIVE_DROP && t - peak.first <= GIVE_DROP_MS) {
                val nd = Decel(peak.second, peak.first)
                nd.min = v; nd.minAt = t
                nd.bad = turn != Turn.OFF || abs(steer) > GIVE_STEER_MAX || t - lastLcAt < GIVE_AFTER_LC_MS || man != null || t - lastHardAt < 5000
                decel = nd
            }
        } else {
            if (v < d.min) { d.min = v; d.minAt = t }
            if (v < GIVE_FLOOR) { decel = null; emit(Event.Rejected("give_stopped", t)); return }
            if (v >= d.min + GIVE_RECOVER) {
                decel = null
                if (d.bad) emit(Event.Rejected("give_context", t))
                else emit(Event.GiveCandidate(d.peak.toInt(), d.min.toInt(), t))
            }
        }
    }

    /** 시간이 흐르는 것만으로 끝나는 판정(직진 복귀 3초, 깜빡이만, 감속 회복 시한). 서비스가 250ms마다 부른다. */
    fun tick(t: Long) {
        // 값이 그대로면 신호가 드물게 오므로, 지금 속도를 0.5초마다 이력에 넣는다(최근 5초 최대값이 사라지지 않게)
        if (gear == Gear.D && t - lastSampleAt >= 500) pushSpeed(t, speed)
        val m = man
        if (m != null) {
            if (m.peak > TURN_DEG && m.settleSince >= 0 && t - m.settleSince >= SETTLE_MS) { finishTurn(t); }
            else if (m.settleSince >= 0 && t - m.settleSince >= SETTLE_MS) finishManeuver(m, t)
            else if (t - m.start > 60_000) { man = null; emit(Event.Rejected("timeout", t)) }
        }
        if (signalOnAt >= 0 && man == null) {
            val offLong = signalOffAt >= 0 && t - signalOffAt > SIGNAL_GRACE_MS
            if (!signalSawSteer && t - signalOnAt > SIGNAL_ONLY_MS) { emit(Event.Rejected("signal_only", t)); signalOnAt = -1 }
            else if (offLong) { if (!signalSawSteer) emit(Event.Rejected("signal_only", t)); signalOnAt = -1 }
        }
        val d = decel
        if (d != null && t - d.minAt > GIVE_RECOVER_MS) { decel = null; emit(Event.Rejected("give_no_recover", t)) }
    }

    private fun finishTurn(t: Long) {
        man = null; lastLcAt = t
        if (signalOnAt >= 0) signalOnAt = -1
        emit(Event.Rejected("turn", t))
    }

    private fun finishManeuver(m: Maneuver, t: Long) {
        man = null
        val avg = if (m.speeds.isEmpty()) speed else m.speeds.average().toFloat()
        lastLcAt = t
        val usedSignal = m.signaled
        if (usedSignal) signalOnAt = -1
        when {
            m.peak > TURN_DEG -> emit(Event.Rejected("turn", t))
            !m.countered -> emit(Event.Rejected("no_counter", t))
            avg < MIN_AVG_KMH -> emit(Event.Rejected("slow", t))
            gear != Gear.D -> emit(Event.Rejected("not_drive", t))
            else -> emit(Event.LaneChange(if (usedSignal) m.side else Turn.OFF, usedSignal, m.abrupt, m.peak, avg, t))
        }
    }
}
