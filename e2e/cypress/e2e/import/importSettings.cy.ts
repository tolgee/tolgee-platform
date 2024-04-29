import 'cypress-file-upload';
import { gcy } from '../../common/shared';
import {
  assertInResultDialog,
  getLanguageRow,
  getShowDataDialog,
  visitImport,
} from '../../common/import';
import { importTestData } from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';

describe('Import Adding files', () => {
  beforeEach(() => {
    importTestData.clean();

    importTestData.generateBase().then((project) => {
      login('franta');
      visitImport(project.body.id);
    });
  });

  it('applies settings when file uploaded (initially without conversion)', () => {
    interceptSettingsAndAssertRequest(
      () => {
        gcy('import-convert-placeholders-to-icu-checkbox').click();
      },
      {
        convertPlaceholdersToIcu: false,
        overrideKeyDescriptions: false,
      }
    );

    gcy('import-file-input').attachFile(['import/po/placeholders.po']);
    gcy('import-result-total-count-cell', { timeout: 60000 }).should('exist');

    goToResult();
    assertInResultDialog('Willkommen zurück, %1$s!');

    cy.get('body').type('{esc}');

    interceptSettingsAndAssertRequest(
      () => {
        gcy('import-convert-placeholders-to-icu-checkbox').click();
      },
      {
        convertPlaceholdersToIcu: true,
        overrideKeyDescriptions: false,
      }
    );

    goToResult();
    assertInResultDialog('Willkommen zurück, 0! Dein letzter Besuch war am 1');
  });

  it('applies settings when file uploaded with settings (initially with conversion)', () => {
    gcy('import-file-input').attachFile(['import/po/placeholders.po']);

    gcy('import-result-total-count-cell', { timeout: 60000 }).should('exist');
    goToResult();
    assertInResultDialog('Willkommen zurück, 0! Dein letzter Besuch war am 1');

    cy.get('body').type('{esc}');

    interceptSettingsAndAssertRequest(
      () => {
        gcy('import-convert-placeholders-to-icu-checkbox').click();
      },
      {
        convertPlaceholdersToIcu: false,
        overrideKeyDescriptions: false,
      }
    );
    goToResult();
    assertInResultDialog('Willkommen zurück, %1$s!');
  });

  it('applies the overrideKeyDescriptions', () => {
    interceptSettingsAndAssertRequest(
      () => {
        gcy('import-override-key-descriptions-checkbox').click();
      },
      {
        convertPlaceholdersToIcu: true,
        overrideKeyDescriptions: true,
      }
    );
  });

  after(() => {
    importTestData.clean();
  });
});

const interceptSettingsAndAssertRequest = (
  fn: () => void,
  body: {
    convertPlaceholdersToIcu: boolean;
    overrideKeyDescriptions: boolean;
  }
) => {
  cy.intercept('PUT', '**/import-settings').as('importSettings');
  fn();

  cy.wait('@importSettings').then((interception) => {
    assert.isNotNull(interception.request.body);
    expect(interception.request.body).to.have.property(
      'convertPlaceholdersToIcu',
      body.convertPlaceholdersToIcu
    );
    expect(interception.request.body).to.have.property(
      'overrideKeyDescriptions',
      body.overrideKeyDescriptions
    );
  });
};

const goToResult = () => {
  getLanguageRow('placeholders.po (de)')
    .findDcy('import-result-show-all-translations-button')
    .click();
  getShowDataDialog().should('be.visible');
};
