import { login } from '../../common/apiCalls/common';
import { branchMergeTestData } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { E2BranchesSection } from '../../compounds/branching/E2BranchesSection';
import { E2BranchTranslationsView } from '../../compounds/branching/E2BranchTranslationsView';
import { E2MergeSection } from '../../compounds/branching/E2MergeSection';

describe('Branch merging', () => {
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

  it('merges updates from feature branch to main', () => {
    const updatedValue = 'Updated text from feature branch';

    // 1. Edit translation on feature branch
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.editTranslation('shared-update-key', 'en', updatedValue);

    // 2. Initiate merge from branches page
    branchesSection.visit(projectId);
    mergeSection.initiateMergeFromBranches('feature');

    // 3. Verify UPDATE tab shows the change
    mergeSection.assertStats({ modifications: 1 });
    mergeSection.selectTab('UPDATE');
    mergeSection.assertKeyInTab('shared-update-key');

    // 4. Apply merge
    mergeSection.applyMerge();

    // 5. Verify result on main branch
    translationsView.visitWithBranch(projectId, 'main');
    waitForGlobalLoading();
    translationsView.assertTranslationValue(
      'shared-update-key',
      'en',
      updatedValue
    );
  });

  it('merges additions from feature branch to main', () => {
    const newKeyName = 'brand-new-key-from-feature';
    const newKeyValue = 'New translation value';

    // 1. Create a new key on feature branch
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.createKey({
      key: newKeyName,
      translation: newKeyValue,
    });
    waitForGlobalLoading();

    // 2. Initiate merge
    branchesSection.visit(projectId);
    mergeSection.initiateMergeFromBranches('feature');

    // 3. Verify ADD tab shows the new key
    mergeSection.selectTab('ADD');
    mergeSection.assertKeyInTab(newKeyName);

    // 4. Apply merge
    mergeSection.applyMerge();

    // 5. Verify new key exists on main branch
    waitForGlobalLoading();
    translationsView.assertKeyExists(newKeyName);
    translationsView.assertTranslationValue(newKeyName, 'en', newKeyValue);
  });

  it('merges deletions from feature branch to main', () => {
    const keyToDelete = 'shared-delete-key';

    // 1. Delete key on feature branch
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.deleteKey(keyToDelete);

    // 2. Initiate merge
    branchesSection.visit(projectId);
    mergeSection.initiateMergeFromBranches('feature');

    // 3. Verify DELETE tab shows the deleted key
    mergeSection.assertStats({ deletions: 1 });
    mergeSection.selectTab('DELETE');
    mergeSection.assertKeyInTab(keyToDelete);

    // 4. Apply merge
    mergeSection.applyMerge();

    // 5. Verify key no longer exists on main branch
    waitForGlobalLoading();
    translationsView.assertKeyNotExists(keyToDelete);
  });

  it('resolves conflicts and merges', () => {
    const conflictKey = 'shared-conflict-key';
    const mainValue = 'Modified on main branch';
    const featureValue = 'Modified on feature branch';

    // 1. Modify translation on main branch
    translationsView.visitWithBranch(projectId, 'main');
    waitForGlobalLoading();
    translationsView.editTranslation(conflictKey, 'en', mainValue);

    // 2. Modify the same translation on feature branch with different value
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.editTranslation(conflictKey, 'en', featureValue);

    // 3. Initiate merge from feature branch
    branchesSection.visit(projectId);
    mergeSection.initiateMergeFromBranches('feature');

    // 4. Verify CONFLICT tab shows unresolved conflicts
    mergeSection.assertStats({ conflicts: '0/1' });

    // 5. Apply button should be disabled with unresolved conflicts
    mergeSection.assertApplyButtonDisabled();

    // 6. Resolve conflict by accepting source (feature branch version)
    mergeSection.selectTab('CONFLICT');
    mergeSection.assertKeyInTab(conflictKey);
    mergeSection.resolveConflictForKey(conflictKey, 'source');

    // 7. Verify conflict is resolved
    mergeSection.assertStats({ conflicts: '1/1' });
    mergeSection.assertConflictResolved();

    // 8. Apply button should now be enabled
    mergeSection.assertApplyButtonEnabled();

    // 9. Apply merge
    mergeSection.applyMerge();

    // 10. Verify correct resolution applied on main branch
    waitForGlobalLoading();
    translationsView.assertTranslationValue(conflictKey, 'en', featureValue);
  });

  it('deletes source branch after merge when option is selected', () => {
    const updatedValue = 'Text to trigger merge';

    // 1. Make a change on feature branch to enable merge
    translationsView.visitWithBranch(projectId, 'feature');
    waitForGlobalLoading();
    translationsView.editTranslation('shared-update-key', 'en', updatedValue);

    // 2. Initiate merge
    branchesSection.visit(projectId);
    mergeSection.initiateMergeFromBranches('feature');
    mergeSection.assertKeyInTab('shared-update-key');

    // 3. Check "delete branch after merge" option
    mergeSection.setDeleteBranchAfterMerge(true);

    // 4. Apply merge
    mergeSection.applyMerge();
    mergeSection.confirmApplyMerge();

    // 5. Navigate to branches list and verify source branch is deleted
    branchesSection.visit(projectId);
    branchesSection.assertBranchNotExists('feature');
  });
});
