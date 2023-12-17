package io.tolgee.util

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.hibernate.StatelessSession

val EntityManager.session: Session
  get() = this.unwrap(org.hibernate.Session::class.java)!!

fun EntityManager.flushAndClear() {
  this.flush()
  this.clear()
}


inline fun <reified T> EntityManager.doInStatelessSession(
  crossinline block: (StatelessSession) -> T
): T {
  return unwrap(Session::class.java).doReturningWork { connection ->
    val statelessSession = unwrap(Session::class.java).sessionFactory.openStatelessSession(connection)
    statelessSession.use { ss ->
      block(ss)
    }
  }
}
