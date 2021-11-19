package io.tolgee.model

import io.tolgee.security.AuthenticationFacade
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.hibernate.envers.RevisionListener as DefaultRevisionListener

@Configurable
class RevisionListener : DefaultRevisionListener {
  @Autowired
  lateinit var factory: ObjectFactory<AuthenticationFacade>

  override fun newRevision(revisionEntity: Any) {
    val authenticationFacade = factory.`object` as AuthenticationFacade
    val revision = revisionEntity as Revision
    revision.authorId = authenticationFacade.userAccountOrNull?.id
  }
}
