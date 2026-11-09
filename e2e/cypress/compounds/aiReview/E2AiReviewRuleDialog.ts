import { gcy, gcyAdvanced } from '../../common/shared';

export class E2AiReviewRuleDialog {
  pickThresholdPreset(option: string) {
    gcyAdvanced({ value: 'ai-review-rule-form-threshold-preset', option })
      .scrollIntoView()
      .click();
    return this;
  }

  // Synthetic key events don't move a native range input, so set its value directly.
  setThresholdSlider(value: number) {
    gcy('ai-review-rule-form-threshold-slider')
      .find('input')
      .then(($input) => {
        const input = $input[0] as HTMLInputElement;
        const setter = Object.getOwnPropertyDescriptor(
          Object.getPrototypeOf(input),
          'value'
        )?.set;
        setter?.call(input, String(value));
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
      });
    return this;
  }

  getScopeOption(option: string) {
    return gcyAdvanced({ value: 'ai-review-rule-form-scope-option', option });
  }

  save() {
    gcy('ai-review-rule-form-save').click();
  }
}
