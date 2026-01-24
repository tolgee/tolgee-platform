import { login } from '../../../common/apiCalls/common';
import { branchTestData } from '../../../common/apiCalls/testData/testData';
import { visitBranches } from '../../../common/branches';
import { visitTranslations } from '../../../common/translations';

describe('Branch hierarchy', () => {
  let projectId: number;

  const getBranchRow = (name: string) =>
    cy
      .gcy('project-settings-branch-item-name')
      .contains(name)
      .closestDcy('project-settings-branch-item');

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

  it('creates branch from non-main branch', () => {
    visitBranches(projectId);

    cy.gcy('project-settings-branches-add').click();
    cy.gcy('branch-name-input').find('input').type('nested-branch');

    // Select feature-branch as origin instead of main
    cy.gcy('label-modal').find('[class*="MuiChip"]').first().click();
    cy.get('[role="menu"]').contains('feature-branch').click();

    cy.gcy('global-form-save-button').click();
    cy.gcy('project-settings-branch-item-name')
      .contains('nested-branch')
      .should('be.visible');
  });

  it('shows merge button targets origin branch', () => {
    visitBranches(projectId);

    getBranchRow('merge-branch')
      .findDcy('project-settings-branches-merge-into-button')
      .trigger('mouseover');
    cy.contains('feature-branch').should('be.visible'); // Origin of merge-branch in tooltip
  });

  it('navigates to translations on specific branch', () => {
    visitTranslations(projectId, 'feature-branch');
    cy.url().should('include', 'tree/feature-branch');
    cy.contains('feature-branch');
  });
});
