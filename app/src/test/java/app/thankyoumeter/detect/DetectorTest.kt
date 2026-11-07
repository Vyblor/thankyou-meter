package app.thankyoumeter.detect

import app.thankyoumeter.vehicle.Gear
import app.thankyoumeter.vehicle.Sig
import app.thankyoumeter.vehicle.Turn
import org.junit.Assert.assertEquals
import org.junit.Test

/** 감지 설계 합격선 8줄을 시각을 흉내 내어 시험한다(에뮬레이터 e2e와 같은 시나리오). */
class DetectorTest {
    private class Sim {
        val events = mutableListOf<Event>()
        val d = Detector { events += it }
        var t = 1_000_000L
        var speed = 60f
        fun at(ms: Long) {
            val end = t + ms
            while (t < end) { t += 250; d.tick(t) }
        }
        fun gear(g: Gear) { d.onSig(Sig.Geared(g, t)) }
        fun signal(v: Turn) { d.onSig(Sig.Signal(v, t)) }
        fun steer(deg: Float) { d.onSig(Sig.Steer(deg, t)) }
        fun speed(v: Float) { speed = v; d.onSig(Sig.Speed(v, t)) }
        fun steerPath(vararg a: Float, step: Long = 500) { for (x in a) { steer(x); at(step) } }
        fun lanes() = events.filterIsInstance<Event.LaneChange>()
        fun gives() = events.filterIsInstance<Event.GiveCandidate>()
        fun start() { gear(Gear.D); speed(60f); steer(0f); at(40_000) }
    }

    private fun laneChange(s: Sim, side: Turn = Turn.LEFT, flash: Boolean = false) {
        s.signal(side); s.at(1000)
        if (flash) { s.signal(Turn.OFF) }
        s.steerPath(8f, 20f, 15f, 2f, -10f, -6f, 0f)
        if (!flash) s.signal(Turn.OFF)
        s.at(12_000)
    }

    @Test fun laneChange10() {
        val s = Sim(); s.start()
        repeat(10) { laneChange(s, if (it % 2 == 0) Turn.LEFT else Turn.RIGHT); s.at(35_000) }
        assertEquals(10, s.lanes().count { it.signaled })
        assertEquals(Turn.RIGHT, s.lanes()[1].side)
    }

    @Test fun signalOnly0of5() {
        val s = Sim(); s.start()
        repeat(5) { s.signal(Turn.LEFT); s.at(3000); s.signal(Turn.OFF); s.at(15_000) }
        assertEquals(0, s.lanes().size)
    }

    @Test fun turn0of3() {
        val s = Sim(); s.start()
        repeat(3) {
            s.speed(25f); s.signal(Turn.RIGHT); s.at(1000)
            s.steerPath(30f, 120f, 200f, 150f, 60f, 10f, 0f)
            s.signal(Turn.OFF); s.at(12_000); s.speed(60f); s.at(35_000)
        }
        assertEquals(0, s.lanes().size)
    }

    @Test fun flash3of3() {
        val s = Sim(); s.start()
        repeat(3) { laneChange(s, Turn.LEFT, flash = true); s.at(35_000) }
        assertEquals(3, s.lanes().count { it.signaled })
    }

    private fun decel(s: Sim, to: Float, recoverTo: Float) {
        var v = 60f
        while (v > to) { v = maxOf(to, v - 3f); s.speed(v); s.at(500) }
        s.at(2000)
        while (v < recoverTo) { v = minOf(recoverTo, v + 3f); s.speed(v); s.at(500) }
        s.at(25_000)
    }

    @Test fun give5of5() {
        val s = Sim(); s.start()
        repeat(5) { decel(s, 40f, 60f) }
        assertEquals(5, s.gives().size)
        assertEquals(0, s.events.filterIsInstance<Event.HardBrake>().size)
    }

    @Test fun stopped0of3() {
        val s = Sim(); s.start()
        repeat(3) { decel(s, 0f, 60f) }
        assertEquals(0, s.gives().size)
    }

    @Test fun afterLaneChange0of3() {
        val s = Sim(); s.start()
        repeat(3) {
            laneChange(s)          // 끝나고 12초 뒤
            decel(s, 40f, 60f)
            s.at(35_000)
        }
        assertEquals(3, s.lanes().size)
        assertEquals(0, s.gives().size)
    }

    @Test fun smallDecel0of3() {
        val s = Sim(); s.start()
        repeat(3) { decel(s, 54f, 60f) }
        assertEquals(0, s.gives().size)
    }

    @Test fun hardBrakeCounted() {
        val s = Sim(); s.start()
        s.speed(40f); s.at(2000); s.speed(60f); s.at(30_000)
        assertEquals(1, s.events.filterIsInstance<Event.HardBrake>().size)
        assertEquals(0, s.gives().size)
    }

    @Test fun unsignaledCountedNotAsked() {
        val s = Sim(); s.start()
        s.steerPath(8f, 20f, 15f, 2f, -10f, -6f, 0f); s.at(12_000)
        assertEquals(1, s.lanes().size)
        assertEquals(false, s.lanes()[0].signaled)
    }

    @Test fun parkNothing() {
        val s = Sim(); s.gear(Gear.P); s.speed(0f)
        s.signal(Turn.LEFT); s.steerPath(8f, 20f, -10f, 0f); s.signal(Turn.OFF); s.at(15_000)
        assertEquals(0, s.lanes().size)
    }
}
