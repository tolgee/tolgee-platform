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
