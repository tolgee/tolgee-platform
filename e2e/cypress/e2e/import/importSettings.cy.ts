import 'cypress-file-upload';
import { gcy } from '../../common/shared';
import { visitImport } from '../../common/import';
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

  it('applies the overrideKeyDescriptions', () => {
    interceptSettingsAndAssertRequest(
      () => {
        gcy('import-override-key-descriptions-checkbox').click();
      },
      {
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
    overrideKeyDescriptions: boolean;
  }
) => {
  cy.intercept('PUT', '**/import-settings').as('importSettings');
  fn();

  cy.wait('@importSettings').then((interception) => {
    assert.isNotNull(interception.request.body);
    expect(interception.request.body).to.have.property(
      'overrideKeyDescriptions',
      body.overrideKeyDescriptions
    );
  });
};
