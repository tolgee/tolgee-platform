import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import {
  getPromptEditor,
  openBasicAiPrompt,
  selectProvider,
  toggleBasicOption,
} from '../../common/prompt';
import { assertMessage, gcy } from '../../common/shared';
import { visitTranslations } from '../../common/translations';

describe('basic prompt', () => {
  beforeEach(() => {
    prompt.clean();
    prompt
      .generateStandard()
      .then((r) => r.body)
      .then((data) => {
        login(
          data.users.find((u) =>
            [u.username, u.name].includes('projectEditor@organization.com')
          )?.username
        );
        visitTranslations(
          data.projects.find((p) => p.name === 'Prompt project').id
        );
      });
  });

  it('saves custom prompt', () => {
    openBasicAiPrompt();
    selectProvider('organization-provider');
    toggleBasicOption('KEY_NAME');
    gcy('ai-prompt-save-as-new-button').click();
    gcy('ai-prompt-save-as-field-name').type('Custom prompt');
    gcy('ai-prompt-save-dialog-save').click();
    assertMessage('Prompt created');
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

  it('loads existing prompt from prompts list', () => {
    gcy('project-menu-item-ai').click();
    gcy('ai-prompt-item-name')
      .contains('Custom prompt')
      .should('exist')
      .click();
    waitForGlobalLoading();
    gcy('ai-prompt-provider-select').should('contain', 'organization-provider');
    getPromptEditor().should('contain', 'Test prompt');
    gcy('ai-prompt-name').should('contain', 'Custom prompt');
  });
});
