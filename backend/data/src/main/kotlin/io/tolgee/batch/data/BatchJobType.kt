package io.tolgee.batch.data

import io.tolgee.activity.data.ActivityType
import io.tolgee.batch.ChunkProcessor
import io.tolgee.batch.processors.*
import kotlin.reflect.KClass

enum class BatchJobType(
  val activityType: ActivityType? = null,
  /**
   * 0 means no chunking
   */
  val maxRetries: Int,
  val processor: KClass<out ChunkProcessor<*, *, *>>,
  val defaultRetryWaitTimeInMs: Int = 2000,
  /**
   * Whether run of this job type should be exclusive for a project
   * So only one job can run at a time for a project
   */
  val exclusive: Boolean = true,
) {
  AI_PLAYGROUND_TRANSLATE(
    maxRetries = 3,
    processor = AiPlaygroundChunkProcessor::class,
  ),
  PRE_TRANSLATE_BT_TM(
    activityType = ActivityType.BATCH_PRE_TRANSLATE_BY_TM,
    maxRetries = 3,
    processor = PreTranslationByTmChunkProcessor::class,
  ),
  MACHINE_TRANSLATE(
    activityType = ActivityType.BATCH_MACHINE_TRANSLATE,
    maxRetries = 3,
    processor = MachineTranslationChunkProcessor::class,
  ),
  AUTO_TRANSLATE(
    activityType = ActivityType.AUTO_TRANSLATE,
    maxRetries = 3,
    processor = AutoTranslateChunkProcessor::class,
  ),
  DELETE_KEYS(
    activityType = ActivityType.KEY_DELETE,
    maxRetries = 3,
    processor = DeleteKeysChunkProcessor::class,
  ),
  SET_TRANSLATIONS_STATE(
    activityType = ActivityType.BATCH_SET_TRANSLATION_STATE,
    maxRetries = 3,
    processor = SetTranslationsStateChunkProcessor::class,
  ),
  CLEAR_TRANSLATIONS(
    activityType = ActivityType.BATCH_CLEAR_TRANSLATIONS,
    maxRetries = 3,
    processor = ClearTranslationsChunkProcessor::class,
  ),
  COPY_TRANSLATIONS(
    activityType = ActivityType.BATCH_COPY_TRANSLATIONS,
    maxRetries = 3,
    processor = CopyTranslationsChunkProcessor::class,
  ),
  TAG_KEYS(
    activityType = ActivityType.BATCH_TAG_KEYS,
    maxRetries = 3,
    processor = TagKeysChunkProcessor::class,
  ),
  UNTAG_KEYS(
    activityType = ActivityType.BATCH_UNTAG_KEYS,
    maxRetries = 3,
    processor = UntagKeysChunkProcessor::class,
  ),
  SET_KEYS_NAMESPACE(
    activityType = ActivityType.BATCH_SET_KEYS_NAMESPACE,
    maxRetries = 3,
    processor = SetKeysNamespaceChunkProcessor::class,
  ),
  AUTOMATION(
    activityType = ActivityType.AUTOMATION,
    maxRetries = 3,
    processor = AutomationChunkProcessor::class,
    exclusive = false,
  ),
  BILLING_TRIAL_EXPIRATION_NOTICE(
    maxRetries = 3,
    processor = TrialExpirationNoticeProcessor::class,
  ),
}
