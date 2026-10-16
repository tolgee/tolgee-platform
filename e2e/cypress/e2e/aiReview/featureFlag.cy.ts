import { login } from '../../common/apiCalls/common';
import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  AI_REVIEW_ADMIN,
  AI_REVIEW_OWNER,
  AiReviewTestContext,
  setAiReviewFlagEnabledForAll,
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AdministrationOrganizations } from '../../compounds/administration/E2AdministrationOrganizations';
import { setFeature } from '../../common/features';
import { HOST } from '../../common/constants';
import { gcy, gcyAdvanced } from '../../common/shared';

const translationsView = new E2TranslationsView();
const rulesView = new E2AiReviewRulesView();
const administrationOrganizations = new E2AdministrationOrganizations();

describe('AI review feature flag', () => {
  let context: AiReviewTestContext;

  beforeEach(() => {
    setAiReviewFlagEnabledForAll(false);
    setupAiReviewTestData().then((c) => {
      context = c;
    });
  });

  afterEach(() => {
    setAiReviewFlagEnabledForAll(true);
    setFeature('AI_REVIEW', true);
    aiReviewTestData.clean();
  });

  it('hides AI review completely when the flag is off', () => {
    cy.visit(`${HOST}/projects/${context.projectId}/ai/prompts`);
    gcy('ai-menu-prompts').should('be.visible');
    gcy('ai-prompt-item-name').should('have.length', 1);
    gcy('ai-menu-review').should('not.exist');
    gcy('ai-prompt-item-type').should('not.exist');

    visitAiReviewTranslations(context.projectId);
    translationsView.getKeyLanguageCell('ui-key', 'de').should('be.visible');
    gcy('translations-cell-ai-review-badge').should('not.exist');

    visitAiReviewTranslations(context.projectId, 'aiPlayground=1');
    gcy('ai-prompt-provider-select').should('be.visible');
    gcy('ai-prompt-type-switch').should('not.exist');
    gcy('ai-prompt-review-criteria').should('not.exist');
  });

  it('server admin enables the flag for the organization', () => {
    login(AI_REVIEW_ADMIN);
    administrationOrganizations.openFeaturesDialog(context.organizationSlug);
    administrationOrganizations
      .getFeatureFlagCheckbox('AI_REVIEW')
      .should('not.be.checked')
      .should('be.enabled')
      .click();
    administrationOrganizations.saveFeatures();

    login(AI_REVIEW_OWNER);
    rulesView.visit(context.projectId);
    gcy('ai-menu-review').should('be.visible');
    gcy('ai-review-rules-list').should('be.visible');
    gcy('ai-review-rule-item').should('have.length', 2);

    cy.visit(`${HOST}/projects/${context.projectId}/ai/prompts`);
    gcy('ai-prompt-item-name').should('have.length', 2);
    gcyAdvanced({ value: 'ai-prompt-item-type', type: 'REVIEW' }).should(
      'have.length',
      1
    );

    visitAiReviewTranslations(context.projectId);
    translationsView.assertAiReviewBadge('ui-key', 'de', 'AUTO_REVIEWED', 95);
  });

  it('server admin grants the AI review plan feature', () => {
    setAiReviewFlagEnabledForAll(true);
    setFeature('AI_REVIEW', false);

    rulesView.visit(context.projectId);
    gcy('disabled-feature-banner').should('be.visible');
    gcy('ai-review-rules-list').should('not.exist');
    visitAiReviewTranslations(context.projectId);
    translationsView.getKeyLanguageCell('ui-key', 'de').should('be.visible');
    translationsView.getAiReviewBadge('ui-key', 'de').should('not.exist');

    login(AI_REVIEW_ADMIN);
    administrationOrganizations.openFeaturesDialog(context.organizationSlug);
    administrationOrganizations
      .getFeatureFlagCheckbox('AI_REVIEW')
      .should('be.checked')
      .should('be.disabled');
    administrationOrganizations
      .getPlanFeatureCheckbox('AI_REVIEW')
      .should('not.be.checked')
      .click();
    administrationOrganizations.saveFeatures();

    administrationOrganizations.openFeaturesDialog(context.organizationSlug);
    administrationOrganizations
      .getPlanFeatureCheckbox('AI_REVIEW')
      .should('be.checked');

    login(AI_REVIEW_OWNER);
    rulesView.visit(context.projectId);
    gcy('ai-review-rules-list').should('be.visible');
    gcy('disabled-feature-banner').should('not.exist');
    visitAiReviewTranslations(context.projectId);
    translationsView.assertAiReviewBadge('ui-key', 'de', 'AUTO_REVIEWED', 95);
  });
});
