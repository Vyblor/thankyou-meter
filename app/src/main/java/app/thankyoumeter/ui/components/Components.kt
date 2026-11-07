package app.thankyoumeter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.thankyoumeter.R
import app.thankyoumeter.data.Kind
import app.thankyoumeter.data.Moment
import app.thankyoumeter.data.Store
import app.thankyoumeter.ui.theme.Layout
import app.thankyoumeter.ui.theme.LocalLayout
import app.thankyoumeter.ui.theme.Tm
import app.thankyoumeter.ui.theme.TmType
import app.thankyoumeter.ui.theme.tc

/** 누름 피드백: 0.12초 scale 0.97 (emil-design-eng). */
@Composable
fun Modifier.press(tag: String?, enabled: Boolean = true, desc: String? = null, onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")
    var m = this.scale(s)
    if (tag != null) m = m.testTag(tag)
    if (desc != null) m = m.semantics { contentDescription = desc }
    return m.clickable(interactionSource = src, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
}

enum class BtnKind { PRIMARY, SECONDARY, FILLED, DANGER, TEXT }
enum class BtnSize(val h: Dp, val pad: Dp, val r: Dp) { SM(64.dp, 24.dp, 16.dp), MD(80.dp, 32.dp, 24.dp), LG(100.dp, 40.dp, 32.dp) }

/** TmButton — Pleos Basic Button 크기(64·80·100). primary(허니)는 화면당 하나. */
@Composable
fun TmButton(
    text: String, tag: String? = null, kind: BtnKind = BtnKind.SECONDARY, size: BtnSize = BtnSize.MD,
    icon: ImageVector? = null, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val col = tc
    val (bg, fg) = when (kind) {
        BtnKind.PRIMARY -> col.accent to col.onAccent
        BtnKind.SECONDARY -> col.btnBasic to col.text
        BtnKind.FILLED -> col.btnFilled to col.btnFilledText
        BtnKind.DANGER -> col.dangerFill to Color.White
        BtnKind.TEXT -> Color.Transparent to col.info
    }
    val shape = RoundedCornerShape(size.r)
    var m = modifier.heightIn(min = size.h).clip(shape).background(if (enabled) bg else col.btnBasic)
    if (kind == BtnKind.PRIMARY && !col.dark) m = m.border(2.dp, col.accentEdge, shape)
    if (kind == BtnKind.SECONDARY && !col.dark) m = m.border(1.dp, col.line, shape)
    Row(
        m.press(tag, enabled, onClick = onClick).padding(horizontal = size.pad),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        val f = if (enabled) fg else col.textOff
        if (icon != null) Icon(icon, null, tint = f, modifier = Modifier.size(if (size == BtnSize.LG) 40.dp else 32.dp))
        Text(text, style = if (size == BtnSize.LG) TmType.title.copy(fontSize = TmType.titleSm.fontSize) else TmType.body, color = f, maxLines = 1)
    }
}

/** TmIconButton — Pleos Icon Button 80/64. 접근성 이름 필수. */
@Composable
fun TmIconButton(icon: ImageVector, desc: String, tag: String? = null, small: Boolean = false, plain: Boolean = false, tint: Color? = null, onClick: () -> Unit) {
    val s = if (small) 64.dp else 80.dp
    Box(
        Modifier.size(s).clip(CircleShape).background(if (plain) Color.Transparent else tc.btnBasic).press(tag, desc = desc, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint ?: tc.text2, modifier = Modifier.size(if (small) 36.dp else 44.dp)) }
}

/** TmGearChip — "P 정차 중". D에서는 앱 화면 자체가 없다. */
@Composable
fun GearChip(label: String = "정차 중") {
    Row(
        Modifier.testTag("gear_chip").clip(RoundedCornerShape(999.dp)).background(tc.surface2).padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(tc.text), contentAlignment = Alignment.Center) { Text("P", style = TmType.body, color = tc.bg) }
        Text(label, style = TmType.label, color = tc.text2)
    }
}

/** 화면 머리: 뒤로(선택) · 제목 · 기어 칩 · 오른쪽 */
@Composable
fun AppHead(title: String, onBack: (() -> Unit)? = null, right: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().height(Tm.headH).padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (onBack != null) TmIconButton(Icons.Rounded.ArrowBackAlias, "뒤로", tag = "back", small = true, onClick = onBack)
        Text(title, style = TmType.headline, color = tc.text, modifier = Modifier.weight(1f).testTag("head_title"), maxLines = 1, overflow = TextOverflow.Ellipsis)
        right()
        GearChip()
    }
}

