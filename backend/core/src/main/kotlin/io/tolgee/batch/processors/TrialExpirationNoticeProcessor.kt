package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor

interface TrialExpirationNoticeProcessor<T> : ChunkProcessor<T, T, T>
