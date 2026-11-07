package app.thankyoumeter.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.thankyoumeter.data.Kind
import app.thankyoumeter.data.Manner
import app.thankyoumeter.data.Moment
import app.thankyoumeter.data.Settings
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.Banner
import app.thankyoumeter.ui.components.BannerKind
import app.thankyoumeter.ui.components.BtnKind
import app.thankyoumeter.ui.components.BtnSize
import app.thankyoumeter.ui.components.Card
import app.thankyoumeter.ui.components.Haechi
import app.thankyoumeter.ui.components.HaechiState
import app.thankyoumeter.ui.components.HeartIcon
import app.thankyoumeter.ui.components.Mute
import app.thankyoumeter.ui.components.Pill
import app.thankyoumeter.ui.components.Popup
import app.thankyoumeter.ui.components.Segmented
import app.thankyoumeter.ui.components.SetRow
import app.thankyoumeter.ui.components.Stat
import app.thankyoumeter.ui.components.Stepper
import app.thankyoumeter.ui.components.SwitchRow
import app.thankyoumeter.ui.components.Tab
import app.thankyoumeter.ui.components.TabScaffold
import app.thankyoumeter.ui.components.TmButton
import app.thankyoumeter.ui.theme.HeartOnHoney
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.Tm
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.tc
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Calendar

// ---------- 계산(순수 함수, 시험 가능) ----------

data class MonthSummary(val ym: Int, val recv: Int, val give: Int, val bestTime: String?, val bestRegion: String?, val vsLast: Int)

fun monthSummary(all: List<Moment>, now: Long = System.currentTimeMillis()): MonthSummary {
    val ym = Store.ym(now)
    val c = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.DAY_OF_MONTH, 1); add(Calendar.MONTH, -1) }
    val lastYm = Store.ym(c.timeInMillis)
    val month = all.filter { Store.ym(it.atMs) == ym && it.kind != Kind.NO }
    val last = all.count { Store.ym(it.atMs) == lastYm && it.kind != Kind.NO }
    val recv = month.filter { it.kind == Kind.RECEIVED }
    val hour = recv.groupingBy { Calendar.getInstance().apply { timeInMillis = it.atMs }.get(Calendar.HOUR_OF_DAY) }.eachCount().maxByOrNull { it.value }?.key
    val bestTime = hour?.let { h -> when (h) { in 7..9 -> "출근길 ${h}시대"; in 17..19 -> "퇴근길 ${h}시대"; else -> "${h}시대" } }
    val bestRegion = month.groupingBy { it.region }.eachCount().maxByOrNull { it.value }?.key
    return MonthSummary(ym, recv.size, month.count { it.kind == Kind.GIVEN }, bestTime, bestRegion, month.size - last)
}

/** 동네 순위 목업. 서버가 없어 다른 동네 값은 고정 예시이고, 우리 동네 값만 이 차의 이번 달 "받은 양보"로 조금 움직인다. */
data class RankRow(val rank: Int, val name: String, val per10k: Double, val me: Boolean = false)

fun mockRanking(region: String, myRecv: Int, lastMonth: Boolean): List<RankRow> {
    val base = if (lastMonth) listOf("제주시" to 47.1, "세종시" to 43.2, "전주시 완산구" to 40.9) else listOf("제주시" to 48.2, "세종시" to 44.0, "전주시 완산구" to 41.7)
    val mine = (if (lastMonth) 30.4 else 31.5) + if (lastMonth) 0.0 else myRecv * 0.1
    return base.mapIndexed { i, (n, v) -> RankRow(i + 1, n, v) } + RankRow(if (lastMonth) 14 else 12, "$region (우리 동네)", Math.round(mine * 10) / 10.0, me = true)
}

