import { login } from '../../common/apiCalls/common';
import { translationSingleTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { assertMessage, confirmStandard } from '../../common/shared';
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

  it('warns about outer spaces when creating a key', () => {
    visitTranslations(projectId);
    waitForGlobalLoading();
    const dialog = new E2TranslationsView().openKeyCreateDialog();
    // type a trailing space without it being the final keystroke (Cypress drops that)
    dialog.getKeyNameInput().type('spaced key x{backspace}');
    dialog.save();

    cy.gcy('global-confirmation-dialog').should('be.visible');
    cy.gcy('global-confirmation-cancel').click();
    cy.gcy('global-confirmation-dialog').should('not.exist');

    dialog.save();
    confirmStandard();
    assertMessage('Key created');
  });

  it('warns when an edit introduces outer spaces', () => {
    visitTranslations(projectId);
    openKeyEditDialog('A key');
    typeNewKeyName('A key x{backspace}');
    getCellSaveButton().click();

    cy.gcy('global-confirmation-dialog').should('be.visible');
    confirmStandard();
    waitForGlobalLoading();
    cy.gcy('translations-key-edit-key-field').should('not.exist');
  });

  it('does not warn when an edit keeps the name trimmed', () => {
    visitTranslations(projectId);
    openKeyEditDialog('A key');
    typeNewKeyName('A key edited');
    getCellSaveButton().click();

    waitForGlobalLoading();
    cy.gcy('global-confirmation-dialog').should('not.exist');
    cy.gcy('translations-key-edit-key-field').should('not.exist');
    cy.contains('A key edited').should('be.visible');
  });
});
