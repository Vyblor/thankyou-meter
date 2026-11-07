package app.thankyoumeter.data

import android.content.Context
import app.thankyoumeter.Log.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

/** 기록 종류. RECEIVED = 양보받음("고마워"), GIVEN = 양보함(감속 후보를 운전자가 고름), NO = "아니"라고 답함(숫자에 안 들어감). */
enum class Kind { RECEIVED, GIVEN, NO }

data class Moment(
    val id: Long,
    val kind: Kind,
    val atMs: Long,
    val region: String,
    /** 예: "차선 변경 왼쪽", "직진 중 58→34 km/h 감속" */
    val detail: String,
)

/** 양보 후보: 직진 중 감속. 운전자가 정차 후 고른다. resolved: null = 아직, true = 양보했어요, false = 아니에요 */
data class Candidate(
    val id: Long,
    val atMs: Long,
    val region: String,
    val fromKmh: Int,
    val toKmh: Int,
    val resolved: Boolean? = null,
)

data class Settings(
    val onboarded: Boolean = false,
    val askEnabled: Boolean = true,
    val dailyMax: Int = 10,
    /** "아니"만 아니오, 나머지 말은 모두 예 */
    val anyYes: Boolean = false,
    /** 0 작게, 1 보통, 2 크게 */
    val voiceLevel: Int = 1,
    val collectGive: Boolean = true,
    val region: String = "성동구",
)

/** 그날의 질문 상태. 날짜가 바뀌면 새로 시작한다. */
data class Day(
    val ymd: Int,
    val asked: Int = 0,
    val noAnswerRun: Int = 0,
    /** null = 쉬지 않음, "user" = "오늘은 그만", "noanswer" = 무응답 3번 */
    val paused: String? = null,
    val lastAskMs: Long = 0,
)

/** 차가 잰 매너 통계(이번 달). 자기 신고 기록은 넣지 않는다. */
data class Manner(
    val ym: Int,
    val laneChanges: Int = 0,
    val signaled: Int = 0,
    val abrupt: Int = 0,
    val hardBrakes: Int = 0,
)

/** 서버 없이 기기 안 JSON 파일 하나에 쌓는다. */
class Store(private val ctx: Context) {
    private val file = File(ctx.filesDir, "tym.json")

    private val _moments = MutableStateFlow<List<Moment>>(emptyList())
    val moments: StateFlow<List<Moment>> = _moments
    private val _candidates = MutableStateFlow<List<Candidate>>(emptyList())
    val candidates: StateFlow<List<Candidate>> = _candidates
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings
    private val _day = MutableStateFlow(Day(ymd(System.currentTimeMillis())))
    val day: StateFlow<Day> = _day
    private val _manner = MutableStateFlow(Manner(ym(System.currentTimeMillis())))
    val manner: StateFlow<Manner> = _manner

    private var nextId = 1L

    init { load() }

    // ---------- 기록 ----------
    fun add(kind: Kind, detail: String, atMs: Long = System.currentTimeMillis()): Moment {
        val m = Moment(nextId++, kind, atMs, settings.value.region, detail)
        _moments.value = (_moments.value + m).sortedByDescending { it.atMs }
        save(); log("STORE add ${m.kind} id=${m.id} detail=$detail")
        return m
    }

    fun delete(id: Long): Moment? {
        val m = _moments.value.firstOrNull { it.id == id } ?: return null
        _moments.value = _moments.value.filterNot { it.id == id }
        save(); log("STORE delete id=$id kind=${m.kind}")
        return m
    }

    fun restore(m: Moment) {
        if (_moments.value.any { it.id == m.id }) return
        _moments.value = (_moments.value + m).sortedByDescending { it.atMs }
        save(); log("STORE restore id=${m.id}")
    }

    fun clearAll() {
        _moments.value = emptyList(); _candidates.value = emptyList()
        _manner.value = Manner(ym(System.currentTimeMillis()))
        save(); log("STORE clearAll")
    }

