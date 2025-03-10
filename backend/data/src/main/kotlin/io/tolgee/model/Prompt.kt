package io.tolgee.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
class Prompt() : AuditModel() {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L

  @field:NotBlank
  var name: String = ""

  @Column(length = 10000)
  var template: String = ""
}
