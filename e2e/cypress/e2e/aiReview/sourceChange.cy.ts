import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import { setupAiReviewTestData } from '../../common/aiReview';
import { v2apiFetch } from '../../common/apiCalls/common';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';

const rulesView = new E2AiReviewRulesView();

describe('AI review source change settings', () => {
  let projectId: number;

  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      projectId = context.projectId;
      rulesView.visit(projectId);
    });
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('shows the defaults', () => {
    rulesView.assertSourceChangeAction(
      'onSourceChangeAiApproved',
      'RETRANSLATE'
    );
    rulesView.assertSourceChangeAction('onSourceChangeHumanEdited', 'KEEP');
    rulesView.assertSourceChangeAction('onSourceChangeHumanReviewed', 'KEEP');
  });

  it('saves a changed setting', () => {
    rulesView.setSourceChangeAction(
      'onSourceChangeHumanReviewed',
      'RETRANSLATE'
    );
    rulesView.assertSourceChangeAction(
      'onSourceChangeHumanReviewed',
      'RETRANSLATE'
    );
    v2apiFetch(`projects/${projectId}/ai-review/settings`)
      .its('body.onSourceChangeHumanReviewed')
      .should('eq', 'RETRANSLATE');

    cy.reload();
    rulesView.assertSourceChangeAction(
      'onSourceChangeHumanReviewed',
      'RETRANSLATE'
    );
  });
});
