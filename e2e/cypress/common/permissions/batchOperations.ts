import { Scope } from '../../../../webapp/src/fixtures/permissions';
import {
  findBatchOperation,
  openBatchOperationMenu,
  selectAll,
} from '../batchOperations';
import { dismissMenu } from '../shared';
import { visitTranslations } from '../translations';
import { getLanguageName, ProjectInfo } from './shared';

function checkSourceLanguages(languages: number[], projectInfo: ProjectInfo) {
  cy.gcy('batch-operation-copy-source-select').click();
  languages.forEach((langId) => {
    cy.gcy('batch-operation-copy-source-select-item')
      .contains(getLanguageName(projectInfo.languages, langId))
      .should('exist');
  });
  dismissMenu();
}

function checkTargetLanguages(languages: number[], projectInfo: ProjectInfo) {
  cy.gcy('batch-operations-section')
    .findDcy('translations-language-select-form-control')
    .click();
  languages.forEach((langId) => {
    cy.gcy('translations-language-select-item')
      .contains(getLanguageName(projectInfo.languages, langId))
      .should('exist');
  });
  dismissMenu();
}

function checkOperationsAccessibility(
  data: Record<string, [scope: Scope, action?: () => void]>,
  projectInfo: ProjectInfo
) {
  const scopes = projectInfo.project.computedPermission.scopes;

  Object.entries(data).forEach(([operation, [scope, action]]) => {
    const operationEl = findBatchOperation(operation);
    if (scopes.includes(scope)) {
      operationEl.should('not.have.attr', 'disabled');
      if (action) {
        findBatchOperation(operation).click();
        action();
        openBatchOperationMenu();
      }
    } else {
      operationEl.should('have.attr', 'disabled');
    }
  });
}

export function testBatchOperations(projectInfo: ProjectInfo) {
  const { viewLanguageIds, translateLanguageIds, stateChangeLanguageIds } =
    projectInfo.project.computedPermission;
  visitTranslations(projectInfo.project.id);
  selectAll();
  openBatchOperationMenu();
  checkOperationsAccessibility(
    {
      'Machine translation': [
        'translations.batch-machine',
        () => checkTargetLanguages(translateLanguageIds, projectInfo),
      ],
      'Pre-translate by TM': [
        'translations.batch-by-tm',
        () => checkTargetLanguages(translateLanguageIds, projectInfo),
      ],
      'Mark as reviewed': [
        'translations.state-edit',
        () => checkTargetLanguages(stateChangeLanguageIds, projectInfo),
      ],
      'Mark as translated': [
        'translations.state-edit',
        () => checkTargetLanguages(stateChangeLanguageIds, projectInfo),
      ],
      'Copy translations': [
        'translations.edit',
        () => {
          checkSourceLanguages(viewLanguageIds, projectInfo);
          checkTargetLanguages(translateLanguageIds, projectInfo);
        },
      ],
      'Clear translations': [
        'translations.edit',
        () => {
          checkTargetLanguages(translateLanguageIds, projectInfo);
        },
      ],
      'Export translations': ['translations.view'],
      'Add tags': ['keys.edit'],
      'Remove tags': ['keys.edit'],
      'Change namespace': ['keys.edit'],
      'Delete keys': ['keys.delete'],
    },
    projectInfo
  );

  dismissMenu();
  cy.waitForDom();
}
