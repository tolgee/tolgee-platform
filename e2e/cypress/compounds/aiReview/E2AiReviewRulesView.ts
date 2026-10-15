import { HOST } from '../../common/constants';
import { gcy, gcyAdvanced } from '../../common/shared';
import { components } from '../../../../webapp/src/service/apiSchema.generated';

type AiReviewRuleMode = components['schemas']['AiReviewRuleModel']['mode'];
type LanguageOverrideMode = 'default' | 'custom' | 'never';

export class E2AiReviewRulesView {
  visit(projectId: number) {
    return cy.visit(`${HOST}/projects/${projectId}/ai/review`);
  }

  getRuleItem(name: string) {
    return gcyAdvanced({ value: 'ai-review-rule-item', name });
  }

  getRuleEnabledSwitch(name: string) {
    return this.getRuleItem(name)
      .findDcy('ai-review-rule-enabled-switch')
      .find('input');
  }

  assertRuleMode(name: string, mode: AiReviewRuleMode) {
    this.getRuleItem(name)
      .findDcy('ai-review-rule-mode')
      .should('have.attr', 'data-cy-mode', mode);
  }

  openRuleDialog() {
    gcy('ai-review-add-rule').click();
    return gcy('ai-review-rule-dialog').should('be.visible');
  }

  fillRuleName(name: string) {
    gcy('ai-review-rule-name-field').find('input').type(name);
  }

  saveRule() {
    gcy('ai-review-rule-save').click();
    gcy('ai-review-rule-dialog').should('not.exist');
  }

  createRule(name: string, mode: AiReviewRuleMode, threshold = 85) {
    this.openRuleDialog();
    this.fillRuleName(name);
    this.setRuleThreshold(threshold);
    this.setRuleMode(mode);
    this.saveRule();
  }

  selectRuleScopeTag(tag: string) {
    gcy('ai-review-rule-scope-keys')
      .findDcy('translations-filter-select')
      .click();
    gcyAdvanced({ value: 'submenu-item', submenu: 'tags' }).click();
    gcyAdvanced({ value: 'filter-item', tag }).click();
    cy.get('body').type('{esc}');
    cy.get('body').type('{esc}');
  }

  setRuleThreshold(threshold: number) {
    gcy('ai-review-rule-threshold-field')
      .find('input')
      .clear()
      .type(String(threshold));
  }

  setRuleMode(mode: AiReviewRuleMode) {
    gcy('ai-review-rule-mode-select').click();
    gcyAdvanced({ value: 'ai-review-rule-mode-option', mode })
      .should('be.visible')
      .click();
  }

  setLanguageOverride(
    languageTag: string,
    mode: LanguageOverrideMode,
    threshold?: number
  ) {
    this.getLanguageOverride(languageTag)
      .findDcy('ai-review-rule-language-override-mode')
      .click();
    gcyAdvanced({
      value: 'ai-review-rule-language-override-option',
      'override-mode': mode,
    })
      .should('be.visible')
      .click();
    if (threshold !== undefined) {
      this.getLanguageOverride(languageTag)
        .findDcy('ai-review-rule-language-override-threshold')
        .find('input')
        .clear()
        .type(String(threshold));
    }
  }

  getLanguageOverride(languageTag: string) {
    return gcyAdvanced({
      value: 'ai-review-rule-language-override',
      language: languageTag,
    });
  }
}
