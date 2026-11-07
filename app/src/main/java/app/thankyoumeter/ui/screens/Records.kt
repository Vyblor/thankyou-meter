package app.thankyoumeter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thankyoumeter.Log.log
import app.thankyoumeter.data.Kind
import app.thankyoumeter.data.Moment
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.Haechi
import app.thankyoumeter.ui.components.HaechiState
import app.thankyoumeter.ui.components.MomentRow
import app.thankyoumeter.ui.components.Mute
import app.thankyoumeter.ui.components.Segmented
import app.thankyoumeter.ui.components.Tab
import app.thankyoumeter.ui.components.TabScaffold
import app.thankyoumeter.ui.components.Toast
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.tc
import kotlinx.coroutines.delay
import java.util.Calendar

/** 기간 필터의 시작 시각. 0 오늘, 1 이번 주(월요일부터), 2 이번 달. */
fun periodStart(p: Int, now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    when (p) {
        1 -> { val dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; c.add(Calendar.DAY_OF_MONTH, -dow) }
        2 -> c.set(Calendar.DAY_OF_MONTH, 1)
    }
    return c.timeInMillis
}

fun filterMoments(all: List<Moment>, period: Int, kind: Int, now: Long = System.currentTimeMillis()): List<Moment> {
    val from = periodStart(period, now)
    return all.filter { it.atMs >= from && it.atMs <= now + 60_000 }.filter {
        when (kind) { 1 -> it.kind == Kind.RECEIVED; 2 -> it.kind == Kind.GIVEN; else -> true }
    }.sortedByDescending { it.atMs }
}

/** C3 기록 · 지우기 · 8초 되돌리기. 확인 창 없음. */
@Composable
fun RecordsScreen(moments: List<Moment>, go: (Tab) -> Unit, onDelete: (Long) -> Moment?, onRestore: (Moment) -> Unit) {
    var period by remember { mutableIntStateOf(0) }
    var kind by remember { mutableIntStateOf(0) }
    var removed by remember { mutableStateOf<Moment?>(null) }
    var toastKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(toastKey) { if (removed != null) { delay(8000); log("RECORDS toast expired"); removed = null } }
    val list = filterMoments(moments, period, kind)
    val portrait = LocalLayout.current == Layout.Portrait
    log("RECORDS period=$period kind=$kind shown=${list.size}")
    TabScaffold(Tab.RECORDS, go) {
        Box(Modifier.fillMaxSize().testTag("screen_records")) {
            Column(Modifier.fillMaxSize()) {
                AppHead("기록")
                val segs: @Composable (Modifier, Modifier) -> Unit = { a, b ->
                    Segmented(listOf("오늘", "이번 주", "이번 달"), period, "period", a) { period = it }
                    Segmented(listOf("모두", "양보받음", "양보함"), kind, "kind", b, md = true) { kind = it }
                }
                if (portrait) Column(Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { segs(Modifier.fillMaxWidth(), Modifier.fillMaxWidth()) }
                else Row(Modifier.padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) { segs(Modifier.width(640.dp), Modifier.width(560.dp)) }
                Text("${list.size}개", style = TmType.label, color = tc.text3,
                    modifier = Modifier.padding(start = 32.dp, top = 16.dp, bottom = 8.dp).testTag("records_count").semantics { contentDescription = "기록 ${list.size}개" })
                if (list.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp).testTag("records_empty"), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Haechi(HaechiState.SLEEP, 220.dp)
                        Text("아직 기록이 없어요", style = TmType.title, color = tc.text)
                        Mute("차선을 바꾸고 \"고마워\"라고 하면 여기에 남아요")
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).testTag("records_list"), contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 140.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { m ->
                            MomentRow(m) { onDelete(m.id)?.let { removed = it; toastKey++; log("RECORDS deleted id=${m.id}") } }
                        }
                    }
                }
            }
            val r = removed
            if (r != null) Toast("기록 1개를 지웠어요", "되돌리기", { onRestore(r); log("RECORDS restored id=${r.id}"); removed = null },
                Modifier.align(Alignment.BottomStart).padding(start = 32.dp, bottom = 40.dp))
            else if (!portrait) Mute("기록은 이 차 안에만 저장돼요", Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 56.dp))
        }
    }
}

