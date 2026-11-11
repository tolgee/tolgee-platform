import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { confirmStandard, gcy, gcyAdvanced } from '../../common/shared';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { E2AiReviewRuleDialog } from '../../compounds/aiReview/E2AiReviewRuleDialog';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { HOST } from '../../common/constants';
import { waitForGlobalLoading } from '../../common/loading';

describe('AI review rules page', () => {
  let projectId: number;
  let emptyProjectId: number;
  const rulesView = new E2AiReviewRulesView();

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
      emptyProjectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_empty_project'
      )!.id;
    });
  });

  afterEach(() => {
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  it('hides the tab without the feature and shows no upsell', () => {
    withoutAiReviewFeature();
    login('test_username');
    cy.visit(`${HOST}/projects/${projectId}/ai/prompts`);
    gcy('ai-menu-prompts').should('be.visible');
    gcy('ai-menu-review').should('not.exist');
  });

  it('hides the tab without the prompts view permission', () => {
    login('translator_username');
    cy.visit(`${HOST}/projects/${projectId}/ai/context-data`);
    gcy('ai-menu-context-data').should('be.visible');
    gcy('ai-menu-review').should('not.exist');
  });

  it('shows the empty state for a project without rules', () => {
    login('test_username');
    rulesView.visit(emptyProjectId);
    gcy('ai-review-empty-state').should('be.visible');
  });

  it('lists the rules with label, chips, counts and backlog', () => {
    login('test_username');
    rulesView.visit(projectId);
    gcy('ai-review-rule-item').should('have.length', 2);
    rulesView.getRule('UI rule').within(() => {
      gcyAdvanced({ value: 'ai-review-scope-chip', scope: 'ui' }).should(
        'exist'
      );
      gcyAdvanced({ value: 'ai-review-rule-counts', mode: 'ACTIVE' }).should(
        'exist'
      );
      gcy('ai-review-rule-review-now').should('exist');
      rulesView.getRuleMode('ACTIVE').should('exist');
    });
    rulesView.getRule('Legal score only').within(() => {
      gcyAdvanced({
        value: 'ai-review-rule-counts',
        mode: 'SCORE_ONLY',
      }).should('exist');
      rulesView.getRuleMode('SCORE_ONLY').should('exist');
    });
  });

  it('toggles a rule enabled state from the menu', () => {
    login('test_username');
    rulesView.visit(projectId);
    rulesView.getRule('UI rule').within(() => {
      gcy('ai-review-rule-disabled').should('not.exist');
    });
    rulesView.toggleRule('UI rule');
    waitForGlobalLoading();
    cy.reload();
    rulesView.getRule('UI rule').within(() => {
      gcy('ai-review-rule-disabled').should('exist');
    });
  });

  it('duplicates a rule with a numbered name', () => {
    login('test_username');
    rulesView.visit(projectId);
    rulesView.duplicateRule('UI rule');
    const dialog = new E2AiReviewRuleDialog();
    dialog.getNameInput().should('have.value', 'UI rule 2');
    dialog.save();
    waitForGlobalLoading();
    gcy('ai-review-rule-item').should('have.length', 3);
    rulesView.getRule('UI rule 2').should('exist');
  });

  it('rules are locked while AI review is off and editable again after', () => {
    login('test_username');
    rulesView.visit(projectId);
    gcy('ai-review-enabled-toggle').click();
    waitForGlobalLoading();
    rulesView.getEnabledToggleInput().should('not.be.checked');
    gcy('ai-review-rule-edit').should('not.exist');
    gcy('ai-review-rule-menu').should('not.exist');
    gcy('ai-review-add-rule-button').should('be.disabled');

    gcy('ai-review-enabled-toggle').click();
    waitForGlobalLoading();
    rulesView.getEnabledToggleInput().should('be.checked');
    const dialog = rulesView.openEditDialog('UI rule');
    dialog.typeName('UI rule on');
    dialog.save();
    waitForGlobalLoading();
    rulesView.getRule('UI rule on').should('exist');
  });

  it('deletes a rule with confirmation', () => {
    login('test_username');
    rulesView.visit(projectId);
    rulesView.deleteRule('UI rule');
    confirmStandard();
    waitForGlobalLoading();
    gcy('ai-review-rule-item').should('have.length', 1);
  });

  it('a user with prompt edit scopes sees the list and edit controls', () => {
    login('reviewer_username');
    rulesView.visit(projectId);
    gcy('ai-review-rule-item').should('have.length', 2);
    gcy('ai-review-add-rule-button').should('be.visible');
    gcyAdvanced({ value: 'ai-review-rule-menu', name: 'UI rule' }).should(
      'exist'
    );
  });
});
