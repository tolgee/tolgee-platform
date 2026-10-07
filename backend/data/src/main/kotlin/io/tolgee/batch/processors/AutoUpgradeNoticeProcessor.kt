package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor

interface AutoUpgradeNoticeProcessor<T> : ChunkProcessor<T, T, T>
