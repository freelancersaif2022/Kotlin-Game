import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.ConsumerIrManager
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.irremote.tv.MainActivity
import com.irremote.tv.screens.AutoSearchScreen
import com.irremote.tv.screens.HomeScreen
import com.irremote.tv.screens.MatchScreen
import com.irremote.tv.screens.RemoteScreen
import com.irremote.tv.ui.DPadView
import com.irremote.tv.ui.KeyButton
import com.irremote.tv.ui.RockerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.io.File
import java.io.FileOutputStream
import java.time.Duration

@Implements(ConsumerIrManager::class)
class FakeIr {
    @Implementation fun __constructor__(context: android.content.Context) {}
    @Implementation fun hasIrEmitter(): Boolean = enabled
    @Implementation fun transmit(freq: Int, pattern: IntArray) { sent.add(freq to pattern.copyOf()) }
    @Implementation fun getCarrierFrequencies(): Array<ConsumerIrManager.CarrierFrequencyRange>? = null

    companion object {
        @JvmStatic var enabled = true
        val sent = java.util.Collections.synchronizedList(ArrayList<Pair<Int, IntArray>>())
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi", shadows = [FakeIr::class])
class UiFlowTest {
    private lateinit var controller: ActivityController<MainActivity>
    private val activity get() = controller.get()
    private val shots = File(System.getProperty("shotdir") ?: "shots").apply { mkdirs() }

    @Before fun setUp() {
        FakeIr.enabled = true
        FakeIr.sent.clear()
        // the transmitter is a process-wide singleton; start every test with a fresh one
        val f = com.irremote.tv.ir.IrTransmitter::class.java.getDeclaredField("instance")
        f.isAccessible = true
        f.set(null, null)
    }

    private fun launch() {
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    private fun idle(ms: Long = 50) {
        // let the IR worker thread / DB loader run, then advance the main looper
        Thread.sleep(30)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
        Thread.sleep(10)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun waitForHome() {
        repeat(80) {
            idle(100)
            if (activity.current is HomeScreen || activity.current is RemoteScreen) return
        }
        error("app never left the splash screen, current=${activity.current}")
    }

    private fun views(v: View = activity.window.decorView): Sequence<View> = sequence {
        yield(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) yieldAll(views(v.getChildAt(i)))
    }

    private fun shown(): Sequence<View> = views().filter { it.isShown }

    private fun textView(text: String): TextView =
        shown().filterIsInstance<TextView>().firstOrNull { it.text.toString() == text }
            ?: error("no visible text '$text'. Visible: " + shown().filterIsInstance<TextView>().map { it.text }.filter { it.isNotEmpty() }.toList())

    private fun tapText(text: String) {
        var v: View? = textView(text)
        while (v != null && !v.isClickable) v = v.parent as? View
        requireNotNull(v) { "nothing clickable around '$text'" }.performClick()
        idle(400)
    }

    private fun touch(v: View, x: Float = v.width / 2f, y: Float = v.height / 2f, holdMs: Long = 0) {
        val t = SystemClock.uptimeMillis()
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x, y, 0))
        idle(if (holdMs > 0) holdMs else 30)
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, x, y, 0))
        idle(300)
    }

    private fun waitForIr(count: Int): List<Pair<Int, IntArray>> {
        repeat(50) {
            if (FakeIr.sent.size >= count) return FakeIr.sent.toList()
            idle(20)
        }
        return FakeIr.sent.toList()
    }

    private fun shot(name: String, view: View = activity.window.decorView, settle: Long = 400) {
        if (settle >= 0) idle(settle)
        val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        FileOutputStream(File(shots, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun fullFlow() {
        launch()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500))
        shot("01_splash", settle = -1)
        waitForHome()
        assertTrue(activity.current is HomeScreen)
        textView("IR blaster ready")
        textView("No remotes yet")
        shot("02_home_empty")

        // brand list + search
        tapText("Add TV Remote")
        textView("Choose TV brand")
        shot("03_brands")
        val search = shown().filterIsInstance<EditText>().first()
        search.setText("hisen")
        idle(200)
        textView("Hisense")
        shot("04_brand_search")
        search.setText("")
        idle(200)

        // code matching for Samsung
        tapText("Samsung")
        assertTrue(activity.current is MatchScreen)
        shot("05_match")
        val power = shown().filterIsInstance<KeyButton>().first { it.key.name == "POWER" }
        FakeIr.sent.clear()
        touch(power)
        val sent = waitForIr(1)
        assertEquals("one IR burst for Power", 1, sent.size)
        assertEquals(38_000, sent[0].first)
        assertEquals(4500, sent[0].second[0]) // Samsung32 leader
        assertEquals(4500, sent[0].second[1])
        tapText("No, next code")
        textView("Code 2 of ${com.irremote.tv.ir.IrDatabase.get(activity).brand("Samsung")!!.sets.size}")
        tapText("Yes, it works")
        shot("06_name_prompt")
        tapText("Save remote")
        idle(600)
        assertTrue(activity.current is RemoteScreen)
        shot("07_remote_top")
        val scroll = shown().filterIsInstance<ScrollView>().first()
        shot("08_remote_full", scroll.getChildAt(0))

        // every enabled key on the remote must transmit exactly once per tap
        val keys = views().filterIsInstance<KeyButton>().filter { it.isShown || it.parent != null }.toList()
        var expected = 0
        for (k in keys) {
            val listener = k.listener!!
            FakeIr.sent.clear()
            k.dispatchTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 5f, 5f, 0))
            k.dispatchTouchEvent(MotionEvent.obtain(0, 10, MotionEvent.ACTION_UP, 5f, 5f, 0))
            val got = waitForIr(1).size
            if (listener.isAvailable(k.key)) {
                assertEquals("key ${k.key}", 1, got); expected++
            } else {
                assertEquals("unavailable key ${k.key} must not send", 0, got)
            }
            idle(150)
        }
        println("remote keys: ${keys.size} buttons, $expected sent IR")
        assertTrue(expected >= 30)

