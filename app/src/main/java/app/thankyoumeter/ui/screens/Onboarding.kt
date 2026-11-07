package app.thankyoumeter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.thankyoumeter.Log.log
import app.thankyoumeter.ui.components.AppHead
import app.thankyoumeter.ui.components.Banner
import app.thankyoumeter.ui.components.BtnKind
import app.thankyoumeter.ui.components.BtnSize
import app.thankyoumeter.ui.components.HandIcon
import app.thankyoumeter.ui.components.Haechi
import app.thankyoumeter.ui.components.HaechiState
import app.thankyoumeter.ui.components.HeartIcon
import app.thankyoumeter.ui.components.IconCircle
import app.thankyoumeter.ui.components.Pill
import app.thankyoumeter.ui.components.SetRow
import app.thankyoumeter.ui.components.TmButton
import app.thankyoumeter.ui.components.VoiceLine
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.Tm
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.tc
import app.thankyoumeter.voice.Answer

/** 첫 실행 A1~A4. A5(준비 끝)는 홈 0번 화면이다. */
@Composable
fun OnboardingScreen(
    step: Int, setStep: (Int) -> Unit,
    onPreviewAsk: () -> Unit,
    onRequestPermission: () -> Unit,
    listenOnce: (done: (Answer) -> Unit) -> Unit,
    stopListen: () -> Unit,
    onEnableAnyYes: () -> Unit,
    onDone: () -> Unit,
) {
    val land = LocalLayout.current == Layout.Landscape
    Column(Modifier.fillMaxSize().background(tc.bg).testTag("screen_onboard_$step")) {
        when (step) {
            1 -> {
                AppHead("")
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    Column(Modifier.padding(start = 96.dp, top = 40.dp, end = if (land) 760.dp else 48.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("땡큐미터", style = TmType.label.copy(fontWeight = TmType.body.fontWeight), color = tc.accentText)
                        Text("도로 위 고마움을\n세어 드려요", style = TmType.display, color = tc.text)
                        Text("양보받으면 \"고마워\" 한 마디로, 내가 양보한 순간은\n정차한 뒤 한 번에 확인해요.", style = TmType.body, color = tc.text3)
                        Spacer(Modifier.height(24.dp))
                        Promise(Icons.Rounded.VisibilityOff, "주행 중엔 화면 없이 목소리로만")
                        Promise(HandIcon, "대답은 하지 않아도 돼요")
                        Promise(Icons.Rounded.Place, "위치는 동네(시군구)까지만 써요")
                    }
                    if (land) Haechi(HaechiState.HELLO, 560.dp, Modifier.align(Alignment.CenterEnd).padding(end = 120.dp))
                }
                Row(Modifier.padding(start = 96.dp, bottom = 48.dp)) {
                    TmButton("시작하기", "onboard_start", BtnKind.PRIMARY, BtnSize.LG, modifier = Modifier.width(420.dp)) { setStep(2) }
                }
            }
            2 -> {
                AppHead("이렇게 동작해요", onBack = { setStep(1) })
                Column(Modifier.weight(1f).padding(horizontal = 96.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
                    val steps = listOf(
                        Triple(Icons.Rounded.AltRoute, "차선을 바꾸면", "깜빡이와 핸들로 알아채요.\n교차로 회전은 묻지 않아요."),
                        Triple(Icons.Rounded.VolumeUp, "한 번 물어요", "바꾸고 몇 초 뒤, 직진할 때\n\"양보받았어요?\""),
                        Triple(HeartIcon, "\"고마워\" 하면 끝", "\"기록했어요\" 한 마디.\n대답 안 해도 괜찮아요."),
                    )
                    val cards: @Composable (Int, Triple<ImageVector, String, String>, Modifier) -> Unit = { i, s, m -> StepCard(i + 1, s.first, s.second, s.third, m, heart = i == 2) }
                    if (land) Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) { steps.forEachIndexed { i, s -> cards(i, s, Modifier.weight(1f)) } }
                    else Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { steps.forEachIndexed { i, s -> cards(i, s, Modifier.fillMaxWidth()) } }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                        VoiceLine("양보받았어요?", car = true); VoiceLine("고마워", car = false); VoiceLine("기록했어요", car = true)
                    }
                }
                Row(Modifier.padding(start = 96.dp, bottom = 48.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TmButton("다음", "onboard_next2", BtnKind.PRIMARY, BtnSize.LG, modifier = Modifier.width(420.dp)) { setStep(3) }
                    TmButton("질문 들어 보기", "preview_ask", size = BtnSize.LG, icon = Icons.Rounded.PlayArrow, onClick = onPreviewAsk)
                }
            }
            3 -> {
                AppHead("필요한 권한 두 가지", onBack = { setStep(2) })
                Column(Modifier.weight(1f).padding(horizontal = 96.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SetRow("마이크", "\"고마워\"를 듣는 데만 써요. 소리는 차 안에서 바로 글자로 바꾸고 저장하지 않아요.") { IconCircle(Icons.Rounded.Mic, bg = tc.accentSoft) }
                    SetRow("차량 신호", "깜빡이·핸들 각도·속도로 차선 변경과 감속만 알아채요.") { IconCircle(Icons.Rounded.AltRoute, bg = tc.accentSoft) }
                }
                Row(Modifier.padding(start = 96.dp, bottom = 48.dp)) {
                    TmButton("허용하기", "onboard_permit", BtnKind.PRIMARY, BtnSize.LG, modifier = Modifier.width(420.dp), onClick = onRequestPermission)
                }
            }
            4 -> VoiceTest(setStep, listenOnce, stopListen, onEnableAnyYes, onDone)
        }
    }
}

@Composable
private fun Promise(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        IconCircle(icon)
        Text(text, style = TmType.body, color = tc.text)
    }
}

