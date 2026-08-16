package com.aistudio.micrhema

data class BadgeProgressSummary(
    val unlockedIds: List<String>,
    val completedIbrLessons: Int,
    val totalIbrLessons: Int,
    val completedIbrCourses: Int,
    val totalIbrCourses: Int,
    val nextLevel: BiblicalBadge?,
    val progressToNextLevel: Float
)

/**
 * Calcula somente conquistas baseadas em dados que o aplicativo já acompanha.
 * Conquistas de oração, gratidão e participação manual permanecem disponíveis
 * para uma etapa posterior com registro próprio.
 */
fun calculateBadgeProgress(member: MemberRequest): BadgeProgressSummary {
    val storedIds = member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }.toMutableSet()
    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }
    val completedLessons = ibrProgressState.count { it.isCompleted }
    val totalCourses = ibrCoursesState.count { it.chapters.isNotEmpty() }
    val completedCourses = ibrCoursesState.count { course ->
        course.chapters.isNotEmpty() && course.chapters.all { chapter ->
            ibrProgressState.any { progress ->
                progress.courseId == course.id && progress.chapterId == chapter.id && progress.isCompleted
            }
        }
    }

    if (completedLessons >= 7) storedIds.add("constante")
    if (completedLessons >= 10) storedIds.add("leitor_da_palavra")
    if (completedCourses >= 1) storedIds.add("estudante_rhema")
    if (completedCourses >= 3) storedIds.add("mestre_da_palavra")

    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels
        .filter { it.id in storedIds }
        .maxOfOrNull { it.level ?: 1 } ?: 1
    val nextLevel = orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }
    val progressToNext = when (nextLevel?.id) {
        "semeador" -> (completedLessons / 7f).coerceIn(0f, 1f)
        "discipulo" -> (completedLessons / 10f).coerceIn(0f, 1f)
        "perseverante" -> (completedLessons / 30f).coerceIn(0f, 1f)
        "estudante_rhema" -> (completedCourses / 1f).coerceIn(0f, 1f)
        "mestre_da_palavra" -> (completedCourses / 3f).coerceIn(0f, 1f)
        "guardiao_da_fe" -> if (highestUnlockedLevel >= 6) 1f else 0f
        else -> 1f
    }

    return BadgeProgressSummary(
        unlockedIds = storedIds.toList(),
        completedIbrLessons = completedLessons,
        totalIbrLessons = totalLessons,
        completedIbrCourses = completedCourses,
        totalIbrCourses = totalCourses,
        nextLevel = nextLevel,
        progressToNextLevel = progressToNext
    )
}
