package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.tolgee.activity.annotation.ActivityLoggedEntity
import io.tolgee.activity.annotation.ActivityLoggedProp
import io.tolgee.model.enums.BasicPromptOption
import io.tolgee.model.enums.PromptType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.ColumnDefault
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
@ActivityLoggedEntity
class Prompt(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  @ActivityLoggedProp
  var name: String = "",
  @ActivityLoggedProp
  @Column(columnDefinition = "text")
  var template: String? = null,
  @ActivityLoggedProp
  @Column
  var providerName: String = "",
  @Type(
    EnumArrayType::class,
    parameters = [
      Parameter(
        name = EnumArrayType.SQL_ARRAY_TYPE,
        value = "varchar",
      ),
    ],
  )
  @ActivityLoggedProp
  @Column(columnDefinition = "varchar[]")
  var basicPromptOptions: Array<BasicPromptOption>? = null,
  @ManyToOne
  @JoinColumn(name = "project_id")
  var project: Project,
) : AuditModel() {
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'TRANSLATE'")
  @Column(nullable = false)
  @ActivityLoggedProp
  var type: PromptType = PromptType.TRANSLATE

  @ActivityLoggedProp
  @Column(columnDefinition = "text")
  var reviewCriteria: String? = null
}
