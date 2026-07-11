package io.tolgee.util

import jakarta.persistence.EntityManager
import org.hibernate.Session

val EntityManager.session: Session
  get() = this.unwrap(Session::class.java)!!

fun EntityManager.flushAndClear() {
  this.flush()
  this.clear()
}
