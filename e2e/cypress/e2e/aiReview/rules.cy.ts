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
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcyAdvanced({ value: 'ai-review-scope-chip', scope: 'ui' }).should(
          'exist'
        );
        gcy('ai-review-rule-counts-active').should('exist');
        gcy('ai-review-rule-backlog').should('exist');
        gcyAdvanced({ value: 'ai-review-rule-mode', mode: 'ACTIVE' }).should(
          'exist'
        );
      }
    );
    gcyAdvanced({
      value: 'ai-review-rule-item',
      name: 'Legal dry run',
    }).within(() => {
      gcy('ai-review-rule-counts-dry-run').should('exist');
      gcyAdvanced({ value: 'ai-review-rule-mode', mode: 'DRY_RUN' }).should(
        'exist'
      );
    });
  });

  it('toggles a rule enabled state', () => {
    login('test_username');
    rulesView.visit(projectId);
    gcyAdvanced({ value: 'ai-review-rule-switch', name: 'UI rule' })
      .find('input')
      .should('be.checked');
    gcyAdvanced({ value: 'ai-review-rule-switch', name: 'UI rule' }).click();
    waitForGlobalLoading();
    cy.reload();
    gcyAdvanced({ value: 'ai-review-rule-switch', name: 'UI rule' })
      .find('input')
      .should('not.be.checked');
  });

  it('deletes a rule with confirmation', () => {
    login('test_username');
    rulesView.visit(projectId);
    gcyAdvanced({ value: 'ai-review-rule-menu', name: 'UI rule' }).click();
    gcy('ai-review-rule-menu-delete').click();
    confirmStandard();
    waitForGlobalLoading();
    gcy('ai-review-rule-item').should('have.length', 1);
  });

  it('a user with prompt edit scopes sees the list and edit controls', () => {
    login('reviewer_username');
    rulesView.visit(projectId);
    gcy('ai-review-rule-item').should('have.length', 2);
    gcy('ai-review-add-rule-button').should('be.visible');
    gcyAdvanced({ value: 'ai-review-rule-switch', name: 'UI rule' }).should(
      'exist'
    );
  });
});
