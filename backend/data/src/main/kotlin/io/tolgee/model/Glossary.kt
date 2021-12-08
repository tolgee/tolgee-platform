package io.tolgee.model

import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.PrePersist
import javax.persistence.PreUpdate
import javax.validation.constraints.NotEmpty

@Entity
class Glossary : StandardAuditModel() {
  @field:NotEmpty
  var name: String = ""

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  var userOwner: UserAccount? = null

  @ManyToOne(optional = true)
  var organizationOwner: Organization? = null

  companion object {
    class GlossaryOwnerListener {
      @PrePersist
      @PreUpdate
      fun preSave(glossary: Glossary) {
        if (!(glossary.organizationOwner == null).xor(glossary.userOwner == null)) {
          throw Exception("Exactly one of organizationOwner or userOwner must be set!")
        }
      }
    }
  }
}
