import { aiReviewTestData } from '../../common/apiCalls/testData/testData';
import {
  getReviewPrompts,
  getRule,
  setupAiReviewTestData,
  visitAiReviewTranslations,
} from '../../common/aiReview';
import { E2TranslationsView } from '../../compounds/E2TranslationsView';
import { gcy } from '../../common/shared';

const translationsView = new E2TranslationsView();

describe('AI review playground', () => {
  let projectId: number;

  beforeEach(() => {
    setupAiReviewTestData().then((context) => {
      projectId = context.projectId;
    });
  });

  afterEach(() => {
    aiReviewTestData.clean();
  });

  it('switches to review mode', () => {
    visitAiReviewTranslations(projectId, 'aiPlayground=1');
    gcy('ai-prompt-type-switch-translate').should(
      'have.attr',
      'aria-pressed',
      'true'
    );
    gcy('ai-prompt-model-label').should('be.visible');
    gcy('ai-prompt-review-criteria').should('not.exist');

    gcy('ai-prompt-type-switch-review').click();
    gcy('ai-prompt-type-switch-review').should(
      'have.attr',
      'aria-pressed',
      'true'
    );
    gcy('ai-prompt-model-label').should('be.visible');
    gcy('ai-prompt-provider-select').should('be.visible');
    gcy('prompt-basic-option').should('have.length.at.least', 1);
    gcy('ai-prompt-review-criteria').should('be.visible');
    cy.url().should('include', 'aiPlaygroundType=review');

    gcy('ai-prompt-type-switch-translate').click();
    gcy('ai-prompt-review-criteria').should('not.exist');
  });

  it('previews review result in the translation row', () => {
    visitAiReviewTranslations(
      projectId,
      'aiPlayground=1&aiPlaygroundType=review'
    );
    gcy('ai-prompt-review-criteria-input').type('Use informal tone.');
    translationsView.getKeyLanguageCell('plain-key', 'de').click();
    gcy('ai-prompt-preview-button').click();

    translationsView
      .getKeyLanguageCell('plain-key', 'de')
      .findDcy('ai-review-playground-preview-score')
      .should('be.visible')
      .should('have.attr', 'data-cy-score', '100');
    gcy('ai-review-playground-preview').should('have.length', 1);
  });

  it('saves new review prompt', () => {
    visitAiReviewTranslations(
      projectId,
      'aiPlayground=1&aiPlaygroundType=review'
    );
    gcy('ai-prompt-review-criteria-input').type(
      'Product names stay in English.'
    );
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-use-as-default').should('not.exist');
    gcy('ai-prompt-save-as-field-name').find('input').type('Informal review');
    gcy('ai-prompt-save-dialog-save').click();
    gcy('ai-prompt-save-dialog-save').should('not.exist');

    getReviewPrompts(projectId).then((prompts) => {
      const created = prompts.find((p) => p.name === 'Informal review');
      expect(created?.type).to.eq('REVIEW');
      expect(created?.reviewCriteria).to.eq('Product names stay in English.');
    });
  });

  it('warns that saving a prompt used by active rule switches it to dry run', () => {
    getReviewPrompts(projectId).then((prompts) => {
      const promptId = prompts.find((p) => p.name === 'Strict review')!.id;
      visitAiReviewTranslations(
        projectId,
        `aiPlayground=1&aiPlaygroundType=review&prompt=${promptId}`
      );
    });
    gcy('ai-prompt-review-criteria-input')
      .should('have.value', 'Be strict about tone.')
      .type(' Check punctuation.');
    gcy('ai-prompt-save-button').click();
    gcy('ai-prompt-save-dry-run-warning').should('be.visible');
    gcy('global-confirmation-confirm').click();
    gcy('ai-prompt-save-dry-run-warning').should('not.exist');
    gcy('ai-prompt-save-button').should('be.disabled');

    getRule(projectId, 'UI rule').its('mode').should('eq', 'DRY_RUN');
    getReviewPrompts(projectId).then((prompts) => {
      expect(
        prompts.find((p) => p.name === 'Strict review')?.reviewCriteria
      ).to.eq('Be strict about tone. Check punctuation.');
    });
  });
});
