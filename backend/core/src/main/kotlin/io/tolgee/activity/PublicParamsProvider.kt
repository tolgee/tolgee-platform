package io.tolgee.activity

interface PublicParamsProvider {
  fun provide(revisionIds: List<Long>): Map<Long, Any?>
}
