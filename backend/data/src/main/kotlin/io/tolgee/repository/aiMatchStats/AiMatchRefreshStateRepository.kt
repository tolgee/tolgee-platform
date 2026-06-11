package io.tolgee.repository.aiMatchStats

import io.tolgee.model.aiMatchStats.AiMatchRefreshState
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface AiMatchRefreshStateRepository : JpaRepository<AiMatchRefreshState, Long>
