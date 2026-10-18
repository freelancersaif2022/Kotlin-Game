package com.smartisland.app.system

import android.content.Context

/** Typed system-service lookup; every service used by the app exists on all supported releases. */
inline fun <reified T : Any> Context.systemService(): T =
    requireNotNull(getSystemService(T::class.java)) { "${T::class.java.simpleName} unavailable" }