enum class Tab(val label: String, val icon: ImageVector, val tag: String) {
    HOME("홈", Icons.Rounded.Home, "tab_home"),
    RECORDS("기록", Icons.Rounded.FormatListBulleted, "tab_records"),
    REGION("동네", Icons.Rounded.Place, "tab_region"),
    MANNER("매너", Icons.Rounded.Speed, "tab_manner"),
    SETTINGS("설정", Icons.Rounded.Settings, "tab_settings"),
}

/** TmRail — landscape 왼쪽(운전석 쪽) 탭 레일. 항목 168×128. */
@Composable
fun Rail(on: Tab, go: (Tab) -> Unit) {
    Column(Modifier.width(Tm.railW).fillMaxHeight().background(tc.surface).padding(vertical = 16.dp, horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (t in listOf(Tab.HOME, Tab.RECORDS, Tab.REGION, Tab.MANNER)) RailItem(t, t == on, go)
        Spacer(Modifier.weight(1f))
        RailItem(Tab.SETTINGS, on == Tab.SETTINGS, go)
    }
}

@Composable
private fun RailItem(t: Tab, on: Boolean, go: (Tab) -> Unit) {
    Column(
        Modifier.fillMaxWidth().height(128.dp).clip(RoundedCornerShape(Tm.rMd)).background(if (on) tc.accentSoft else Color.Transparent).press(t.tag) { go(t) },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(t.icon, null, tint = if (on) tc.accentText else tc.text3, modifier = Modifier.size(44.dp))
        Text(t.label, style = TmType.label, color = if (on) tc.text else tc.text3)
    }
}

/** TmTabBar — portrait 840 아래 탭(설정은 머리 오른쪽). */
@Composable
fun TabBar(on: Tab, go: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(112.dp).background(tc.surface)) {
        for (t in listOf(Tab.HOME, Tab.RECORDS, Tab.REGION, Tab.MANNER)) {
            Column(Modifier.weight(1f).fillMaxHeight().press(t.tag) { go(t) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(t.icon, null, tint = if (t == on) tc.accentText else tc.text3, modifier = Modifier.size(40.dp))
                Text(t.label, style = TmType.label, color = if (t == on) tc.text else tc.text3)
            }
        }
    }
}

/** 탭 화면 틀: landscape = 레일 + 내용, portrait = 내용 + 아래 탭. */
@Composable
fun TabScaffold(on: Tab, go: (Tab) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (LocalLayout.current == Layout.Landscape) {
        Row(Modifier.fillMaxSize().background(tc.bg)) {
            Rail(on, go)
            Column(Modifier.weight(1f).fillMaxHeight(), content = content)
        }
    } else {
        Column(Modifier.fillMaxSize().background(tc.bg)) {
            Column(Modifier.weight(1f).fillMaxWidth(), content = content)
            TabBar(on, go)
        }
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 32.dp, tag: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(Tm.rXl)
    var m = modifier.clip(shape).background(tc.surface)
    if (!tc.dark) m = m.border(1.dp, tc.line, shape)
    if (tag != null) m = m.testTag(tag)
    Column(m.padding(pad), content = content)
}

val HeartIcon = Icons.Rounded.Favorite
val HandIcon = Icons.Rounded.PanTool

/** TmCountHero — 숫자 두 개: 받은 양보(로즈 하트) / 내가 한 양보(허니 손). */
@Composable
fun CountHero(recv: Int, give: Int, sub: String, modifier: Modifier = Modifier, numStyle: TextStyle = TmType.numXl, haechi: (@Composable BoxScope.() -> Unit)? = null) {
    Card(modifier.testTag("count_hero")) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                CountCell("받은 양보", HeartIcon, tc.heart, recv, "count_recv", numStyle)
                CountCell("내가 한 양보", HandIcon, tc.accentText, give, "count_give", numStyle)
            }
            if (haechi != null) haechi()
        }
        Text(sub, style = TmType.body, color = tc.text3, modifier = Modifier.testTag("count_sub"))
    }
}

@Composable
private fun CountCell(label: String, icon: ImageVector, tint: Color, n: Int, tag: String, numStyle: TextStyle) {
    Column(Modifier.widthIn(min = 200.dp).testTag(tag).semantics { contentDescription = "$label $n" }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(36.dp))
            Text(label, style = TmType.body, color = tc.text2)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$n", style = numStyle, color = tc.text)
            Text("번", style = TmType.title, color = tc.text2, modifier = Modifier.padding(start = 12.dp, bottom = 16.dp))
        }
    }
}

