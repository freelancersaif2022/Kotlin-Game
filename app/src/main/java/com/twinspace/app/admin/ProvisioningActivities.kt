package com.twinspace.app.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle

/** Sent by Android (8.0+) inside the new profile once it has been created. */
class ProvisioningSuccessActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpaceSetup.finish(this, intent)
        finish()
    }
}

/** Android 10+ may ask which kind of provisioning we want; we only ever create a space (work profile). */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(
            RESULT_OK,
            Intent().putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE,
            ),
        )
        finish()
    }
}

/** Android 10+ final step of provisioning: apply our (minimal) setup and report success. */
class PolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpaceSetup.finish(this, intent)
        setResult(RESULT_OK)
        finish()
    }
}
