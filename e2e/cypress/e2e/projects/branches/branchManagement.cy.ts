import { HOST } from '../../../common/constants';
import { waitForGlobalLoading } from '../../../common/loading';
import {
  createTestProject,
  deleteProject,
  login,
  v2apiFetch,
} from '../../../common/apiCalls/common';
import { visitBranches } from '../../../common/branches';
import { branchTestData } from '../../../common/apiCalls/testData/testData';

describe('Branch management', () => {
  let projectId: number;

  const openBranchActions = (name: string) => {
    cy.gcy('project-settings-branch-item-name')
      .contains(name)
      .scrollIntoView()
      .should('be.visible')
      .closestDcy('project-settings-branch-item')
      .findDcy('project-settings-branches-actions-menu')
      .click();
  };

  const enableBranching = (projectId: number) => {
    return cy.then(() => {
      const toggle = Cypress.$(
        '[data-cy="project-settings-use-branching-switch"]'
      );
      if (toggle.length) {
        return cy.wrap(toggle).click();
      }
      return v2apiFetch(`projects/${projectId}`).then((res) => {
        const project = res.body;
        return v2apiFetch(`projects/${projectId}`, {
          method: 'PUT',
          body: {
            name: project.name,
            slug: project.slug,
            baseLanguageId: project.baseLanguage?.id,
            useNamespaces: project.useNamespaces,
            useBranching: true,
            defaultNamespaceId: project.defaultNamespace?.id,
            description: project.description,
            icuPlaceholders: project.icuPlaceholders,
            suggestionsMode: project.suggestionsMode,
            translationProtection: project.translationProtection,
          },
        });
      });
    });
  };

  beforeEach(() => {
    branchTestData.clean();
    branchTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Project with branches'
      );
      projectId = project.id;
      login('branch');
    });
  });

  afterEach(() => {
    branchTestData.clean();
  });

  it('lists branches and supports create/rename/delete', () => {
    visitBranches(projectId);

    [
      'main',
      'feature-branch',
      'merge-branch',
      'merged-and-archived-branch',
      'z-archived-branch',
    ].forEach((name) => {
      cy.gcy('project-settings-branch-item-name')
        .contains(name)
        .should('be.visible');
    });

    cy.gcy('project-settings-branches-add').click();
    cy.gcy('branch-name-input').find('input').type('e2e-branch');
    cy.gcy('global-form-save-button').click();
    cy.gcy('project-settings-branch-item-name')
      .contains('e2e-branch')
      .should('be.visible');

    openBranchActions('e2e-branch');
    cy.gcy('project-settings-branches-rename-button')
      .should('be.visible')
      .click();
    cy.gcy('branch-name-input')
      .find('input')
      .clear()
      .type('e2e-branch-renamed');
    cy.gcy('global-form-save-button').click();
    cy.gcy('project-settings-branch-item-name')
      .contains('e2e-branch-renamed')
      .should('be.visible');

    openBranchActions('e2e-branch-renamed');
    cy.gcy('project-settings-branches-remove-button').click();
    cy.gcy('global-confirmation-confirm').click();
    cy.gcy('project-settings-branch-item-name')
      .contains('e2e-branch-renamed')
      .should('not.exist');

    openBranchActions('main');
    cy.gcy('project-settings-branches-remove-button').should('not.exist');
  });

  it('enables branching and creates default branch', () => {
    createTestProject().then((res) => {
      const newProjectId = res.body.id;

      cy.visit(`${HOST}/projects/${newProjectId}/manage/advanced`);
      enableBranching(newProjectId);
      waitForGlobalLoading();

      visitBranches(newProjectId);
      cy.gcy('project-settings-branch-item-name')
        .contains('main')
        .should('be.visible');

      deleteProject(newProjectId);
    });
  });

  it('toggles branch protection status', () => {
    visitBranches(projectId);

    // feature-branch is unprotected by default, protect it
    openBranchActions('feature-branch');
    cy.gcy('project-settings-branches-protection-button')
      .should('have.attr', 'data-cy-protect', 'true')
      .click();
    cy.gcy('global-confirmation-confirm').click();
    openBranchActions('feature-branch');
    cy.gcy('project-settings-branches-protection-button').should(
      'have.attr',
      'data-cy-protect',
      'false'
    );

    // Now unprotect it
    cy.gcy('project-settings-branches-protection-button').click();
    cy.gcy('global-confirmation-confirm').click();
    openBranchActions('feature-branch');
    cy.gcy('project-settings-branches-protection-button').should(
      'have.attr',
      'data-cy-protect',
      'true'
    );
  });
});