    // ---------- 양보 후보 ----------
    fun addCandidate(fromKmh: Int, toKmh: Int, atMs: Long = System.currentTimeMillis()): Candidate {
        val cnd = Candidate(nextId++, atMs, settings.value.region, fromKmh, toKmh)
        _candidates.value = _candidates.value + cnd
        save(); log("STORE candidate id=${cnd.id} $fromKmh->$toKmh")
        return cnd
    }

    /** 운전자가 고른다. true면 "양보함" 기록이 생긴다. */
    fun resolve(id: Long, gave: Boolean) {
        val cnd = _candidates.value.firstOrNull { it.id == id && it.resolved == null } ?: return
        _candidates.value = _candidates.value.map { if (it.id == id) it.copy(resolved = gave) else it }
        if (gave) {
            val m = Moment(nextId++, Kind.GIVEN, cnd.atMs, cnd.region, "직진 중 ${cnd.fromKmh}→${cnd.toKmh} km/h 감속 · 내가 확인")
            _moments.value = (_moments.value + m).sortedByDescending { it.atMs }
        }
        save(); log("STORE resolve id=$id gave=$gave")
    }

    /** 오늘 생긴 후보만 남긴다(고르지 않은 후보는 자정에 지운다). */
    fun pruneCandidates(now: Long = System.currentTimeMillis()) {
        val today = ymd(now)
        val keep = _candidates.value.filter { ymd(it.atMs) == today }
        if (keep.size != _candidates.value.size) { _candidates.value = keep; save(); log("STORE prune candidates left=${keep.size}") }
    }

    fun pending(): List<Candidate> = _candidates.value.filter { it.resolved == null }

    // ---------- 설정 ----------
    fun update(f: (Settings) -> Settings) {
        _settings.value = f(_settings.value); save(); log("STORE settings ${_settings.value}")
    }

    // ---------- 그날 질문 상태 ----------
    fun dayNow(now: Long = System.currentTimeMillis()): Day {
        val d = _day.value
        if (d.ymd != ymd(now)) { _day.value = Day(ymd(now)); save() }
        return _day.value
    }

    fun updateDay(f: (Day) -> Day) { dayNow(); _day.value = f(_day.value); save(); log("STORE day ${_day.value}") }

    // ---------- 매너 ----------
    fun updateManner(f: (Manner) -> Manner) {
        val now = ym(System.currentTimeMillis())
        if (_manner.value.ym != now) _manner.value = Manner(now)
        _manner.value = f(_manner.value); save(); log("STORE manner ${_manner.value}")
    }

