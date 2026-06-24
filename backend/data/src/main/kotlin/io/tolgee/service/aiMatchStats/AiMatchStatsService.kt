package io.tolgee.service.aiMatchStats

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.LockingProvider
import io.tolgee.dtos.aiMatchStats.AiMatchBucketView
import io.tolgee.dtos.aiMatchStats.AiMatchLangInfo
import io.tolgee.dtos.aiMatchStats.AiMatchLangView
import io.tolgee.dtos.aiMatchStats.AiMatchLanguagesView
import io.tolgee.dtos.aiMatchStats.AiMatchPromptView
import io.tolgee.dtos.aiMatchStats.AiMatchPromptsView
import io.tolgee.dtos.aiMatchStats.AiMatchSummaryView
import io.tolgee.model.aiMatchStats.AiMatchRefreshState
import io.tolgee.repository.aiMatchStats.AiMatchRefreshStateRepository
import io.tolgee.service.queryBuilders.AiMatchStatsProvider
import io.tolgee.util.executeInNewRepeatableTransaction
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Service
@Transactional
class AiMatchStatsService(
  private val entityManager: EntityManager,
  private val lockingProvider: LockingProvider,
  private val platformTransactionManager: PlatformTransactionManager,
  private val currentDateProvider: CurrentDateProvider,
  private val refreshStateRepository: AiMatchRefreshStateRepository,
) {
  private val provider get() = AiMatchStatsProvider(entityManager)

  /**
   * Brings the materialized table up to date with activity newer than the stored watermark. Cheap
   * no-op when nothing new happened. Work is **bounded per call**: at most [MAX_TRANSLATIONS_PER_CALL]
   * translations are recomputed before returning, so a huge first-time backfill can never run an
   * unbounded request — it advances the watermark as far as it got and resumes on the next call.
   */
  fun ensureFresh(projectId: Long) {
    val maxRev = provider.getMaxRevisionId(projectId) ?: return
    val current = refreshStateRepository.findById(projectId).orElse(null)
    if (current != null && maxRev <= current.lastProcessedActivityRevisionId) return

    lockingProvider.withLocking("ai-match-refresh-$projectId") {
      executeInNewRepeatableTransaction(platformTransactionManager) tx@{
        val state = refreshStateRepository.findById(projectId).orElse(null)
        var watermark = state?.lastProcessedActivityRevisionId ?: 0L
        val currentMax = provider.getMaxRevisionId(projectId) ?: return@tx
        if (state != null && currentMax <= watermark) return@tx

        val resolver = PromptVersionResolver(provider.getPromptVersionBoundaries(projectId))
        var processed = 0
        while (processed < MAX_TRANSLATIONS_PER_CALL) {
          val slice = provider.getAffectedSince(projectId, watermark, REVISION_SCAN_LIMIT)
          val lastRevisionId = slice.lastRevisionId ?: break
          slice.translationIds.chunked(RECOMPUTE_CHUNK).forEach { chunk ->
            recomputeChunk(projectId, chunk, resolver)
          }
          watermark = lastRevisionId
          processed += slice.translationIds.size
        }

        val newState = state ?: AiMatchRefreshState(projectId)
        newState.lastProcessedActivityRevisionId = watermark
        newState.lastUpdatedAt = currentDateProvider.date
        refreshStateRepository.save(newState)
      }
    }
  }

  /** Whether the materialized table reflects all activity (false while a large backfill is catching up). */
  private fun isUpToDate(projectId: Long): Boolean {
    val maxRev = provider.getMaxRevisionId(projectId) ?: return true
    val watermark =
      refreshStateRepository.findById(projectId).orElse(null)?.lastProcessedActivityRevisionId ?: return false
    return maxRev <= watermark
  }

  /**
   * Recomputes a batch of translations entirely set-based: two batched reads (current rows, activity
   * rows), then one bulk upsert of the qualifying rows and one bulk delete of the rest. No
   * per-translation queries and no per-row inserts, so cost is O(chunks), not O(translations).
   */
  private fun recomputeChunk(
    projectId: Long,
    ids: List<Long>,
    resolver: PromptVersionResolver,
  ) {
    val currents = provider.getCurrentTranslations(projectId, ids)
    val activity = provider.getActivityRowsForTranslations(projectId, ids)

    val toUpsert = ArrayList<TranslationAiMatchRow>(ids.size)
    val toDelete = ArrayList<Long>()
    ids.forEach { id ->
      val row = computeMatch(projectId, id, currents[id], activity[id].orEmpty(), resolver)
      if (row == null) toDelete += id else toUpsert += row
    }

    provider.upsertMatches(toUpsert)
    provider.deleteMatches(toDelete)
  }

  private fun computeMatch(
    projectId: Long,
    translationId: Long,
    current: CurrentTranslationRow?,
    rows: List<AiMatchActivityRow>,
    resolver: PromptVersionResolver,
  ): TranslationAiMatchRow? {
    if (current == null || current.state != REVIEWED_STATE) return null
    val walk = AiTextReconstructor.walk(rows)
    val aiText = walk.aiText
    val finalText = current.text ?: ""
    val reviewedAt = walk.reviewedAt
    if (aiText == null || reviewedAt == null || (aiText.isEmpty() && finalText.isNotEmpty())) return null
    return TranslationAiMatchRow(
      translationId = translationId,
      projectId = projectId,
      languageId = current.languageId,
      matchScore = AiMatchScorer.score(aiText, finalText),
      reviewedAt = reviewedAt,
      wordCount = current.wordCount,
      promptId = walk.aiSource?.promptId,
      mtProvider = walk.aiSource?.mtProvider,
      promptVersionTs = resolver.resolve(walk.aiSource?.promptId, walk.aiSource?.producedAt),
    )
  }

  fun getSummary(
    projectId: Long,
    languages: List<AiMatchLangInfo>,
    reviewedAfter: Long?,
    reviewedBefore: Long?,
  ): AiMatchSummaryView {
    ensureFresh(projectId)
    val langIds = languages.map { it.id }
    val after = reviewedAfter.toAfter()
    val before = reviewedBefore.toBefore()
    val reviewed = provider.getReviewedPerLanguage(projectId, langIds, after, before)
    val notReviewed = provider.getNotReviewedPerLanguage(projectId, langIds)

    val reviewedWords = reviewed.sumOf { it.reviewedWords }
    val notReviewedWords = notReviewed.sumOf { it.words }

    return AiMatchSummaryView(
      projectId = projectId,
      reviewedAfter = reviewedAfter,
      reviewedBefore = reviewedBefore,
      generatedAt = generatedAt(projectId),
      upToDate = isUpToDate(projectId),
      b100 =
        AiMatchBucketView(
          reviewed.sumOf { it.b100Words },
          reviewed.sumOf { it.b100Keys },
          reviewed.count {
            it.b100Words >
              0
          },
        ),
      b9990 =
        AiMatchBucketView(
          reviewed.sumOf { it.b9990Words },
          reviewed.sumOf { it.b9990Keys },
          reviewed.count {
            it.b9990Words >
              0
          },
        ),
      b8980 =
        AiMatchBucketView(
          reviewed.sumOf { it.b8980Words },
          reviewed.sumOf { it.b8980Keys },
          reviewed.count {
            it.b8980Words >
              0
          },
        ),
      b7970 =
        AiMatchBucketView(
          reviewed.sumOf { it.b7970Words },
          reviewed.sumOf { it.b7970Keys },
          reviewed.count {
            it.b7970Words >
              0
          },
        ),
      bno =
        AiMatchBucketView(
          reviewed.sumOf { it.bnoWords },
          reviewed.sumOf { it.bnoKeys },
          reviewed.count {
            it.bnoWords >
              0
          },
        ),
      reviewedWords = reviewedWords,
      reviewedKeys = reviewed.sumOf { it.reviewedKeys },
      notReviewedWords = notReviewedWords,
      notReviewedKeys = notReviewed.sumOf { it.keys },
      langCount = reviewed.size,
      avgMatchScore = weightedAvg(reviewed.sumOf { it.weightedScoreSum }, reviewedWords),
      reviewedPct = pct(reviewedWords, reviewedWords + notReviewedWords),
    )
  }

  fun getLanguages(
    projectId: Long,
    languages: List<AiMatchLangInfo>,
    reviewedAfter: Long?,
    reviewedBefore: Long?,
  ): AiMatchLanguagesView {
    ensureFresh(projectId)
    val langIds = languages.map { it.id }
    val reviewed =
      provider
        .getReviewedPerLanguage(
          projectId,
          langIds,
          reviewedAfter.toAfter(),
          reviewedBefore.toBefore(),
        ).associateBy {
          it.languageId
        }
    val notReviewed = provider.getNotReviewedPerLanguage(projectId, langIds).associateBy { it.languageId }

    val perLang =
      languages.mapNotNull { info ->
        val r = reviewed[info.id]
        val nr = notReviewed[info.id]
        if (r == null && nr == null) return@mapNotNull null
        val reviewedWords = r?.reviewedWords ?: 0
        val notReviewedWords = nr?.words ?: 0
        val total = reviewedWords + notReviewedWords
        AiMatchLangView(
          tag = info.tag,
          name = info.name,
          flag = info.flag,
          total = total,
          b100 = r?.b100Words ?: 0,
          b9990 = r?.b9990Words ?: 0,
          b8980 = r?.b8980Words ?: 0,
          b7970 = r?.b7970Words ?: 0,
          bno = r?.bnoWords ?: 0,
          notReviewed = notReviewedWords,
          avgMatchScore = weightedAvg(r?.weightedScoreSum ?: 0, reviewedWords),
          b100Pct = pctOf(r?.b100Words ?: 0, total),
          b9990Pct = pctOf(r?.b9990Words ?: 0, total),
          b8980Pct = pctOf(r?.b8980Words ?: 0, total),
          b7970Pct = pctOf(r?.b7970Words ?: 0, total),
          bnoPct = pctOf(r?.bnoWords ?: 0, total),
          notReviewedPct = pctOf(notReviewedWords, total),
        )
      }
    return AiMatchLanguagesView(generatedAt(projectId), perLang)
  }

  fun getPrompts(
    projectId: Long,
    languages: List<AiMatchLangInfo>,
    reviewedAfter: Long?,
    reviewedBefore: Long?,
  ): AiMatchPromptsView {
    ensureFresh(projectId)
    val langIds = languages.map { it.id }
    val aggs = provider.getPerPrompt(projectId, langIds, reviewedAfter.toAfter(), reviewedBefore.toBefore())
    val notReviewed =
      provider.getNotReviewedPerPrompt(projectId, langIds).associateBy {
        PromptKey(it.promptId, it.mtProvider)
      }

    val versionsByPrompt =
      aggs
        .filter { it.promptId != null && it.promptVersionTs != null }
        .groupBy { it.promptId!! }
        .mapValues { (_, list) -> list.mapNotNull { it.promptVersionTs }.distinct().sorted() }

    val rows = mutableListOf<AiMatchPromptView>()
    val reviewedByPrompt = aggs.groupBy { PromptKey(it.promptId, it.mtProvider) }
    reviewedByPrompt.forEach { (key, group) ->
      // notReviewed is prompt-level, so attach it only to the prompt's newest version row.
      val latestVersionTs = group.maxByOrNull { it.promptVersionTs?.time ?: Long.MIN_VALUE }?.promptVersionTs
      group.forEach { a ->
        val nr = if (a.promptVersionTs == latestVersionTs) notReviewed[key] else null
        rows += promptRow(a, nr?.words ?: 0, nr?.keys ?: 0, versionsByPrompt)
      }
    }
    // Prompts whose AI cells are all still unreviewed have no reviewed row yet — surface them too.
    notReviewed.forEach { (key, nr) ->
      if (key !in reviewedByPrompt) rows += notReviewedOnlyPromptRow(key, nr)
    }

    val perPrompt = rows.sortedWith(compareBy({ it.promptId ?: Long.MAX_VALUE }, { -(it.versionTs ?: 0L) }))
    return AiMatchPromptsView(generatedAt(projectId), perPrompt)
  }

  private fun promptRow(
    a: PromptAgg,
    notReviewedWords: Long,
    notReviewedKeys: Long,
    versionsByPrompt: Map<Long, List<Date>>,
  ): AiMatchPromptView {
    val total = a.reviewedWords + notReviewedWords
    return AiMatchPromptView(
      promptId = a.promptId,
      promptName = promptName(a.promptId, a.mtProvider, a.promptName),
      provider = a.mtProvider,
      versionTs = a.promptVersionTs?.time,
      versionLabel = versionLabel(a.promptId, a.promptVersionTs, versionsByPrompt),
      total = total,
      reviewedWords = a.reviewedWords,
      reviewedKeys = a.reviewedKeys,
      notReviewed = notReviewedWords,
      notReviewedKeys = notReviewedKeys,
      b100 = a.b100Words,
      b9990 = a.b9990Words,
      b8980 = a.b8980Words,
      b7970 = a.b7970Words,
      bno = a.bnoWords,
      avgMatchScore = weightedAvg(a.weightedScoreSum, a.reviewedWords),
      b100Pct = pctOf(a.b100Words, total),
      b9990Pct = pctOf(a.b9990Words, total),
      b8980Pct = pctOf(a.b8980Words, total),
      b7970Pct = pctOf(a.b7970Words, total),
      bnoPct = pctOf(a.bnoWords, total),
      notReviewedPct = pctOf(notReviewedWords, total),
    )
  }

  private fun notReviewedOnlyPromptRow(
    key: PromptKey,
    nr: NotReviewedPromptAgg,
  ): AiMatchPromptView =
    AiMatchPromptView(
      promptId = key.promptId,
      promptName = promptName(key.promptId, key.provider, nr.promptName),
      provider = key.provider,
      versionTs = null,
      versionLabel = null,
      total = nr.words,
      reviewedWords = 0,
      reviewedKeys = 0,
      notReviewed = nr.words,
      notReviewedKeys = nr.keys,
      b100 = 0,
      b9990 = 0,
      b8980 = 0,
      b7970 = 0,
      bno = 0,
      avgMatchScore = 0,
      b100Pct = 0.0,
      b9990Pct = 0.0,
      b8980Pct = 0.0,
      b7970Pct = 0.0,
      bnoPct = 0.0,
      notReviewedPct = pctOf(nr.words, nr.words),
    )

  private data class PromptKey(
    val promptId: Long?,
    val provider: String?,
  )

  private fun promptName(
    promptId: Long?,
    mtProvider: String?,
    name: String?,
  ): String? {
    if (promptId == null) {
      if (mtProvider == PROMPT_PROVIDER) return "Default prompt"
      return null
    }
    return name ?: "Prompt #$promptId"
  }

  private fun versionLabel(
    promptId: Long?,
    versionTs: Date?,
    versionsByPrompt: Map<Long, List<Date>>,
  ): String? {
    if (promptId == null || versionTs == null) return null
    val versions = versionsByPrompt[promptId] ?: return null
    val ordinal = versions.indexOf(versionTs) + 1
    if (ordinal == 0) return null
    return "v$ordinal (${dateFormat().format(versionTs)})"
  }

  private fun generatedAt(projectId: Long): Long? =
    refreshStateRepository
      .findById(projectId)
      .orElse(null)
      ?.lastUpdatedAt
      ?.time

  private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd").apply { timeZone = TimeZone.getTimeZone("UTC") }

  private fun Long?.toAfter(): Date = this?.let { Date(it) } ?: Date(0)

  private fun Long?.toBefore(): Date = this?.let { Date(it) } ?: Date(FAR_FUTURE_MILLIS)

  private fun weightedAvg(
    weightedSum: Long,
    words: Long,
  ): Int {
    if (words == 0L) return 0
    return (weightedSum.toDouble() / words).roundToInt()
  }

  private fun pct(
    part: Long,
    total: Long,
  ): Int {
    if (total == 0L) return 0
    return (100.0 * part / total).roundToInt()
  }

  private fun pctOf(
    part: Long,
    total: Long,
  ): Double {
    if (total == 0L) return 0.0
    return (1000.0 * part / total).roundToLong() / 10.0
  }

  companion object {
    private const val REVIEWED_STATE = 2
    private const val PROMPT_PROVIDER = "PROMPT"
    private const val RECOMPUTE_CHUNK = 5000

    /** Caps the work one stats request will do; a bigger backfill resumes on the next call. */
    private const val MAX_TRANSLATIONS_PER_CALL = 100_000
    private const val REVISION_SCAN_LIMIT = 20_000

    /** 9999-12-31, comfortably within Postgres timestamp range, used as the open upper bound. */
    private const val FAR_FUTURE_MILLIS = 253402214400000L
  }
}
