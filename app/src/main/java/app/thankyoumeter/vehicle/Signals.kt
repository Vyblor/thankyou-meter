package app.thankyoumeter.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import app.thankyoumeter.Log.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Gear { P, R, N, D, UNKNOWN }

/** 방향지시등. Blink·Flash(원터치) 모두 앱에는 LEFT/RIGHT로 온다(에뮬레이터 실측 2026-11-07). */
enum class Turn { OFF, LEFT, RIGHT }

/** 감지기가 받는 신호 하나. */
sealed interface Sig {
    val t: Long
    data class Geared(val g: Gear, override val t: Long) : Sig
    data class Signal(val v: Turn, override val t: Long) : Sig
    data class Steer(val deg: Float, override val t: Long) : Sig
    data class Speed(val kmh: Float, override val t: Long) : Sig
}

interface VehicleSignals {
    val name: String
    val gear: StateFlow<Gear>
    val turn: StateFlow<Turn>
    val speedKmh: StateFlow<Float>
    fun start()
    /** 감지기 연결. 신호가 올 때마다 부른다(메인 스레드). */
    fun listen(f: (Sig) -> Unit)
}

/**
 * Pleos Vehicle SDK 2.3.2(방향지시등·조향각·기어) + AAOS PERF_VEHICLE_SPEED(속도, m/s).
 * API 모양은 contest/probe-app 시험 앱에서 에뮬레이터로 확인한 호출만 쓴다.
 */
class PleosSignals(private val ctx: Context) : VehicleSignals {
    override val name = "pleos"
    private val vehicle = Vehicle(ctx)
    private val main = Handler(Looper.getMainLooper())
    override val gear = MutableStateFlow(Gear.UNKNOWN)
    override val turn = MutableStateFlow(Turn.OFF)
    override val speedKmh = MutableStateFlow(0f)
    private val sinks = mutableListOf<(Sig) -> Unit>()

    override fun listen(f: (Sig) -> Unit) { sinks += f }
    private fun emit(s: Sig) { main.post { sinks.forEach { it(s) } } }

    override fun start() {
        vehicle.initialize()
        log("VEH pleos initialize")
        setupSpeed()
        // 초기화 직후보다 약 2.5초 뒤 등록이 안정적이었다(시험 앱)
        main.postDelayed({ setupVehicle() }, 2500)
    }

    private fun mapGear(g: VehicleGear?): Gear {
        val n = g?.name?.uppercase() ?: return Gear.UNKNOWN
        return when {
            n.contains("PARK") || n == "P" -> Gear.P
            n.contains("REVERSE") || n == "R" -> Gear.R
            n.contains("NEUTRAL") || n == "N" -> Gear.N
            n.contains("DRIVE") || n == "D" || n.startsWith("GEAR_") -> Gear.D
            else -> Gear.UNKNOWN
        }
    }

    private fun mapTurn(v: TurnSignalValue?): Turn {
        val n = v?.name?.uppercase() ?: return Turn.OFF
        return when {
            n.contains("LEFT") -> Turn.LEFT
            n.contains("RIGHT") -> Turn.RIGHT
            else -> Turn.OFF
        }
    }

    private fun setupVehicle() {
        try {
            vehicle.getGear().registerGearSelection(object : GearSelectionListener {
                override fun onSelected(g: VehicleGear?) {
                    val m = mapGear(g); gear.value = m
                    log("VEH gear raw=$g mapped=$m"); emit(Sig.Geared(m, System.currentTimeMillis()))
                }
                override fun onFailed(e: Exception) { log("VEH gear listener failed $e") }
            })
        } catch (e: Exception) { log("VEH gear register failed $e") }
        try {
            vehicle.getTurnSignal().registerTurnSignalState(object : TurnSignalStateListener {
                override fun onTurnSignalStateUpdated(v: TurnSignalValue?) {
                    val m = mapTurn(v); turn.value = m
                    log("VEH turn raw=$v mapped=$m"); emit(Sig.Signal(m, System.currentTimeMillis()))
                }
                override fun onFailed(e: Exception) { log("VEH turn listener failed $e") }
            })
        } catch (e: Exception) { log("VEH turn register failed $e") }
        try {
            vehicle.getSteeringWheel().registerSteeringWheelAngle(object : SteeringWheelAngleListener {
                override fun onSteeringWheelAngleUpdated(v: Float?) {
                    val d = v ?: return
                    emit(Sig.Steer(d, System.currentTimeMillis()))
                }
                override fun onFailed(e: Exception) { log("VEH steer listener failed $e") }
            })
        } catch (e: Exception) { log("VEH steer register failed $e") }
        log("VEH signals registered")
    }

    private var speedDone = false

    /** 속도 권한(CAR_SPEED)은 런타임 권한이다. 권한을 받은 뒤 다시 부르면 등록한다. */
    fun retrySpeed() { if (!speedDone) setupSpeed() }

    private fun setupSpeed() {
        if (ctx.checkSelfPermission("android.car.permission.CAR_SPEED") != android.content.pm.PackageManager.PERMISSION_GRANTED) { log("VEH speed no permission"); return }
        try {
            val car = Car.createCar(ctx)
            val pm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            val cb = object : CarPropertyManager.CarPropertyEventCallback {
                override fun onChangeEvent(v: CarPropertyValue<*>) {
                    val ms = (v.value as? Float) ?: return
                    val kmh = ms * 3.6f
                    if (kotlin.math.abs(kmh - speedKmh.value) >= 1f) log("VEH speed ${"%.0f".format(kmh)}")
                    speedKmh.value = kmh
                    // 이벤트가 몰려 도착해도 감속 폭을 바르게 재도록, 신호가 생긴 시각(elapsedRealtimeNanos 기준)을 벽시계로 바꿔 쓴다
                    val ageMs = ((android.os.SystemClock.elapsedRealtimeNanos() - v.timestamp) / 1_000_000L).coerceIn(0L, 5000L)
                    emit(Sig.Speed(kmh, System.currentTimeMillis() - ageMs))
                }
                override fun onErrorEvent(id: Int, zone: Int) { log("VEH speed err $id") }
            }
            val ok = pm.registerCallback(cb, VehiclePropertyIds.PERF_VEHICLE_SPEED, 10f)
            speedDone = ok
            log("VEH speed register ok=$ok")
        } catch (e: Exception) { log("VEH speed setup failed $e") }
    }
}

/** SDK가 없는 기기(일반 안드로이드)용. 항상 P, 신호 없음. */
class FakeSignals : VehicleSignals {
    override val name = "fake"
    override val gear = MutableStateFlow(Gear.P)
    override val turn = MutableStateFlow(Turn.OFF)
    override val speedKmh = MutableStateFlow(0f)
    override fun start() { log("VEH fake start") }
    override fun listen(f: (Sig) -> Unit) {}
}