/** TmStat — 작은 숫자 칸. */
@Composable
fun Stat(k: String, v: String, unit: String, modifier: Modifier = Modifier, tag: String? = null) {
    val shape = RoundedCornerShape(Tm.rLg)
    var m = modifier.clip(shape).background(tc.surface)
    if (!tc.dark) m = m.border(1.dp, tc.line, shape)
    if (tag != null) m = m.testTag(tag).semantics { contentDescription = "$k $v$unit" }
    Column(m.padding(horizontal = 28.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(k, style = TmType.label, color = tc.text3, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(v, style = TmType.headline.copy(fontSize = TmType.headline.fontSize * 1.16f), color = tc.text)
            Text(unit, style = TmType.body, color = tc.text3, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        }
    }
}

/** TmMomentRow — 기록 한 줄. 점 세 가지: 받은(로즈 하트) / 한(허니 손) / 아니(회색). */
@Composable
fun MomentRow(m: Moment, onDelete: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(Tm.rLg)
    var mod = Modifier.fillMaxWidth().heightIn(min = 100.dp).clip(shape).background(tc.surface)
    if (!tc.dark) mod = mod.border(1.dp, tc.line, shape)
    val title = when (m.kind) { Kind.RECEIVED -> "양보받았어요"; Kind.GIVEN -> "양보했어요"; Kind.NO -> "아니라고 했어요" }
    Row(mod.testTag("moment_${m.id}").semantics { contentDescription = "${m.kind} $title" }.padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        KindDot(m.kind)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = TmType.body, color = tc.text, maxLines = 1)
            Text("${m.region} · ${m.detail}${if (m.kind == Kind.NO) " · 숫자에 안 들어가요" else ""}", style = TmType.label, color = tc.text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(Store.hm(m.atMs), style = TmType.titleSm, color = tc.text2)
        if (onDelete != null) TmIconButton(Icons.Rounded.DeleteAlias, "기록 지우기", tag = "delete_${m.id}", small = true, plain = true, onClick = onDelete)
    }
}

@Composable
fun KindDot(k: Kind, size: Dp = 56.dp) {
    val (bg, fg, icon) = when (k) {
        Kind.RECEIVED -> Triple(tc.heartSoft, tc.heart, HeartIcon)
        Kind.GIVEN -> Triple(tc.accent, tc.onAccent, HandIcon)
        Kind.NO -> Triple(tc.surface2, tc.text3, Icons.Rounded.Remove)
    }
    var m = Modifier.size(size).clip(CircleShape).background(bg)
    if (k == Kind.GIVEN && !tc.dark) m = m.border(2.dp, tc.accentEdge, CircleShape)
    Box(m, contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.57f)) }
}

enum class BannerKind { SOFT, INFO, NEG }

/** TmBanner — 정보·안내 띠. 경고에 허니를 쓰지 않는다. */
@Composable
fun Banner(icon: ImageVector, title: String, sub: String? = null, kind: BannerKind = BannerKind.SOFT, tag: String? = null, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    val (bg, fg) = when (kind) { BannerKind.SOFT -> tc.surface2 to tc.text2; BannerKind.INFO -> tc.infoSoft to tc.info; BannerKind.NEG -> tc.negSoft to tc.neg }
    Row(modifier.fillMaxWidth().then(if (tag != null) Modifier.testTag(tag) else Modifier).clip(RoundedCornerShape(Tm.rLg)).background(bg).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = TmType.body, color = tc.text)
            if (sub != null) Text(sub, style = TmType.label, color = tc.text3)
        }
        action?.invoke()
    }
}

/** TmSwitchRow — Pleos 스위치 80×48, 켜짐 #02C265. */
@Composable
fun SwitchRow(title: String, sub: String?, on: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    SetRow(title, sub, Modifier.press(tag, desc = "$title ${if (on) "켜짐" else "꺼짐"}") { onChange(!on) }) {
        val track = if (on) tc.switchOn else tc.surface3
        Box(Modifier.width(80.dp).height(48.dp).clip(RoundedCornerShape(999.dp)).background(track).padding(4.dp), contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White))
        }
    }
}

@Composable
fun SetRow(title: String, sub: String?, modifier: Modifier = Modifier, right: @Composable () -> Unit) {
    val shape = RoundedCornerShape(Tm.rLg)
    var m = Modifier.fillMaxWidth().heightIn(min = 112.dp).clip(shape).background(tc.surface)
    if (!tc.dark) m = m.border(1.dp, tc.line, shape)
    Row(m.then(modifier).padding(horizontal = 28.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TmType.body, color = tc.text)
            if (sub != null) Text(sub, style = TmType.label, color = tc.text3)
        }
        right()
    }
}

/** TmSegmented — Pleos 세그먼트 높이 100/80. */
@Composable
fun Segmented(options: List<String>, sel: Int, tagPrefix: String, modifier: Modifier = Modifier, md: Boolean = false, onSel: (Int) -> Unit) {
    Row(modifier.height(if (md) 80.dp else 100.dp).clip(RoundedCornerShape(Tm.rLg)).background(tc.surface2).padding(6.dp)) {
        options.forEachIndexed { i, o ->
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(Tm.rMd)).background(if (i == sel) tc.surface else Color.Transparent).press("${tagPrefix}_$i", desc = "$o${if (i == sel) " 선택됨" else ""}") { onSel(i) }, contentAlignment = Alignment.Center) {
                Text(o, style = TmType.body, color = if (i == sel) tc.text else tc.text3, maxLines = 1)
            }
        }
    }
}

