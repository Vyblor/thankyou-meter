package app.thankyoumeter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.thankyoumeter.R

/**
 * 땡큐미터 디자인 토큰 v0.2 (워크스페이스 ideas/thankyou-meter-design/tokens.css)를 Compose로 옮긴 것.
 * Pleos Connect는 160dpi라 1dp = 1px. 와이어프레임 숫자를 그대로 dp로 쓴다.
 * 테마는 시스템 낮·밤을 따른다. 허니는 상태 색으로 쓰지 않는다(호박색 경고등과 헷갈림 방지).
 */
data class TmColors(
    val dark: Boolean,
    val bg: Color, val surface: Color, val surface2: Color, val surface3: Color,
    val line: Color, val dim: Color,
    val text: Color, val text2: Color, val text3: Color, val textOff: Color,
    val btnFilled: Color, val btnFilledText: Color, val btnBasic: Color, val btnBasicPress: Color,
    val accent: Color, val accentPress: Color, val accentEdge: Color, val accentText: Color, val accentSoft: Color, val onAccent: Color,
    val heart: Color, val heartSoft: Color,
    val ok: Color, val info: Color, val neg: Color, val dangerFill: Color,
    val okSoft: Color, val infoSoft: Color, val negSoft: Color,
    val switchOn: Color,
)

val LightColors = TmColors(
    dark = false,
    bg = Color(0xFFF7F8FA), surface = Color.White, surface2 = Color(0xFFEDEEF2), surface3 = Color(0xFFE0E1E6),
    line = Color(0x1A131417), dim = Color(0x8C131417),
    text = Color(0xFF131417), text2 = Color(0xD6131417), text3 = Color(0xA3131417), textOff = Color(0x52131417),
    btnFilled = Color(0xFF44464E), btnFilledText = Color.White, btnBasic = Color(0x0D131417), btnBasicPress = Color(0x1A131417),
    accent = Color(0xFFFFC857), accentPress = Color(0xFFF0B43C), accentEdge = Color(0xFFB07400), accentText = Color(0xFF8A5300),
    accentSoft = Color(0x47FFC857), onAccent = Color(0xFF131417),
    heart = Color(0xFFE8365F), heartSoft = Color(0x24E8365F),
    ok = Color(0xFF0B7A41), info = Color(0xFF0064FF), neg = Color(0xFFC4320A), dangerFill = Color(0xFFC62828),
    okSoft = Color(0x1F02C265), infoSoft = Color(0x140064FF), negSoft = Color(0x14FE3D16),
    switchOn = Color(0xFF02C265),
)

val DarkColors = TmColors(
    dark = true,
    bg = Color(0xFF131417), surface = Color(0xFF1D1E21), surface2 = Color(0xFF313236), surface3 = Color(0xFF44464E),
    line = Color(0x1AFFFFFF), dim = Color(0xB8131417),
    text = Color.White, text2 = Color(0xD6FFFFFF), text3 = Color(0xA3FFFFFF), textOff = Color(0x52FFFFFF),
    btnFilled = Color(0xFFE0E1E6), btnFilledText = Color(0xFF131417), btnBasic = Color(0x0DFFFFFF), btnBasicPress = Color(0x1AFFFFFF),
    accent = Color(0xFFFFC857), accentPress = Color(0xFFF0B43C), accentEdge = Color.Transparent, accentText = Color(0xFFFFC857),
    accentSoft = Color(0x29FFC857), onAccent = Color(0xFF131417),
    heart = Color(0xFFFF7A98), heartSoft = Color(0x2EFF7A98),
    ok = Color(0xFF00E074), info = Color(0xFF4DA3FF), neg = Color(0xFFFF7A5C), dangerFill = Color(0xFFC62828),
    okSoft = Color(0x2400E074), infoSoft = Color(0x290082FF), negSoft = Color(0x24FF4C28),
    switchOn = Color(0xFF02C265),
)

/** 허니 카드(월간 카드) 위 하트. #FFC857 위 3.82:1 */
val HeartOnHoney = Color(0xFFC2185B)

object Tm {
    val touchMin = 64.dp
    val touchMd = 80.dp
    val touchLg = 100.dp
    val railW = 168.dp
    val headH = 112.dp
    val rSm = 16.dp
    val rMd = 24.dp
    val rLg = 32.dp
    val rXl = 40.dp
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun asta(weight: Int) = Font(
    R.font.asta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val AstaSans = FontFamily(asta(400), asta(500), asta(700), asta(800))

object TmType {
    val numXl = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.ExtraBold, fontSize = 160.sp, lineHeight = 160.sp, letterSpacing = (-0.03).em)
    val numLg = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.ExtraBold, fontSize = 96.sp, lineHeight = 104.sp, letterSpacing = (-0.03).em)
    val display = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.ExtraBold, fontSize = 72.sp, lineHeight = 88.sp)
    val headline = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, lineHeight = 64.sp)
    val title = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 52.sp)
    val titleSm = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp)
    val body = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp)
    val bodySm = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp)
    val label = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 28.sp)
    val caption = TextStyle(fontFamily = AstaSans, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 24.sp)
}

private val Double.em get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Em)

/** 창 폭으로 정한 배치: 넓으면 landscape(앱 영역 1682), 좁으면 portrait(분할 840). */
enum class Layout { Landscape, Portrait }

val LocalLayout = staticCompositionLocalOf { Layout.Landscape }
val LocalTm = staticCompositionLocalOf { LightColors }

/** 현재 테마 색. */
val tc: TmColors @Composable get() = LocalTm.current

@Composable
fun TymTheme(dark: Boolean, layout: Layout, content: @Composable () -> Unit) {
    val col = if (dark) DarkColors else LightColors
    val scheme = if (dark) darkColorScheme(primary = col.accent, onPrimary = col.onAccent, background = col.bg, surface = col.surface, onBackground = col.text, onSurface = col.text)
    else lightColorScheme(primary = col.accent, onPrimary = col.onAccent, background = col.bg, surface = col.surface, onBackground = col.text, onSurface = col.text)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalTm provides col, LocalLayout provides layout) { content() }
    }
}
