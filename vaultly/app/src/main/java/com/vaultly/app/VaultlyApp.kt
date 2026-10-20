package com.vaultly.app

import android.app.Application
import com.vaultly.app.ads.AdsManager
import com.vaultly.app.billing.BillingManager
import com.vaultly.app.data.AppDatabase
import com.vaultly.app.data.ItemRepository
import com.vaultly.app.data.Prefs
import com.vaultly.app.ocr.ReceiptReader
import com.vaultly.app.pdf.ClaimPackExporter
import com.vaultly.app.reminders.ReminderWorker
import com.vaultly.app.storage.ImageStore

class VaultlyApp : Application() {
    val prefs by lazy { Prefs(this) }
    val images by lazy { ImageStore(this) }
    val repository by lazy { ItemRepository(AppDatabase.create(this).items(), images) }
    val reader by lazy { ReceiptReader(this) }
    val exporter by lazy { ClaimPackExporter(this) }
    val ads by lazy { AdsManager(this, prefs) }
    val billing by lazy { BillingManager(this, prefs) }

    override fun onCreate() {
        super.onCreate()
        ReminderWorker.createChannel(this)
        ReminderWorker.schedule(this)
        billing.connect()
    }
}
