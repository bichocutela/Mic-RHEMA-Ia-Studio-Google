package com.aistudio.micrhema

private const val LEVEL_MISSION_BASELINE_PREFIX = "__level_mission_baseline__:"
private const val BASELINE_IBR_COURSES = "completed_ibr_courses"

private val coreLevelActivityKeys = listOf(
    BadgeActivityKeys.PLANS,
    BadgeActivityKeys.PLAN_THEMES,
    BadgeActivityKeys.BOOKS,
    BadgeActivityKeys.VIDEOS,
    BadgeActivityKeys.BIBLE_CHAPTERS,
    BadgeActivityKeys.BIBLE_NEWS,
    BadgeActivityKeys.DEVOTIONALS,
    BadgeActivityKeys.AUDIOS
)
private val quizLevelActivityKeys = listOf(
    BadgeActivityKeys.QUIZ_CORRECT,
    BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT,
    BadgeActivityKeys.QUIZ_CORRECT_NO_HINT,
    BadgeActivityKeys.QUIZ_HARD_CORRECT
)
private val allLevelActivityKeys = coreLevelActivityKeys + quizLevelActivityKeys

data class BadgeProgressSummary(
    val unlockedIds: List<String>,
    val completedIbrLessons: Int,
    val totalIbrLessons: Int,
    val completedIbrCourses: Int,
    val totalIbrCourses: Int,
    val activityCounts: Map<String, Int>,
    val activeMinutes: Int,
    val levelActivityCounts: Map<String, Int>,
    val levelActiveMinutes: Int,
    val levelCompletedIbrCourses: Int,
    val totalXp: Int,
    val nextLevel: BiblicalBadge?,
    val progressToNextLevel: Float
)

private fun MemberRequest.activityCount(key: String): Int = badgeActivityIds[key].orEmpty().distinct().size

private fun completedIbrCourseCount(): Int =
    ibrCoursesState.count { course ->
        course.chapters.isNotEmpty() && course.chapters.all { chapter ->
            ibrProgressState.any { progress ->
                progress.courseId == course.id && progress.chapterId == chapter.id && progress.isCompleted
            }
        }
    }

private fun rawActivityCounts(member: MemberRequest): Map<String, Int> =
    allLevelActivityKeys.associateWith { key -> member.activityCount(key) }

private fun missionBaselineKey(badgeId: String): String = "$LEVEL_MISSION_BASELINE_PREFIX$badgeId"

private fun storedNextLevel(member: MemberRequest): BiblicalBadge? {
    val storedIds = member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }.toSet()
    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels.filter { it.id in storedIds }.maxOfOrNull { it.level ?: 1 } ?: 1
    return orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }
}

private fun currentMissionSnapshot(member: MemberRequest, includeIbrCourses: Boolean = true): Map<String, Int> = buildMap {
    putAll(rawActivityCounts(member))
    put(BadgeActivityKeys.ACTIVE_MINUTES, member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES))
    if (includeIbrCourses) put(BASELINE_IBR_COURSES, completedIbrCourseCount())
}

private fun readMissionBaseline(member: MemberRequest, badgeId: String): Map<String, Int>? {
    val values = member.badgeActivityIds[missionBaselineKey(badgeId)] ?: return null
    val parsed = values.mapNotNull { entry ->
        val separator = entry.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        val key = entry.substring(0, separator)
        val value = entry.substring(separator + 1).toIntOrNull() ?: return@mapNotNull null
        key to value
    }.toMap()
    return parsed.takeIf { it.isNotEmpty() }
}

/** Cada novo nível registra seu próprio ponto zero antes da primeira ação. */
fun ensureCurrentLevelMissionBaseline(
    member: MemberRequest,
    allowIbrDependentBaseline: Boolean = false
): MemberRequest {
    val nextLevel = storedNextLevel(member) ?: return member
    val baselineKey = missionBaselineKey(nextLevel.id)
    val existing = readMissionBaseline(member, nextLevel.id)
    val needsIbrCourseBaseline = nextLevel.id == "mestre_da_palavra" && existing?.containsKey(BASELINE_IBR_COURSES) != true

    if (existing != null && (!needsIbrCourseBaseline || !allowIbrDependentBaseline)) return member

    val snapshot = if (existing == null) {
        currentMissionSnapshot(member, includeIbrCourses = nextLevel.id != "mestre_da_palavra" || allowIbrDependentBaseline).toMutableMap()
    } else existing.toMutableMap()

    if (needsIbrCourseBaseline && allowIbrDependentBaseline) snapshot[BASELINE_IBR_COURSES] = completedIbrCourseCount()

    val updatedActivities = member.badgeActivityIds.toMutableMap()
    updatedActivities[baselineKey] = snapshot.toSortedMap().map { (key, value) -> "$key=$value" }
    return member.copy(badgeActivityIds = updatedActivities)
}

