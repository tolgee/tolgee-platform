import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  getRule,
  getRules,
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
import { E2AiReviewRunDialog } from '../../compounds/aiReview/E2AiReviewRunDialog';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { waitForGlobalLoading } from '../../common/loading';
import { gcy } from '../../common/shared';

const rulesView = new E2AiReviewRulesView();
const translationsView = new E2TranslationsView();
const runDialog = new E2AiReviewRunDialog();

describe('AI review rules', () => {
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

  it('lists seeded rules', () => {
    gcy('ai-review-rules-list').should('be.visible');
    gcy('ai-review-rule-item').should('have.length', 2);
    rulesView.assertRuleMode('UI rule', 'ACTIVE');
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-title')
      .should('be.visible');
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-scope')
      .should('exist');
    rulesView.assertRuleMode('Legal dry run', 'DRY_RUN');
  });

  it('creates rule with scope, threshold and language overrides', () => {
    rulesView.openRuleDialog();
    rulesView.fillRuleName('Checkout rule');
    rulesView.selectRuleScopeTag('ui');
    rulesView.setRuleThreshold(90);
    gcy('ai-review-rule-language-overrides-toggle').click();
    rulesView.setLanguageOverride('cs', 'never');
    rulesView.setLanguageOverride('de', 'custom', 95);
    rulesView.setRuleMode('ACTIVE');
    rulesView.saveRule();
    runDialog.assertWillReview(1).later();
    waitForGlobalLoading();
    gcy('ai-review-rule-item').should('have.length', 3);
    rulesView.assertRuleMode('Checkout rule', 'ACTIVE');
    rulesView
      .getRuleItem('Checkout rule')
      .findDcy('ai-review-rule-overrides')
      .should('be.visible');

    getRule(projectId, 'Checkout rule').then((rule) => {
      expect(rule.threshold).to.eq(90);
      expect(rule.mode).to.eq('ACTIVE');
      expect(rule.enabled).to.eq(true);
      expect(rule.tagIn).to.deep.eq(['ui']);
      expect(rule.tagNotIn).to.deep.eq([]);
      expect(rule.languageThresholds).to.have.length(2);
      expect(rule.languageThresholds.filter((o) => o.never)).to.have.length(1);
      expect(
        rule.languageThresholds.filter((o) => !o.never).map((o) => o.threshold)
      ).to.deep.eq([95]);
    });
  });

  it('edits rule', () => {
    rulesView.getRuleItem('UI rule').findDcy('ai-review-rule-edit').click();
    gcy('ai-review-rule-dialog').should('be.visible');
    gcy('ai-review-rule-threshold-field')
      .find('input')
      .should('have.value', '80');
    rulesView.setRuleThreshold(70);
    rulesView.setRuleMode('DRY_RUN');
    rulesView.saveRule();
    runDialog.assertWillReview(1).later();
    rulesView.assertRuleMode('UI rule', 'DRY_RUN');
    getRule(projectId, 'UI rule').then((rule) => {
      expect(rule.threshold).to.eq(70);
      expect(rule.mode).to.eq('DRY_RUN');
      expect(rule.tagIn).to.deep.eq(['ui']);
    });
  });

  it('scopes rule to branches with wildcard patterns', () => {
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-scope')
      .should('contain', 'draft/*');
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-edit')
      .click();
    rulesView.getBranchInput('exclude').should('contain', 'draft/*');
    rulesView.addBranchPattern('include', 'main');
    rulesView.addBranchPattern('include', 'feature/*');
    rulesView.saveRule();
    runDialog.later();
    getRule(projectId, 'Legal dry run').then((rule) => {
      expect(rule.branchIn).to.deep.eq(['main', 'feature/*']);
      expect(rule.branchNotIn).to.deep.eq(['draft/*']);
    });
  });

  it('disables and enables rule', () => {
    rulesView.getRuleEnabledSwitch('UI rule').should('be.checked').click();
    rulesView.getRuleEnabledSwitch('UI rule').should('not.be.checked');
    getRule(projectId, 'UI rule').its('enabled').should('eq', false);

    rulesView.getRuleEnabledSwitch('UI rule').click();
    runDialog.assertWillReview(1).later();
    rulesView.getRuleEnabledSwitch('UI rule').should('be.checked');
    getRule(projectId, 'UI rule').its('enabled').should('eq', true);
  });

  it('deletes rule', () => {
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-menu')
      .click();
    gcy('ai-review-rule-delete').click();
    gcy('global-confirmation-confirm').click();

    rulesView.getRuleItem('Legal dry run').should('not.exist');
    gcy('ai-review-rule-item').should('have.length', 1);
    getRules(projectId).then((rules) => {
      expect(rules.map((rule) => rule.name)).to.deep.eq(['UI rule']);
    });
  });

  it('retries failed reviews of a rule', () => {
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-stats-failed')
      .should('have.attr', 'data-cy-count', '1');
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-stats-failed')
      .should('not.exist');

    rulesView.getRuleItem('UI rule').findDcy('ai-review-rule-menu').click();
    gcy('ai-review-rule-retry-failed').click();
    gcy('notistack-snackbar').should('be.visible');
    rulesView
      .getRuleItem('UI rule')
      .findDcy('ai-review-rule-stats-failed')
      .should('not.exist');

    visitAiReviewTranslations(projectId);
    translationsView.assertAiReviewBadge('ui-key', 'fr', 'AUTO_REVIEWED', 100);
    translationsView.assertTranslationState('ui-key', 'fr', 'REVIEWED');
    getRule(projectId, 'UI rule').its('stats.failed').should('eq', 0);
  });

  it('hides retry of failed reviews when the rule has none', () => {
    rulesView
      .getRuleItem('Legal dry run')
      .findDcy('ai-review-rule-menu')
      .click();
    gcy('ai-review-rule-delete').should('be.visible');
    gcy('ai-review-rule-retry-failed').should('not.exist');
  });
});
