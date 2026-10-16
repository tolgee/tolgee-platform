import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import { setupAiReviewTestData } from '../../common/aiReview';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';
import { gcy } from '../../common/shared';
import { waitForGlobalLoading } from '../../common/loading';

const rulesView = new E2AiReviewRulesView();
const runDialog = new E2AiReviewRunDialog();

describe('AI review of existing translations from rules', () => {
  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      rulesView.visit(context.projectId);
    });
    gcy('ai-review-rule-item').should('have.length', 2);
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('offers to review existing translations after activating a rule', () => {
    rulesView.createRule('Everything', 'ACTIVE');
    runDialog
      .getDialog()
      .should('have.attr', 'data-cy-variant', 'rule')
      .should('have.attr', 'data-cy-dry-run', 'false');
    runDialog.assertWillReview(3).assertSkipped('alreadyHasResult', 3);
    runDialog.confirm();
    runDialog.finishStartedJob();

    rulesView
      .getRuleItem('Everything')
      .findDcy('ai-review-rule-backlog')
      .should('not.exist');
    rulesView
      .getRuleItem('Everything')
      .findDcy('ai-review-rule-stats')
      .should('have.attr', 'data-cy-auto-reviewed', '3');
  });

  it('leaves a hint on the rule row when postponed', () => {
    rulesView.createRule('Everything', 'ACTIVE');
    runDialog.assertWillReview(3).later();

    rulesView
      .getRuleItem('Everything')
      .findDcy('ai-review-rule-backlog')
      .should('have.attr', 'data-cy-count', '2');
    rulesView
      .getRuleItem('Everything')
      .findDcy('ai-review-rule-backlog-run')
      .click();
    runDialog.assertWillReview(3).confirm();
    runDialog.finishStartedJob();
    rulesView
      .getRuleItem('Everything')
      .findDcy('ai-review-rule-backlog')
      .should('not.exist');
  });

  it('offers scoring for a dry run rule', () => {
    rulesView.createRule('Everything dry', 'DRY_RUN');
    runDialog.getDialog().should('have.attr', 'data-cy-dry-run', 'true');
    runDialog.assertWillReview(3).later();
  });

  it('reviews existing translations from the rule menu', () => {
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-backlog')
      .should('not.exist');
    rulesView.getRuleItem('UI rule').findDcy('ai-review-rule-menu').click();
    gcy('ai-review-rule-review-existing').click();
    runDialog.assertWillReview(1).confirm();
    runDialog.finishStartedJob();
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-stats-failed')
      .should('not.exist');
  });

  it('explains from the rule menu that there is nothing to review', () => {
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-menu')
      .click();
    gcy('ai-review-rule-review-existing').click();
    runDialog.assertNothingToReview().ok();
  });

  it('does not ask when nothing would be reviewed', () => {
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-edit')
      .click();
    rulesView.fillRuleName(' renamed');
    rulesView.saveRule();
    waitForGlobalLoading();
    rulesView.getRuleItem('Legal dry run renamed').should('be.visible');
    gcy('ai-review-run-dialog').should('not.exist');
  });
});
