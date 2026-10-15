import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { gcy, gcyAdvanced } from '../../common/shared';

const translationsView = new E2TranslationsView();

describe('AI review filters', () => {
  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      visitAiReviewTranslations(context.projectId);
    });
    translationsView.assertTranslationsRowsCount(4);
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('filters translations reviewed by AI', () => {
    translationsView.filterByAiReviewOutcomes('AUTO_REVIEWED');
    translationsView.assertVisibleKeys('ui-key');
  });

  it('combines outcomes with OR', () => {
    translationsView.filterByAiReviewOutcomes('WOULD_AUTO_REVIEW', 'FAILED');
    translationsView.assertVisibleKeys('ui-key', 'legal-key');
  });

  it('applies the filter to the chosen language', () => {
    translationsView
      .openAiReviewFilter()
      .selectAiReviewOutcome('BELOW_THRESHOLD');
    gcy('translations-filter-ai-review-menu')
      .findDcy('translations-filter-apply-for-expand')
      .click();
    translationsView.applyAiReviewFilterForLanguage('de').closeFilterMenus();
    translationsView.assertTranslationsRowsCount(0);

    translationsView
      .openAiReviewFilter()
      .applyAiReviewFilterForLanguage('cs')
      .closeFilterMenus();
    translationsView.assertVisibleKeys('ui-key');
  });

  it('filters by rule and by score', () => {
    translationsView
      .openAiReviewFilter()
      .selectAiReviewRule('Legal dry run')
      .closeFilterMenus();
    cy.get('body').type('{esc}');
    translationsView.assertVisibleKeys('legal-key');

    gcy('translations-filter-select-clear').click();
    translationsView
      .openAiReviewFilter()
      .selectAiReviewScoreBelow(80)
      .closeFilterMenus();
    cy.get('body').type('{esc}');
    translationsView.assertVisibleKeys('ui-key');
  });

  it('is combined with the translation state filter', () => {
    translationsView.openFilterSelect();
    gcy('submenu-item').first().should('be.visible');
    gcyAdvanced({ value: 'submenu-item', submenu: 'translations' }).click();
    gcyAdvanced({ value: 'filter-item', state: 'REVIEWED' }).click();
    translationsView.closeFilterMenus();
    translationsView.filterByAiReviewOutcomes('WOULD_AUTO_REVIEW');
    translationsView.assertTranslationsRowsCount(0);
  });
});
