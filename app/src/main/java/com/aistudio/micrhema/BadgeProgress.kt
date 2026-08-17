package com.aistudio.micrhema

data class BadgeProgressSummary(
    val unlockedIds: List<String>,
    val completedIbrLessons: Int,
    val totalIbrLessons: Int,
    val completedIbrCourses: Int,
    val totalIbrCourses: Int,
    val activityCounts: Map<String, Int>,
    val activeMinutes: Int,
    val nextLevel: BiblicalBadge?,
    val progressToNextLevel: Float
)

private fun MemberRequest.activityCount(key: String): Int = badgeActivityIds[key].orEmpty().distinct().size

/**
 * Calcula o progresso usando eventos reais e deduplicados do usuário.
 * Conteúdos são contados por ID, portanto abrir o mesmo item várias vezes não cria progresso falso.
 */
fun calculateBadgeProgress(member: MemberRequest): BadgeProgressSummary {
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

    val counts = mapOf(
        BadgeActivityKeys.PLANS to member.activityCount(BadgeActivityKeys.PLANS),
        BadgeActivityKeys.PLAN_THEMES to member.activityCount(BadgeActivityKeys.PLAN_THEMES),
        BadgeActivityKeys.BOOKS to member.activityCount(BadgeActivityKeys.BOOKS),
        BadgeActivityKeys.VIDEOS to member.activityCount(BadgeActivityKeys.VIDEOS),
        BadgeActivityKeys.BIBLE_CHAPTERS to member.activityCount(BadgeActivityKeys.BIBLE_CHAPTERS),
        BadgeActivityKeys.BIBLE_NEWS to member.activityCount(BadgeActivityKeys.BIBLE_NEWS),
        BadgeActivityKeys.DEVOTIONALS to member.activityCount(BadgeActivityKeys.DEVOTIONALS),
        BadgeActivityKeys.AUDIOS to member.activityCount(BadgeActivityKeys.AUDIOS)
    )
    val activeMinutes = member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    val storedIds = member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }.toMutableSet()

    val semeador = counts[BadgeActivityKeys.DEVOTIONALS]!! >= 3 && counts[BadgeActivityKeys.PLAN_THEMES]!! >= 1
    val discipulo = counts[BadgeActivityKeys.PLANS]!! >= 1 && counts[BadgeActivityKeys.PLAN_THEMES]!! >= 3 && counts[BadgeActivityKeys.BIBLE_CHAPTERS]!! >= 3
    val perseverante = activeMinutes >= 60 && counts.values.sum() >= 10
    val estudante = counts[BadgeActivityKeys.BOOKS]!! >= 3 && counts[BadgeActivityKeys.VIDEOS]!! >= 3 && counts[BadgeActivityKeys.AUDIOS]!! >= 2
    val mestre = completedCourses >= 1 && counts[BadgeActivityKeys.BIBLE_NEWS]!! >= 3 && counts[BadgeActivityKeys.BIBLE_CHAPTERS]!! >= 10
    val guardiao = mestre && counts.values.all { it >= 1 } && activeMinutes >= 180

    if (semeador) storedIds.add("semeador")
    if (discipulo) storedIds.add("discipulo")
    if (perseverante) storedIds.add("perseverante")
    if (estudante) storedIds.add("estudante_rhema")
    if (mestre) storedIds.add("mestre_da_palavra")
    if (guardiao) storedIds.add("guardiao_da_fe")

    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels.filter { it.id in storedIds }.maxOfOrNull { it.level ?: 1 } ?: 1
    val nextLevel = orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }
    val progressToNext = when (nextLevel?.id) {
        "semeador" -> minOf(counts[BadgeActivityKeys.DEVOTIONALS]!! / 3f, counts[BadgeActivityKeys.PLAN_THEMES]!! / 1f)
        "discipulo" -> minOf(counts[BadgeActivityKeys.PLANS]!! / 1f, counts[BadgeActivityKeys.PLAN_THEMES]!! / 3f, counts[BadgeActivityKeys.BIBLE_CHAPTERS]!! / 3f)
        "perseverante" -> minOf(activeMinutes / 60f, counts.values.sum() / 10f)
        "estudante_rhema" -> minOf(counts[BadgeActivityKeys.BOOKS]!! / 3f, counts[BadgeActivityKeys.VIDEOS]!! / 3f, counts[BadgeActivityKeys.AUDIOS]!! / 2f)
        "mestre_da_palavra" -> minOf(completedCourses / 1f, counts[BadgeActivityKeys.BIBLE_NEWS]!! / 3f, counts[BadgeActivityKeys.BIBLE_CHAPTERS]!! / 10f)
        "guardiao_da_fe" -> minOf(1f, counts.values.minOrNull()?.toFloat() ?: 0f, activeMinutes / 180f)
        else -> 1f
    }.coerceIn(0f, 1f)

    return BadgeProgressSummary(
        unlockedIds = storedIds.toList(),
        completedIbrLessons = completedLessons,
        totalIbrLessons = totalLessons,
        completedIbrCourses = completedCourses,
        totalIbrCourses = totalCourses,
        activityCounts = counts,
        activeMinutes = activeMinutes,
        nextLevel = nextLevel,
        progressToNextLevel = progressToNext
    )
}
