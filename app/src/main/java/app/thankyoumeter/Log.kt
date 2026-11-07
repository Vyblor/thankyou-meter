package app.thankyoumeter

/** 모든 로그는 태그 TYM. e2e 시험이 이 로그로 판정한다. */
object Log {
    fun log(s: String) { android.util.Log.i("TYM", s) }
}
