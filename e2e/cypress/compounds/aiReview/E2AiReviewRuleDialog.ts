import { gcy, gcyAdvanced } from '../../common/shared';

export class E2AiReviewRuleDialog {
  getNameInput() {
    return gcy('ai-review-rule-form-name').find('input');
  }

  typeName(name: string) {
    this.getNameInput().clear().type(name);
    return this;
  }

  setThreshold(value: number) {
    this.getThresholdInput().clear().type(String(value)).blur();
    return this;
  }

  getThresholdInput() {
    return gcy('ai-review-rule-form-threshold-input');
  }

  selectActiveMode() {
    gcy('ai-review-rule-form-mode-active').click();
    return this;
  }

  selectScoreOnlyMode() {
    gcy('ai-review-rule-form-mode-score-only').click();
    return this;
  }

  getSaveButton() {
    return gcy('ai-review-rule-form-save');
  }

  getScopeOption(option: string) {
    return gcyAdvanced({ value: 'ai-review-rule-form-scope-option', option });
  }

  save() {
    this.getSaveButton().click();
  }
}
