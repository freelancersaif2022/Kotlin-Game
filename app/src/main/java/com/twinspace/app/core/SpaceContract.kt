package com.twinspace.app.core

/**
 * The protocol spoken between the main app (personal side) and its copy running inside the space
 * (the managed work profile). Requests travel through the system's cross-profile forwarder.
 */
object SpaceContract {
    const val ACTION_SPACE_OP = "com.twinspace.app.action.SPACE_OP"

    const val EXTRA_OP = "op"
    const val EXTRA_PACKAGES = "packages"
    const val EXTRA_HIDDEN = "hidden"
    const val EXTRA_NAME = "name"
    const val EXTRA_TOKEN = "token"

    /** Result extras. */
    const val EXTRA_DONE = "done"
    const val EXTRA_FAILED = "failed"
    const val EXTRA_ERROR = "error"

    /** Keys inside EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE. */
    const val ADMIN_EXTRA_TOKEN = "twinspace.token"
    const val ADMIN_EXTRA_NAME = "twinspace.name"

    enum class Op { PING, CLONE, REMOVE, FREEZE, RENAME_SPACE, DELETE_SPACE }
}
