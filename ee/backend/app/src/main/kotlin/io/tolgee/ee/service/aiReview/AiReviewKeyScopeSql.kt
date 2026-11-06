package io.tolgee.ee.service.aiReview

import io.tolgee.ee.service.aiReview.matching.KeyScope
import jakarta.persistence.Tuple

object AiReviewKeyScopeSql {
  private const val TAG_SEPARATOR = '\u001F'

  /** Needs the aliases `k` (key), `n` (namespace), `b` (branch) and the parameter `:defaultBranch`. */
  const val COLUMNS = """
    coalesce(n.name, '') as namespace_name,
    coalesce(b.name, :defaultBranch) as branch_name,
    coalesce(
      (select string_agg(tg.name, chr(31) order by tg.name)
       from key_meta km
       join key_meta_tags kmt on kmt.key_metas_id = km.id
       join tag tg on tg.id = kmt.tags_id
       where km.key_id = k.id),
      ''
    ) as tag_names
  """

  fun Tuple.toKeyScope(keyId: Long): KeyScope =
    KeyScope(
      keyId = keyId,
      namespace = get("namespace_name", String::class.java),
      branchName = get("branch_name", String::class.java),
      tags =
        get("tag_names", String::class.java)
          .split(TAG_SEPARATOR)
          .filter { it.isNotEmpty() }
          .toSet(),
    )
}
