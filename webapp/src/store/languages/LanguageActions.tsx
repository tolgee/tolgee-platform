import { container, singleton } from 'tsyringe';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { useSelector } from 'react-redux';
import { AppState } from '../index';
import { ActionType } from '../Action';
import { ProjectActions } from '../project/ProjectActions';
import React from 'react';
import { T } from '@tolgee/react';
import { ApiSchemaHttpService } from '../../service/http/ApiSchemaHttpService';

export class LanguagesState extends StateWithLoadables<LanguageActions> {}

@singleton()
export class LanguageActions extends AbstractLoadableActions<LanguagesState> {
  constructor(private schemaService: ApiSchemaHttpService) {
    super(new LanguagesState());
  }

  loadableDefinitions = {
    list: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages',
        'get'
      )
    ),
    globalList: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages',
        'get'
      )
    ),
    language: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages/{languageId}',
        'get'
      )
    ),
    create: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages',
        'post'
      ),
      undefined,
      <T>language_created_message</T>
    ),
    edit: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages/{languageId}',
        'put'
      ),
      undefined,
      <T>language_edited_message</T>
    ),
    delete: this.createLoadableDefinition(
      this.schemaService.schemaRequest(
        '/v2/projects/{projectId}/languages/{languageId}',
        'delete'
      ),
      undefined,
      <T>language_deleted_message</T>
    ),
  };

  useSelector<T>(selector: (state: LanguagesState) => T): T {
    return useSelector((state: AppState) => selector(state.languages));
  }

  customReducer(
    state: LanguagesState,
    action: ActionType<any>,
    appState
  ): LanguagesState {
    if (
      action.type ===
      container.resolve(ProjectActions).loadableActions.project.fulfilledType
    ) {
      this.resetLoadable(state, 'list');
    }
    return state;
  }

  get prefix(): string {
    return 'LANGUAGES';
  }
}
