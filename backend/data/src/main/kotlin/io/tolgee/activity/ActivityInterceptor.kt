package io.tolgee.activity

import io.tolgee.security.AuthenticationFacade
import org.hibernate.EmptyInterceptor
import org.hibernate.type.Type
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.Serializable

@Component
class ActivityInterceptor : EmptyInterceptor() {

  @Autowired
  lateinit var factory: ObjectFactory<AuthenticationFacade>

  override fun onFlushDirty(
    entity: Any?,
    id: Serializable?,
    currentState: Array<out Any>?,
    previousState: Array<out Any>?,
    propertyNames: Array<out String>?,
    types: Array<out Type>?
  ): Boolean {
    println("-----------")
    println(factory.`object`.userAccount.name)
    println(previousState)
    println(currentState)
    println("-----------")
    return true
  }
}
