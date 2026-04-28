import { HOST } from '../../common/constants';
import { confirmHardMode, gcy } from '../../common/shared';
import { E2TranslationMemoryCreateEditDialog } from './E2TranslationMemoryCreateEditDialog';
import { TestDataStandardResponse } from '../../common/apiCalls/testData/generator';
import { getOrganizationByNameFromTestData } from '../../common/apiCalls/testData/testData';

export class E2TranslationMemoriesView {
  findAndVisit(data: TestDataStandardResponse, orgName: string) {
    const organization = getOrganizationByNameFromTestData(data, orgName);
    this.visit(organization.slug);
  }

  visit(organizationSlug: string) {
    cy.visit(`${HOST}/organizations/${organizationSlug}/translation-memories`);
  }

  openMenu(name: string) {
    // Filter on the TM-name child specifically — the list item now also renders the
    // names of every project the TM is assigned to (Used-in-projects header), so a plain
    // `:contains(name)` against the whole row matches every TM whose project list happens
    // to mention this name.
    gcy('translation-memory-list-item')
      .filter(
        `:has([data-cy="translation-memory-list-name"]:contains("${name}"))`
      )
      .findDcy('translation-memories-list-more-button')
      .click();
  }

  deleteTranslationMemory(name: string) {
    this.openMenu(name);
    gcy('translation-memory-delete-button').click();
    confirmHardMode();
  }

  openCreateDialog(isFirst = false): E2TranslationMemoryCreateEditDialog {
    if (isFirst) {
      gcy('translation-memories-empty-add-button').click();
    } else {
      gcy('global-plus-button').click();
    }
    gcy('create-translation-memory-dialog').should('be.visible');
    return new E2TranslationMemoryCreateEditDialog();
  }

  openTm(name: string) {
    gcy('translation-memory-list-item')
      .filter(
        `:has([data-cy="translation-memory-list-name"]:contains("${name}"))`
      )
      .click();
  }

  findAndVisitTm(
    data: TestDataStandardResponse,
    orgName: string,
    tmName: string
  ) {
    this.findAndVisit(data, orgName);
    this.openTm(tmName);
  }

  openSettingsDialog(name: string) {
    this.openMenu(name);
    gcy('translation-memory-edit-button').click();
    gcy('tm-settings-dialog').should('be.visible');
  }
}
