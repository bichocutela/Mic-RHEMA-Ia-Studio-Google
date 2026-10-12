package com.aistudio.micrhema

/** Numeric release comparison: 2.0.1.100 is newer than 2.0.1.99. */
internal object ForcedUpdatePolicy {
    fun parts(version: String): List<Long>? {
        val value = version.trim().removePrefix("v")
        if (!Regex("[0-9]+(\\.[0-9]+)*").matches(value)) return null
        return value.split('.').map { it.toLongOrNull() ?: return null }
    }
    fun compare(left: String, right: String): Int? {
        val a = parts(left) ?: return null
        val b = parts(right) ?: return null
        for (i in 0 until maxOf(a.size, b.size)) {
            val result = a.getOrElse(i) { 0L }.compareTo(b.getOrElse(i) { 0L })
            if (result != 0) return result
        }
        return 0
    }
    fun required(enabled: Boolean, minimum: String, current: String, admin: Boolean): Boolean =
        enabled && !admin && (compare(current, minimum)?.let { it < 0 } == true)
}
