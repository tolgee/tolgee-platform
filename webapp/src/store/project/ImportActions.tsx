import { T } from '@tolgee/react';
import { useSelector } from 'react-redux';

import { components } from 'tg.service/apiSchema.generated';
import { apiSchemaHttpService } from 'tg.service/http/ApiSchemaHttpService';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';
import { AppState } from '../index';

export class ImportState extends StateWithLoadables<ImportActions> {
  result?: components['schemas']['PagedModelImportLanguageModel'] = undefined;
  /**
   * Whether user already tried to apply import (Import button clicked)
   **/
  applyTouched?: boolean;
}

export class ImportActions extends AbstractLoadableActions<ImportState> {
  constructor() {
    super(new ImportState());
  }

  resetResult = this.createAction('RESET_RESULT').build.on((state) => {
    return { ...state, result: undefined };
  });

  touchApply = this.createAction('TOUCH_APPLY').build.on((state) => {
    return { ...state, applyTouched: true };
  });

  loadableDefinitions = {
    cancelImport: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import',
        'delete'
      )
    ),
    conflicts: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/translations',
        'get'
      )
    ),
    translations: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/translations',
        'get'
      )
    ),
    resolveConflictsLanguage: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}',
        'get'
      )
    ),
    deleteLanguage: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}',
        'delete'
      ),
      undefined,
      <T>import_language_deleted</T>
    ),
    resolveTranslationConflictOverride: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-override',
        'put'
      )
    ),
    resolveTranslationConflictKeep: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/translations/{translationId}/resolve/set-keep-existing',
        'put'
      )
    ),
    resolveAllOverride: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/resolve-all/set-override',
        'put'
      ),
      undefined,
      <T>import_resolve_override_all_success</T>
    ),
    resolveAllKeepExisting: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{languageId}/resolve-all/set-keep-existing',
        'put'
      ),
      undefined,
      <T>import_resolve_keep_all_existing_success</T>
    ),
    applyImport: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/apply',
        'put'
      ),
      undefined,
      <T>import_successfully_applied_message</T>
    ),
    selectLanguage: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{importLanguageId}/select-existing/{existingLanguageId}',
        'put'
      )
    ),
    resetExistingLanguage: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/languages/{importLanguageId}/reset-existing',
        'put'
      )
    ),
    addFiles: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import',
        'post'
      ),
      (state, action): ImportState => {
        return { ...state, result: action.payload.result };
      }
    ),
    getResult: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result',
        'get',
        {
          disableNotFoundHandling: true,
        }
      ),
      (state, action): ImportState => {
        return { ...state, result: action.payload };
      }
    ),
    getFileIssues: this.createLoadableDefinition(
      apiSchemaHttpService.schemaRequest(
        '/v2/projects/{projectId}/import/result/files/{importFileId}/issues',
        'get'
      )
    ),
  };

  get prefix(): string {
    return 'IMPORT';
  }

  useSelector<T>(selector: (state: ImportState) => T): T {
    return useSelector((state: AppState) => selector(state.import));
  }
}

export const importActions = new ImportActions();
