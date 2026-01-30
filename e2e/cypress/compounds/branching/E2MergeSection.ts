import { waitForGlobalLoading } from '../../common/loading';
import { confirmStandard, gcy } from '../../common/shared';

export class E2MergeSection {
  initiateMergeFromBranches(branchName: string) {
    gcy('project-settings-branch-item-name')
      .contains(branchName)
      .scrollIntoView()
      .should('be.visible')
      .closestDcy('project-settings-branch-item')
      .findDcy('project-settings-branches-merge-into-button')
      .click();
  }

  confirmMergeCreation() {
    // Confirm the merge creation dialog
    gcy('global-confirmation-confirm').should('be.visible').click();
    // Wait for merge creation to complete
    waitForGlobalLoading();
    // After confirming, we should be redirected to the merge detail page
    gcy('project-branch-merge-detail').should('be.visible');
  }

  assertStats({
    additions,
    modifications,
    deletions,
    conflicts,
  }: {
    additions?: number;
    modifications?: number;
    deletions?: number;
    conflicts?: string;
  }) {
    if (additions !== undefined) {
      gcy('merge-stat-additions-count').should('contain', additions.toString());
    }
    if (modifications !== undefined) {
      gcy('merge-stat-modifications-count').should(
        'contain',
        modifications.toString()
      );
    }
    if (deletions !== undefined) {
      gcy('merge-stat-deletions-count').should('contain', deletions.toString());
    }
    if (conflicts !== undefined) {
      gcy('merge-stat-conflicts-count').should('contain', conflicts);
    }
  }

  selectTab(type: 'CONFLICT' | 'ADD' | 'UPDATE' | 'DELETE') {
    const tabSelectors = {
      CONFLICT: 'merge-tab-conflict',
      ADD: 'merge-tab-add',
      UPDATE: 'merge-tab-update',
      DELETE: 'merge-tab-delete',
    } as const;
    gcy(tabSelectors[type]).click();
    waitForGlobalLoading();
  }

  assertKeyInTab(keyName: string) {
    gcy('project-branch-merge-change')
      .findDcy('translations-key-name')
      .contains(keyName)
      .should('be.visible');
  }

  resolveConflictForKey(keyName: string, resolution: 'source' | 'target') {
    const changeCard = gcy('project-branch-merge-change')
      .findDcy('translations-key-name')
      .contains(keyName)
      .closest('[data-cy="project-branch-merge-change"]');

    const acceptButtons = changeCard.findDcy('project-branch-merge-accept');
    const buttonIndex = resolution === 'source' ? 0 : 1;
    acceptButtons.eq(buttonIndex).click();
    waitForGlobalLoading();
  }

  assertConflictResolved() {
    // Check for the green "Accepted" chip which appears after resolution
    cy.contains('Accepted').should('be.visible');
  }

  setDeleteBranchAfterMerge(checked: boolean) {
    gcy('project-branch-merge-delete-branch-checkbox').then(($checkbox) => {
      const isChecked = $checkbox.is(':checked');
      if (isChecked !== checked) {
        cy.wrap($checkbox).click();
      }
    });
  }

  applyMerge() {
    gcy('project-branch-merge-apply').click();
    waitForGlobalLoading();
  }

  confirmApplyMerge() {
    confirmStandard();
  }

  assertApplyButtonEnabled() {
    gcy('project-branch-merge-apply').should('not.be.disabled');
  }

  assertApplyButtonDisabled() {
    gcy('project-branch-merge-apply').should('be.disabled');
  }
}
