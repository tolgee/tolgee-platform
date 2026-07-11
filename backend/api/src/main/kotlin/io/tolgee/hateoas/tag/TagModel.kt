package io.tolgee.api.v2.hateoas.invitation

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "tags", itemRelation = "tag")
open class TagModel(
  val id: Long,
  val name: String,
) : RepresentationModel<TagModel>() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!super.equals(other)) return false

    other as TagModel

    return id == other.id
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + id.hashCode()
    return result
  }
}
