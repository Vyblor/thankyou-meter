package app.thankyoumeter.detect

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.thankyoumeter.Log.log
import app.thankyoumeter.MainActivity
import app.thankyoumeter.R
import app.thankyoumeter.app

/**
 * 주행 중에는 앱 화면이 막히므로(distractionOptimized 없음) 이 서비스가 신호를 받고 소리로 묻는다.
 * 2026-11-05 시험 앱에서 specialUse 포그라운드 서비스가 화면이 뒤에 있어도 신호를 받는 것을 확인했다.
 */
class DetectService : Service() {
    companion object {
        private const val CH = "tym_detect"
        fun start(ctx: Context) {
            try { ctx.startForegroundService(Intent(ctx, DetectService::class.java)) }
            catch (e: Exception) { log("SVC start failed $e") }
        }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "땡큐미터 감지", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("땡큐미터")
            .setContentText("차선을 바꾸면 한 번 물어볼게요")
            .setContentIntent(open).setOngoing(true).build()
        // 마이크 형식은 앱이 앞에 있을 때만 시작할 수 있다. 안 되면 specialUse만으로 띄운다.
        val ok = try {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE); "special+mic"
        } catch (e: Exception) {
            try { startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE); "special" } catch (e2: Exception) { log("SVC foreground failed $e2"); "none" }
        }
        log("SVC foreground=$ok sdk=${Build.VERSION.SDK_INT}")
        val a = app
        val b = a.brain ?: Brain(a).also { a.brain = it }
        a.wireBrain(b)
        b.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        app.brain?.stop()
        log("SVC destroy")
        super.onDestroy()
    }
}

/** 차를 켜면(부팅) 첫 실행을 마친 경우 감지를 다시 시작한다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        log("SVC boot ${i.action}")
        if (c.app.store.settings.value.onboarded) DetectService.start(c)
    }
}
