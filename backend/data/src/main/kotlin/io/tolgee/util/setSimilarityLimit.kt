package io.tolgee.util

import jakarta.persistence.EntityManager

fun EntityManager.setSimilarityLimit(limit: Double) {
  this.createNativeQuery("select set_limit($limit);").resultList
}
