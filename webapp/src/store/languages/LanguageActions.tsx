import { container, singleton } from 'tsyringe';

import { LanguageService } from '../../service/LanguageService';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { useSelector } from 'react-redux';
import { AppState } from '../index';
import { ActionType } from '../Action';
import { RepositoryActions } from '../repository/RepositoryActions';
import React from 'react';
import { T } from '@tolgee/react';

export class LanguagesState extends StateWithLoadables<LanguageActions> {}

@singleton()
export class LanguageActions extends AbstractLoadableActions<LanguagesState> {
  private service = container.resolve(LanguageService);

  constructor() {
    super(new LanguagesState());
  }

  loadableDefinitions = {
    list: this.createLoadableDefinition(this.service.getLanguages),
    language: this.createLoadableDefinition(this.service.get),
    create: this.createLoadableDefinition(
      this.service.create,
      undefined,
      <T>language_created_message</T>
    ),
    edit: this.createLoadableDefinition(
      this.service.editLanguage,
      undefined,
      <T>language_edited_message</T>
    ),
    delete: this.createLoadableDefinition(
      this.service.delete,
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
      container.resolve(RepositoryActions).loadableActions.repository
        .fulfilledType
    ) {
      this.resetLoadable(state, 'list');
    }
    return state;
  }

  get prefix(): string {
    return 'LANGUAGES';
  }
}
