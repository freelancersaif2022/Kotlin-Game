package android.net
class Uri private constructor(private val s: String) {
    val scheme: String? get() = s.substringBefore(':', "")
    val path: String? get() = s.substringAfter("://")
    override fun toString() = s
    companion object { @JvmStatic fun parse(s: String) = Uri(s); @JvmStatic fun fromFile(f: java.io.File) = Uri("file://" + f.absolutePath) }
}
