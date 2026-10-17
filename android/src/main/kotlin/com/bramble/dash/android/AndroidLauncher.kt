package com.bramble.dash.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.bramble.dash.BrambleDash

/** Android entry point: full-screen immersive landscape, drawing under display cutouts (UI uses safe insets). */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            renderUnderCutout = true
            useAccelerometer = false
            useCompass = false
            useGyroscope = false
            numSamples = 0
        }
        initialize(BrambleDash(), config)
    }
}
