import java.time.LocalTime

fun main() {
    val times = listOf("19:00", "19:30", "09:00", " 19h30 ", "19", "invalid")
    for (time in times) {
        var parsedTime = LocalTime.of(23, 59)
        try {
            val cleanTime = time.replace("h", ":", ignoreCase = true).filter { it.isDigit() || it == ':' }
            val timeParts = cleanTime.split(":")
            if (timeParts.size >= 2) {
                parsedTime = LocalTime.of(timeParts[0].toInt(), timeParts[1].take(2).toInt())
            }
        } catch (e: Exception) {
            
        }
        println("$time -> $parsedTime")
    }
}
