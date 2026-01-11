package io.tolgee.api

import io.tolgee.model.views.activity.ProjectActivityView

interface IProjectActivityModelAssembler {
  fun toModel(view: ProjectActivityView): IProjectActivityModel
}
