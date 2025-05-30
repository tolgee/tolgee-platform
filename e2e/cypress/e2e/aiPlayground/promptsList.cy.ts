import { login } from '../../common/apiCalls/common';
import { prompt } from '../../common/apiCalls/testData/testData';
import { waitForGlobalLoading } from '../../common/loading';
import { getPromptEditor, visitAiSettings } from '../../common/prompt';
import {
  assertMessage,
  confirmStandard,
  gcy,
  gcyAdvanced,
} from '../../common/shared';

describe('ai prompts list', () => {
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
        visitAiSettings(
          data.projects.find((p) => p.name === 'Prompt project').id
        );
      });
  });

  it('loads existing prompt from prompts list', () => {
    gcy('ai-prompt-item-name')
      .contains('Custom prompt')
      .should('exist')
      .click();
    waitForGlobalLoading();
    gcy('ai-prompt-provider-select').should('contain', 'organization-provider');
    getPromptEditor().should('contain', 'Test prompt');
    gcy('ai-prompt-name').should('contain', 'Custom prompt');
  });

  it('deletes custom prompt', () => {
    gcyAdvanced({
      value: 'ai-prompt-item-menu',
      name: 'Custom prompt',
    }).click();
    gcy('ai-prompts-menu-item-delete').click();
    confirmStandard();
    assertMessage('Prompt deleted');
  });

  it('renames custom prompt', () => {
    gcyAdvanced({
      value: 'ai-prompt-item-menu',
      name: 'Custom prompt',
    }).click();
    gcy('ai-prompts-menu-item-rename').click();
    gcy('ai-prompt-rename-name-field').clear().type('Renamed prompt');
    gcy('ai-prompt-rename-save').click();
    waitForGlobalLoading();
    gcy('ai-prompt-item-name').should('contain', 'Renamed prompt');
  });

  it('opens default prompt when creating new prompt', () => {
    gcy('ai-prompts-add-prompt').click();
    gcy('ai-prompt-name').should('contain', 'Default prompt');
    gcy('ai-prompt-provider-select').should('contain', 'server-provider');
  });
});
