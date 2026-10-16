package io.tolgee.repository.aiReview

import io.tolgee.model.aiReview.AiReviewProjectSettings
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AiReviewProjectSettingsRepository : JpaRepository<AiReviewProjectSettings, Long>
