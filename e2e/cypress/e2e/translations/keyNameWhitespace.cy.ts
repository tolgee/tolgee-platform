import { login } from '../../common/apiCalls/common';
import { translationSingleTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage } from '../../common/shared';
import {
  getCellSaveButton,
  openKeyEditDialog,
  typeNewKeyName,
  visitTranslations,
} from '../../common/translations';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('Key name whitespace warning', () => {
  let projectId: number;

  beforeEach(() => {
    translationSingleTestData.clean();
    translationSingleTestData.generate().then((data) => {
      projectId = data.body.id;
      login('pepa', 'admin');
    });
  });

  afterEach(() => {
    waitForGlobalLoading();
    translationSingleTestData.clean();
  });

  it('warns about outer spaces on create and trims them', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    const dialog = new E2TranslationsView().openKeyCreateDialog();

    // trailing space typed without it being the final keystroke (Cypress drops that)
    dialog.getKeyNameInput().type('spaced key x{backspace}');
    cy.gcy('key-name-whitespace-warning').should('be.visible');

    cy.gcy('key-name-whitespace-trim').click();
    cy.gcy('key-name-whitespace-warning').should('not.exist');

    dialog.save();
    assertMessage('Key created');
    cy.gcy('translations-table-cell')
      .contains('spaced key')
      .should('be.visible');
  });

  it('warns when an edit introduces outer spaces and trims them', () => {
    visitTranslations(projectId);
    openKeyEditDialog('A key');
    typeNewKeyName('A key x{backspace}');
    cy.gcy('key-name-whitespace-warning').should('be.visible');

    cy.gcy('key-name-whitespace-trim').click();
    cy.gcy('key-name-whitespace-warning').should('not.exist');

    getCellSaveButton().click();
    waitForGlobalLoading();
    cy.gcy('translations-key-edit-key-field').should('not.exist');
  });

  it('does not warn when the key name is trimmed', () => {
    visitTranslations(projectId);
    const dialog = new E2TranslationsView().openKeyCreateDialog();
    dialog.getKeyNameInput().type('clean key');
    cy.gcy('key-name-whitespace-warning').should('not.exist');
  });

  it('warns when editing a key whose whitespace was already there', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    const dialog = new E2TranslationsView().openKeyCreateDialog();

    // persist a key that keeps its outer whitespace (no trim)
    dialog.getKeyNameInput().type('preexisting key x{backspace}');
    cy.gcy('key-name-whitespace-warning').should('be.visible');
    dialog.save();
    assertMessage('Key created');
    waitForGlobalLoading();

    // exact-text lookup would miss the trailing space, so match by substring
    cy.gcy('translations-key-name').contains('preexisting key').click();
    cy.gcy('translations-key-edit-key-field').should('be.visible');
    cy.gcy('key-name-whitespace-warning').should('be.visible');
  });
});
