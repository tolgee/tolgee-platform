package io.tolgee.model.activity

import io.tolgee.activity.groups.ActivityGroupType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToMany
import jakarta.persistence.SequenceGenerator

@Entity
class ActivityGroup(
  @Enumerated(EnumType.STRING)
  var type: ActivityGroupType,
) {
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

  /**
   * We don't want a foreign key, since user could have been deleted
   */
  var authorId: Long? = null

  /**
   * Project of the change
   */
  var projectId: Long? = null

  @ManyToMany(mappedBy = "activityGroups")
  var activityRevisions: MutableList<ActivityRevision> = mutableListOf()

  var matchingString: String? = null
}
