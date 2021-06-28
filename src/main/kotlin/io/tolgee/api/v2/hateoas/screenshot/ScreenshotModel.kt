package io.tolgee.api.v2.hateoas.screenshot

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.*

@Suppress("unused")
@Relation(collectionRelation = "screenshots", itemRelation = "screenshot")
open class ScreenshotModel(val id: Long,
                           val filename: String,
                           val createdAt: Date?
) : RepresentationModel<ScreenshotModel>()
