import 'cypress-file-upload';
import { fillAndSubmitSignUpForm, visitSignUp } from '../common/login';
import { waitForGlobalLoading } from '../common/loading';
import { E2TranslationsView } from '../compounds/E2TranslationsView';
import { gcy } from '../common/shared';
import { selfHostedLimitsTestData } from '../common/apiCalls/testData/testData';
import { TestDataStandardResponse } from '../common/apiCalls/testData/generator';
import { login } from '../common/apiCalls/common';
import { visitTranslations } from '../common/translations';

/**
 * This is not a traditional test. We mock the results of the API calls,
 * so they return errors.
 *
 * That's because it would be extremely complicated to set up a self-hosted and
 * billing instance at the same time for testing.
 */
describe('Self-hosted Limits', () => {
  let testData: TestDataStandardResponse;

  beforeEach(() => {
    // Clean existing test data and generate new test data
    selfHostedLimitsTestData.clean();
    selfHostedLimitsTestData.generateStandard().then((r) => {
      testData = r.body;
      waitForGlobalLoading();
    });

    // Mock the subscription usage and license info endpoints
    mockSubscriptionUsage();
  });

  afterEach(() => {
    selfHostedLimitsTestData.clean();
  });

  describe('Seat limit', () => {
    it('shows error when exceeding seat spending limit during signup', () => {
      mockSignUp('seats_spending_limit_exceeded');
      visitSignUp();
      fillAndSubmitSignUpForm('user@example.com', true);
      cy.wait('@signUp');
      gcy('signup-error-seats-spending-limit').should('be.visible');
    });

    it('shows error when exceeding seat fixed limit during signup', () => {
      mockSignUp('plan_seat_limit_exceeded');

      visitSignUp();
      fillAndSubmitSignUpForm('user@example.com', true);
      cy.wait('@signUp');
      gcy('signup-error-plan-seat-limit').should('be.visible');
    });

    it('shows error when exceeding free self-hosted seat limit during signup', () => {
      mockSignUp('free_self_hosted_seat_limit_exceeded');

      visitSignUp();
      fillAndSubmitSignUpForm('user@example.com', true);
      cy.wait('@signUp');
      gcy('signup-error-free-seat-limit').should('be.visible');
    });
  });

  describe('Key limit', () => {
    it('shows error when exceeding key spending limit when adding a new key', () => {
      loginAndVisitTranslations(testData);
      mockKeyCreation('keys_spending_limit_exceeded');
      tryCreateKey();
      cy.wait('@createKey');
      assertSpendingLimitExceededPopoverVisible();
    });

    it('shows error when exceeding key fixed limit when adding a new key', () => {
      loginAndVisitTranslations(testData);
      mockKeyCreation('plan_key_limit_exceeded');
      mockSubscriptionUsage();
      tryCreateKey();
      cy.wait('@createKey');
      assertPlanLimitPopoverWithUsageVisible();
    });
  });

  describe('MT Credits limit', () => {
    it('shows error when exceeding mtCredits fixed limit when using machine translation', () => {
      loginAndVisitTranslations(testData);
      mockMtStreamingWithError('out_of_credits');
      openSuggestionPanel();
      assertPlanLimitPopoverWithUsageVisible();
    });

    it('shows error when exceeding mtCredits spending limit when using machine translation', () => {
      loginAndVisitTranslations(testData);
      mockMtStreamingWithError('credit_spending_limit_exceeded');
      openSuggestionPanel();
      assertSpendingLimitExceededPopoverVisible();
    });

    /**
     * For mt credits limit, the popover is shown only once.
     * We don't want to disturb the translators with the popover every time
     */
    it('the popover is not shown twice (fixed limit)', () => {
      loginAndVisitTranslations(testData);
      mockMtStreamingWithError('out_of_credits');

      openSuggestionPanel();
      assertPlanLimitPopoverWithUsageVisible();
      cy.gcy('plan-limit-dialog-close').click();
      assertPlanLimitPopoverNotExists();
      new E2TranslationsView().closeTranslationEdit();
      openSuggestionPanel();
      cy.waitForDom();
      assertPlanLimitPopoverNotExists();
    });

    /**
     * For mt credits limit, the popover is shown only once.
     * We don't want to disturb the translators with the popover every time
     */
    it('the popover is not shown twice (spending limit)', () => {
      loginAndVisitTranslations(testData);
      mockMtStreamingWithError('credit_spending_limit_exceeded');

      openSuggestionPanel();
      assertSpendingLimitExceededPopoverVisible();
      cy.gcy('spending-limit-dialog-close').click();
      assertSpendingLimitExceededPopoverNotExists();
      new E2TranslationsView().closeTranslationEdit();
      openSuggestionPanel();
      cy.waitForDom();
      assertSpendingLimitExceededPopoverNotExists();
    });
  });
});

