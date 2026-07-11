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
    branchesSection.deleteBranch('merge-branch');
    branchesSection.assertBranchNotExists('merge-branch');
  });

  it('cannot delete a branch with children', () => {
    branchesSection.visit(projectId);
    branchesSection.deleteBranch('feature-branch');
    cy.contains(
      'Cannot delete this branch because other branches were created from it'
    ).should('be.visible');
    branchesSection.assertBranchExists('feature-branch');
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

  context('Branch naming validation', () => {
    beforeEach(() => {
      branchesSection.visit(projectId);
      branchesSection.openCreateBranchDialog();
    });

    it('accepts valid branch names', () => {
      // Rules:
      // - Start/end: letter, number, or dot
      // - Allowed chars: a-z, 0-9, . - _ / (lowercase only)
      // - No //, .., /., or .lock
      const validNames = [
        'ab',
        'feature',
        'feature123',
        'feature-branch',
        'feature_branch',
        'feature/branch',
        'feature/sub/branch',
        '123feature',
        '.hidden', // starts with dot
        'feature.', // ends with dot
        'v1.0.0', // dots allowed
        'feature.branch', // dots allowed
        'a'.repeat(100),
      ];

      validNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasNoError();
      });
    });

    it('rejects names with invalid length', () => {
      branchesSection.clearBranchNameAndBlur();
      branchesSection.assertBranchNameInputHasError('This field is required');

      branchesSection.typeBranchNameAndBlur('a');
      branchesSection.assertBranchNameInputHasError(
        'This field has to contain at least 2 characters'
      );

      branchesSection.typeBranchNameAndBlur('a'.repeat(101));
      branchesSection.assertBranchNameInputHasError(
        'This field can contain at maximum 100 characters'
      );
    });

    it('rejects names with invalid start/end characters', () => {
      // Must start and end with letter, number, or dot
      const invalidNames = [
        '-branch',
        '_branch',
        '/branch',
        'branch-',
        'branch_',
        'branch/',
      ];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });

    it('rejects uppercase names', () => {
      // Lowercase only
      const invalidNames = ['Feature', 'BRANCH', 'myBranch'];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });

    it('rejects names with consecutive slashes or dots', () => {
      const invalidNames = ['feature//branch', 'feature..branch'];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });

    it('rejects names with part starting with dot after slash', () => {
      const invalidNames = ['feature/.hidden', 'a/.b'];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });

    it('rejects names ending with .lock', () => {
      const invalidNames = ['branch.lock', 'feature/branch.lock'];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });

    it('rejects names with disallowed characters', () => {
      // Spaces and special characters are not allowed
      const invalidNames = [
        'feature branch',
        'feature@branch',
        'feature#branch',
      ];

      invalidNames.forEach((name) => {
        branchesSection.typeBranchNameAndBlur(name);
        branchesSection.assertBranchNameInputHasError();
      });
    });
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