@Composable
private fun StepCard(n: Int, icon: ImageVector, title: String, body: String, modifier: Modifier, heart: Boolean) {
    val shape = RoundedCornerShape(Tm.rXl)
    var m = modifier.heightIn(min = 340.dp).clip(shape).background(tc.surface)
    if (!tc.dark) m = m.border(1.dp, tc.line, shape)
    Column(m.padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(tc.text), contentAlignment = Alignment.Center) { Text("$n", style = TmType.body, color = tc.bg) }
        Icon(icon, null, tint = if (heart) tc.heart else tc.text, modifier = Modifier.size(64.dp))
        Text(title, style = TmType.titleSm, color = tc.text)
        Text(body, style = TmType.bodySm, color = tc.text3)
    }
}

/** A4 목소리 시험: 세 번 중 두 번 들리면 통과. */
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.VoiceTest(setStep: (Int) -> Unit, listenOnce: ((Answer) -> Unit) -> Unit, stopListen: () -> Unit, onEnableAnyYes: () -> Unit, onDone: () -> Unit) {
    val results = remember { mutableStateListOf<Boolean?>(null, null, null) }
    var attempt by remember { mutableIntStateOf(0) }
    var anyYesOn by remember { mutableStateOf(false) }
    val ok = results.count { it == true }
    val failed = results.count { it == false }
    LaunchedEffect(attempt) {
        if (attempt < 3 && ok < 2) {
            listenOnce { a ->
                val heard = a == Answer.YES
                log("ONBOARD voice attempt=${attempt + 1} heard=$heard")
                results[attempt] = heard
                attempt += 1
            }
        }
    }
    val land = LocalLayout.current == Layout.Landscape
    AppHead("목소리 시험", onBack = { stopListen(); setStep(3) })
    Box(Modifier.fillMaxWidth().height(if (land) 900.dp else 860.dp)) {
        Column(Modifier.padding(start = 96.dp, top = 32.dp, end = if (land) 760.dp else 48.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Text(if (ok >= 2) "잘 들려요" else "\"고마워\"라고 말해 보세요", style = TmType.title, color = tc.text, modifier = Modifier.testTag("voice_title"))
            Text(if (ok >= 2) "주행 중에도 이렇게 대답하면 돼요." else "평소 운전할 때 목소리로 세 번이면 돼요.", style = TmType.body, color = tc.text3)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                results.forEachIndexed { i, r ->
                    val label = when { r == true -> "${i + 1}번째 들었어요"; r == false -> "${i + 1}번째 못 들었어요"; i == attempt && ok < 2 -> "${i + 1}번째 듣는 중"; else -> "${i + 1}번째" }
                    Box(Modifier.semantics { contentDescription = label }.testTag("voice_pill_$i")) {
                        Pill(label, icon = when (r) { true -> Icons.Rounded.Check; false -> Icons.Rounded.Close; else -> Icons.Rounded.Mic }, accent = r == true)
                    }
                }
            }
            if (failed >= 2 && ok < 2) {
                Banner(Icons.Rounded.Info, "잘 못 알아들었어요", if (anyYesOn) "\"아무 말이나 예로 듣기\"를 켰어요" else "\"아무 말이나 예로 듣기\"로 바꿔 드릴게요", tag = "voice_fallback") {
                    if (!anyYesOn) TmButton("켜기", "voice_anyyes", size = BtnSize.SM) { anyYesOn = true; onEnableAnyYes() }
                }
            } else if (attempt >= 3 && ok < 2) {
                Banner(Icons.Rounded.Info, "설정에서 다시 시험할 수 있어요", null)
            }
        }
        if (land) Haechi(if (ok >= 2) HaechiState.CHEER else HaechiState.LISTEN, 520.dp, Modifier.align(Alignment.CenterEnd).padding(end = 140.dp))
    }
    Spacer(Modifier.weight(1f))
    Row(Modifier.padding(start = 96.dp, bottom = 48.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        TmButton("다음", "voice_next", BtnKind.PRIMARY, BtnSize.LG, modifier = Modifier.width(420.dp)) { stopListen(); onDone() }
        if (ok < 2) TmButton("건너뛰기", "voice_skip", size = BtnSize.LG) { stopListen(); onDone() }
    }
}