/** TmToast — 아래 왼쪽, 되돌리기 버튼. */
@Composable
fun Toast(text: String, action: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.testTag("toast").clip(RoundedCornerShape(999.dp)).background(tc.btnFilled).padding(start = 32.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(text, style = TmType.body, color = tc.btnFilledText)
        if (action != null) Row(Modifier.height(64.dp).clip(RoundedCornerShape(999.dp)).background(tc.btnFilledText.copy(alpha = 0.14f)).press("toast_action", onClick = onAction).padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically) { Text(action, style = TmType.body, color = tc.btnFilledText) }
    }
}

/** TmPopup — Pleos 팝업 808×366. 되돌릴 수 없는 일에만. 위험 버튼은 오른쪽. */
@Composable
fun Popup(title: String, body: String, cancel: String, confirm: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(Modifier.fillMaxSize().background(tc.dim).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCancel() }, contentAlignment = Alignment.Center) {
        Column(Modifier.width(808.dp).heightIn(min = 366.dp).testTag("popup").clip(RoundedCornerShape(Tm.rXl)).background(tc.surface).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}.padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = TmType.title, color = tc.text)
            Text(body, style = TmType.body, color = tc.text3, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TmButton(cancel, "popup_cancel", modifier = Modifier.weight(1f), size = BtnSize.LG, onClick = onCancel)
                TmButton(confirm, "popup_confirm", kind = BtnKind.DANGER, modifier = Modifier.weight(1f), size = BtnSize.LG, onClick = onConfirm)
            }
        }
    }
}

/** TmStepper — Pleos 420×100. */
@Composable
fun Stepper(v: Int, unit: String, tagPrefix: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.width(420.dp).height(100.dp).clip(RoundedCornerShape(Tm.rLg)).background(tc.surface2).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        TmIconButton(Icons.Rounded.Remove, "줄이기", "${tagPrefix}_minus", onClick = onMinus)
        Row(Modifier.weight(1f).testTag("${tagPrefix}_value").semantics { contentDescription = "$v$unit" }, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Text("$v", style = TmType.title, color = tc.text)
            Text(unit, style = TmType.body, color = tc.text3, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        }
        TmIconButton(Icons.Rounded.AddAlias, "늘리기", "${tagPrefix}_plus", onClick = onPlus)
    }
}

/** TmHaechi — 상태 7종 PNG(시안 A). */
enum class HaechiState(val res: Int) {
    IDLE(R.drawable.haechi_idle), HELLO(R.drawable.haechi_hello), LISTEN(R.drawable.haechi_listen), BOW(R.drawable.haechi_bow),
    COUNT(R.drawable.haechi_count), CHEER(R.drawable.haechi_cheer), SLEEP(R.drawable.haechi_sleep)
}

@Composable
fun Haechi(state: HaechiState, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(state.res), "해치 ${state.name.lowercase()}", modifier.size(size).testTag("haechi_${state.name.lowercase()}"))
}

/** 보조 글자 */
@Composable
fun Mute(text: String, modifier: Modifier = Modifier) { Text(text, style = TmType.label, color = tc.text3, modifier = modifier) }

@Composable
fun SecTitle(text: String, modifier: Modifier = Modifier) { Text(text, style = TmType.titleSm, color = tc.text, modifier = modifier) }

/** 알약 */
@Composable
fun Pill(text: String, icon: ImageVector? = null, accent: Boolean = false, tag: String? = null) {
    Row(Modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier).clip(RoundedCornerShape(999.dp)).background(if (accent) tc.accentSoft else tc.surface2).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (icon != null) Icon(icon, null, tint = if (accent) tc.accentText else tc.text3, modifier = Modifier.size(28.dp))
        Text(text, style = TmType.label, color = tc.text2)
    }
}

/** TmVoiceLine — 말풍선(차 = 허니 면, 나 = filled 면). P단 화면 설명용, 주행 중에는 그리지 않는다. */
@Composable
fun VoiceLine(text: String, car: Boolean) {
    val bg = if (car) tc.accent else tc.btnFilled
    val fg = if (car) tc.onAccent else tc.btnFilledText
    Row(Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 28.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (car) Icon(Icons.Rounded.VolumeUpAlias, null, tint = fg, modifier = Modifier.size(32.dp))
        Text(text, style = TmType.body, color = fg)
    }
}

/** 둥근 아이콘 원 */
@Composable
fun IconCircle(icon: ImageVector, size: Dp = 72.dp, bg: Color = tc.surface2, fg: Color = tc.text2) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.5f)) }
}

