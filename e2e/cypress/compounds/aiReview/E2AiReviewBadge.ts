import { gcy, gcyAdvanced } from '../../common/shared';
import { getTranslationCell } from '../../common/translations';

export class E2AiReviewBadge {
  constructor(
    private readonly key: string,
    private readonly languageTag = 'de'
  ) {}

  assertVisible(status: string) {
    getTranslationCell(this.key, this.languageTag).within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status }).should('be.visible');
    });
    return this;
  }

  assertAbsent() {
    getTranslationCell(this.key, this.languageTag).within(() => {
      gcy('ai-review-badge').should('not.exist');
    });
    return this;
  }

  openPopover(status: string, options?: Partial<Cypress.ClickOptions>) {
    getTranslationCell(this.key, this.languageTag).within(() => {
      gcyAdvanced({ value: 'ai-review-badge', status }).click(options);
    });
    gcy('ai-review-popover').should('be.visible');
    return this;
  }
}
