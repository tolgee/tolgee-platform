import { gcy, gcyAdvanced } from '../../common/shared';
import { getTranslationCell } from '../../common/translations';

type BadgeOptions = {
  timeout?: number;
  lastFailed?: boolean;
  queued?: boolean;
};

export class E2AiReviewBadge {
  constructor(
    private readonly key: string,
    private readonly languageTag = 'de'
  ) {}

  assertVisible(status: string, options?: BadgeOptions) {
    this.withBadge(status, options, (badge) => badge.should('be.visible'));
    return this;
  }

  assertExists(status: string, options?: BadgeOptions) {
    this.withBadge(status, options, (badge) => badge.should('exist'));
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

  private withBadge(
    status: string,
    options: BadgeOptions | undefined,
    fn: (badge: Cypress.Chainable<JQuery>) => void
  ) {
    getTranslationCell(this.key, this.languageTag).within(() => {
      const selector: { value: 'ai-review-badge'; [key: string]: string } = {
        value: 'ai-review-badge',
        status,
      };
      if (options?.lastFailed !== undefined) {
        selector['last-failed'] = String(options.lastFailed);
      }
      if (options?.queued !== undefined) {
        selector['queued'] = String(options.queued);
      }
      fn(
        gcyAdvanced(
          selector,
          options?.timeout !== undefined
            ? { timeout: options.timeout }
            : undefined
        )
      );
    });
  }
}
