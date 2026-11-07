package app.thankyoumeter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.thankyoumeter.data.Candidate
import app.thankyoumeter.data.Kind
import app.thankyoumeter.data.Moment
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.Banner
import app.thankyoumeter.ui.components.BannerKind
import app.thankyoumeter.ui.components.BtnKind
import app.thankyoumeter.ui.components.BtnSize
import app.thankyoumeter.ui.components.CountHero
import app.thankyoumeter.ui.components.HandIcon
import app.thankyoumeter.ui.components.Haechi
import app.thankyoumeter.ui.components.HaechiState
import app.thankyoumeter.ui.components.MomentRow
import app.thankyoumeter.ui.components.SecTitle
import app.thankyoumeter.ui.components.Stat
import app.thankyoumeter.ui.components.SwitchRow
import app.thankyoumeter.ui.components.Tab
import app.thankyoumeter.ui.components.TabScaffold
import app.thankyoumeter.ui.components.TmButton
import app.thankyoumeter.ui.components.TmIconButton
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.TmType

data class HomeState(
    val today: List<Moment>,
    val month: List<Moment>,
    val pending: List<Candidate>,
    val signalRate: Int?,
    val askEnabled: Boolean,
    val dailyMax: Int,
    val paused: String?,
    val micOff: Boolean,
    val newSinceSeen: Boolean,
)

@Composable
fun HomeScreen(s: HomeState, go: (Tab) -> Unit, onConfirm: () -> Unit, onRecords: () -> Unit, onMonth: () -> Unit, onAskToggle: (Boolean) -> Unit, onResume: () -> Unit, onMicOn: () -> Unit) {
    val recv = s.today.count { it.kind == Kind.RECEIVED }
    val give = s.today.count { it.kind == Kind.GIVEN }
    val counted = s.today.filter { it.kind != Kind.NO }
    val sub = when {
        s.micOff -> "마이크를 켜면 다시 물어요"
        counted.isNotEmpty() -> lastLabel(counted.first())
        else -> "차선을 바꾸면 한 번 물어볼게요"
    }
    val hState = when { s.micOff || s.paused != null -> HaechiState.SLEEP; s.newSinceSeen -> HaechiState.COUNT; counted.isEmpty() -> HaechiState.IDLE; else -> HaechiState.COUNT }
    val land = LocalLayout.current == Layout.Landscape
    TabScaffold(Tab.HOME, go) {
        Box(Modifier.fillMaxSize().testTag("screen_home")) {
            Column(Modifier.fillMaxSize()) {
                AppHead(if (land) "오늘의 고마움" else "오늘", right = { if (!land) TmIconButton(Icons.Rounded.Settings, "설정", "head_settings", small = true, plain = true) { go(Tab.SETTINGS) } })
                val top: @Composable () -> Unit = {
                    if (s.micOff) Banner(Icons.Rounded.MicOff, "마이크가 꺼져 있어요", "대답을 들을 수 없어 질문을 쉬고 있어요", BannerKind.NEG, tag = "banner_mic") { TmButton("켜기", "mic_on", size = BtnSize.SM, onClick = onMicOn) }
                    else if (s.paused != null) Banner(Icons.Rounded.Bedtime, "오늘은 질문을 쉬어요", if (s.paused == "user") "\"오늘은 그만\"이라고 하셨어요 · 내일 다시 물어요" else "세 번 연달아 대답이 없었어요 · 내일 다시 물어요", tag = "banner_paused") { TmButton("다시 켜기", "resume_today", size = BtnSize.SM, onClick = onResume) }
                }
                if (land) {
                    Column(Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        top()
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            Column(Modifier.width(820.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                                CountHero(recv, give, sub, Modifier.fillMaxWidth().height(if (s.micOff || s.paused != null) 440.dp else 520.dp)) {
                                    Haechi(hState, 280.dp, Modifier.align(Alignment.BottomEnd))
                                }
                                StatsRow(s, 3)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) { RightColumn(s, onConfirm, onRecords, onAskToggle) }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(Modifier.padding(start = 32.dp, bottom = 40.dp)) {
                        TmButton("이번 달 카드 보기", "home_month", BtnKind.PRIMARY, BtnSize.LG, Icons.Rounded.CreditCard, modifier = Modifier.width(420.dp), onClick = onMonth)
                    }
                } else {
                    Column(Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        top()
                        CountHero(recv, give, sub, Modifier.fillMaxWidth().height(440.dp)) { Haechi(hState, 210.dp, Modifier.align(Alignment.BottomEnd)) }
                        StatsRow(s, 2)
                        if (s.pending.isNotEmpty()) PendingBanner(s.pending.size, onConfirm, short = true)
                        s.today.firstOrNull()?.let { MomentRow(it) }
                        Spacer(Modifier.weight(1f))
                        TmButton("이번 달 카드 보기", "home_month", BtnKind.PRIMARY, BtnSize.MD, Icons.Rounded.CreditCard, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), onClick = onMonth)
                    }
                }
            }
        }
    }
}

private fun lastLabel(m: Moment): String {
    val h = java.util.Calendar.getInstance().apply { timeInMillis = m.atMs }.get(java.util.Calendar.HOUR_OF_DAY)
    val ampm = if (h < 12) "오전" else "오후"
    val hh = if (h % 12 == 0) 12 else h % 12
    val mm = java.util.Calendar.getInstance().apply { timeInMillis = m.atMs }.get(java.util.Calendar.MINUTE)
    return "마지막 기록 $ampm $hh:%02d · ${m.region}".format(mm)
}

@Composable
private fun StatsRow(s: HomeState, n: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Stat("이번 달 양보받음", "${s.month.count { it.kind == Kind.RECEIVED }}", "번", Modifier.weight(1f), "stat_month_recv")
        Stat("이번 달 양보함", "${s.month.count { it.kind == Kind.GIVEN }}", "번", Modifier.weight(1f), "stat_month_give")
        if (n >= 3) Stat("깜빡이 사용", s.signalRate?.toString() ?: "–", if (s.signalRate != null) "%" else "", Modifier.weight(1f), "stat_signal")
    }
}

@Composable
private fun PendingBanner(n: Int, onConfirm: () -> Unit, short: Boolean = false) {
    Banner(HandIcon, "확인할 양보 ${n}개", if (short) null else "직진 중 속도를 줄인 순간이에요", tag = "banner_pending") {
        TmButton(if (short) "확인" else "확인하기", "pending_confirm", BtnKind.PRIMARY, BtnSize.SM, onClick = onConfirm)
    }
}

@Composable
private fun RightColumn(s: HomeState, onConfirm: () -> Unit, onRecords: () -> Unit, onAskToggle: (Boolean) -> Unit) {
    if (s.pending.isNotEmpty()) { PendingBanner(s.pending.size, onConfirm); Spacer(Modifier.height(12.dp)) }
    if (s.today.isEmpty()) {
        SecTitle("준비 끝", Modifier.testTag("home_ready"))
        SwitchRow("주행 중 질문", if (s.askEnabled) "켜짐 · 하루 최대 ${s.dailyMax}번" else "꺼짐", s.askEnabled, "home_ask_switch", onAskToggle)
        Banner(Icons.Rounded.Shield, "출발하면 화면은 꺼져요", "질문과 대답은 소리로만 해요")
    } else {
        SecTitle("오늘 기록", Modifier.padding(bottom = 4.dp))
        s.today.take(3).forEach { MomentRow(it) }
        TmButton("기록 모두 보기", "home_records", icon = null, onClick = onRecords)
    }
}
