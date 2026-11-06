import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { HOST } from '../../common/constants';
import { getTranslationCell } from '../../common/translations';
import { waitForGlobalLoading } from '../../common/loading';

describe('AI review filters', () => {
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
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  function visit() {
    cy.visit(
      `${HOST}/projects/${projectId}/translations?languages=en&languages=de&languages=fr`
    );
  }

  function openAiReviewSubfilter() {
    gcy('translations-filter-select').click();
    gcyAdvanced({ value: 'submenu-item', id: 'ai-review' }).click();
  }

  function toggleOutcome(outcome: string) {
    gcyAdvanced({ value: 'ai-review-filter-item', outcome }).click();
  }

  function keyVisible(key: string) {
    getTranslationCell(key, 'de').should('exist');
  }

  function keyAbsent(key: string) {
    getTranslationCell(key, 'de').should('not.exist');
  }

  it('filters by reviewed-by-AI, below-threshold and failed outcomes', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('REVIEWED_BY_AI');
    waitForGlobalLoading();
    keyVisible('key_approved');
    keyAbsent('key_below_threshold');
    keyAbsent('key_untagged');

    toggleOutcome('REVIEWED_BY_AI');
    toggleOutcome('BELOW_THRESHOLD');
    waitForGlobalLoading();
    keyVisible('key_below_threshold');
    keyVisible('key_with_issues');
    keyAbsent('key_approved');

    toggleOutcome('BELOW_THRESHOLD');
    toggleOutcome('FAILED');
    waitForGlobalLoading();
    keyVisible('key_failed');
    keyAbsent('key_approved');
  });

  it('filters by the human-resolved outcomes', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('CONFIRMED');
    waitForGlobalLoading();
    keyVisible('key_confirmed');
    keyAbsent('key_approved');

    toggleOutcome('CONFIRMED');
    toggleOutcome('REJECTED');
    waitForGlobalLoading();
    keyVisible('key_rejected');
    keyAbsent('key_approved');
  });

  it('filters by the two dry run outcomes', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('DRY_RUN_PASS');
    waitForGlobalLoading();
    keyVisible('key_dry_run_pass');
    keyAbsent('key_dry_run_fail');

    toggleOutcome('DRY_RUN_PASS');
    toggleOutcome('DRY_RUN_FAIL');
    waitForGlobalLoading();
    keyVisible('key_dry_run_fail');
    keyAbsent('key_dry_run_pass');
  });

  it('not reviewed yet excludes keys no rule covers', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('NOT_REVIEWED_YET');
    // Scope to German; with all languages almost every key has an
    // unreviewed French translation and the list stays long.
    cy.gcy('translations-filter-apply-for-expand').click();
    gcyAdvanced({
      value: 'translations-filter-apply-for-language',
      language: 'de',
    }).click();
    waitForGlobalLoading();
    keyVisible('key_not_reviewed');
    keyAbsent('key_untagged');
    keyAbsent('key_approved');
  });

  it('two outcomes combine with OR', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('DRY_RUN_PASS');
    toggleOutcome('FAILED');
    waitForGlobalLoading();
    keyVisible('key_dry_run_pass');
    keyVisible('key_failed');
    keyAbsent('key_approved');
  });

  it('language selection limits the filter', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('BELOW_THRESHOLD');
    waitForGlobalLoading();
    keyVisible('key_below_threshold');

    cy.gcy('translations-filter-apply-for-expand').click();
    gcyAdvanced({
      value: 'translations-filter-apply-for-language',
      language: 'fr',
    }).click();
    waitForGlobalLoading();
    keyAbsent('key_below_threshold');

    gcyAdvanced({
      value: 'translations-filter-apply-for-language',
      language: 'de',
    }).click();
    waitForGlobalLoading();
    keyVisible('key_below_threshold');
  });

  it('is hidden without the feature', () => {
    withoutAiReviewFeature();
    visit();
    gcy('translations-filter-select').click();
    gcyAdvanced({ value: 'submenu-item', id: 'tags' }).should('be.visible');
    gcyAdvanced({ value: 'submenu-item', id: 'ai-review' }).should('not.exist');
  });

  it('survives a reload through the URL state', () => {
    visit();
    openAiReviewSubfilter();
    toggleOutcome('FAILED');
    waitForGlobalLoading();
    keyVisible('key_failed');
    keyAbsent('key_approved');
    cy.reload();
    waitForGlobalLoading();
    keyVisible('key_failed');
    keyAbsent('key_approved');
  });
});
