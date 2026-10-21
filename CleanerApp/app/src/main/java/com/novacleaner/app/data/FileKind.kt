package com.novacleaner.app.data

enum class FileKind(val label: String) {
    VIDEO("Videos"), IMAGE("Images"), AUDIO("Audio"), DOCUMENT("Docs"), ARCHIVE("Archives"), APK("APKs"), OTHER("Other");

    val storageCategory: StorageCategory
        get() = when (this) {
            VIDEO -> StorageCategory.VIDEOS
            IMAGE -> StorageCategory.IMAGES
            AUDIO -> StorageCategory.AUDIO
            DOCUMENT, ARCHIVE -> StorageCategory.DOCUMENTS
            APK -> StorageCategory.APKS
            OTHER -> StorageCategory.OTHER
        }

    companion object {
        private val docExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv", "epub")
        private val archiveExt = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "obb")

        fun from(mime: String?, name: String): FileKind {
            val m = mime.orEmpty()
            val ext = name.substringAfterLast('.', "").lowercase()
            return when {
                ext == "apk" || ext == "xapk" || ext == "apks" || m == "application/vnd.android.package-archive" -> APK
                m.startsWith("video/") -> VIDEO
                m.startsWith("image/") -> IMAGE
                m.startsWith("audio/") -> AUDIO
                ext in archiveExt || m.contains("zip") || m.contains("compressed") -> ARCHIVE
                ext in docExt || m.startsWith("text/") || m.contains("pdf") || m.contains("document") || m.contains("msword") || m.contains("sheet") || m.contains("presentation") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
