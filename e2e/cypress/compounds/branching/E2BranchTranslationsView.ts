import { waitForGlobalLoading } from '../../common/loading';
import { gcy, gcyAdvanced } from '../../common/shared';
import { E2TranslationsView } from '../E2TranslationsView';
import {
  editTranslation,
  getTranslationCell,
  visitTranslations,
} from '../../common/translations';

export class E2BranchTranslationsView extends E2TranslationsView {
  visitWithBranch(projectId: number, branchName?: string) {
    return visitTranslations(projectId, branchName);
  }

  switchToBranch(branchName: string) {
    gcy('branch-selector').click();
    gcyAdvanced({ value: 'branch-select-item', branch: branchName }).click();
    waitForGlobalLoading();
  }

  editTranslation(keyName: string, languageTag: string, newValue: string) {
    editTranslation({ key: keyName, languageTag, newValue });
    waitForGlobalLoading();
  }

  deleteKey(keyName: string) {
    // Select the key row using checkbox
    gcy('translations-row')
      .contains('[data-cy="translations-key-name"]', keyName)
      .closest('[data-cy="translations-row"]')
      .findDcy('translations-row-checkbox')
      .click();

    // Open batch operations select and choose Delete
    gcy('batch-operations-select').click();
    gcy('batch-select-item').contains('Delete').click();

    // Click submit button
    gcy('batch-operations-submit-button').click();

    // Confirm the deletion
    gcy('global-confirmation-confirm').click();
    waitForGlobalLoading();
  }

  assertTranslationValue(
    keyName: string,
    languageTag: string,
    expectedValue: string
  ) {
    getTranslationCell(keyName, languageTag).should('contain', expectedValue);
  }

  assertKeyExists(keyName: string) {
    gcy('translations-key-name').contains(keyName).should('be.visible');
  }

  assertKeyNotExists(keyName: string) {
    gcy('translations-key-name').should('not.contain', keyName);
  }
}
