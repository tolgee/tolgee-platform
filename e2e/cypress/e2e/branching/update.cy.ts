import { login } from '../../common/apiCalls/common';
import { branchMergeTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy } from '../../common/shared';
import { E2BranchesSection } from '../../compounds/branching/E2BranchesSection';
import { E2BranchTranslationsView } from '../../compounds/branching/E2BranchTranslationsView';
import { E2MergeSection } from '../../compounds/branching/E2MergeSection';

describe('Branch update', () => {
  let projectId: number;
  const mergeSection = new E2MergeSection();
  const branchesSection = new E2BranchesSection();
  const translationsView = new E2BranchTranslationsView();

  const setupTestData = (username: string) => {
    branchMergeTestData.clean();
    branchMergeTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Project prepared for branch merge tests'
      );
      projectId = project.id;
      login(username);
    });
  };

  beforeEach(() => {
    setupTestData('branch_merge');
  });

  afterEach(() => {
    branchMergeTestData.clean();
  });

  it('starts an update of the feature branch from main', () => {
    translationsView.visitWithBranch(projectId, 'main');
    waitForGlobalLoading();
    translationsView.editTranslation(
      'shared-update-key',
      'en',
      'Hotfix on main'
    );

    branchesSection.visit(projectId);
    mergeSection.initiateUpdateFromBranches('feature');

    gcy('project-branch-merge-detail').should('be.visible');
    mergeSection.assertStats({ modifications: 1 });
    gcy('project-branch-merge-detail').should(
      'have.attr',
      'data-cy-direction',
      'update'
    );
    gcy('project-branch-merge-delete-branch-checkbox').should('not.exist');
    mergeSection.applyMerge();
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.assertTranslationValue(
      'shared-update-key',
      'en',
      'Hotfix on main'
    );
    branchesSection.visit(projectId);
    branchesSection.assertBranchExists('feature');
  });

  it('reopens the pending update instead of creating a second one', () => {
    branchesSection.visit(projectId);
    mergeSection.initiateUpdateFromBranches('feature');
    gcy('project-branch-merge-detail').should('be.visible');
    cy.url().should('include', '/merge/');
    cy.url().then((firstUrl) => {
      branchesSection.visit(projectId);
      mergeSection.initiateUpdateFromBranches('feature');
      gcy('project-branch-merge-detail').should('be.visible');
      cy.url().should('eq', firstUrl);
    });
  });
});
