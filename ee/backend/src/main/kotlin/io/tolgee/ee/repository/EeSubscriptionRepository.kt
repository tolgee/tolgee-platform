package io.tolgee.ee.repository

import io.tolgee.ee.model.EeSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EeSubscriptionRepository : JpaRepository<EeSubscription, Int>