        // D-pad zones and rocker auto-repeat
        val pad = views().filterIsInstance<DPadView>().first()
        for ((x, y) in listOf(0.5f to 0.08f, 0.5f to 0.92f, 0.08f to 0.5f, 0.92f to 0.5f, 0.5f to 0.5f)) {
            FakeIr.sent.clear()
            touch(pad, pad.width * x, pad.height * y)
            assertEquals("dpad tap at $x,$y", 1, waitForIr(1).size)
        }
        val vol = views().filterIsInstance<RockerView>().first()
        FakeIr.sent.clear()
        val t = SystemClock.uptimeMillis()
        vol.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, vol.width / 2f, vol.height * 0.2f, 0))
        repeat(20) { idle(50) }
        vol.dispatchTouchEvent(MotionEvent.obtain(t, t + 1000, MotionEvent.ACTION_UP, vol.width / 2f, vol.height * 0.2f, 0))
        idle(300)
        println("volume held ~1s -> ${FakeIr.sent.size} bursts")
        assertTrue("holding volume repeats", FakeIr.sent.size >= 3)

        // remote menu
        shotMenu()

        // back to home with the saved remote
        activity.onBackPressed()
        idle(500)
        assertTrue(activity.current is HomeScreen)
        textView("Samsung TV")
        shot("10_home_saved")

        // auto search
        tapText("Add TV Remote")
        tapText("Auto Search")
        assertTrue(activity.current is AutoSearchScreen)
        shot("11_auto_ready")
        fun clickNow(label: String) {
            var v: View? = textView(label); while (v != null && !v.isClickable) v = v.parent as? View
            v!!.performClick()
        }
        // cadence: the whole list is swept, one power code every 1.8 s
        FakeIr.sent.clear()
        val t0 = SystemClock.uptimeMillis()
        clickNow("Start search")
        idle(50)
        val elapsed = SystemClock.uptimeMillis() - t0
        val total = com.irremote.tv.ir.IrDatabase.get(activity).powerSearchList.size
        println("auto search swept ${FakeIr.sent.size}/$total codes over ${elapsed} ms of device time")
        assertEquals(total, FakeIr.sent.size)
        assertTrue(elapsed >= 1800L * (total - 1))
        tapText("Restart from the first code")
        // stop while running: the code that was just sent is offered for confirmation
        clickNow("Start search")
        shot("12_auto_running", settle = -1)
        clickNow("TV turned off — Stop")
        idle(200)
        shot("13_auto_found")
        FakeIr.sent.clear()
        tapText("Test Power")
        assertEquals(1, waitForIr(1).size)
        tapText("It works — save remote")
        tapText("Save remote")
        idle(500)
        assertTrue(activity.current is RemoteScreen)

        // universal brand
        activity.onBackPressed(); idle(400)
        tapText("Add TV Remote")
        val s2 = shown().filterIsInstance<EditText>().first()
        s2.setText("walton"); idle(200)
        tapText("Walton")
        textView("Universal codes")
        shot("14_universal_match")
        val n = com.irremote.tv.ir.IrDatabase.get(activity).brand("Walton")!!.sets.size
        FakeIr.sent.clear()
        val t1 = SystemClock.uptimeMillis()
        clickNow("Auto-try codes (Power every 2.5 s)")
        repeat(3) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(2500)); Thread.sleep(20) }
        waitForIr(4)
        val el = SystemClock.uptimeMillis() - t1
        println("auto-try sent ${FakeIr.sent.size}/$n codes over $el ms of device time")
        assertEquals(4, FakeIr.sent.size) // t=0, 2.5, 5.0, 7.5 s
        textView("Code 4 of $n")
        clickNow("Stop — my TV responded")
        idle(400)
        textView("Name this remote")
        shot("15_universal_save")
        activity.dismissSheet(); idle(300)

        // relaunch opens the last used remote directly
        controller.pause().stop().destroy()
        launch()
        waitForHome()
        assertTrue("last remote reopened", activity.current is RemoteScreen)
    }

    private fun shotMenu() {
        // the overflow is the last icon button in the top bar
        val top = shown().filterIsInstance<android.widget.LinearLayout>().first { l -> l.minimumHeight > 0 }
        top.getChildAt(top.childCount - 1).performClick()
        idle(400)
        textView("Change code / re-match")
        shot("09_remote_menu")
        activity.dismissSheet()
        idle(300)
    }

    @Test fun splashRenders() {
        launch()
        val v = com.irremote.tv.screens.SplashScreen(activity).createView()
        val root = activity.window.decorView
        v.measure(View.MeasureSpec.makeMeasureSpec(root.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(root.height, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, root.width, root.height)
        v.animate().cancel(); views(v).forEach { it.animate().cancel(); it.alpha = 1f; it.scaleX = 1f; it.scaleY = 1f }
        shot("01_splash", v, settle = -1)
    }

    @Test fun noIrBlaster() {
        FakeIr.enabled = false
        launch()
        waitForHome()
        textView("No IR blaster found")
        shot("16_no_ir")
    }
}
