package com.clonehub.app.data

/** An installed, launchable application discovered on the device. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * A user-created clone entry. A clone is a renamable, re-styleable shortcut to an
 * installed app that is persisted locally forever (until the user deletes it).
 */
data class Clone(
    val id: Long,
    val packageName: String,
    val label: String,
    val accent: Long,
    val createdAt: Long,
)
