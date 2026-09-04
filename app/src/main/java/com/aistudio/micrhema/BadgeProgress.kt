package com.aistudio.micrhema

private const val LEVEL_MISSION_BASELINE_PREFIX = "__level_mission_baseline__:"
private const val BASELINE_IBR_COURSES = "completed_ibr_courses"

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
    val nextLevel: BiblicalBadge?,
    val progressToNextLevel: Float
)

private fun MemberRequest.activityCount(key: String): Int =
    badgeActivityIds[key].orEmpty().distinct().size

private fun completedIbrCourseCount(): Int =
    ibrCoursesState.count { course ->
        course.chapters.isNotEmpty() && course.chapters.all { chapter ->
            ibrProgressState.any { progress ->
                progress.courseId == course.id &&
                    progress.chapterId == chapter.id &&
                    progress.isCompleted
            }
        }
    }

private fun rawActivityCounts(member: MemberRequest): Map<String, Int> = mapOf(
    BadgeActivityKeys.PLANS to member.activityCount(BadgeActivityKeys.PLANS),
    BadgeActivityKeys.PLAN_THEMES to member.activityCount(BadgeActivityKeys.PLAN_THEMES),
    BadgeActivityKeys.BOOKS to member.activityCount(BadgeActivityKeys.BOOKS),
    BadgeActivityKeys.VIDEOS to member.activityCount(BadgeActivityKeys.VIDEOS),
    BadgeActivityKeys.BIBLE_CHAPTERS to member.activityCount(BadgeActivityKeys.BIBLE_CHAPTERS),
    BadgeActivityKeys.BIBLE_NEWS to member.activityCount(BadgeActivityKeys.BIBLE_NEWS),
    BadgeActivityKeys.DEVOTIONALS to member.activityCount(BadgeActivityKeys.DEVOTIONALS),
    BadgeActivityKeys.AUDIOS to member.activityCount(BadgeActivityKeys.AUDIOS)
)

private fun missionBaselineKey(badgeId: String): String =
    "$LEVEL_MISSION_BASELINE_PREFIX$badgeId"

private fun storedNextLevel(member: MemberRequest): BiblicalBadge? {
    val storedIds = member.unlockedBadgeIds
        .ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }
        .toSet()
    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels
        .filter { it.id in storedIds }
        .maxOfOrNull { it.level ?: 1 } ?: 1
    return orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }
}

