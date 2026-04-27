import {
  assertMessage,
  gcy,
  gcyAdvanced,
  visitProjectDeveloperContentDelivery,
} from '../../common/shared';
import { contentDeliveryBranchingTestData } from '../../common/apiCalls/testData/testData';
import { login, setContentStorageBypass } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { setFeature } from '../../common/features';

describe('Content delivery with branching', () => {
  let projectId: number;

  beforeEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setFeature('BRANCHING', true);
    setContentStorageBypass(true);
    contentDeliveryBranchingTestData.clean();
    contentDeliveryBranchingTestData.generateStandard().then((response) => {
      login('test_username');
      projectId = response.body.projects[0].id;
      visitProjectDeveloperContentDelivery(projectId);
    });
  });

  afterEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setFeature('BRANCHING', true);
    setContentStorageBypass(false);
    contentDeliveryBranchingTestData.clean();
  });

  it('shows branch chip on list items', () => {
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Feature CDN' })
      .should('be.visible')
      .contains('feature');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Main CDN' })
      .should('be.visible')
      .contains('main');
  });

  it('shows branch selector in create dialog', () => {
    gcy('content-delivery-add-button').click();
    gcy('content-delivery-form-branch').should('be.visible');
    gcy('content-delivery-form-branch')
      .findDcy('branch-selector')
      .should('be.visible');
  });

  it('hides branch selector when branching is not enabled', () => {
    setFeature('BRANCHING', false);
    cy.reload();
    waitForGlobalLoading();
    gcy('content-delivery-add-button').click();
    gcy('content-delivery-form-branch').should('not.exist');
  });

  it('creates content delivery with selected branch', () => {
    gcy('content-delivery-add-button').click();
    gcy('content-delivery-form-name').find('input').clear().type('New CDN');
    selectBranch('feature');
    gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    assertMessage('Content delivery successfully created!');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'New CDN' })
      .should('be.visible')
      .contains('feature');
  });

  it('creates content delivery with default branch pre-selected', () => {
    gcy('content-delivery-add-button').click();
    gcy('content-delivery-form-branch')
      .findDcy('branch-selector')
      .should('contain', 'main');
  });

  it('shows branch in edit dialog', () => {
    openEditDialog('Feature CDN');
    gcy('content-delivery-form-branch')
      .findDcy('branch-selector')
      .should('contain', 'feature');
  });

  it('updates content delivery to change branch', () => {
    openEditDialog('Main CDN');
    gcy('content-delivery-form-branch')
      .findDcy('branch-selector')
      .should('contain', 'main');
    selectBranch('feature');
    gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    assertMessage('Content delivery successfully updated!');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Main CDN' })
      .should('be.visible')
      .contains('feature');
  });

  it('persists branch after edit', () => {
    openEditDialog('Feature CDN');
    gcy('content-delivery-form-name').find('input').clear().type('Edited CDN');
    gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    assertMessage('Content delivery successfully updated!');
    openEditDialog('Edited CDN');
    gcy('content-delivery-form-branch')
      .findDcy('branch-selector')
      .should('contain', 'feature');
  });

  it('edits branched content delivery when branching gets disabled', () => {
    // Seed data is created with BRANCHING enabled so the config has a branch
    // stored on the backend. Now simulate the feature being turned off.
    setFeature('BRANCHING', false);
    cy.reload();
    waitForGlobalLoading();

    openEditDialog('Main CDN');
    // Branch selector must be hidden when branching is not enabled.
    gcy('content-delivery-form-branch').should('not.exist');

    cy.gcy('content-delivery-form-name').find('input').clear().type('Renamed');
    cy.gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    // Before the fix the PUT body carried the stale filterBranch which the
    // backend rejected with FEATURE_NOT_ENABLED_FOR_PROJECT.
    assertMessage('Content delivery successfully updated!');
  });
});

function openEditDialog(name: string) {
  gcyAdvanced({ value: 'content-delivery-list-item', name })
    .findDcy('content-delivery-item-edit')
    .click();
}

function selectBranch(branchName: string) {
  gcy('content-delivery-form-branch').findDcy('branch-selector').click();
  gcyAdvanced({ value: 'branch-select-item', branch: branchName }).click();
}
