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

  it('shows a badge per status and none for human-resolved or pending rows', () => {
    const badges: [string, string][] = [
      ['key_approved', 'APPROVED'],
      ['key_below_threshold', 'BELOW_THRESHOLD'],
      ['key_dry_run_pass', 'DRY_RUN_PASS'],
      ['key_dry_run_fail', 'DRY_RUN_FAIL'],
      ['key_failed', 'FAILED'],
    ];
    badges.forEach(([key, status]) => {
      visitKey(key);
      new E2AiReviewBadge(key).assertVisible(status);
    });
    ['key_pending'].forEach((key) => {
      visitKey(key);
      new E2AiReviewBadge(key).assertAbsent();
    });
  });

  it('confirmed and rejected rows show a hover chip that opens the popover', () => {
    visitKey('key_confirmed');
    new E2AiReviewBadge('key_confirmed').openPopover('CONFIRMED', {
      force: true,
    });
    gcy('ai-review-popover-status').should('be.visible');
    cy.get('body').type('{esc}');
    visitKey('key_rejected');
    new E2AiReviewBadge('key_rejected').openPopover('REJECTED', {
      force: true,
    });
    gcy('ai-review-popover-status').should('be.visible');
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
    getTranslationCell('key_last_failed', 'de').within(() => {
      gcyAdvanced({
        value: 'ai-review-badge',
        status: 'APPROVED',
        'last-failed': 'true',
      }).should('be.visible');
    });
    new E2AiReviewBadge('key_last_failed').openPopover('APPROVED');
    gcy('ai-review-popover-re-review-failed').should('be.visible');
  });

  it('shows score, threshold, rule, prompt, model and issues in the popover', () => {
    visitKey('key_with_issues');
    new E2AiReviewBadge('key_with_issues').openPopover('BELOW_THRESHOLD');
    gcy('ai-review-popover-score').should('be.visible');
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
    getTranslationCell('key_failed', 'de').within(() => {
      gcyAdvanced(
        { value: 'ai-review-badge', status: 'APPROVED' },
        { timeout: 60_000 }
      ).should('be.visible');
    });
  });

  it('confirm keeps the translation Reviewed and hides the badge', () => {
    visitKey('key_approved');
    new E2AiReviewBadge('key_approved').openPopover('APPROVED');
    gcy('ai-review-popover-confirm').click();
    getTranslationCell('key_approved', 'de').within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status: 'CONFIRMED' }).should(
        'exist'
      );
    });
    new E2AiReviewBadge('key_approved').openPopover('CONFIRMED', {
      force: true,
    });
    gcy('ai-review-popover-resolved').should('be.visible');
    cy.get('body').type('{esc}');
    getTranslationCell('key_approved', 'de')
      .closestDcy('translations-table-cell-translation')
      .findDcy('translations-state-indicator')
      .should('have.attr', 'data-cy-state', 'REVIEWED');
  });

  it('reject sets the translation back to Translated and hides the badge', () => {
    visitKey('key_approved');
    new E2AiReviewBadge('key_approved').openPopover('APPROVED');
    gcy('ai-review-popover-reject').click();
    getTranslationCell('key_approved', 'de').within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status: 'REJECTED' }).should(
        'exist'
      );
    });
    getTranslationCell('key_approved', 'de')
      .closestDcy('translations-table-cell-translation')
      .findDcy('translations-state-indicator')
      .should('have.attr', 'data-cy-state', 'TRANSLATED');
  });

  it('hides confirm, reject and retry without the permissions', () => {
    login('prompts_viewer_username');
    visitKey('key_approved');
    new E2AiReviewBadge('key_approved').openPopover('APPROVED');
    gcy('ai-review-popover-confirm').should('not.exist');
    gcy('ai-review-popover-reject').should('not.exist');
    cy.get('body').type('{esc}');
    visitKey('key_failed');
    new E2AiReviewBadge('key_failed').openPopover('FAILED');
    gcy('ai-review-popover-retry').should('not.exist');
  });

  it('an edit replaces the result with a fresh review of the new text', () => {
    visitKey('key_below_threshold');
    new E2AiReviewBadge('key_below_threshold').assertVisible('BELOW_THRESHOLD');
    editCell('Deutsch für key_below_threshold', 'Neuer Text');
    getTranslationCell('key_below_threshold', 'de').within(() => {
      gcyAdvanced(
        { value: 'ai-review-badge', status: 'APPROVED' },
        { timeout: 30_000 }
      ).should('be.visible');
    });
  });
});
