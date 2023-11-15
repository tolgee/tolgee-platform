package io.tolgee.component.cdn.cachePurging

interface CdnCachePurgingFactory {
  fun create(config: Any): CdnCachePurging
}
