import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { editCell, getTranslationCell } from '../../common/translations';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { E2AiReviewBadge } from '../../compounds/aiReview/E2AiReviewBadge';

describe('AI review badge', () => {
  let projectId: number;

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  function visitKey(search: string) {
    new E2TranslationsView().visit(projectId, {
      languages: ['en', 'de'],
      search,
    });
  }

  it('shows a badge per status and a queued badge for a pending row', () => {
    const badges: [string, string][] = [
      ['key_approved', 'APPROVED'],
      ['key_below_threshold', 'BELOW_THRESHOLD'],
      ['key_score_only_pass', 'SCORE_ONLY_PASS'],
      ['key_score_only_fail', 'SCORE_ONLY_FAIL'],
      ['key_failed', 'FAILED'],
    ];
    badges.forEach(([key, status]) => {
      visitKey(key);
      new E2AiReviewBadge(key).assertVisible(status, { queued: false });
    });
    visitKey('key_pending');
    new E2AiReviewBadge('key_pending').assertVisible('PENDING', {
      queued: true,
    });
  });

  it('an overridden row shows a hover chip and the popover names the human', () => {
    visitKey('key_overridden_after_failed_rereview');
    new E2AiReviewBadge('key_overridden_after_failed_rereview').openPopover(
      'OVERRIDDEN',
      {
        force: true,
      }
    );
    gcy('ai-review-popover-status').should('be.visible');
    gcy('ai-review-popover-resolved')
      .should('be.visible')
      .should('contain', 'Test User');
    gcy('ai-review-popover-retry').should('not.exist');
  });

  it('hides a failed badge after a human review', () => {
    visitKey('key_human_failed');
    new E2AiReviewBadge('key_human_failed').assertAbsent();
  });

  it('shows below-threshold on a protected reviewed translation with the protection note', () => {
    ['key_protect_below', 'key_human_text_below'].forEach((key) => {
      visitKey(key);
      new E2AiReviewBadge(key)
        .assertVisible('BELOW_THRESHOLD')
        .openPopover('BELOW_THRESHOLD');
      gcy('ai-review-popover-protected-note').should('be.visible');
      cy.get('body').type('{esc}');
    });
  });

  it('shows the warning mark and previous-result note for a failed re-review', () => {
    visitKey('key_last_failed');
    new E2AiReviewBadge('key_last_failed').assertVisible('APPROVED', {
      lastFailed: true,
    });
    new E2AiReviewBadge('key_last_failed').openPopover('APPROVED');
    gcy('ai-review-popover-re-review-failed').should('be.visible');
  });

  it('shows score, threshold, rule, prompt, model and issues in the popover', () => {
    visitKey('key_with_issues');
    new E2AiReviewBadge('key_with_issues').openPopover('BELOW_THRESHOLD');
    gcyAdvanced({
      value: 'ai-review-popover-score',
      'with-threshold': 'true',
    }).should('be.visible');
    gcyAdvanced({ value: 'ai-review-popover-rule', rule: 'UI rule' }).should(
      'be.visible'
    );
    gcy('ai-review-popover-prompt').should('be.visible');
    gcy('ai-review-popover-model').should('be.visible');
    gcy('ai-review-issue').should('have.length', 2);
    gcyAdvanced({
      value: 'ai-review-issue',
      severity: 'critical',
      points: '35',
    }).should('be.visible');
  });

  it('a score only popover shows the score without the threshold', () => {
    visitKey('key_score_only_pass');
    new E2AiReviewBadge('key_score_only_pass').openPopover('SCORE_ONLY_PASS');
    gcyAdvanced({
      value: 'ai-review-popover-score',
      'with-threshold': 'false',
    }).should('be.visible');
  });

  it('shows "Rule: deleted" when the rule no longer exists', () => {
    visitKey('key_rule_deleted');
    new E2AiReviewBadge('key_rule_deleted').openPopover('APPROVED');
    gcyAdvanced({ value: 'ai-review-popover-rule', rule: 'deleted' }).should(
      'be.visible'
    );
  });

  it('shows the failure reason and Retry re-reviews the translation', () => {
    visitKey('key_failed');
    new E2AiReviewBadge('key_failed').openPopover('FAILED');
    gcy('ai-review-popover-failure-reason').should('be.visible');
    gcy('ai-review-popover-retry').click();
    new E2AiReviewBadge('key_failed').assertVisible('APPROVED', {
      timeout: 60_000,
    });
  });

  it('a human un-review of an AI approval leaves an overridden chip', () => {
    visitKey('key_approved');
    getTranslationCell('key_approved', 'de')
      .closestDcy('translations-table-cell-translation')
      .trigger('mouseover')
      .findDcy('translation-state-button')
      .click();
    new E2AiReviewBadge('key_approved').assertExists('OVERRIDDEN');
    getTranslationCell('key_approved', 'de')
      .closestDcy('translations-table-cell-translation')
      .findDcy('translations-state-indicator')
      .should('have.attr', 'data-cy-state', 'TRANSLATED');
  });

  it('hides retry without the permissions', () => {
    login('prompts_viewer_username');
    visitKey('key_failed');
    new E2AiReviewBadge('key_failed').openPopover('FAILED');
    gcy('ai-review-popover-retry').should('not.exist');
  });

  it('an edit replaces the result with a fresh review of the new text', () => {
    visitKey('key_below_threshold');
    new E2AiReviewBadge('key_below_threshold').assertVisible('BELOW_THRESHOLD');
    editCell('Deutsch für key_below_threshold', 'Neuer Text');
    new E2AiReviewBadge('key_below_threshold').assertVisible('APPROVED', {
      timeout: 30_000,
    });
  });
});
