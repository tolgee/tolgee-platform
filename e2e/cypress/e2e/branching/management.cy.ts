import { login } from '../../common/apiCalls/common';
import { branchTestData } from '../../common/apiCalls/testData/testData';
import { E2BranchesSection } from '../../compounds/branching/E2BranchesSection';

describe('Branch management', () => {
  let projectId: number;
  const branchesSection = new E2BranchesSection();

  const setupTestData = (username: string) => {
    branchTestData.clean();
    branchTestData.generateStandard().then((res) => {
      const project = res.body.projects.find(
        (item) => item.name === 'Project with branches'
      );
      projectId = project.id;
      login(username);
    });
  };

  beforeEach(() => {
    setupTestData('branch');
  });

  afterEach(() => {
    branchTestData.clean();
  });

  it('creates a new branch from default', () => {
    branchesSection.visit(projectId);
    branchesSection.createBranch('e2e-branch');
    branchesSection.assertBranchExists('e2e-branch');
  });

  it('renames an existing branch', () => {
    branchesSection.visit(projectId);
    branchesSection.renameBranch('feature-branch', 'feature-branch-renamed');
    branchesSection.assertBranchExists('feature-branch-renamed');
  });

  it('deletes an existing branch', () => {
    branchesSection.visit(projectId);
    branchesSection.deleteBranch('feature-branch');
    branchesSection.assertBranchNotExists('feature-branch');
  });

  it('protects a branch', () => {
    branchesSection.visit(projectId);
    branchesSection.protectBranch('feature-branch');
    branchesSection.assertBranchIsProtected('feature-branch');
  });

  it('disables a branch protection', () => {
    branchesSection.visit(projectId);
    branchesSection.unprotectBranch('main');
    branchesSection.assertBranchIsNotProtected('main');
  });

  it('default branch has chip and cannot be deleted', () => {
    branchesSection.visit(projectId);
    branchesSection.assertBranchIsDefault('main');
    branchesSection.openBranchActions('main');
    branchesSection.assertDeleteButtonNotInMenu();
  });

  context('Permissions', () => {
    beforeEach(() => {
      setupTestData('branch-viewer');
    });

    it('user without branch.management can view branches but not manage them', () => {
      branchesSection.visit(projectId);
      branchesSection.assertBranchExists('main');
      branchesSection.assertBranchExists('feature-branch');
      branchesSection.assertCreateButtonNotVisible();
      branchesSection.assertActionsMenuNotVisible('main');
    });
  });
});
