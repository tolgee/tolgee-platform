package io.tolgee.model.activity

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.Temporal
import jakarta.persistence.TemporalType
import org.hibernate.annotations.Type
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import java.util.*

@Entity
@Table(
  indexes = [
    Index(columnList = "projectId"),
    Index(columnList = "authorId"),
    Index(columnList = "type"),
  ],
)
@EntityListeners(ActivityRevision.Companion.ActivityRevisionListener::class)
class ActivityRevision : java.io.Serializable {
  @Id
  @SequenceGenerator(
    name = "activitySequenceGenerator",
    sequenceName = "activity_sequence",
    initialValue = 0,
    allocationSize = 10,
  )
  @GeneratedValue(
    strategy = GenerationType.SEQUENCE,
    generator = "activitySequenceGenerator",
  )
  val id: Long = 0

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "timestamp", nullable = false, updatable = false)
  lateinit var timestamp: Date

  /**
   * We don't want a foreign key, since user could have been deleted
   */
  var authorId: Long? = null

  @Column(columnDefinition = "jsonb")
  @Type(JsonBinaryType::class)
  var meta: MutableMap<String, Any?>? = null

  @Enumerated(EnumType.STRING)
  var type: ActivityType? = null

  /**
   * Project of the change
   */
  var projectId: Long? = null

//  /**
//   * Glossary of the change
//   */
//  var glossaryId: Long? = null // TODO

  @OneToMany(mappedBy = "activityRevision")
  var describingRelations: MutableList<ActivityDescribingEntity> = mutableListOf()

  @OneToMany(mappedBy = "activityRevision")
  var modifiedEntities: MutableList<ActivityModifiedEntity> = mutableListOf()

  /**
   * For chunked jobs, this field is set for every chunk.
   * When job is running, each chunk has it's own activity revision.
   * When job is finished, all the chunks revisions are merged into one revision and
   * this field is set to null.
   *
   * Instead, [batchJob] is set.
   */
  @OneToOne(fetch = FetchType.LAZY)
  var batchJobChunkExecution: BatchJobChunkExecution? = null

  @OneToOne(fetch = FetchType.LAZY)
  var batchJob: BatchJob? = null

  /**
   * The instance is created in the Holder by default, but it is not initialized by the interceptor,
   * so projectId and authorId might be null.
   *
   * This flag is set to true when the instance is initialized by the interceptor.
   */
  @Transient
  @Column(insertable = false, updatable = false)
  var isInitializedByInterceptor: Boolean = false

  @Transient
  @Column(insertable = false, updatable = false)
  var cancelledBatchJobExecutionCount: Int? = null

  companion object {
    @Configurable
    class ActivityRevisionListener {
      @Autowired
      lateinit var provider: ObjectFactory<CurrentDateProvider>

      @PrePersist
      fun preRemove(activityRevision: ActivityRevision) {
        activityRevision.timestamp = provider.`object`.date
      }
    }
  }
}
