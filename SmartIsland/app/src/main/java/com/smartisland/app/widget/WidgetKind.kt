package com.smartisland.app.widget

import android.appwidget.AppWidgetProvider
import com.smartisland.app.R

enum class WidgetSize(val label: String) { SMALL("2 × 2"), WIDE("4 × 1"), LARGE("4 × 2") }

enum class WidgetKind(
    val id: String,
    val title: String,
    val description: String,
    val premium: Boolean,
    val size: WidgetSize,
    val layout: Int,
    val provider: Class<out AppWidgetProvider>,
) {
    DIGITAL_CLOCK("digital_clock", "Digital Clock", "Large live clock with date and battery.", false, WidgetSize.LARGE, R.layout.widget_digital_clock, DigitalClockWidget::class.java),
    ANALOG_CLOCK("analog_clock", "Analog Clock", "Classic dial with a smooth sweeping second hand.", false, WidgetSize.SMALL, R.layout.widget_analog_clock, AnalogClockWidget::class.java),
    BATTERY_RING("battery_ring", "Battery Ring", "Circular battery gauge that turns green while charging.", false, WidgetSize.SMALL, R.layout.widget_battery_ring, BatteryRingWidget::class.java),
    CALENDAR("calendar", "Calendar", "Today's date at a glance with your next alarm.", false, WidgetSize.SMALL, R.layout.widget_calendar, CalendarWidget::class.java),
    POWER("power", "Power Buttons", "Power menu, lock, screenshot and notifications.", false, WidgetSize.WIDE, R.layout.widget_power, PowerWidget::class.java),
    VOLUME("volume", "Volume Control", "Volume − / + with a live 0–100% bar and mute.", false, WidgetSize.WIDE, R.layout.widget_volume, VolumeWidget::class.java),
    CHARGING("charging", "Charging Pulse", "Live charging speed, watts, temperature and health.", true, WidgetSize.WIDE, R.layout.widget_charging, ChargingWidget::class.java),
    WORLD_CLOCK("world_clock", "World Clock", "Live time in New York, London, Dubai and Tokyo.", true, WidgetSize.WIDE, R.layout.widget_world_clock, WorldClockWidget::class.java),
    MEDIA("media", "Now Playing", "Album art, track info and playback controls.", true, WidgetSize.LARGE, R.layout.widget_media, MediaWidget::class.java),
    STOPWATCH("stopwatch", "Stopwatch", "Real-time stopwatch that keeps running in the background.", true, WidgetSize.SMALL, R.layout.widget_stopwatch, StopwatchWidget::class.java),
    SYSTEM_STATS("system_stats", "System Monitor", "RAM, storage and battery usage bars.", true, WidgetSize.LARGE, R.layout.widget_system_stats, SystemStatsWidget::class.java),
    QUICK_TOGGLES("quick_toggles", "Quick Toggles", "Flashlight, ringer mode, Do Not Disturb and island.", true, WidgetSize.WIDE, R.layout.widget_quick_toggles, QuickTogglesWidget::class.java),
    ISLAND_GLANCE("island_glance", "Island Glance", "A glowing island pill with live time, battery and now playing.", true, WidgetSize.WIDE, R.layout.widget_island_glance, IslandGlanceWidget::class.java),
    DAY_PROGRESS("day_progress", "Day Progress", "How much of today, this week, month and year has passed.", true, WidgetSize.LARGE, R.layout.widget_day_progress, DayProgressWidget::class.java),
    STORAGE_RING("storage_ring", "Storage Ring", "Used and free storage in a sleek gradient ring.", true, WidgetSize.SMALL, R.layout.widget_storage_ring, StorageRingWidget::class.java),
    DUAL_CLOCK("dual_clock", "Dual Time", "Your time and London time side by side, ticking live.", true, WidgetSize.WIDE, R.layout.widget_dual_clock, DualClockWidget::class.java),
    SMART_GLANCE("smart_glance", "Smart Glance", "Greeting, date, battery, next alarm and music in one card.", true, WidgetSize.LARGE, R.layout.widget_smart_glance, SmartGlanceWidget::class.java),
    KITTY("kitty", "Kitty Clock", "An animated cat that blinks and wags its tail, with the live time.", true, WidgetSize.SMALL, R.layout.widget_kitty, KittyWidget::class.java),
    PUPPY("puppy", "Puppy Pal", "A happy animated puppy wagging its tail, with today's date.", true, WidgetSize.SMALL, R.layout.widget_puppy, PuppyWidget::class.java),
    CAMPFIRE("campfire", "Cozy Fire", "A flickering campfire with rising embers, live time and battery.", true, WidgetSize.SMALL, R.layout.widget_campfire, CampfireWidget::class.java),
    ORBIT("orbit", "Solar Orbit", "Planets circling a glowing sun under twinkling stars, with the time.", true, WidgetSize.SMALL, R.layout.widget_orbit, OrbitWidget::class.java),
    AQUARIUM("aquarium", "Aquarium", "Fish swimming among swaying plants and rising bubbles.", true, WidgetSize.WIDE, R.layout.widget_aquarium, AquariumWidget::class.java);

    companion object {
        fun byId(id: String?): WidgetKind? = entries.firstOrNull { it.id == id }
    }
}

class DigitalClockWidget : BaseWidgetProvider(WidgetKind.DIGITAL_CLOCK)
class AnalogClockWidget : BaseWidgetProvider(WidgetKind.ANALOG_CLOCK)
class BatteryRingWidget : BaseWidgetProvider(WidgetKind.BATTERY_RING)
class CalendarWidget : BaseWidgetProvider(WidgetKind.CALENDAR)
class PowerWidget : BaseWidgetProvider(WidgetKind.POWER)
class VolumeWidget : BaseWidgetProvider(WidgetKind.VOLUME)
class ChargingWidget : BaseWidgetProvider(WidgetKind.CHARGING)
class WorldClockWidget : BaseWidgetProvider(WidgetKind.WORLD_CLOCK)
class MediaWidget : BaseWidgetProvider(WidgetKind.MEDIA)
class StopwatchWidget : BaseWidgetProvider(WidgetKind.STOPWATCH)
class SystemStatsWidget : BaseWidgetProvider(WidgetKind.SYSTEM_STATS)
class QuickTogglesWidget : BaseWidgetProvider(WidgetKind.QUICK_TOGGLES)
class IslandGlanceWidget : BaseWidgetProvider(WidgetKind.ISLAND_GLANCE)
class DayProgressWidget : BaseWidgetProvider(WidgetKind.DAY_PROGRESS)
class StorageRingWidget : BaseWidgetProvider(WidgetKind.STORAGE_RING)
class DualClockWidget : BaseWidgetProvider(WidgetKind.DUAL_CLOCK)
class SmartGlanceWidget : BaseWidgetProvider(WidgetKind.SMART_GLANCE)
class KittyWidget : BaseWidgetProvider(WidgetKind.KITTY)
class PuppyWidget : BaseWidgetProvider(WidgetKind.PUPPY)
class CampfireWidget : BaseWidgetProvider(WidgetKind.CAMPFIRE)
class OrbitWidget : BaseWidgetProvider(WidgetKind.ORBIT)
class AquariumWidget : BaseWidgetProvider(WidgetKind.AQUARIUM)
