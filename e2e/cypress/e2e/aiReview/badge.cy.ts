import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { editCell } from '../../common/translations';
import { HOST } from '../../common/constants';

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
    cy.visit(
      `${HOST}/projects/${projectId}/translations?languages=en,de&search=${search}`
    );
  }

  function cell(key: string) {
    return gcyAdvanced({
      value: 'translations-table-cell',
      key,
      language: 'de',
    });
  }

  function badgeIn(key: string, status: string) {
    return cell(key).within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status }).should('be.visible');
    });
  }

  function noBadgeIn(key: string) {
    cell(key).within(() => {
      gcy('ai-review-badge').should('not.exist');
    });
  }

  function openPopover(key: string, status: string) {
    cell(key).within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status }).click();
    });
    gcy('ai-review-popover').should('be.visible');
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
      badgeIn(key, status);
    });
    ['key_pending', 'key_confirmed', 'key_rejected'].forEach((key) => {
      visitKey(key);
      noBadgeIn(key);
    });
  });

  it('hides below-threshold and failed badges after a human review', () => {
    visitKey('key_human_below');
    noBadgeIn('key_human_below');
    visitKey('key_human_failed');
    noBadgeIn('key_human_failed');
  });

  it('shows below-threshold on a protected reviewed translation with the protection note', () => {
    visitKey('key_protect_below');
    badgeIn('key_protect_below', 'BELOW_THRESHOLD');
    openPopover('key_protect_below', 'BELOW_THRESHOLD');
    gcy('ai-review-popover-protected-note').should('be.visible');
  });

  it('shows the warning mark and previous-result note for a failed re-review', () => {
    visitKey('key_last_failed');
    cell('key_last_failed').within(() => {
      gcyAdvanced({
        value: 'ai-review-badge',
        status: 'APPROVED',
        'last-failed': 'true',
      }).should('be.visible');
    });
    openPopover('key_last_failed', 'APPROVED');
    gcy('ai-review-popover-re-review-failed').should('be.visible');
  });

  it('shows score, threshold, rule, prompt, model and issues in the popover', () => {
    visitKey('key_with_issues');
    openPopover('key_with_issues', 'BELOW_THRESHOLD');
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
      points: '-35',
    }).should('be.visible');
  });

  it('shows "Rule: deleted" when the rule no longer exists', () => {
    visitKey('key_rule_deleted');
    openPopover('key_rule_deleted', 'APPROVED');
    gcyAdvanced({ value: 'ai-review-popover-rule', rule: 'deleted' }).should(
      'be.visible'
    );
  });

  it('shows the failure reason and Retry re-reviews the translation', () => {
    visitKey('key_failed');
    openPopover('key_failed', 'FAILED');
    gcy('ai-review-popover-failure-reason').should('be.visible');
    gcy('ai-review-popover-retry').click();
    cell('key_failed').within(() => {
      gcyAdvanced(
        { value: 'ai-review-badge', status: 'APPROVED' },
        { timeout: 60_000 }
      ).should('be.visible');
    });
  });

  it('confirm keeps the review and hides the badge', () => {
    visitKey('key_approved');
    openPopover('key_approved', 'APPROVED');
    gcy('ai-review-popover-confirm').click();
    cell('key_approved').within(() => {
      gcy('ai-review-badge', { timeout: 10_000 }).should('not.exist');
    });
  });

  it('reject hides the badge', () => {
    visitKey('key_approved');
    openPopover('key_approved', 'APPROVED');
    gcy('ai-review-popover-reject').click();
    cell('key_approved').within(() => {
      gcy('ai-review-badge', { timeout: 10_000 }).should('not.exist');
    });
  });

  it('hides confirm, reject and retry without the permissions', () => {
    login('prompts_viewer_username');
    visitKey('key_approved');
    openPopover('key_approved', 'APPROVED');
    gcy('ai-review-popover-confirm').should('not.exist');
    gcy('ai-review-popover-reject').should('not.exist');
    cy.get('body').type('{esc}');
    visitKey('key_failed');
    openPopover('key_failed', 'FAILED');
    gcy('ai-review-popover-retry').should('not.exist');
  });

  it('removes the badge after the text is edited', () => {
    visitKey('key_approved');
    badgeIn('key_approved', 'APPROVED');
    editCell('Deutsch für key_approved', 'Neuer Text');
    cell('key_approved').within(() => {
      gcy('ai-review-badge', { timeout: 10_000 }).should('not.exist');
    });
  });
});