fun qrBitmap(text: String, px: Int = 840): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, px, px)
    val b = Bitmap.createBitmap(m.width, m.height, Bitmap.Config.RGB_565)
    for (x in 0 until m.width) for (y in 0 until m.height) b.setPixel(x, y, if (m[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    return b
}

// ---------- D1 월간 카드 ----------

@Composable
fun MonthScreen(moments: List<Moment>, region: String, go: (Tab) -> Unit, onBack: () -> Unit, onQr: () -> Unit) {
    val s = monthSummary(moments)
    val portrait = LocalLayout.current == Layout.Portrait
    val rank = mockRanking(region, s.recv, false).last()
    TabScaffold(Tab.HOME, go) {
        Column(Modifier.fillMaxSize().testTag("screen_month")) {
            AppHead("${s.ym % 100}월 카드", onBack = onBack)
            val card: @Composable (Modifier) -> Unit = { mod ->
                Column(mod.clip(RoundedCornerShape(Tm.rXl)).background(tc.accent).border(2.dp, tc.accentEdge, RoundedCornerShape(Tm.rXl)).padding(48.dp).testTag("month_card"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${s.ym / 100}년 ${s.ym % 100}월의 고마움", style = TmType.titleSm, color = tc.onAccent)
                    Row(horizontalArrangement = Arrangement.spacedBy(56.dp), modifier = Modifier.padding(top = 16.dp)) {
                        CardNum(s.recv, "양보받았어요", "card_recv")
                        CardNum(s.give, "양보했어요", "card_give")
                    }
                    Text("$region · 전국 ${rank.rank}위 (예시)", style = TmType.body, color = tc.onAccent, modifier = Modifier.padding(top = 16.dp))
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Haechi(HaechiState.CHEER, 300.dp) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(HeartIcon, null, tint = HeartOnHoney, modifier = Modifier.size(32.dp)); Text("땡큐미터", style = TmType.label, color = tc.onAccent)
                    }
                }
            }
            val side: @Composable (Modifier) -> Unit = { mod ->
                Column(mod, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Stat("가장 고마웠던 시간", s.bestTime ?: "—", "", Modifier.fillMaxWidth(), tag = "month_time")
                    Stat("가장 많이 기록한 동네", s.bestRegion ?: "—", "", Modifier.fillMaxWidth(), tag = "month_region")
                    Stat("지난달보다", (if (s.vsLast >= 0) "+" else "") + s.vsLast, "번", Modifier.fillMaxWidth(), tag = "month_vs")
                    Mute("카드에는 이름·차 번호·정확한 위치가 들어가지 않아요.")
                    Spacer(Modifier.weight(1f))
                    TmButton("휴대폰으로 받기", "month_qr", BtnKind.PRIMARY, BtnSize.LG, modifier = Modifier.width(420.dp).padding(bottom = 40.dp), onClick = onQr)
                }
            }
            if (portrait) Column(Modifier.weight(1f).padding(horizontal = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                card(Modifier.fillMaxWidth().height(760.dp)); side(Modifier.fillMaxWidth().height(720.dp))
            } else Row(Modifier.weight(1f).padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                card(Modifier.width(720.dp).fillMaxHeight().padding(bottom = 40.dp)); side(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CardNum(n: Int, line: String, tag: String) {
    Column(Modifier.testTag(tag).semantics { contentDescription = "$line $n" }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$n", style = TmType.numLg, color = tc.onAccent)
            Text("번", style = TmType.title, color = tc.onAccent, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        }
        Text(line, style = TmType.body.copy(fontWeight = FontWeight.Bold), color = tc.onAccent)
    }
}

// ---------- D2 QR ----------

@Composable
fun QrScreen(url: String, go: (Tab) -> Unit, onBack: () -> Unit) {
    val bmp = remember(url) { qrBitmap(url).asImageBitmap() }
    TabScaffold(Tab.HOME, go) {
        Column(Modifier.fillMaxSize().testTag("screen_qr")) {
            AppHead("휴대폰으로 받기", onBack = onBack)
            Row(Modifier.weight(1f).padding(start = 96.dp, end = 32.dp, top = 48.dp), horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                Image(bmp, "카드 받기 QR", Modifier.size(420.dp).clip(RoundedCornerShape(Tm.rMd)).testTag("qr_image"))
                Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.width(560.dp)) {
                    Text("휴대폰 카메라로\n비추세요", style = TmType.title, color = tc.text)
                    Text("카드 이미지를 받아 메신저·SNS로 나눌 수 있어요.", style = TmType.body, color = tc.text3)
                    Pill("링크는 10분 뒤 사라져요", tag = "qr_pill")
                    Mute("시연판: 서버가 없어 예시 주소예요")
                }
            }
            TmButton("닫기", "qr_close", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.padding(start = 96.dp, bottom = 40.dp).width(320.dp), onClick = onBack)
        }
    }
}

// ---------- E1 동네 · X4 오프라인 ----------

@Composable
fun RegionScreen(region: String, myRecv: Int, todayCount: Int, online: Boolean, go: (Tab) -> Unit) {
    var sel by remember { mutableIntStateOf(0) }
    val rows = mockRanking(region, myRecv, sel == 1)
    val portrait = LocalLayout.current == Layout.Portrait
    TabScaffold(Tab.REGION, go) {
        Column(Modifier.fillMaxSize().testTag("screen_region").padding(bottom = 24.dp)) {
            AppHead("가장 고마운 동네")
            Column(Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!online) Banner(Icons.Rounded.CloudOff, "인터넷 없이 기록 중", "순위는 마지막으로 받은 값이에요 · 오늘 기록 ${todayCount}개는 연결되면 올라가요", BannerKind.INFO, tag = "banner_offline")
                Segmented(listOf("이번 달", "지난달"), sel, "rank_period", Modifier.width(520.dp), md = true) { sel = it }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.then(if (portrait) Modifier.fillMaxWidth() else Modifier.width(900.dp)), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        rows.forEachIndexed { i, r ->
                            if (r.me && i > 0) Mute("⋯", Modifier.padding(horizontal = 28.dp))
                            RankLine(r)
                        }
                    }
                    if (!portrait) Card(Modifier.weight(1f)) {
                        Text("어떻게 세나요", style = TmType.label, color = tc.accentText)
                        Text("그 동네에서 \"고마워\"라고 답한 횟수를 땡큐미터 사용자 1만 명당으로 나눠요.", style = TmType.bodySm, color = tc.text)
                        Mute("시연판: 서버가 없어 다른 동네 값은 예시예요.")
                    }
                }
            }
        }
    }
}

@Composable
private fun RankLine(r: RankRow) {
    val shape = RoundedCornerShape(Tm.rLg)
    var m = Modifier.fillMaxWidth().heightIn(min = 100.dp).clip(shape).background(if (r.me) tc.accentSoft else tc.surface)
    m = m.border(if (r.me) 2.dp else 1.dp, if (r.me) tc.accentEdge else tc.line, shape)
    Row(m.testTag(if (r.me) "rank_me" else "rank_${r.rank}").semantics { contentDescription = "${r.rank}위 ${r.name} ${r.per10k}" }.padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("${r.rank}", style = TmType.titleSm, color = tc.text2, modifier = Modifier.width(56.dp))
        Text(r.name, style = TmType.body, color = tc.text, modifier = Modifier.weight(1f))
        Text("${r.per10k}", style = TmType.titleSm, color = tc.text)
        Text("/1만 명", style = TmType.label, color = tc.text3)
    }
}

// ---------- F1 매너 ----------

@Composable
fun MannerScreen(m: Manner, go: (Tab) -> Unit) {
    val rate = if (m.laneChanges > 0) m.signaled * 100 / m.laneChanges else null
    val portrait = LocalLayout.current == Layout.Portrait
    TabScaffold(Tab.MANNER, go) {
        Column(Modifier.fillMaxSize().testTag("screen_manner")) {
            AppHead("매너 운전")
            Row(Modifier.padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Card(if (portrait) Modifier.fillMaxWidth() else Modifier.width(900.dp)) {
                    Text("차가 직접 잰 값이에요 · 이번 달", style = TmType.label, color = tc.accentText)
                    Gauge("깜빡이 켜고 차선 변경", rate?.let { "$it%" } ?: "—", (rate ?: 0) / 100f, if (m.laneChanges > 0) "차선 변경 ${m.laneChanges}번 중 ${m.signaled}번" else "아직 차선 변경이 없어요", "gauge_signal")
                    Gauge("급한 차선 변경", "${m.abrupt}번", minOf(1f, m.abrupt / 25f), "0.5초 안에 핸들을 크게 돌린 횟수", "gauge_abrupt")
                    Gauge("급제동", "${m.hardBrakes}번", minOf(1f, m.hardBrakes / 25f), "1초 안에 15 km/h 넘게 줄인 횟수 · 기준값은 시험 뒤 확정", "gauge_brake")
                }
                if (!portrait) Card(Modifier.weight(1f)) {
                    Text("양보 기록은 왜 없나요", style = TmType.label, color = tc.accentText)
                    Text("\"고마워\"와 \"양보했어요\"는 내가 말하거나 고른 기록이라 점수에 넣지 않아요. 매너 점수는 차가 잰 신호로만 만들어요.", style = TmType.bodySm, color = tc.text)
                    Mute("보험·지자체 연계도 이 점수로만 해요.")
                }
            }
        }
    }
}

@Composable
private fun Gauge(title: String, value: String, frac: Float, hint: String, tag: String) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).testTag(tag).semantics { contentDescription = "$title $value" }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, style = TmType.body.copy(fontWeight = FontWeight.Bold), color = tc.text, modifier = Modifier.weight(1f))
            Text(value, style = TmType.titleSm, color = tc.text)
        }
        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(999.dp)).background(tc.surface2)) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(999.dp)).background(tc.info))
        }
        Mute(hint)
    }
}

