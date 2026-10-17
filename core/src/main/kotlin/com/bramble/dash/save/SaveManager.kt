package com.bramble.dash.save

/** Persistence backend. Android/desktop use LibGDX Preferences; tests use memory. */
interface SaveStore {
    fun read(key: String): String?
    /** Write all pairs and flush them as one unit. */
    fun write(values: Map<String, String>)
}

class MemoryStore : SaveStore {
    val map = HashMap<String, String>()
    override fun read(key: String) = map[key]
    override fun write(values: Map<String, String>) { map.putAll(values) }
}

class PreferencesStore(name: String) : SaveStore {
    private val prefs = com.badlogic.gdx.Gdx.app.getPreferences(name)
    override fun read(key: String): String? = if (prefs.contains(key)) prefs.getString(key) else null
    override fun write(values: Map<String, String>) {
        values.forEach { (k, v) -> prefs.putString(k, v) }
        prefs.flush()
    }
}

/**
 * Loads, repairs and commits [SaveData]. The whole state is serialized into one value, so every
 * commit replaces it in a single write; the previous good copy is kept as a backup and used if
 * the primary copy fails to parse.
 */
class SaveManager(private val store: SaveStore) {
    var data: SaveData = load()
        private set
    var recoveredFromBackup = false
        private set
    var resetToDefaults = false
        private set

    private fun load(): SaveData {
        val primary = store.read(KEY)
        SaveData.fromJson(primary)?.let { return it }
        val backup = SaveData.fromJson(store.read(BACKUP_KEY))
        if (backup != null) { recoveredFromBackup = primary != null; return backup }
        resetToDefaults = primary != null
        return SaveData().also { it.validate() }
    }

    /** Apply a change and persist it immediately. */
    inline fun transact(change: (SaveData) -> Unit) {
        change(data)
        commit()
    }

    fun commit() {
        data.validate()
        val json = data.toJson()
        val previous = store.read(KEY)
        val values = LinkedHashMap<String, String>()
        if (previous != null && SaveData.fromJson(previous) != null) values[BACKUP_KEY] = previous
        values[KEY] = json
        store.write(values)
    }

    fun resetProgress() {
        val keepSettings = data.settings
        data = SaveData().also {
            it.settings.musicVolume = keepSettings.musicVolume; it.settings.sfxVolume = keepSettings.sfxVolume
            it.settings.muted = keepSettings.muted; it.settings.vibration = keepSettings.vibration
            it.settings.quality = keepSettings.quality; it.settings.leftHanded = keepSettings.leftHanded
            it.settings.swipeControls = keepSettings.swipeControls
            it.validate()
        }
        commit()
    }

    companion object {
        const val KEY = "save"
        const val BACKUP_KEY = "save_backup"
        const val PREFS_NAME = "com.bramble.dash.save"
    }
}
