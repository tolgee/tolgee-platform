import {
  aiReviewTestData,
  getProjectByNameFromTestData,
} from '../../common/apiCalls/testData/testData';
import { login } from '../../common/apiCalls/common';
import { gcy, gcyAdvanced } from '../../common/shared';
import {
  loadPrompt,
  openPlayground,
  visitAiSettings,
} from '../../common/prompt';
import {
  restoreAiReviewFeature,
  withoutAiReviewFeature,
} from '../../common/aiReview';
import { E2AiReviewRulesView } from '../../compounds/aiReview/E2AiReviewRulesView';
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

  it('creates and saves a review prompt', () => {
    openPlayground(projectId, 'key_untagged');
    gcy('ai-prompt-type-review').click();
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('My review prompt');
    gcy('ai-prompt-save-dialog-save').click();
    gcy('ai-prompt-name').should('contain', 'My review prompt');
    gcy('ai-prompt-type-review')
      .should('have.attr', 'aria-pressed', 'true')
      .should('be.disabled');
  });

  it('switching the type warns when a draft has unsaved edits', () => {
    openPlayground(projectId, 'key_untagged');
    gcyAdvanced({ value: 'prompt-basic-option', option: 'KEY_NAME' }).click();
    gcy('ai-prompt-type-review').click();
    gcy('global-confirmation-cancel').click();
    gcy('ai-prompt-type-translate').should('have.attr', 'aria-pressed', 'true');
    gcy('ai-prompt-type-review').click();
    gcy('global-confirmation-confirm').click();
    gcy('ai-prompt-type-review').should('have.attr', 'aria-pressed', 'true');
  });

  it('loading a saved review prompt locks the switch', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
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

  it('saving a changed prompt switches the active rule to Score only', () => {
    openPlayground(projectId, 'key_untagged');
    loadPrompt('Brand review');
    gcyAdvanced({ value: 'prompt-basic-option', option: 'KEY_NAME' }).click();
    gcy('ai-prompt-save-button').click();
    gcy('ai-review-prompt-save-warning').should('be.visible');
    cy.gcy('global-confirmation-confirm').click();
    const rulesView = new E2AiReviewRulesView();
    rulesView.visit(projectId);
    rulesView.getRule('UI rule').within(() => {
      rulesView.getRuleMode('SCORE_ONLY').should('exist');
    });
  });

  it('prompts list shows a type chip per prompt', () => {
    visitAiSettings(projectId);
    gcy('ai-prompt-item-name').should('have.length', 2);
    gcy('ai-prompt-review-chip').should('have.length', 1);
    gcy('ai-prompt-translate-chip').should('have.length', 1);
  });

  it('prompts list shows no translate chip without the feature', () => {
    withoutAiReviewFeature();
    visitAiSettings(projectId);
    gcy('ai-prompt-item-name').should('have.length', 1);
    gcy('ai-prompt-translate-chip').should('not.exist');
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