// ---------- G1 설정 · G2 모두 지우기 ----------

@Composable
fun SettingsScreen(s: Settings, total: Int, go: (Tab) -> Unit, update: ((Settings) -> Settings) -> Unit,
                   onPreview: () -> Unit, onVoiceTest: () -> Unit, onClearAll: () -> Unit, onSeed: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val portrait = LocalLayout.current == Layout.Portrait
    TabScaffold(Tab.SETTINGS, go) {
        Box(Modifier.fillMaxSize().testTag("screen_settings")) {
            Column(Modifier.fillMaxSize()) {
                AppHead("설정")
                val left: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow("주행 중 질문", "차선을 바꾸면 한 번 물어요", s.askEnabled, "set_ask") { v -> update { it.copy(askEnabled = v) } }
                        SetRow("하루 최대 질문", "넘으면 그날은 묻지 않아요") {
                            Stepper(s.dailyMax, "번", "set_max", onMinus = { update { it.copy(dailyMax = (it.dailyMax - 1).coerceAtLeast(3)) } }, onPlus = { update { it.copy(dailyMax = (it.dailyMax + 1).coerceAtMost(20)) } })
                        }
                        SwitchRow("내가 한 양보 모으기", "직진 중 속도를 줄인 순간을 모아 정차 후 물어요", s.collectGive, "set_give") { v -> update { it.copy(collectGive = v) } }
                        SwitchRow("아무 말이나 \"예\"로 듣기", "\"아니\"만 아니오로 들어요", s.anyYes, "set_anyyes") { v -> update { it.copy(anyYes = v) } }
                        SetRow("질문 목소리", "알림음 크기예요") {
                            Segmented(listOf("작게", "보통", "크게"), s.voiceLevel, "set_voice", Modifier.width(520.dp), md = true) { v -> update { it.copy(voiceLevel = v) } }
                        }
                    }
                }
                val right: @Composable (Modifier) -> Unit = { mod ->
                    Column(mod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TmButton("질문 들어 보기", "set_preview", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.fillMaxWidth(), onClick = onPreview)
                        TmButton("목소리 시험", "set_voicetest", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.fillMaxWidth(), onClick = onVoiceTest)
                        TmButton("시연용 예시 기록 넣기", "set_seed", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.fillMaxWidth(), onClick = onSeed)
                        TmButton("기록 모두 지우기", "set_clear", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.fillMaxWidth()) { confirm = true }
                        Mute("소리는 차 안에서 바로 글자로 바꾸고 저장하지 않아요.", Modifier.padding(top = 8.dp))
                    }
                }
                if (portrait) Column(Modifier.weight(1f).padding(horizontal = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) { left(Modifier.fillMaxWidth()); right(Modifier.fillMaxWidth().padding(bottom = 40.dp)) }
                else Row(Modifier.weight(1f).padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) { left(Modifier.width(1040.dp).verticalScroll(rememberScrollState())); right(Modifier.weight(1f)) }
            }
            if (confirm) Popup("기록을 모두 지울까요?", "이 차에 저장된 고마움 기록 ${total}개가 사라지고 되돌릴 수 없어요.", "취소", "모두 지우기",
                onCancel = { confirm = false }, onConfirm = { confirm = false; onClearAll() })
        }
    }
}
