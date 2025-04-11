package io.tolgee.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
class Prompt(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  var name: String = "",
  @Column(length = 10000)
  var template: String = "",
  @Column
  var providerName: String = "",
  @ManyToOne
  @JoinColumn(name = "project_id")
  var project: Project,
) : AuditModel()
