package io.tolgee.model

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.tolgee.model.enums.BasicPromptOption
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type

@Entity
@Table(
  indexes = [
    Index(columnList = "project_id"),
  ],
)
class Prompt(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  var name: String = "",
  @Column(columnDefinition = "text")
  var template: String? = null,
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
  @Column(columnDefinition = "varchar[]")
  var basicPromptOptions: Array<BasicPromptOption>? = null,
  @ManyToOne
  @JoinColumn(name = "project_id")
  var project: Project,
) : AuditModel()
