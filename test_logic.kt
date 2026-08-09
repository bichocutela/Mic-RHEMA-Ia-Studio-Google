import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek

data class Service(val day: String, val time: String)

fun main() {
    val weeklyServicesState = listOf(
        Service("Domingo", "09:00"),
        Service("Quarta", "19:30"),
        Service("Domingo", "18:00")
    )
    
    val today = LocalDate.now()
    val currentTime = LocalTime.now()
    
    val dayMap = mapOf(
        "Domingo" to DayOfWeek.SUNDAY,
        "Segunda" to DayOfWeek.MONDAY,
        "Segunda-feira" to DayOfWeek.MONDAY,
        "Terça" to DayOfWeek.TUESDAY,
        "Terça-feira" to DayOfWeek.TUESDAY,
        "Quarta" to DayOfWeek.WEDNESDAY,
        "Quarta-feira" to DayOfWeek.WEDNESDAY,
        "Quinta" to DayOfWeek.THURSDAY,
        "Quinta-feira" to DayOfWeek.THURSDAY,
        "Sexta" to DayOfWeek.FRIDAY,
        "Sexta-feira" to DayOfWeek.FRIDAY,
        "Sábado" to DayOfWeek.SATURDAY
    )
    
    val currentDayOfWeek = today.dayOfWeek
    val validServices = weeklyServicesState.sortedBy { service ->
        val serviceDay = dayMap[service.day] ?: DayOfWeek.SUNDAY
        var diff = serviceDay.value - currentDayOfWeek.value
        
        var serviceTime = java.time.LocalTime.MAX
        try {
            val cleanTime = service.time.replace(Regex("[^0-9:]"), "")
            val formattedTime = if (cleanTime.length == 4 && cleanTime.contains(":")) "0$cleanTime" else cleanTime.take(5)
            if (formattedTime.length == 5) {
                serviceTime = java.time.LocalTime.parse(formattedTime)
            }
        } catch (e: Exception) {}
        
        if (diff < 0) {
            diff += 7
        } else if (diff == 0) {
            if (serviceTime.isBefore(currentTime)) {
                diff += 7
            }
        }
        
        today.plusDays(diff.toLong()).atTime(serviceTime)
    }.take(3)
    
    println("Today: $today, Now: $currentTime, Day: $currentDayOfWeek")
    validServices.forEach { println(it) }
}
