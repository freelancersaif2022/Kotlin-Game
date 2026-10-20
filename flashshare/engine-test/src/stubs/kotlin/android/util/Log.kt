package android.util
object Log {
    @JvmStatic fun d(t: String, m: String) = 0
    @JvmStatic fun w(t: String, m: String, e: Throwable? = null): Int { System.err.println("W/$t: $m ${e ?: ""}"); return 0 }
}
