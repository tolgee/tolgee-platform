import { login } from '../../../common/apiCalls/common';
import { suggestionsTestData } from '../../../common/apiCalls/testData/testData';
import { visitImport } from '../../../common/import';
import 'cypress-file-upload';
import { waitForGlobalLoading } from '../../../common/loading';
import { visitTranslations } from '../../../common/translations';
import { assertHasState } from '../../../common/state';
import { assertMessage } from '../../../common/shared';

describe('Reviewed translation protect in import', () => {
  let projectId: number;

  beforeEach(() => {
    suggestionsTestData.clean();
  });

  afterEach(() => {
    suggestionsTestData.clean();
  });

  it('translator cannot override reviewed translation', () => {
    generateData('translator@test.com');

    cy.gcy('dropzone')
      .should('be.visible')
      .attachFile('translationProtection/conflict.json', {
        subjectType: 'drag-n-drop',
      });
    cy.gcy('import-row-language-select-form-control').click();
    cy.contains('Czech').click();
    cy.gcy('import-result-resolve-button').click();

    cy.gcy('import-resolution-dialog-new-translation').click();
    cy.gcy('import-resolution-dialog-new-translation').should(
      'not.have.attr',
      'data-cy-selected'
    );
  });

  it('reviewer can override reviewed translation', () => {
    generateData('reviewer@test.com');

    cy.gcy('dropzone')
      .should('be.visible')
      .attachFile('translationProtection/conflict.json', {
        subjectType: 'drag-n-drop',
      });
    cy.gcy('import-row-language-select-form-control').click();
    cy.contains('Czech').click();
    cy.gcy('import-result-resolve-button').click();

    cy.gcy('import-resolution-dialog-new-translation').click();
    cy.gcy('import-resolution-dialog-new-translation').should(
      'have.attr',
      'data-cy-selected'
    );

    cy.gcy('import-resolution-dialog-close-button').click();
    cy.gcy('import_apply_import_button').click();
    assertMessage('Import successful');
    waitForGlobalLoading().then(() => {
      visitTranslations(projectId);
      assertHasState('Conflicting translation', 'Reviewed');
    });
  });

  it("reviewer can't override disabled translation", () => {
    generateData('organization.owner@test.com', true);

    cy.gcy('dropzone')
      .should('be.visible')
      .attachFile('translationProtection/conflict.json', {
        subjectType: 'drag-n-drop',
      });
    cy.gcy('import-row-language-select-form-control').click();
    cy.contains('Czech').click();
    cy.gcy('import-result-resolve-button').click();

    cy.gcy('import-resolution-dialog-new-translation').click();
    cy.gcy('import-resolution-dialog-new-translation').should(
      'not.have.attr',
      'data-cy-selected'
    );

    cy.gcy('import-resolution-dialog-existing-translation').click();
    cy.gcy('import-resolution-dialog-existing-translation').should(
      'have.attr',
      'data-cy-selected'
    );

    cy.gcy('import-resolution-dialog-close-button').click();
    cy.gcy('import_apply_import_button').click();
    assertMessage('Import successful');
  });

  function generateData(user: string, disableTranslation = false) {
    return suggestionsTestData
      .generate({
        translationProtection: 'PROTECT_REVIEWED',
        disableTranslation,
      })
      .then((r) => {
        projectId = r.body.projects[0].id;
        login(user);
        visitImport(projectId);
      });
  }
});
