import 'cypress-file-upload';
import { createKey, login } from '../../../common/apiCalls/common';
import {
  branchMergeTestData,
  branchTestData,
} from '../../../common/apiCalls/testData/testData';
import { getFileName, visitExport } from '../../../common/export';
import { visitImport } from '../../../common/import';
import {
  selectInAutocomplete,
  typeToAutocomplete,
} from '../../../common/languages';
import { visitTasks } from '../../../common/tasks';
import { visitTranslations } from '../../../common/translations';
import {
  assertMessage,
  dismissMenu,
  visitProjectLanguages,
} from '../../../common/shared';

const ensureExportLanguageSelected = (language: string) => {
  cy.gcy('export-language-selector').click();
  cy.gcy('export-language-selector-item')
    .contains(language)
    .closestDcy('export-language-selector-item')
    .find('input')
    .then(($input) => {
      if (!$input.is(':checked')) {
        cy.wrap($input).click({ force: true });
      }
    });
  dismissMenu();
};

describe('Branch scope - import & export', () => {
  const downloadsFolder = Cypress.config('downloadsFolder');
  let projectId: number;
  let projectName: string;

  beforeEach(() => {
    branchTestData.clean();
    branchTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Project with branches'
      );
      projectId = project.id;
      projectName = project.name;
      login('branch');
    });
  });

  afterEach(() => {
    branchTestData.clean();
  });

  it('imports apply to the selected branch only', () => {
    const branchName = 'feature-branch';

    visitImport(projectId, branchName);
    cy.gcy('import-file-input').attachFile('import/simple.json');
    cy.gcy('import-result-row').should('exist');
    cy.gcy('import-row-language-select-form-control').click();
    cy.get('[role="listbox"]').contains('English').click();
    cy.gcy('import_apply_import_button').should('be.enabled').click();
    assertMessage('Import successful');
    cy.gcy('import-result-row').should('not.exist');

    visitTranslations(projectId, branchName);
    cy.gcy('translations-key-name').contains('test').should('be.visible');

    cy.window().then((win) => win.localStorage.removeItem('projectBranch'));
    visitTranslations(projectId);
    cy.contains('No translations');
  });

  it('exports data for the selected branch only', () => {
    const branchName = 'feature-branch';
    createKey(
      projectId,
      'branch-only-key',
      { en: 'Branch only' },
      { branch: branchName }
    );
    createKey(projectId, 'default-only-key', { en: 'Default only' });

    visitExport(projectId, branchName);
    ensureExportLanguageSelected('English');
    cy.gcy('export-submit-button').click();
    const branchFileName = getFileName(projectName, 'json', 'en', branchName);
    const branchFilePath = `${downloadsFolder}/${branchFileName}`;
    cy.verifyDownload(branchFileName);
    cy.readFile(branchFilePath).should(
      'have.property',
      'branch-only-key',
      'Branch only'
    );

    visitExport(projectId);
    ensureExportLanguageSelected('English');
    cy.gcy('export-submit-button').click();
    const defaultFileName = getFileName(projectName, 'json', 'en');
    const defaultFilePath = `${downloadsFolder}/${defaultFileName}`;
    cy.verifyDownload(defaultFileName);
    cy.readFile(defaultFilePath).should('not.have.property', 'branch-only-key');
  });

  it('shares languages across branches', () => {
    visitProjectLanguages(projectId);
    cy.gcy('project-settings-languages-add').click();
    typeToAutocomplete('cs');
    selectInAutocomplete('čeština');
    cy.gcy('languages-add-dialog-submit').click();

    visitTranslations(projectId, 'feature-branch');
    cy.gcy('translations-language-select-form-control').click();
    cy.gcy('translations-language-select-item')
      .contains('Czech')
      .should('be.visible');
  });
});

describe('Branch scope - tasks', () => {
  let projectId: number;

  beforeEach(() => {
    branchMergeTestData.clean();
    branchMergeTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Project prepared for branch merge tests'
      );
      projectId = project.id;
      login('branch_merge');
    });
  });

  afterEach(() => {
    branchMergeTestData.clean();
  });

  it('shows tasks per branch', () => {
    visitTasks(projectId);
    cy.gcy('task-item').contains('Main branch task').should('be.visible');
    cy.gcy('task-item').contains('Merged feature task').should('be.visible');
    cy.gcy('task-item')
      .contains('Feature branch open task')
      .should('not.exist');

    visitTasks(projectId, 'feature');
    cy.gcy('task-item')
      .contains('Feature branch open task')
      .should('be.visible');
    cy.gcy('task-item')
      .contains('Feature branch finished task')
      .should('be.visible');
    cy.gcy('task-item').contains('Main branch task').should('not.exist');
  });
});
