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

  clickSidebarEntry() {
    gcy('settings-menu-item')
      .filter(':contains("Translation memories")')
      .click();
  }

  getListItems() {
    return gcy('translation-memory-list-item');
  }

  getListItem(name: string) {
    return this.getListItems().filter(
      `:has([data-cy="translation-memory-list-name"]:contains("${name}"))`
    );
  }

  getEntriesCountFor(name: string) {
    return this.getListItem(name).findDcy(
      'translation-memory-list-entries-count'
    );
  }

  openMenu(name: string) {
    this.getListItem(name)
      .findDcy('translation-memories-list-more-button')
      .click();
  }

  getMoreButtonFor(name: string) {
    return this.getListItem(name).findDcy(
      'translation-memories-list-more-button'
    );
  }

  getEditMenuItem() {
    return gcy('translation-memory-edit-button');
  }

  getDeleteMenuItem() {
    return gcy('translation-memory-delete-button');
  }

  deleteTranslationMemory(name: string) {
    this.openMenu(name);
    this.getDeleteMenuItem().click();
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

  openSettingsDialog(name: string): E2TranslationMemoryCreateEditDialog {
    this.openMenu(name);
    this.getEditMenuItem().click();
    gcy('tm-settings-dialog').should('be.visible');
    return new E2TranslationMemoryCreateEditDialog();
  }

  getSettingsDialog() {
    return gcy('tm-settings-dialog');
  }

  openTm(name: string) {
    this.getListItem(name).click();
  }

  findAndVisitTm(
    data: TestDataStandardResponse,
    orgName: string,
    tmName: string
  ) {
    this.findAndVisit(data, orgName);
    this.openTm(tmName);
  }
}