private fun currentMissionSnapshot(
    member: MemberRequest,
    includeIbrCourses: Boolean = true
): Map<String, Int> = buildMap {
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

/**
 * Cria o ponto zero do próximo nível usando exatamente o estado atual do usuário.
 * Assim, tudo que foi feito em níveis anteriores continua no histórico geral, mas não
 * preenche automaticamente nenhuma missão nova.
 */
fun ensureCurrentLevelMissionBaseline(
    member: MemberRequest,
    allowIbrDependentBaseline: Boolean = false
): MemberRequest {
    val nextLevel = storedNextLevel(member) ?: return member
    val baselineKey = missionBaselineKey(nextLevel.id)
    val existing = readMissionBaseline(member, nextLevel.id)

    val needsIbrCourseBaseline =
        nextLevel.id == "mestre_da_palavra" &&
            existing?.containsKey(BASELINE_IBR_COURSES) != true

    if (existing != null && (!needsIbrCourseBaseline || !allowIbrDependentBaseline)) {
        return member
    }

    val snapshot = if (existing == null) {
        currentMissionSnapshot(
            member = member,
            includeIbrCourses = nextLevel.id != "mestre_da_palavra" || allowIbrDependentBaseline
        ).toMutableMap()
    } else {
        existing.toMutableMap()
    }

    if (needsIbrCourseBaseline && allowIbrDependentBaseline) {
        snapshot[BASELINE_IBR_COURSES] = completedIbrCourseCount()
    }

    val updatedActivities = member.badgeActivityIds.toMutableMap()
    updatedActivities[baselineKey] = snapshot
        .toSortedMap()
        .map { (key, value) -> "$key=$value" }

    return member.copy(badgeActivityIds = updatedActivities)
}

private data class LevelMissionCounters(
    val counts: Map<String, Int>,
    val activeMinutes: Int,
    val completedIbrCourses: Int
) {
    val totalActivities: Int get() = counts.values.sum()
}

private fun levelMissionCounters(
    member: MemberRequest,
    nextLevel: BiblicalBadge?
): LevelMissionCounters {
    val rawCounts = rawActivityCounts(member)
    val rawActiveMinutes = member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    val rawCompletedCourses = completedIbrCourseCount()

    if (nextLevel == null) {
        return LevelMissionCounters(
            counts = rawCounts.mapValues { 0 },
            activeMinutes = 0,
            completedIbrCourses = 0
        )
    }

    val baseline = readMissionBaseline(member, nextLevel.id)
        ?: currentMissionSnapshot(member)

    val deltaCounts = rawCounts.mapValues { (key, current) ->
        (current - (baseline[key] ?: current)).coerceAtLeast(0)
    }
    return LevelMissionCounters(
        counts = deltaCounts,
        activeMinutes = (rawActiveMinutes - (baseline[BadgeActivityKeys.ACTIVE_MINUTES] ?: rawActiveMinutes))
            .coerceAtLeast(0),
        completedIbrCourses = (rawCompletedCourses - (baseline[BASELINE_IBR_COURSES] ?: rawCompletedCourses))
            .coerceAtLeast(0)
    )
}

private fun Int?.orZero(): Int = this ?: 0

private fun meetsLevelRequirements(
    badgeId: String,
    counters: LevelMissionCounters
): Boolean = when (badgeId) {
    "semeador" ->
        counters.counts[BadgeActivityKeys.DEVOTIONALS].orZero() >= 3 &&
            counters.counts[BadgeActivityKeys.PLAN_THEMES].orZero() >= 1

    "discipulo" ->
        counters.counts[BadgeActivityKeys.PLANS].orZero() >= 1 &&
            counters.counts[BadgeActivityKeys.PLAN_THEMES].orZero() >= 3 &&
            counters.counts[BadgeActivityKeys.BIBLE_CHAPTERS].orZero() >= 3

    "perseverante" ->
        counters.activeMinutes >= 60 &&
            counters.totalActivities >= 10

    "estudante_rhema" ->
        counters.counts[BadgeActivityKeys.BOOKS].orZero() >= 3 &&
            counters.counts[BadgeActivityKeys.VIDEOS].orZero() >= 3 &&
            counters.counts[BadgeActivityKeys.AUDIOS].orZero() >= 2

    "mestre_da_palavra" ->
        counters.completedIbrCourses >= 1 &&
            counters.counts[BadgeActivityKeys.BIBLE_NEWS].orZero() >= 3 &&
            counters.counts[BadgeActivityKeys.BIBLE_CHAPTERS].orZero() >= 10

    "guardiao_da_fe" ->
        counters.counts.values.all { it >= 1 } &&
            counters.activeMinutes >= 180

    else -> false
}

private fun levelProgress(
    badgeId: String,
    counters: LevelMissionCounters
): Float = when (badgeId) {
    "semeador" -> minOf(
        counters.counts[BadgeActivityKeys.DEVOTIONALS].orZero() / 3f,
        counters.counts[BadgeActivityKeys.PLAN_THEMES].orZero() / 1f
    )

    "discipulo" -> minOf(
        counters.counts[BadgeActivityKeys.PLANS].orZero() / 1f,
        counters.counts[BadgeActivityKeys.PLAN_THEMES].orZero() / 3f,
        counters.counts[BadgeActivityKeys.BIBLE_CHAPTERS].orZero() / 3f
    )

    "perseverante" -> minOf(
        counters.activeMinutes / 60f,
        counters.totalActivities / 10f
    )

    "estudante_rhema" -> minOf(
        counters.counts[BadgeActivityKeys.BOOKS].orZero() / 3f,
        counters.counts[BadgeActivityKeys.VIDEOS].orZero() / 3f,
        counters.counts[BadgeActivityKeys.AUDIOS].orZero() / 2f
    )

    "mestre_da_palavra" -> minOf(
        counters.completedIbrCourses / 1f,
        counters.counts[BadgeActivityKeys.BIBLE_NEWS].orZero() / 3f,
        counters.counts[BadgeActivityKeys.BIBLE_CHAPTERS].orZero() / 10f
    )

    "guardiao_da_fe" -> minOf(
        counters.counts.values.minOrNull()?.toFloat() ?: 0f,
        counters.activeMinutes / 180f
    )

    else -> 0f
}.coerceIn(0f, 1f)

/**
 * Calcula o progresso usando somente atividades feitas depois que o nível atual
 * começou. O histórico geral continua intacto e deduplicado, porém cada novo nível
 * recebe seu próprio ponto zero.
 */
fun calculateBadgeProgress(member: MemberRequest): BadgeProgressSummary {
    val totalLessons = ibrCoursesState.sumOf { it.chapters.size }
    val completedLessons = ibrProgressState.count { it.isCompleted }
    val totalCourses = ibrCoursesState.count { it.chapters.isNotEmpty() }
    val completedCourses = completedIbrCourseCount()

    val counts = rawActivityCounts(member)
    val activeMinutes = member.activityCount(BadgeActivityKeys.ACTIVE_MINUTES)
    val storedIds = member.unlockedBadgeIds
        .ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }
        .toMutableSet()

    val currentTarget = storedNextLevel(member)
    val currentCounters = levelMissionCounters(member, currentTarget)

    if (currentTarget != null && meetsLevelRequirements(currentTarget.id, currentCounters)) {
        storedIds.add(currentTarget.id)
    }

    val orderedLevels = biblicalLevelBadges.sortedBy { it.level }
    val highestUnlockedLevel = orderedLevels
        .filter { it.id in storedIds }
        .maxOfOrNull { it.level ?: 1 } ?: 1
    val nextLevel = orderedLevels.firstOrNull { (it.level ?: 1) > highestUnlockedLevel }

    val nextCounters = if (nextLevel?.id == currentTarget?.id) {
        currentCounters
    } else {
        LevelMissionCounters(
            counts = counts.mapValues { 0 },
            activeMinutes = 0,
            completedIbrCourses = 0
        )
    }

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
        nextLevel = nextLevel,
        progressToNextLevel = nextLevel?.let { levelProgress(it.id, nextCounters) } ?: 1f
    )
}
