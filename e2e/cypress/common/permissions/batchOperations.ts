import { Scope } from '../../../../webapp/src/fixtures/permissions';
import {
  findBatchOperation,
  openBatchOperationMenu,
  selectAll,
} from '../batchOperations';
import { ProjectInfo } from './shared';

function checkOperationsAccessibility(
  data: Record<string, Scope>,
  { project }: ProjectInfo
) {
  const scopes = project.computedPermission.scopes;

  Object.entries(data).forEach(([operation, scope]) => {
    const operationEl = findBatchOperation(operation);
    if (scopes.includes(scope)) {
      operationEl.should('not.have.attr', 'disabled');
    } else {
      operationEl.should('have.attr', 'disabled');
    }
  });
}

export function testBatchOperations(projectInfo: ProjectInfo) {
  selectAll();
  openBatchOperationMenu();
  checkOperationsAccessibility(
    {
      'Machine translation': 'translations.batch-machine',
      'Pre-translate by TM': 'translations.batch-by-tm',
      'Mark as reviewed': 'translations.state-edit',
      'Mark as translated': 'translations.state-edit',
      'Copy translations': 'translations.edit',
      'Clear translations': 'translations.edit',
      'Add tags': 'keys.edit',
      'Remove tags': 'keys.edit',
      'Change namespace': 'keys.edit',
      'Delete keys': 'keys.delete',
    },
    projectInfo
  );
  cy.get('body').click(0, 0);
  cy.waitForDom();
}
