import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { E2AiReviewPopover } from '../../compounds/aiReview/E2AiReviewPopover';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  getCellSaveButton,
  getTranslationEditor,
} from '../../common/translations';

const translationsView = new E2TranslationsView();
const popover = new E2AiReviewPopover();

describe('AI review badges', () => {
  let projectId: number;

  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      projectId = context.projectId;
      visitAiReviewTranslations(projectId);
    });
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('shows badges for stored results', () => {
    translationsView.assertAiReviewBadge('ui-key', 'de', 'AUTO_REVIEWED', 95);
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'BELOW_THRESHOLD', 70);
    translationsView.assertAiReviewBadge('ui-key', 'fr', 'FAILED');
    translationsView.assertAiReviewBadge(
      'legal-key',
      'de',
      'WOULD_AUTO_REVIEW',
      100
    );
    translationsView.getAiReviewBadge('plain-key', 'de').should('not.exist');
    translationsView.getAiReviewBadge('ui-key', 'en').should('not.exist');
    translationsView.assertTranslationState('ui-key', 'de', 'REVIEWED');
    translationsView.assertTranslationState('ui-key', 'cs', 'TRANSLATED');
  });

  it('shows auto-reviewed details with actions', () => {
    translationsView.openAiReviewPopover('ui-key', 'de');
    popover.assertOutcome('AUTO_REVIEWED');
    popover.assertScore(95, 80);
    gcy('ai-review-issue')
      .should('have.length', 1)
      .should('have.attr', 'data-cy-severity', 'MINOR');
    gcy('ai-review-confirm').should('be.visible');
    gcy('ai-review-reject').should('be.visible');
  });

  it('shows below threshold details without actions', () => {
    translationsView.openAiReviewPopover('ui-key', 'cs');
    popover.assertOutcome('BELOW_THRESHOLD');
    popover.assertScore(70, 80);
    gcy('ai-review-issue').should('have.length', 2);
    gcyAdvanced({ value: 'ai-review-issue', severity: 'MAJOR' }).should(
      'have.length',
      1
    );
    gcy('ai-review-confirm').should('not.exist');
    gcy('ai-review-reject').should('not.exist');
  });

  it('shows dry run details without actions', () => {
    translationsView.openAiReviewPopover('legal-key', 'de');
    popover.assertOutcome('WOULD_AUTO_REVIEW');
    popover.assertScore(100);
    gcy('ai-review-issue').should('not.exist');
    gcy('ai-review-confirm').should('not.exist');
    translationsView.assertTranslationState('legal-key', 'de', 'TRANSLATED');
  });

  it('shows failed details', () => {
    translationsView.openAiReviewPopover('ui-key', 'fr');
    popover.assertOutcome('FAILED');
    gcy('ai-review-popover-error').should('be.visible');
    gcy('ai-review-popover-score').should('not.exist');
    gcy('ai-review-confirm').should('not.exist');
  });

  it('retries failed AI review', () => {
    translationsView.openAiReviewPopover('ui-key', 'fr');
    gcy('ai-review-retry').click();
    gcy('ai-review-popover').should('not.exist');
    translationsView.assertAiReviewBadge('ui-key', 'fr', 'AUTO_REVIEWED', 100);
    translationsView.assertTranslationState('ui-key', 'fr', 'REVIEWED');
  });

  it('hides below threshold badge once a human reviews the translation', () => {
    translationsView.clickStateButton('ui-key', 'cs');
    translationsView.assertTranslationState('ui-key', 'cs', 'REVIEWED');
    translationsView.getAiReviewBadge('ui-key', 'cs').should('not.exist');

    visitAiReviewTranslations(projectId);
    translationsView.assertTranslationState('ui-key', 'cs', 'REVIEWED');
    translationsView.getAiReviewBadge('ui-key', 'cs').should('not.exist');
  });

  it('confirms AI review', () => {
    translationsView.openAiReviewPopover('ui-key', 'de');
    gcy('ai-review-confirm').click();
    gcy('ai-review-popover').should('not.exist');
    translationsView.getAiReviewBadge('ui-key', 'de').should('not.exist');
    translationsView.assertTranslationState('ui-key', 'de', 'REVIEWED');

    visitAiReviewTranslations(projectId);
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'BELOW_THRESHOLD', 70);
    translationsView.getAiReviewBadge('ui-key', 'de').should('not.exist');
    translationsView.assertTranslationState('ui-key', 'de', 'REVIEWED');
  });

  it('rejects AI review', () => {
    translationsView.openAiReviewPopover('ui-key', 'de');
    gcy('ai-review-reject').click();
    gcy('ai-review-popover').should('not.exist');
    translationsView.getAiReviewBadge('ui-key', 'de').should('not.exist');
    translationsView.assertTranslationState('ui-key', 'de', 'TRANSLATED');

    visitAiReviewTranslations(projectId);
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'BELOW_THRESHOLD', 70);
    translationsView.getAiReviewBadge('ui-key', 'de').should('not.exist');
    translationsView.assertTranslationState('ui-key', 'de', 'TRANSLATED');
  });

  it('reviews edited translation and updates the cell live', () => {
    editCell('ui-key', 'fr', 'Payer maintenant');
    translationsView.assertAiReviewBadge('ui-key', 'fr', 'AUTO_REVIEWED', 100);
    translationsView.assertTranslationState('ui-key', 'fr', 'REVIEWED');

    editCell('ui-key', 'cs', 'Zaplatit hned FAKE_AI_REVIEW_MAJOR');
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'BELOW_THRESHOLD', 75);
    translationsView.assertTranslationState('ui-key', 'cs', 'TRANSLATED');
  });

  it('dry run rule scores edited translation without changing state', () => {
    editCell('legal-key', 'de', 'Nutzungsbedingungen FAKE_AI_REVIEW_MINOR');
    translationsView.assertAiReviewBadge(
      'legal-key',
      'de',
      'WOULD_AUTO_REVIEW',
      95
    );
    translationsView.assertTranslationState('legal-key', 'de', 'TRANSLATED');
  });
});

function editCell(keyName: string, languageTag: string, text: string) {
  translationsView.getKeyLanguageCell(keyName, languageTag).click();
  getTranslationEditor().clear().type(text);
  getCellSaveButton().click();
  getTranslationEditor().should('not.exist');
}
