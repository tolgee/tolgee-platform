package io.tolgee.repository.activity

import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntityId
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface ActivityDescribingEntityRepository : JpaRepository<ActivityDescribingEntity, ActivityDescribingEntityId>