    // ---------- 시연용 예시 ----------
    /** 지난 날짜에 예시 기록을 넣는다(월간 카드·기록 탭 시연용). 오늘 기록은 건드리지 않는다. */
    fun seedDemo(now: Long = System.currentTimeMillis()) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val regions = listOf("광진구", "성동구", "동대문구", "중구")
        val list = mutableListOf<Moment>()
        for (d in 1 until day) {
            val n = (d * 7) % 4 + 1
            for (k in 0 until n) {
                val t = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.DAY_OF_MONTH, d); set(Calendar.HOUR_OF_DAY, 8 + (k * 5 + d) % 11); set(Calendar.MINUTE, (d * 13 + k * 17) % 60) }.timeInMillis
                val give = (d + k) % 3 == 0
                list += Moment(nextId++, if (give) Kind.GIVEN else Kind.RECEIVED, t, regions[(d + k) % regions.size],
                    if (give) "직진 중 ${50 + k * 3}→${30 + k * 2} km/h 감속 · 내가 확인" else "차선 변경 ${if ((d + k) % 2 == 0) "왼쪽" else "오른쪽"}")
            }
        }
        _moments.value = (_moments.value + list).sortedByDescending { it.atMs }
        updateManner { it.copy(laneChanges = it.laneChanges + 48, signaled = it.signaled + 44, abrupt = it.abrupt + 2, hardBrakes = it.hardBrakes + 1) }
        save(); log("STORE seedDemo added=${list.size}")
    }

    // ---------- 파일 ----------
    private fun save() {
        val o = JSONObject()
        o.put("nextId", nextId)
        o.put("moments", JSONArray(_moments.value.map { JSONObject().put("id", it.id).put("kind", it.kind.name).put("at", it.atMs).put("region", it.region).put("detail", it.detail) }))
        o.put("candidates", JSONArray(_candidates.value.map { JSONObject().put("id", it.id).put("at", it.atMs).put("region", it.region).put("from", it.fromKmh).put("to", it.toKmh).put("resolved", it.resolved ?: JSONObject.NULL) }))
        val s = _settings.value
        o.put("settings", JSONObject().put("onboarded", s.onboarded).put("askEnabled", s.askEnabled).put("dailyMax", s.dailyMax).put("anyYes", s.anyYes).put("voiceLevel", s.voiceLevel).put("collectGive", s.collectGive).put("region", s.region))
        val d = _day.value
        o.put("day", JSONObject().put("ymd", d.ymd).put("asked", d.asked).put("noAnswerRun", d.noAnswerRun).put("paused", d.paused ?: JSONObject.NULL).put("lastAskMs", d.lastAskMs))
        val mn = _manner.value
        o.put("manner", JSONObject().put("ym", mn.ym).put("laneChanges", mn.laneChanges).put("signaled", mn.signaled).put("abrupt", mn.abrupt).put("hardBrakes", mn.hardBrakes))
        val tmp = File(file.parentFile, "tym.json.tmp")
        tmp.writeText(o.toString()); tmp.renameTo(file)
    }

    private fun load() {
        if (!file.exists()) return
        try {
            val o = JSONObject(file.readText())
            nextId = o.optLong("nextId", 1)
            val ms = o.optJSONArray("moments") ?: JSONArray()
            _moments.value = (0 until ms.length()).map { ms.getJSONObject(it) }.map {
                Moment(it.getLong("id"), Kind.valueOf(it.getString("kind")), it.getLong("at"), it.getString("region"), it.getString("detail"))
            }.sortedByDescending { it.atMs }
            val cs = o.optJSONArray("candidates") ?: JSONArray()
            _candidates.value = (0 until cs.length()).map { cs.getJSONObject(it) }.map {
                Candidate(it.getLong("id"), it.getLong("at"), it.getString("region"), it.getInt("from"), it.getInt("to"), if (it.isNull("resolved")) null else it.getBoolean("resolved"))
            }
            o.optJSONObject("settings")?.let {
                _settings.value = Settings(it.optBoolean("onboarded"), it.optBoolean("askEnabled", true), it.optInt("dailyMax", 10), it.optBoolean("anyYes"), it.optInt("voiceLevel", 1), it.optBoolean("collectGive", true), it.optString("region", "성동구"))
            }
            o.optJSONObject("day")?.let {
                _day.value = Day(it.getInt("ymd"), it.optInt("asked"), it.optInt("noAnswerRun"), if (it.isNull("paused")) null else it.getString("paused"), it.optLong("lastAskMs"))
            }
            o.optJSONObject("manner")?.let {
                _manner.value = Manner(it.getInt("ym"), it.optInt("laneChanges"), it.optInt("signaled"), it.optInt("abrupt"), it.optInt("hardBrakes"))
            }
        } catch (e: Exception) { log("STORE load failed $e") }
    }

    companion object {
        fun ymd(ms: Long): Int { val c = Calendar.getInstance().apply { timeInMillis = ms }; return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH) }
        fun ym(ms: Long): Int = ymd(ms) / 100
        fun hm(ms: Long): String { val c = Calendar.getInstance().apply { timeInMillis = ms }; return "%d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) }
    }
}
