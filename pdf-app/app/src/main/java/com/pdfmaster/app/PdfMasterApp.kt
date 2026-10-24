package com.pdfmaster.app

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp
import kotlin.concurrent.thread

@HiltAndroidApp
class PdfMasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warm up PdfBox's font and glyph tables off the main thread so the first tool run is fast.
        thread(name = "pdfbox-init", priority = Thread.MIN_PRIORITY) { PDFBoxResourceLoader.init(this) }
    }
}
