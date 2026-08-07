package io.tolgee.repository.apps

import io.tolgee.model.apps.AppDelivery
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Date

@Repository
@Lazy
interface AppDeliveryRepository : JpaRepository<AppDelivery, Long> {
  fun findAllByAppIdentifierOrderByCreatedAtDesc(appIdentifier: String): List<AppDelivery>

  @Query(
    "select d from AppDelivery d " +
      "where d.deliveredAt is null and d.abandonedAt is null and d.createdAt < :createdBefore",
  )
  fun findUnfinishedCreatedBefore(createdBefore: Date): List<AppDelivery>
}