private data class LevelMissionCounters(
    val counts: Map<String, Int>,
    val activeMinutes: Int,
    val completedIbrCourses: Int,
    val totalXp: Int
) {
    val totalActivities: Int get() = coreLevelActivityKeys.sumOf { counts[it] ?: 0 }
    val usedCoreAreas: Int get() = coreLevelActivityKeys.count { (counts[it] ?: 0) >= 1 }
}

private fun levelMissionCounters(member: MemberRequest, nextLevel: BiblicalBadge?): LevelMissionCounters {
    val rawCounts = rawActivityCounts(member)
    val rawActiveMinutes = member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    val rawCompletedCourses = completedIbrCourseCount()
    val xp = BibleJourneyProgressTracker.totalXp(member)

    if (nextLevel == null) return LevelMissionCounters(rawCounts.mapValues { 0 }, 0, 0, xp)

    val baseline = readMissionBaseline(member, nextLevel.id) ?: currentMissionSnapshot(member)
    val deltaCounts = rawCounts.mapValues { (key, current) -> (current - (baseline[key] ?: current)).coerceAtLeast(0) }
    return LevelMissionCounters(
        counts = deltaCounts,
        activeMinutes = (rawActiveMinutes - (baseline[BadgeActivityKeys.ACTIVE_MINUTES] ?: rawActiveMinutes)).coerceAtLeast(0),
        completedIbrCourses = (rawCompletedCourses - (baseline[BASELINE_IBR_COURSES] ?: rawCompletedCourses)).coerceAtLeast(0),
        totalXp = xp
    )
}

private fun Int?.orZero(): Int = this ?: 0
private fun LevelMissionCounters.count(key: String): Int = counts[key].orZero()

private fun meetsLevelRequirements(badgeId: String, c: LevelMissionCounters): Boolean = when (badgeId) {
    "semeador" -> c.count(BadgeActivityKeys.DEVOTIONALS) >= 3 && c.count(BadgeActivityKeys.PLAN_THEMES) >= 1
    "discipulo" -> c.count(BadgeActivityKeys.PLANS) >= 1 && c.count(BadgeActivityKeys.PLAN_THEMES) >= 3 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 3
    "perseverante" -> c.activeMinutes >= 60 && c.totalActivities >= 10
    "estudante_rhema" -> c.count(BadgeActivityKeys.BOOKS) >= 3 && c.count(BadgeActivityKeys.VIDEOS) >= 3 && c.count(BadgeActivityKeys.AUDIOS) >= 2
    "mestre_da_palavra" -> c.completedIbrCourses >= 1 && c.count(BadgeActivityKeys.BIBLE_NEWS) >= 3 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 10
    "guardiao_da_fe" -> c.usedCoreAreas >= coreLevelActivityKeys.size && c.activeMinutes >= 180
    "semente_da_fe" -> c.totalXp >= 100 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 2 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 2
    "caminho_da_promessa" -> c.totalXp >= 200 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 3 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 3
    "escudo_da_fe" -> c.totalXp >= 350 && c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT) >= 5 && c.activeMinutes >= 10
    "aguas_vivas" -> c.totalXp >= 500 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 5 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 5
    "videira_verdadeira" -> c.totalXp >= 650 && c.count(BadgeActivityKeys.DEVOTIONALS) >= 2 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 6
    "luz_do_mundo" -> c.totalXp >= 850 && c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT) >= 8 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 5
    "armadura_de_deus" -> c.totalXp >= 1050 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 10 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 3 && c.activeMinutes >= 15
    "leao_de_juda" -> c.totalXp >= 1250 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 10 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 10 && c.activeMinutes >= 20
    "chama_do_espirito" -> c.totalXp >= 1450 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 5 && c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT) >= 10
    "coroa_da_vida" -> c.totalXp >= 1650 && c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT) >= 12 && c.activeMinutes >= 30
    "asas_da_promessa" -> c.totalXp >= 1850 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 15 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 8 && c.activeMinutes >= 30
    "tabernaculo" -> c.totalXp >= 2050 && c.usedCoreAreas >= coreLevelActivityKeys.size && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 10
    "arca_da_alianca" -> c.totalXp >= 2300 && c.count(BadgeActivityKeys.BIBLE_CHAPTERS) >= 20 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 12 && c.activeMinutes >= 45
    "nova_jerusalem" -> c.totalXp >= 2600 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 20 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 15 && c.activeMinutes >= 60
    "gloria_eterna" -> c.totalXp >= 3000 && c.count(BadgeActivityKeys.QUIZ_CORRECT) >= 30 && c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT) >= 20 && c.activeMinutes >= 120
    else -> false
}

private fun ratio(current: Int, target: Int): Float = if (target <= 0) 1f else current / target.toFloat()

