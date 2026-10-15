import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  AI_REVIEW_TRANSLATOR,
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';
import { selectAll } from '../../common/batchOperations';
import { gcy, gcyAdvanced } from '../../common/shared';
import { setFeature } from '../../common/features';

const translationsView = new E2TranslationsView();
const runDialog = new E2AiReviewRunDialog();

describe('AI review batch operation', () => {
  let projectId: number;

  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      projectId = context.projectId;
    });
  });

  afterEach(() => {
    setFeature('AI_REVIEW', true);
    aiReviewTestData.clean();
  });

  it('counts the selection and reviews what is eligible', () => {
    visitAiReviewTranslations(projectId);
    selectAll();
    runDialog.openFromBatchOperations();
    runDialog
      .assertWillReview(1)
      .assertSkipped('alreadyHasResult', 3)
      .assertSkipped('noRule', 2)
      .assertNoSkippedReason('humanReviewed');
    runDialog.confirm();

    runDialog.finishStartedJob();
    translationsView.assertAiReviewBadge('ui-key', 'fr', 'AUTO_REVIEWED', 100);
    translationsView.assertTranslationState('ui-key', 'fr', 'REVIEWED');
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'BELOW_THRESHOLD', 70);
  });

  it('re-reviews translations that already have a result when asked', () => {
    visitAiReviewTranslations(projectId);
    selectAll();
    runDialog.openFromBatchOperations();
    runDialog.assertWillReview(1).toggleRereview().assertWillReview(4);
    runDialog.assertNoSkippedReason('alreadyHasResult');
    runDialog.confirm();

    runDialog.finishStartedJob();
    translationsView.assertAiReviewBadge('ui-key', 'cs', 'AUTO_REVIEWED', 100);
    translationsView.assertAiReviewBadge(
      'legal-key',
      'de',
      'WOULD_AUTO_REVIEW',
      100
    );
  });

  it('explains when there is nothing to review', () => {
    visitAiReviewTranslations(projectId);
    translationsView.selectRowCheckbox('legal-key');
    runDialog.openFromBatchOperations();
    runDialog.assertWillReview(0).assertSkipped('alreadyHasResult', 1);
    gcy('ai-review-run-confirm').should('not.exist');
    gcy('ai-review-run-ok').should('be.visible');

    runDialog.toggleRereview().assertWillReview(1);
    gcy('ai-review-run-ok').should('not.exist');
    gcy('ai-review-run-cancel').click();
    gcy('ai-review-run-dialog').should('not.exist');
  });

  it('is disabled without the state change permission', () => {
    setupAiReviewTestData(AI_REVIEW_TRANSLATOR).then((context) => {
      visitAiReviewTranslations(context.projectId);
    });
    selectAll();
    runDialog
      .openBatchOperationsSelect()
      .getBatchOperationItem()
      .should('have.class', 'Mui-disabled');
  });

  it('is hidden without the plan feature', () => {
    setFeature('AI_REVIEW', false);
    visitAiReviewTranslations(projectId);
    selectAll();
    runDialog.openBatchOperationsSelect();
    gcyAdvanced({
      value: 'batch-select-item',
      operation: 'machine_translate',
    }).should('be.visible');
    runDialog.getBatchOperationItem().should('not.exist');
  });
});
