import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcyAdvanced } from '../../common/shared';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { waitForGlobalLoading } from '../../common/loading';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';

describe('AI review filters', () => {
  let projectId: number;
  const view = new E2TranslationsView();

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
    view.visit(projectId, { languages: ['en', 'de', 'fr'] });
  }

  it('filters by reviewed-by-AI, below-threshold and failed outcomes', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('REVIEWED_BY_AI');
    waitForGlobalLoading();
    view.getTranslationCell('key_approved', 'de').should('exist');
    view.getTranslationCell('key_below_threshold', 'de').should('not.exist');
    view.getTranslationCell('key_untagged', 'de').should('not.exist');

    view.toggleAiReviewOutcome('REVIEWED_BY_AI');
    view.toggleAiReviewOutcome('BELOW_THRESHOLD');
    waitForGlobalLoading();
    view.getTranslationCell('key_below_threshold', 'de').should('exist');
    view.getTranslationCell('key_with_issues', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');

    view.toggleAiReviewOutcome('BELOW_THRESHOLD');
    view.toggleAiReviewOutcome('FAILED');
    waitForGlobalLoading();
    view.getTranslationCell('key_failed', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });

  it('overridden by human shows only human-overridden results', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('OVERRIDDEN');
    waitForGlobalLoading();
    view
      .getTranslationCell('key_overridden_after_failed_rereview', 'de')
      .should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });

  it('the score-only option matches passing and failing scores alike', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('SCORE_ONLY');
    waitForGlobalLoading();
    view.getTranslationCell('key_score_only_pass', 'de').should('exist');
    view.getTranslationCell('key_score_only_fail', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });

  it('not reviewed yet excludes keys no rule covers', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('NOT_REVIEWED_YET');
    view.applyFilterForExpand();
    view.applyAiReviewFilterForLanguage('de');
    waitForGlobalLoading();
    view.getTranslationCell('key_not_reviewed', 'de').should('exist');
    view.getTranslationCell('key_untagged', 'de').should('not.exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });

  it('two outcomes combine with OR', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('SCORE_ONLY');
    view.toggleAiReviewOutcome('FAILED');
    waitForGlobalLoading();
    view.getTranslationCell('key_score_only_pass', 'de').should('exist');
    view.getTranslationCell('key_failed', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });

  it('language selection limits the filter', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('BELOW_THRESHOLD');
    waitForGlobalLoading();
    view.getTranslationCell('key_below_threshold', 'de').should('exist');

    view.applyFilterForExpand();
    view.applyAiReviewFilterForLanguage('fr');
    waitForGlobalLoading();
    view.getTranslationCell('key_below_threshold', 'de').should('not.exist');

    view.applyAiReviewFilterForLanguage('de');
    waitForGlobalLoading();
    view.getTranslationCell('key_below_threshold', 'de').should('exist');
  });

  it('is hidden without the feature', () => {
    withoutAiReviewFeature();
    visit();
    view.openFilterSelect();
    gcyAdvanced({ value: 'submenu-item', id: 'tags' }).should('be.visible');
    gcyAdvanced({ value: 'submenu-item', id: 'ai-review' }).should('not.exist');
  });

  it('survives a reload through the URL state', () => {
    visit();
    view.openAiReviewFilter();
    view.toggleAiReviewOutcome('FAILED');
    waitForGlobalLoading();
    view.getTranslationCell('key_failed', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
    cy.reload();
    waitForGlobalLoading();
    view.getTranslationCell('key_failed', 'de').should('exist');
    view.getTranslationCell('key_approved', 'de').should('not.exist');
  });
});
