import {
  assertMessage,
  confirmStandard,
  gcyAdvanced,
  visitProjectDeveloperContentDelivery,
} from '../../common/shared';
import { contentDelivery } from '../../common/apiCalls/testData/testData';
import { login, setContentStorageBypass } from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { setFeature } from '../../common/features';

describe('Content delivery', () => {
  let projectId: number;
  beforeEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(true);
    contentDelivery.clean();
    contentDelivery.generateStandard().then((response) => {
      login();
      projectId = response.body.projects[0].id;
      visitProjectDeveloperContentDelivery(projectId);
    });
  });

  afterEach(() => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', true);
    setContentStorageBypass(false);
  });

  it('publishes content manually', () => {
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-publish')
      .click();
    waitForGlobalLoading();
    assertMessage('Content published sucessfuly!');
  });

  it('creates content delivery', () => {
    const name = 'Crazy content delivery';
    cy.gcy('content-delivery-add-button').click();
    cy.gcy('content-delivery-form-name').type(name);
    cy.gcy('content-delivery-storage-selector').click();
    cy.gcy('content-delivery-storage-selector-item').contains('Azure').click();
    cy.gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' }).should(
      'be.visible'
    );
    assertMessage('Content delivery successfully created!');
  });

  it('updates existing content delivery', () => {
    const name = 'Azure edited';
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-type')
      .contains('Manual');
    gcyAdvanced({ value: 'content-delivery-list-item', name: 'Azure' })
      .findDcy('content-delivery-item-edit')
      .click();
    cy.gcy('content-delivery-form-name').clear().type(name);
    cy.gcy('content-delivery-auto-publish-checkbox').click();
    cy.gcy('content-delivery-form-save').click();
    waitForGlobalLoading();
    gcyAdvanced({ value: 'content-delivery-list-item', name }).should(
      'be.visible'
    );
    assertMessage('Content delivery successfully updated!');
    gcyAdvanced({ value: 'content-delivery-list-item', name })
      .findDcy('content-delivery-item-type')
      .contains('Auto');
  });

  it('deletes content delivery', () => {
    deleteDelivery('Azure');
  });

  it('shows info if feature not enabled', () => {
    setFeature('MULTIPLE_CONTENT_DELIVERY_CONFIGS', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );

    deleteDelivery('Azure');
    deleteDelivery('S3');

    cy.contains('Only single content delivery configuration enabled');
    cy.gcy('content-delivery-add-button').should('be.disabled');
  });

  function deleteDelivery(name: string) {
    gcyAdvanced({ value: 'content-delivery-list-item', name })
      .findDcy('content-delivery-item-edit')
      .click();
    cy.gcy('content-delivery-delete-button').click();
    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Content delivery successfully deleted!');
    cy.gcy('content-delivery-list-item').contains(name).should('not.exist');
  }
});
