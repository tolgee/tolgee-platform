import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import { loadPrompt, openPlayground } from '../../common/aiPlayground';
import { visitAiSettings } from '../../common/prompt';
import {
  restoreAiReviewFeature,
  visitAiReviewRules,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { HOST } from '../../common/constants';

describe('AI review prompts in the playground', () => {
  let projectId: number;

  beforeEach(() => {
    aiReviewTestData.clean();
    aiReviewTestData.generateStandard().then((res) => {
      projectId = getProjectByNameFromTestData(
        res.body,
        'ai_review_project'
      )!.id;
    });
    login('test_username');
  });

  afterEach(() => {
    restoreAiReviewFeature();
    aiReviewTestData.clean();
  });

  it('type switch is hidden without the feature', () => {
    withoutAiReviewFeature();
    openPlayground(projectId, 'key_untagged');
    gcy('ai-prompt-type-switch').should('not.exist');
  });

  it('creates and saves a review prompt with criteria', () => {
    openPlayground(projectId, 'key_untagged');
    gcy('ai-prompt-type-review').click();
    gcy('ai-prompt-review-criteria').type('Use a formal tone.');
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('My review prompt');
    gcy('ai-prompt-save-dialog-save').click();
    gcy('ai-prompt-name').should('contain', 'My review prompt');
    gcy('ai-prompt-type-review')
      .should('have.attr', 'aria-pressed', 'true')
      .should('be.disabled');
  });

  it('loading a saved review prompt locks the switch', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
    gcy('ai-prompt-review-criteria')
      .find('textarea')
      .first()
      .should('have.value', 'Use informal language.');
    gcy('ai-prompt-type-review')
      .should('have.attr', 'aria-pressed', 'true')
      .should('be.disabled');
    gcy('ai-prompt-type-translate').should('be.disabled');
  });

  it('running a review prompt shows the parsed score and issue summary', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
    gcy('ai-prompt-preview-button').click();
    gcy('ai-prompt-result-review', { timeout: 60_000 }).should('be.visible');
    gcy('ai-prompt-result-review').within(() => {
      gcyAdvanced({
        value: 'ai-playground-preview-score',
        score: '100',
      }).should('be.visible');
      gcy('ai-playground-preview-no-issues').should('be.visible');
    });
  });

  it('model select sits under the type switch', () => {
    openPlayground(projectId, 'key_untagged');
    gcy('ai-prompt-type-switch').then(($switch) => {
      const switchBottom = $switch[0].getBoundingClientRect().bottom;
      gcy('ai-prompt-provider-select').then(($select) => {
        expect($select[0].getBoundingClientRect().top).to.be.at.least(
          switchBottom
        );
      });
    });
  });

  it('saving a changed prompt switches the active rule to Dry run', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
    gcy('ai-prompt-review-criteria').find('textarea').first().type(' More.');
    gcy('ai-prompt-save-button').click();
    gcy('ai-review-prompt-save-warning').should('be.visible');
    cy.gcy('global-confirmation-confirm').click();
    visitAiReviewRules(projectId);
    gcyAdvanced({ value: 'ai-review-rule-item', name: 'UI rule' }).within(
      () => {
        gcyAdvanced({ value: 'ai-review-rule-mode', mode: 'DRY_RUN' }).should(
          'exist'
        );
      }
    );
  });

  it('prompts list shows the review chip', () => {
    visitAiSettings(projectId);
    gcy('ai-prompt-item-name').should('have.length', 2);
    gcy('ai-prompt-review-chip').should('have.length', 1);
  });

  it('deleting a prompt used by an active rule warns', () => {
    visitAiSettings(projectId);
    gcyAdvanced({ value: 'ai-prompt-item-menu', name: 'Brand review' }).click();
    gcy('ai-prompts-menu-item-delete').click();
    gcy('ai-review-prompt-delete-warning').should('be.visible');
    cy.gcy('global-confirmation-cancel').click();
  });

  it('MT language settings list only translate prompts', () => {
    cy.visit(`${HOST}/projects/${projectId}/languages/mt`);
    cy.gcy('machine-translations-settings-language-options').first().click();
    gcy('mt-language-dialog-prompt-select').click();
    gcyAdvanced({
      value: 'mt-language-dialog-prompt-item',
      name: 'Plain translate',
    }).should('exist');
    gcyAdvanced({
      value: 'mt-language-dialog-prompt-item',
      name: 'Brand review',
    }).should('not.exist');
  });
});
