import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import {
  getBasicOption,
  getPromptEditor,
  openBasicAiPrompt,
  selectProvider,
  toggleBasicOption,
} from '../../common/prompt';
import { assertMessage, gcy } from '../../common/shared';
import { visitTranslations } from '../../common/translations';

describe('prompt saving and editing', () => {
  beforeEach(() => {
    prompt.clean();
    prompt
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        login(
          data.users.find((u) =>
            [u.username, u.name].includes('owner@organization.com')
          )?.username
        );
        visitTranslations(
          data.projects.find((p) => p.name === 'Prompt project').id
        );
      });
  });

  it('creates new prompt', () => {
    openBasicAiPrompt();
    selectProvider('organization-provider');
    toggleBasicOption('KEY_NAME');
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('New prompt');
    gcy('ai-prompt-save-dialog-save').click();
    assertMessage('Prompt created');
    gcy('ai-prompt-name').should('contain', 'New prompt');
  });

  it('can save prompt as new prompt', () => {
    openBasicAiPrompt();
    gcy('ai-prompt-open-existing-prompt-select').click();
    gcy('ai-prompt-open-existing-prompt-item')
      .contains('Custom prompt')
      .click();
    gcy('ai-prompt-save-more-button').click();
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('Prompt created from custom');
    gcy('ai-prompt-save-dialog-save').click();
    assertMessage('Prompt created');
    gcy('ai-prompt-name').should('contain', 'Prompt created from custom');
  });

  it('loads existing prompt', () => {
    openBasicAiPrompt();
    gcy('ai-prompt-open-existing-prompt-select').click();
    gcy('ai-prompt-open-existing-prompt-item')
      .contains('Custom prompt')
      .click();
    gcy('ai-prompt-provider-select').should('contain', 'organization-provider');
    getPromptEditor().should('contain', 'Test prompt');
    gcy('ai-prompt-name').should('contain', 'Custom prompt');
  });

  it('can rename prompt', () => {
    openBasicAiPrompt();
    gcy('ai-prompt-open-existing-prompt-select').click();
    gcy('ai-prompt-open-existing-prompt-item')
      .contains('Custom prompt')
      .click();
    gcy('ai-prompt-provider-rename-button').click();
    gcy('ai-prompt-rename-name-field').clear().type('Renamed prompt');
    gcy('ai-prompt-rename-save').click();
    waitForGlobalLoading();
    gcy('ai-prompt-name').should('contain', 'Renamed prompt');
  });

  it('creates new prompt and uses as default mt provider', () => {
    openBasicAiPrompt();
    selectProvider('organization-provider');
    toggleBasicOption('KEY_NAME');
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('New prompt');
    gcy('ai-prompt-save-as-field-use-as-default').click();
    gcy('ai-prompt-save-dialog-save').click();
    assertMessage('Prompt created');
    gcy('ai-prompt-playground-close').click();
    gcy('translation-tools-machine-translation-item-prompt').should(
      'contain',
      'English translation 1 translated with PROMPT from en to cs'
    );
  });

  it('saves basic prompt with options', () => {
    openBasicAiPrompt();
    toggleBasicOption('SCREENSHOT');
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('New prompt');
    gcy('ai-prompt-save-as-field-use-as-default').click();
    gcy('ai-prompt-save-dialog-save').click();
    assertMessage('Prompt created');
    getBasicOption('SCREENSHOT').find('input').should('not.be.checked');
  });
});
