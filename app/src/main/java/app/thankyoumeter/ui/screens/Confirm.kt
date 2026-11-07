package app.thankyoumeter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thankyoumeter.data.Candidate
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.BtnKind
import app.thankyoumeter.ui.components.BtnSize
import app.thankyoumeter.ui.components.Card
import app.thankyoumeter.ui.components.Haechi
import app.thankyoumeter.ui.components.HaechiState
import app.thankyoumeter.ui.components.HandIcon
import app.thankyoumeter.ui.components.Mute
import app.thankyoumeter.ui.components.Pill
import app.thankyoumeter.ui.components.Tab
import app.thankyoumeter.ui.components.TabScaffold
import app.thankyoumeter.ui.components.TmButton
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.Tm
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.tc

/** C4 양보한 순간 확인(P단). 오늘 생긴 감속 후보만. 고른 줄은 흐려지고 결과 알약만 남는다. 확인 창 없음. */
@Composable
fun ConfirmScreen(candidates: List<Candidate>, go: (Tab) -> Unit, onBack: () -> Unit, onResolve: (Long, Boolean) -> Unit) {
    val today = Store.ymd(System.currentTimeMillis())
    val list = candidates.filter { Store.ymd(it.atMs) == today }.sortedByDescending { it.atMs }
    val open = list.count { it.resolved == null }
    val portrait = LocalLayout.current == Layout.Portrait
    TabScaffold(Tab.HOME, go) {
        Column(Modifier.fillMaxSize().testTag("screen_confirm")) {
            AppHead("양보한 순간 확인", onBack = onBack)
            Row(Modifier.weight(1f).padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("오늘 속도를 줄인 순간 ${list.size}개", style = TmType.titleSm, color = tc.text, modifier = Modifier.testTag("confirm_title"))
                    Text("맞는 것만 고르면 \"내가 한 양보\"로 기록돼요.", style = TmType.bodySm, color = tc.text3, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                    if (list.isEmpty()) Mute("확인할 순간이 없어요", Modifier.testTag("confirm_empty"))
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { c -> YieldRow(c, portrait, onResolve) }
                    }
                    TmButton(if (open == 0) "홈으로" else "나중에", "confirm_later", BtnKind.SECONDARY, BtnSize.LG, modifier = Modifier.padding(bottom = 40.dp).width(320.dp), onClick = onBack)
                }
                if (!portrait) Column(Modifier.width(560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Card {
                        Text("왜 묻나요", style = TmType.label, color = tc.accentText)
                        Text("차는 다른 차를 볼 수 없어요. 직진하다 속도를 줄였다가 다시 낸 순간만 모아요.", style = TmType.bodySm, color = tc.text)
                        Mute("고르지 않은 순간은 자정에 지워져요.")
                    }
                    Haechi(HaechiState.HELLO, 300.dp, Modifier.padding(top = 32.dp))
                }
            }
        }
    }
}

@Composable
private fun YieldRow(c: Candidate, portrait: Boolean, onResolve: (Long, Boolean) -> Unit) {
    val shape = RoundedCornerShape(Tm.rLg)
    var m = Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(shape).background(tc.surface)
    if (!tc.dark) m = m.border(1.dp, tc.line, shape)
    val done = c.resolved != null
    val what = "${c.fromKmh} → ${c.toKmh} km/h로 부드럽게 줄였어요"
    val body: @Composable () -> Unit = {
        Text(Store.hm(c.atMs), style = TmType.titleSm, color = tc.text2)
        Column {
            Text("${c.fromKmh} → ${c.toKmh} km/h", style = TmType.body, color = tc.text, maxLines = 1)
            Text("부드럽게 줄였어요 · ${c.region}", style = TmType.label, color = tc.text3, maxLines = 1)
        }
    }
    val actions: @Composable () -> Unit = {
        if (done) Pill(if (c.resolved == true) "양보했어요로 기록함" else "아니에요", Icons.Rounded.Check, tag = "yc_done_${c.id}")
        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TmButton("양보했어요", "yc_yes_${c.id}", BtnKind.PRIMARY, BtnSize.MD, icon = HandIcon) { onResolve(c.id, true) }
            TmButton("아니에요", "yc_no_${c.id}", BtnKind.SECONDARY, BtnSize.MD) { onResolve(c.id, false) }
        }
    }
    val rowMod = m.testTag("yc_${c.id}").semantics { contentDescription = "$what ${if (done) "고름" else "고르지 않음"}" }.padding(horizontal = 28.dp, vertical = 16.dp)
    if (portrait) Column(rowMod, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.then(if (done) Modifier.alpha(0.55f) else Modifier), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) { body() }
        actions()
    } else Row(rowMod, horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).then(if (done) Modifier.alpha(0.55f) else Modifier)) { Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) { body() } }
        actions()
    }
}
