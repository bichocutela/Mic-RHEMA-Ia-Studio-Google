import java.lang.reflect.Method

fun main() {
    try {
        val clazz = Class.forName("androidx.compose.material.icons.outlined.IconsKt")
        val methods = clazz.declaredMethods
        for (method in methods) {
            val name = method.name
            if (name.contains("Hand", ignoreCase = true) || name.contains("Pray", ignoreCase = true)) {
                println(name)
            }
        }
    } catch (e: Exception) {
        println(e)
    }
}
