package io.tolgee.batch.processors

import io.tolgee.batch.ChunkProcessor

interface AutoUpgradeRenewalProcessor<T> : ChunkProcessor<T, T, T>
