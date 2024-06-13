import {
  assertMessage,
  confirmStandard,
  gcyAdvanced,
  visitProjectDeveloperHooks,
} from '../../common/shared';
import { contentDeliveryTestData } from '../../common/apiCalls/testData/testData';
import {
  login,
  setWebhookControllerStatus,
} from '../../common/apiCalls/common';
import { waitForGlobalLoading } from '../../common/loading';
import { API_URL } from '../../common/constants';
import { setFeature } from '../../common/features';

describe('Content delivery', () => {
  const testUrl = API_URL + '/internal/webhook-testing';

  beforeEach(() => {
    setWebhookControllerStatus(200);
    setFeature('WEBHOOKS', true);
    contentDeliveryTestData.clean();
    contentDeliveryTestData.generateStandard().then((response) => {
      login();
      const projectId = response.body.projects[0].id;
      visitProjectDeveloperHooks(projectId);
    });
  });

  afterEach(() => {
    setFeature('WEBHOOKS', true);
    setWebhookControllerStatus(400);
  });

  it('creates webhook', () => {
    createWebhook();
  });

  it('updates webhook', () => {
    const newUrl = 'testurl';
    createWebhook();
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl })
      .findDcy('webhooks-item-edit')
      .click();
    cy.gcy('webhook-form-url').find('input').clear().type(newUrl);
    cy.gcy('webhook-form-save').click();
    waitForGlobalLoading();
    assertMessage('Webhook successfully updated!');
    gcyAdvanced({ value: 'webhooks-list-item', url: newUrl }).should(
      'be.visible'
    );
  });

  it('deletes webhook', () => {
    createWebhook();
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl })
      .findDcy('webhooks-item-edit')
      .click();
    cy.gcy('webhook-form-delete').click();
    confirmStandard();
    waitForGlobalLoading();
    assertMessage('Webhook successfully deleted!');
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl }).should(
      'not.exist'
    );
  });

  it('tests valid webhook', () => {
    createWebhook();
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl })
      .findDcy('webhooks-item-test')
      .click();
    waitForGlobalLoading();
    assertMessage('Test request sent to the webhook successfully');
  });

  it('tests invalid webhook', () => {
    createWebhook('invalid');
    gcyAdvanced({ value: 'webhooks-list-item', url: 'invalid' })
      .findDcy('webhooks-item-test')
      .click();
    waitForGlobalLoading();
    assertMessage('Test failed!');
  });

  it('tests failing webhook', () => {
    setWebhookControllerStatus(400);
    createWebhook();
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl })
      .findDcy('webhooks-item-test')
      .click();
    waitForGlobalLoading();
    assertMessage('Test failed!');
  });

  it('shows info if feature not enabled', () => {
    createWebhook();
    setFeature('WEBHOOKS', false);
    cy.reload();
    cy.contains('You are over limit, modification is restricted').should(
      'be.visible'
    );
    gcyAdvanced({ value: 'webhooks-list-item', url: testUrl })
      .findDcy('webhooks-item-edit')
      .click();
    cy.gcy('webhook-form-delete').click();
    confirmStandard();
    waitForGlobalLoading();

    cy.contains("Your plan doesn't include the webhooks feature").should(
      'be.visible'
    );
    cy.gcy('webhooks-add-item-button').should('be.disabled');
  });

  function createWebhook(url: string = testUrl) {
    cy.gcy('webhooks-add-item-button').click();
    cy.gcy('webhook-form-url').find('input').type(url);
    cy.gcy('webhook-form-save').click();

    waitForGlobalLoading();
    gcyAdvanced({ value: 'webhooks-list-item', url }).should('be.visible');
    assertMessage('Webhook successfully created!');
  }
});