/**
 * Helper functions for mocking API responses
 */

/**
 * Mocks the sign up endpoint with an error
 * @param errorCode - The error code to include in the response
 * @param errorParams - The error parameters to include in the response
 */
function mockSignUp(errorCode: string, errorParams: number[] = [10, 11]) {
  cy.intercept('POST', '/api/public/sign_up', {
    statusCode: 400,
    body: {
      code: errorCode,
      params: errorParams,
    },
  }).as('signUp');
}

/**
 * Mocks the key creation endpoint with an error
 * @param errorCode - The error code to include in the response
 * @param errorParams - The error parameters to include in the response
 */
function mockKeyCreation(
  errorCode: string,
  errorParams: number[] = [1000, 1001]
) {
  cy.intercept('POST', '/v2/projects/*/keys/create', {
    statusCode: 400,
    body: {
      code: errorCode,
      params: errorParams,
    },
  }).as('createKey');
}

/**
 * Mocks the subscription usage endpoint
 */
function mockSubscriptionUsage() {
  cy.intercept('GET', '/v2/ee-current-subscription-usage', {
    statusCode: 200,
    body: {
      isPayAsYouGo: false,
      seats: { current: 5, included: 10, limit: 10 },
      keys: { current: 900, included: 1000, limit: 1000 },
      strings: { current: 0, included: -1, limit: -1 },
      credits: { current: 5000, included: 10000, limit: 10000 },
    },
  }).as('subscriptionUsage');
}

/**
 * Mocks the machine translation streaming endpoint with an error
 * @param errorMessage - The error message to include in the response
 * @param errorParams - The error parameters to include in the response
 */
function mockMtStreamingWithError(
  errorMessage: string,
  errorParams: number[] = [10000, 0]
) {
  cy.intercept(
    'POST',
    '/v2/projects/*/suggest/machine-translations-streaming*',
    {
      statusCode: 200,
      body:
        // First line is the StreamedSuggestionInfo
        '{"servicesTypes":["TOLGEE"],"baseBlank":false}\n' +
        // Second line is the StreamedSuggestionItem with error
        `{"serviceType":"TOLGEE","result":null,"errorMessage":"${errorMessage}","errorParams":[${errorParams.join(
          ','
        )}],"errorException":"io.tolgee.exceptions.BadRequestException"}\n`,
    }
  ).as('mtSuggestionStreaming');
}

function assertPlanLimitPopoverWithUsageVisible() {
  gcy('plan-limit-exceeded-popover')
    .should('be.visible')
    // the usage is visible
    .should('contain.text', '5 / 10')
    .should('contain.text', '900 / 1000')
    .should('contain.text', '5000 / 10000');
}

function assertPlanLimitPopoverNotExists() {
  gcy('plan-limit-exceeded-popover').should('not.exist');
}

function assertSpendingLimitExceededPopoverVisible() {
  gcy('spending-limit-exceeded-popover').should('be.visible');
}

function assertSpendingLimitExceededPopoverNotExists() {
  gcy('spending-limit-exceeded-popover').should('not.exist');
}

function openSuggestionPanel() {
  const view = new E2TranslationsView();
  view.getTranslationCell('test', 'de').click();
}

function loginAndVisitTranslations(testData: TestDataStandardResponse) {
  const projectId = testData.projects[0].id;
  login('franta');
  visitTranslations(projectId);
}

function tryCreateKey() {
  const translationsView = new E2TranslationsView();
  const keyCreateDialog = translationsView.openKeyCreateDialog();
  keyCreateDialog.getKeyNameInput().type('test_key');
  keyCreateDialog.save();
}