private fun levelProgress(badgeId: String, c: LevelMissionCounters): Float = when (badgeId) {
    "semeador" -> minOf(ratio(c.count(BadgeActivityKeys.DEVOTIONALS), 3), ratio(c.count(BadgeActivityKeys.PLAN_THEMES), 1))
    "discipulo" -> minOf(ratio(c.count(BadgeActivityKeys.PLANS), 1), ratio(c.count(BadgeActivityKeys.PLAN_THEMES), 3), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 3))
    "perseverante" -> minOf(ratio(c.activeMinutes, 60), ratio(c.totalActivities, 10))
    "estudante_rhema" -> minOf(ratio(c.count(BadgeActivityKeys.BOOKS), 3), ratio(c.count(BadgeActivityKeys.VIDEOS), 3), ratio(c.count(BadgeActivityKeys.AUDIOS), 2))
    "mestre_da_palavra" -> minOf(ratio(c.completedIbrCourses, 1), ratio(c.count(BadgeActivityKeys.BIBLE_NEWS), 3), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 10))
    "guardiao_da_fe" -> minOf(ratio(c.usedCoreAreas, coreLevelActivityKeys.size), ratio(c.activeMinutes, 180))
    "semente_da_fe" -> minOf(ratio(c.totalXp, 100), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 2), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 2))
    "caminho_da_promessa" -> minOf(ratio(c.totalXp, 200), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 3), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 3))
    "escudo_da_fe" -> minOf(ratio(c.totalXp, 350), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT), 5), ratio(c.activeMinutes, 10))
    "aguas_vivas" -> minOf(ratio(c.totalXp, 500), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 5), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 5))
    "videira_verdadeira" -> minOf(ratio(c.totalXp, 650), ratio(c.count(BadgeActivityKeys.DEVOTIONALS), 2), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 6))
    "luz_do_mundo" -> minOf(ratio(c.totalXp, 850), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT), 8), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 5))
    "armadura_de_deus" -> minOf(ratio(c.totalXp, 1050), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 10), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 3), ratio(c.activeMinutes, 15))
    "leao_de_juda" -> minOf(ratio(c.totalXp, 1250), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 10), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 10), ratio(c.activeMinutes, 20))
    "chama_do_espirito" -> minOf(ratio(c.totalXp, 1450), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 5), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT), 10))
    "coroa_da_vida" -> minOf(ratio(c.totalXp, 1650), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT), 12), ratio(c.activeMinutes, 30))
    "asas_da_promessa" -> minOf(ratio(c.totalXp, 1850), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 15), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 8), ratio(c.activeMinutes, 30))
    "tabernaculo" -> minOf(ratio(c.totalXp, 2050), ratio(c.usedCoreAreas, coreLevelActivityKeys.size), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 10))
    "arca_da_alianca" -> minOf(ratio(c.totalXp, 2300), ratio(c.count(BadgeActivityKeys.BIBLE_CHAPTERS), 20), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 12), ratio(c.activeMinutes, 45))
    "nova_jerusalem" -> minOf(ratio(c.totalXp, 2600), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 20), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 15), ratio(c.activeMinutes, 60))
    "gloria_eterna" -> minOf(ratio(c.totalXp, 3000), ratio(c.count(BadgeActivityKeys.QUIZ_CORRECT), 30), ratio(c.count(BadgeActivityKeys.QUIZ_HARD_CORRECT), 20), ratio(c.activeMinutes, 120))
    else -> 0f
}.coerceIn(0f, 1f)

fun calculateBadgeProgress(member: MemberRequest): BadgeProgressSummary {
    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }
    val completedLessons = ibrProgressState.count { it.isCompleted }
    val totalCourses = ibrCoursesState.count { it.chapters.isNotEmpty() }
    val completedCourses = completedIbrCourseCount()
    val counts = rawActivityCounts(member)
    val activeMinutes = member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    val totalXp = BibleJourneyProgressTracker.totalXp(member)
    val storedIds = member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }.toMutableSet()

    val currentTarget = storedNextLevel(member)
    val currentCounters = levelMissionCounters(member, currentTarget)
    if (currentTarget != null && meetsLevelRequirements(currentTarget.id, currentCounters)) storedIds.add(currentTarget.id)

    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels.filter { it.id in storedIds }.maxOfOrNull { it.level ?: 1 } ?: 1
    val nextLevel = orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }
    val nextCounters = if (nextLevel?.id == currentTarget?.id) currentCounters else LevelMissionCounters(counts.mapValues { 0 }, 0, 0, totalXp)

    return BadgeProgressSummary(
        unlockedIds = storedIds.toList(),
        completedIbrLessons = completedLessons,
        totalIbrLessons = totalLessons,
        completedIbrCourses = completedCourses,
        totalIbrCourses = totalCourses,
        activityCounts = counts,
        activeMinutes = activeMinutes,
        levelActivityCounts = nextCounters.counts,
        levelActiveMinutes = nextCounters.activeMinutes,
        levelCompletedIbrCourses = nextCounters.completedIbrCourses,
        totalXp = totalXp,
        nextLevel = nextLevel,
        progressToNextLevel = nextLevel?.let { levelProgress(it.id, nextCounters) } ?: 1f
    )
}
