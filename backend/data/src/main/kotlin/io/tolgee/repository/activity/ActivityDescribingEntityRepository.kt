package io.tolgee.repository.activity

import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityDescribingEntityId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityDescribingEntityRepository : JpaRepository<ActivityDescribingEntity, ActivityDescribingEntityId>
