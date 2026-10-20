package com.tvremote.universal.tv

import com.tvremote.universal.AppContainer
import com.tvremote.universal.core.ClientIdentity
import com.tvremote.universal.data.TvDevice
import com.tvremote.universal.data.TvPlatform

object ControllerFactory {
    fun create(
        device: TvDevice,
        container: AppContainer,
        approvalTimeoutMs: Long = 15_000,
        onDeviceChanged: (TvDevice) -> Unit,
    ): TvController {
        val identity = { ClientIdentity.get(container.app) }
        return when (device.platform) {
            TvPlatform.ANDROID_TV -> AndroidTvController(device, identity, container.certPins, onDeviceChanged)
            TvPlatform.SAMSUNG -> SamsungController(device, container.http, container.certPins, onDeviceChanged, approvalTimeoutMs)
            TvPlatform.LG -> LgController(device, container.http, container.certPins, onDeviceChanged, approvalTimeoutMs)
            TvPlatform.ROKU -> RokuController(device, container.http, onDeviceChanged)
            TvPlatform.VIZIO -> VizioController(device, container.http, container.certPins, onDeviceChanged)
            TvPlatform.FIRE_TV -> FireTvController(device, identity, onDeviceChanged, approvalTimeoutMs.toInt())
        }
    }
}
