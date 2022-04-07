package io.tolgee.repository.activity

import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityModifiedEntityId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ActivityModifiedEntityRepository : JpaRepository<ActivityModifiedEntity, ActivityModifiedEntityId>
