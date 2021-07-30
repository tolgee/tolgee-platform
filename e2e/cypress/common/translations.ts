import { getAnyContainingText } from './xPath';

export function getCellEditButton(content: string) {
  return cy
    .contains(content)
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-edit-button']"
    )
    .invoke('show');
}

export function getCellCancelButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-cancel-button']"
    );
}

export function getCellSaveButton() {
  return cy
    .gcy('global-editor')
    .xpath(
      "./ancestor::*[@data-cy='translations-table-cell']//*[@data-cy='translations-cell-save-button']"
    );
}

export function createTranslation(testKey: string, testTranslated?: string) {
  cy.gcy('translations-add-button').click();
  cy.gcy('global-editor').should('be.visible');
  cy.gcy('global-editor').find('textarea').type(testKey, { force: true });
  cy.xpath(getAnyContainingText('save')).click();
  cy.contains('Key created').should('be.visible');

  if (testTranslated) {
    cy.gcy('translations-view-list').contains('en').first().click();
    cy.gcy('global-editor')
      .find('textarea')
      .type(testTranslated, { force: true });
    cy.xpath(getAnyContainingText('save')).click();
    cy.gcy('global-base-view-loading').should('not.exist');
  }
}
